package com.pcontrol.child.network

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import org.json.JSONObject

object AppUpdateChecker {

    fun checkForUpdate(context: Context) {
        ApiClient.get2("/api/version/child") { success, response ->
            if (!success || response == null) return@get2
            try {
                val obj = JSONObject(response)
                val latestVersionCode = obj.getInt("versionCode")
                val apkUrl = obj.getString("apkUrl")
                val currentVersionCode = getCurrentVersionCode(context)

                if (latestVersionCode > currentVersionCode) {
                    showUpdateDialog(context, apkUrl, obj.optString("releaseNotes", ""))
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun getCurrentVersionCode(context: Context): Int {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.longVersionCode.toInt()
        } catch (e: PackageManager.NameNotFoundException) {
            0
        }
    }

    private fun showUpdateDialog(context: Context, apkUrl: String, notes: String) {
        AlertDialog.Builder(context)
            .setTitle("Update available")
            .setMessage("A newer version is available.\n\n$notes\n\nTap Download to get it in your browser, then open the file to install.")
            .setPositiveButton("Download") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl))
                context.startActivity(intent)
            }
            .setNegativeButton("Later", null)
            .show()
    }
}
