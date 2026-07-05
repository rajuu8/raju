package com.pcontrol.child.network

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {

    const val BASE_URL = "https://raju-4xh6.onrender.com"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun post(path: String, json: JSONObject, callback: (Boolean, String?) -> Unit) {
        val body = json.toString().toRequestBody(JSON)
        val request = Request.Builder().url("$BASE_URL$path").post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string()
                callback(response.isSuccessful, bodyStr)
                response.close()
            }
        })
    }

    fun get2(path: String, callback: (Boolean, String?) -> Unit) {
        val request = Request.Builder().url("$BASE_URL$path").get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string()
                callback(response.isSuccessful, bodyStr)
                response.close()
            }
        })
    }
}
