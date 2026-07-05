package com.pcontrol.child.ui

import android.Manifest
import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pcontrol.child.R
import com.pcontrol.child.network.ApiClient
import com.pcontrol.child.network.AppUpdateChecker
import com.pcontrol.child.network.DeviceConfig
import com.pcontrol.child.services.MonitoringService
import org.json.JSONObject
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private val REQUEST_LOCATION = 100
    private val REQUEST_BACKGROUND_LOCATION = 101
    private val REQUEST_CALL_LOG = 102
    private val REQUEST_NOTIFICATIONS = 103

    private lateinit var pairingCodeInput: EditText
    private var monitoringStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AppUpdateChecker.checkForUpdate(this)

        pairingCodeInput = findViewById(R.id.pairingCodeInput)
        val pairButton = findViewById<Button>(R.id.pairButton)
        val statusText = findViewById<TextView>(R.id.statusText)

        if (DeviceConfig.isPaired(this)) {
            pairingCodeInput.visibility = android.view.View.GONE
            statusText.text = "This device is linked. Tap below to check/grant permissions."
            pairButton.text = "Check Permissions"
        }

        pairButton.setOnClickListener {
            if (DeviceConfig.isPaired(this)) {
                requestAllPermissions()
            } else {
                val code = pairingCodeInput.text.toString().trim()
                if (code.isEmpty()) {
                    Toast.makeText(this, "Enter the pairing code from parent app", Toast.LENGTH_SHORT).show()
                } else {
                    pairDevice(code)
                }
            }
        }
    }

    private fun pairDevice(code: String) {
        val json = JSONObject().apply { put("pairingCode", code) }
        ApiClient.post("/api/device/pair", json) { success, response ->
            runOnUiThread {
                if (success && response != null) {
                    try {
                        val obj = JSONObject(response)
                        DeviceConfig.saveDeviceId(this, obj.getString("deviceId"))
                        Toast.makeText(this, "Paired successfully!", Toast.LENGTH_SHORT).show()
                        requestAllPermissions()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Unexpected response, try again", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Invalid code, try again", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun requestAllPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS
            )
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION
            )
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), REQUEST_BACKGROUND_LOCATION
            )
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.READ_CALL_LOG), REQUEST_CALL_LOG
            )
            return
        }

        if (!hasUsageStatsPermission()) {
            Toast.makeText(this, "Please enable Usage Access on the next screen", Toast.LENGTH_LONG).show()
            try {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open settings, please enable manually", Toast.LENGTH_LONG).show()
            }
            return
        }

        startMonitoring()
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun startMonitoring() {
        if (monitoringStarted) return
        try {
            val intent = Intent(this, MonitoringService::class.java)
            ContextCompat.startForegroundService(this, intent)
            monitoringStarted = true
            Toast.makeText(this, "Monitoring is now active", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not start monitoring: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        requestAllPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (!monitoringStarted && DeviceConfig.isPaired(this) && hasUsageStatsPermission()) {
            startMonitoring()
        }
    }
}
