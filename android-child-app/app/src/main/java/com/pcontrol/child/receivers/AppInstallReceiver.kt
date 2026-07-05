package com.pcontrol.child.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pcontrol.child.network.ApiClient
import com.pcontrol.child.network.DeviceConfig
import org.json.JSONObject

/**
 * Fires whenever a new app is installed on the child's device.
 * Sends an alert to the parent app so they're notified in real time.
 */
class AppInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
            val packageName = intent.data?.schemeSpecificPart ?: return
            val deviceId = DeviceConfig.getDeviceId(context) ?: return

            val appName = try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                packageName
            }

            val meta = JSONObject().apply { put("packageName", packageName) }
            val json = JSONObject().apply {
                put("type", "NEW_APP_INSTALLED")
                put("message", "New app installed: $appName")
                put("meta", meta)
            }

            ApiClient.post("/api/alerts/$deviceId", json) { _, _ -> }
        }
    }
}
