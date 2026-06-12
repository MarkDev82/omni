package com.example.omni

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object NetworkClient {
    // 10.0.2.2 is the special IP to reach the localhost of the host machine from an Android emulator
    private const val BASE_URL = "http://10.0.2.2:3000/api"

    suspend fun redeemPin(pin: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/enrollment/redeem")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true

            val jsonInputString = JSONObject().apply {
                put("pin", pin)
                put("model_name", android.os.Build.MODEL)
                put("os_version", android.os.Build.VERSION.RELEASE)
            }.toString()

            val input = jsonInputString.toByteArray(Charsets.UTF_8)
            connection.setRequestProperty("Content-Length", input.size.toString())
            connection.outputStream.write(input)
            connection.outputStream.flush()

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(response)
                val deviceId = jsonObject.getString("device_id")
                val deviceSecret = jsonObject.getString("device_secret")
                Result.success(Pair(deviceId, deviceSecret))
            } else {
                val errorMsg = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Result.failure(Exception("Failed to redeem PIN: ${connection.responseCode} - $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
