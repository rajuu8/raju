package com.pcontrol.child.network

import android.content.Context
import android.content.SharedPreferences

/**
 * Stores the deviceId (received after pairing with parent's pairing code)
 * locally so the app knows which device record to post updates to.
 */
object DeviceConfig {
    private const val PREFS = "pcontrol_prefs"
    private const val KEY_DEVICE_ID = "device_id"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun saveDeviceId(context: Context, deviceId: String) {
        prefs(context).edit().putString(KEY_DEVICE_ID, deviceId).apply()
    }

    fun getDeviceId(context: Context): String? =
        prefs(context).getString(KEY_DEVICE_ID, null)

    fun isPaired(context: Context): Boolean = getDeviceId(context) != null
}
