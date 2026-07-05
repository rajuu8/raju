package com.pcontrol.child.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.pcontrol.child.network.DeviceConfig
import com.pcontrol.child.services.MonitoringService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (DeviceConfig.isPaired(context)) {
                val serviceIntent = Intent(context, MonitoringService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }
}
