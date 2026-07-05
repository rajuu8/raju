package com.pcontrol.parent.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pcontrol.parent.R
import com.pcontrol.parent.network.ApiClient
import com.pcontrol.parent.network.AuthManager
import org.json.JSONObject

/**
 * Parent generates a pairing code here, then reads it out / shows it
 * to enter into the child app during its setup.
 */
class PairDeviceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pair_device)

        val deviceNameInput = findViewById<EditText>(R.id.deviceNameInput)
        val generateButton = findViewById<Button>(R.id.generateButton)
        val codeText = findViewById<TextView>(R.id.pairingCodeText)

        generateButton.setOnClickListener {
            val token = AuthManager.getToken(this) ?: return@setOnClickListener
            val name = deviceNameInput.text.toString().ifEmpty { "New Device" }

            val json = JSONObject().apply { put("deviceName", name) }
            ApiClient.post("/api/device/create-pairing", json, token) { success, response ->
                runOnUiThread {
                    if (success && response != null) {
                        val obj = JSONObject(response)
                        codeText.text = "Pairing Code: ${obj.getString("pairingCode")}\n\nEnter this in the child app."
                    } else {
                        Toast.makeText(this, "Failed to generate code", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
