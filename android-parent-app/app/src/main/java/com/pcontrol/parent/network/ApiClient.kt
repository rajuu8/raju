package com.pcontrol.parent.network

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

    fun post(path: String, json: JSONObject, authToken: String? = null, callback: (Boolean, String?) -> Unit) {
        val body = json.toString().toRequestBody(JSON)
        val builder = Request.Builder().url("$BASE_URL$path").post(body)
        authToken?.let { builder.addHeader("Authorization", "Bearer $it") }

        client.newCall(builder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = callback(false, e.message)
            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string()
                callback(response.isSuccessful, bodyStr)
                response.close()
            }
        })
    }

    fun get(path: String, authToken: String? = null, callback: (Boolean, String?) -> Unit) {
        val builder = Request.Builder().url("$BASE_URL$path").get()
        authToken?.let { builder.addHeader("Authorization", "Bearer $it") }

        client.newCall(builder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = callback(false, e.message)
            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string()
                callback(response.isSuccessful, bodyStr)
                response.close()
            }
        })
    }

    fun patch(path: String, authToken: String? = null, callback: (Boolean, String?) -> Unit) {
        val body = "{}".toRequestBody(JSON)
        val builder = Request.Builder().url("$BASE_URL$path").patch(body)
        authToken?.let { builder.addHeader("Authorization", "Bearer $it") }

        client.newCall(builder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = callback(false, e.message)
            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string()
                callback(response.isSuccessful, bodyStr)
                response.close()
            }
        })
    }
}
