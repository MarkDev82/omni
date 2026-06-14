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

    suspend fun redeemPin(pin: String, fcmToken: String? = null): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
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
                if (fcmToken != null) {
                    put("fcm_token", fcmToken)
                }
            }.toString()

            val input = jsonInputString.toByteArray(Charsets.UTF_8)
            connection.setRequestProperty("Content-Length", input.size.toString())
            connection.outputStream.write(input)
            connection.outputStream.flush()

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(response)
                val deviceId = jsonObject.getString("device_id")
                val deviceSecret = jsonObject.getString("device_secret")
                Result.success(Pair(deviceId, deviceSecret))
            } else {
                Result.failure(Exception("Failed to redeem PIN: $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun postLocation(
        deviceId: String, 
        deviceSecret: String, 
        lat: Double, 
        lng: Double,
        batteryLevel: Int? = null,
        isCharging: Boolean? = null,
        networkType: String? = null,
        wifiSsid: String? = null,
        screenOn: Boolean? = null,
        speedMps: Double? = null,
        uptimeSeconds: Long? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/location")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true

            val jsonBuilder = StringBuilder()
            jsonBuilder.append("""{"device_id":"$deviceId","device_secret":"$deviceSecret","lat":$lat,"lng":$lng""")
            
            if (batteryLevel != null) jsonBuilder.append(""","battery_level":$batteryLevel""")
            if (isCharging != null) jsonBuilder.append(""","is_charging":$isCharging""")
            if (networkType != null) jsonBuilder.append(""","network_type":"$networkType"""")
            if (wifiSsid != null) jsonBuilder.append(""","wifi_ssid":"$wifiSsid"""")
            if (screenOn != null) jsonBuilder.append(""","screen_on":$screenOn""")
            if (speedMps != null) jsonBuilder.append(""","speed_mps":$speedMps""")
            if (uptimeSeconds != null) jsonBuilder.append(""","uptime_seconds":$uptimeSeconds""")
            
            jsonBuilder.append("}")

            val body = jsonBuilder.toString()
            connection.outputStream.use { os ->
                os.write(body.toByteArray())
                os.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to post location: $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
