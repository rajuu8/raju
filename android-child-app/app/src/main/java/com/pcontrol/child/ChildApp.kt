package com.pcontrol.child

import android.app.Application
import android.content.Context
import java.io.PrintWriter
import java.io.StringWriter

class ChildApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val prefs = getSharedPreferences("crash_log", Context.MODE_PRIVATE)
                prefs.edit().putString("last_crash", sw.toString()).apply()
            } catch (e: Exception) {
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        fun getLastCrash(context: Context): String? {
            val prefs = context.getSharedPreferences("crash_log", Context.MODE_PRIVATE)
            return prefs.getString("last_crash", null)
        }

        fun clearLastCrash(context: Context) {
            val prefs = context.getSharedPreferences("crash_log", Context.MODE_PRIVATE)
            prefs.edit().remove("last_crash").apply()
        }
    }
}
