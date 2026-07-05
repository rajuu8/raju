package com.pcontrol.parent.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pcontrol.parent.R
import com.pcontrol.parent.network.ApiClient
import com.pcontrol.parent.network.AuthManager
import org.json.JSONArray

/**
 * Shows all child devices linked to this parent account.
 * Tap a device to see its location, screen, usage, and alerts.
 */
class DashboardActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val devices = mutableListOf<DeviceItem>()

    data class DeviceItem(val id: String, val name: String, val lastSeen: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        recyclerView = findViewById(R.id.deviceRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = DeviceAdapter()

        findViewById<Button>(R.id.addDeviceButton).setOnClickListener {
            startActivity(Intent(this, PairDeviceActivity::class.java))
        }

        loadDevices()
    }

    override fun onResume() {
        super.onResume()
        loadDevices()
    }

    private fun loadDevices() {
        val token = AuthManager.getToken(this) ?: return
        ApiClient.get("/api/device/list", token) { success, response ->
            runOnUiThread {
                if (success && response != null) {
                    devices.clear()
                    val arr = JSONArray(response)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        devices.add(
                            DeviceItem(
                                obj.getString("_id"),
                                obj.optString("deviceName", "Device"),
                                obj.optString("lastSeen", "")
                            )
                        )
                    }
                    recyclerView.adapter?.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Could not load devices", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    inner class DeviceAdapter : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameText: TextView = view.findViewById(R.id.deviceNameText)
            val lastSeenText: TextView = view.findViewById(R.id.deviceLastSeenText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_device, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val device = devices[position]
            holder.nameText.text = device.name
            holder.lastSeenText.text = "Last seen: ${device.lastSeen}"
            holder.itemView.setOnClickListener {
                val intent = Intent(this@DashboardActivity, DeviceDetailActivity::class.java)
                intent.putExtra("deviceId", device.id)
                intent.putExtra("deviceName", device.name)
                startActivity(intent)
            }
        }

        override fun getItemCount() = devices.size
    }
}
