package com.pcontrol.child.network

import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Simple wrapper around OkHttp to talk to the backend.
 * IMPORTANT: Replace BASE_URL with your deployed backend URL
 * (e.g. from Render/Railway free hosting).
 */
object ApiClient {

    // TODO: change this to your deployed backend, e.g. "https://your-app.onrender.com"
    const val BASE_URL = "http://10.0.2.2:5000" // default = Android emulator localhost

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON = MediaType.parse("application/json; charset=utf-8")

    fun post(path: String, json: JSONObject, callback: (Boolean, String?) -> Unit) {
        val body = RequestBody.create(JSON, json.toString())
        val request = Request.Builder().url("$BASE_URL$path").post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body()?.string()
                callback(response.isSuccessful, bodyStr)
                response.close()
            }
        })
    }
}
