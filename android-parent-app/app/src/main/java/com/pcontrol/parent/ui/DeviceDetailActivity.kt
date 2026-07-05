package com.pcontrol.parent.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pcontrol.parent.R
import com.pcontrol.parent.network.ApiClient
import com.pcontrol.parent.network.AuthManager
import org.json.JSONObject

class DeviceDetailActivity : AppCompatActivity() {

    private lateinit var deviceId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_detail)

        deviceId = intent.getStringExtra("deviceId") ?: return
        val deviceName = intent.getStringExtra("deviceName") ?: "Device"

        findViewById<TextView>(R.id.deviceTitle).text = deviceName

        findViewById<Button>(R.id.viewLocationButton).setOnClickListener { loadLocation() }
        findViewById<Button>(R.id.requestScreenButton).setOnClickListener { requestScreenshot() }
        findViewById<Button>(R.id.viewAlertsButton).setOnClickListener { loadAlerts() }

        loadLocation()
    }

    private fun loadLocation() {
        val token = AuthManager.getToken(this) ?: return
        ApiClient.get("/api/location/$deviceId/latest", token) { success, response ->
            runOnUiThread {
                val locationText = findViewById<TextView>(R.id.locationText)
                if (success && response != null && response != "{}") {
                    val obj = JSONObject(response)
                    val lat = obj.optDouble("latitude")
                    val lng = obj.optDouble("longitude")
                    val recordedAt = obj.optString("recordedAt", "")
                    locationText.text = "Lat: $lat, Lng: $lng\nAt: $recordedAt\n\n" +
                            "Open in Maps: https://maps.google.com/?q=$lat,$lng"
                } else {
                    locationText.text = "No location data yet"
                }
            }
        }
    }

    private fun requestScreenshot() {
        val token = AuthManager.getToken(this) ?: return
        // Ask child device to push a fresh screenshot (only works while child
        // has actively started screen sharing and approved MediaProjection).
        ApiClient.post("/api/screen/$deviceId/request", JSONObject(), token) { _, _ -> }

        // Give it a moment then fetch the latest snapshot
        android.os.Handler(mainLooper).postDelayed({ loadScreenshot() }, 2000)
        Toast.makeText(this, "Requesting screen snapshot...", Toast.LENGTH_SHORT).show()
    }

    private fun loadScreenshot() {
        val token = AuthManager.getToken(this) ?: return
        ApiClient.get("/api/screen/$deviceId/latest", token) { success, response ->
            runOnUiThread {
                val imageView = findViewById<ImageView>(R.id.screenImageView)
                if (success && response != null) {
                    val obj = JSONObject(response)
                    val base64 = obj.optString("imageBase64", "")
                    if (base64.isNotEmpty()) {
                        val bytes = Base64.decode(base64, Base64.NO_WRAP)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        imageView.setImageBitmap(bitmap)
                    } else {
                        Toast.makeText(this, "No screenshot available - child hasn't shared screen", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun loadAlerts() {
        val token = AuthManager.getToken(this) ?: return
        ApiClient.get("/api/alerts/$deviceId", token) { success, response ->
            runOnUiThread {
                val alertsText = findViewById<TextView>(R.id.alertsText)
                if (success && response != null) {
                    val arr = org.json.JSONArray(response)
                    val sb = StringBuilder()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        sb.append("[${obj.optString("type")}] ${obj.optString("message")}\n")
                    }
                    alertsText.text = if (sb.isEmpty()) "No alerts yet" else sb.toString()
                } else {
                    alertsText.text = "Could not load alerts"
                }
            }
        }
    }
}
