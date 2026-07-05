package com.pcontrol.child.services

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.*
import com.pcontrol.child.R
import com.pcontrol.child.network.ApiClient
import com.pcontrol.child.network.DeviceConfig
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MonitoringService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    companion object {
        const val CHANNEL_ID = "monitoring_channel"
        const val NOTIFICATION_ID = 1001
        const val HEARTBEAT_INTERVAL_MS = 5 * 60 * 1000L
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
        } catch (e: Exception) {
            stopSelf()
            return
        }

        startLocationUpdates()
        startPeriodicSync()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Family Safety is active")
            .setContentText("Location & activity monitoring is running")
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Monitoring Status",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Shows when parental monitoring is active"
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startLocationUpdates() {
        val deviceId = DeviceConfig.getDeviceId(this) ?: return

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10 * 60 * 1000L
        ).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { sendLocation(deviceId, it) }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, mainLooper)
        } catch (e: SecurityException) {
        }
    }

    private fun sendLocation(deviceId: String, location: Location) {
        val json = JSONObject().apply {
            put("latitude", location.latitude)
            put("longitude", location.longitude)
            put("accuracy", location.accuracy.toDouble())
        }
        ApiClient.post("/api/location/$deviceId", json) { _, _ -> }
    }

    private fun startPeriodicSync() {
        handler.post(object : Runnable {
            override fun run() {
                sendHeartbeat()
                syncAppUsage()
                handler.postDelayed(this, HEARTBEAT_INTERVAL_MS)
            }
        })
    }

    private fun sendHeartbeat() {
        val deviceId = DeviceConfig.getDeviceId(this) ?: return
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val json = JSONObject().apply { put("batteryLevel", batteryLevel) }
        ApiClient.post("/api/device/$deviceId/heartbeat", json) { _, _ -> }

        if (batteryLevel in 1..14) {
            val alertJson = JSONObject().apply {
                put("type", "LOW_BATTERY")
                put("message", "Battery is at $batteryLevel%")
            }
            ApiClient.post("/api/alerts/$deviceId", alertJson) { _, _ -> }
        }
    }

    private fun syncAppUsage() {
        val deviceId = DeviceConfig.getDeviceId(this) ?: return
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfDay = calendar.timeInMillis
        val now = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startOfDay, now
        )

        val appsArray = JSONArray()
        val packageManager = packageManager

        stats?.filter { it.totalTimeInForeground > 0 }?.forEach { usageStat ->
            val appName = try {
                val appInfo = packageManager.getApplicationInfo(usageStat.packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                usageStat.packageName
            }

            appsArray.put(JSONObject().apply {
                put("packageName", usageStat.packageName)
                put("appName", appName)
                put("usageMinutes", usageStat.totalTimeInForeground / 1000 / 60)
            })
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val json = JSONObject().apply {
            put("date", dateFormat.format(Date()))
            put("apps", appsArray)
        }
        ApiClient.post("/api/usage/$deviceId", json) { _, _ -> }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
