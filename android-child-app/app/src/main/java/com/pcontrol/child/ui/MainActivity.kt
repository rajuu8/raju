package com.pcontrol.child.ui

import android.Manifest
import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pcontrol.child.R
import com.pcontrol.child.network.ApiClient
import com.pcontrol.child.network.DeviceConfig
import com.pcontrol.child.services.MonitoringService
import org.json.JSONObject
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

/**
 * First screen shown to the child/device holder.
 * - Explains clearly that this is a parental monitoring app (transparency).
 * - Takes the pairing code from the parent app.
 * - Requests all permissions one by one, with system dialogs (child sees each request).
 * - Starts MonitoringService once paired + permissions granted.
 */
class MainActivity : AppCompatActivity() {

    private val REQUEST_LOCATION = 100
    private val REQUEST_BACKGROUND_LOCATION = 101
    private val REQUEST_CALL_LOG = 102

    private lateinit var pairingCodeInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pairingCodeInput = findViewById(R.id.pairingCodeInput)
        val pairButton = findViewById<Button>(R.id.pairButton)
        val statusText = findViewById<TextView>(R.id.statusText)

        if (DeviceConfig.isPaired(this)) {
            statusText.text = "This device is already linked to a parent account."
            pairButton.text = "Re-check Permissions"
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
                    val obj = JSONObject(response)
                    DeviceConfig.saveDeviceId(this, obj.getString("deviceId"))
                    Toast.makeText(this, "Paired successfully!", Toast.LENGTH_SHORT).show()
                    requestAllPermissions()
                } else {
                    Toast.makeText(this, "Invalid code, try again", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun requestAllPermissions() {
        // Step 1: Foreground location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION
            )
            return
        }

        // Step 2: Background location (Android 10+, separate prompt required)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), REQUEST_BACKGROUND_LOCATION
            )
            return
        }

        // Step 3: Call log (optional feature)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.READ_CALL_LOG), REQUEST_CALL_LOG
            )
            return
        }

        // Step 4: Usage access (special setting screen, not a normal permission dialog)
        if (!hasUsageStatsPermission()) {
            Toast.makeText(this, "Please enable Usage Access on the next screen", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
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
        val intent = Intent(this, MonitoringService::class.java)
        ContextCompat.startForegroundService(this, intent)
        Toast.makeText(this, "Monitoring is now active", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Whatever the result, continue the permission chain (skips already-denied ones gracefully)
        requestAllPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (DeviceConfig.isPaired(this) && hasUsageStatsPermission()) {
            startMonitoring()
        }
    }
}
