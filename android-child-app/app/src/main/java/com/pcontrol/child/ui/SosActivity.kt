package com.pcontrol.child.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.LocationServices
import com.pcontrol.child.R
import com.pcontrol.child.network.ApiClient
import com.pcontrol.child.network.DeviceConfig
import org.json.JSONObject
import android.widget.Button
import android.widget.Toast

/**
 * Simple SOS screen - child taps a big button, it immediately sends
 * an alert + current location to the parent app.
 */
class SosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sos)

        findViewById<Button>(R.id.sosButton).setOnClickListener {
            sendSos()
        }
    }

    private fun sendSos() {
        val deviceId = DeviceConfig.getDeviceId(this) ?: return

        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            fusedClient.lastLocation.addOnSuccessListener { location ->
                val meta = JSONObject()
                if (location != null) {
                    meta.put("latitude", location.latitude)
                    meta.put("longitude", location.longitude)
                }

                val json = JSONObject().apply {
                    put("type", "SOS")
                    put("message", "Emergency button pressed")
                    put("meta", meta)
                }

                ApiClient.post("/api/alerts/$deviceId", json) { success, _ ->
                    runOnUiThread {
                        val msg = if (success) "SOS sent to parent" else "Failed to send, try again"
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Location permission needed for SOS", Toast.LENGTH_SHORT).show()
        }
    }
}
