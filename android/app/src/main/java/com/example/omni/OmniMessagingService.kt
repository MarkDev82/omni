package com.example.omni

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.media.RingtoneManager
import android.net.Uri
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.android.gms.location.LocationServices

class OmniMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("OmniFCM", "Refreshed token: $token")
        // Ideally we would send this token to the server if the device is already enrolled.
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("OmniFCM", "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("OmniFCM", "Message data payload: ${remoteMessage.data}")
            
            val command = remoteMessage.data["command"]
            handleCommand(command)
        }
    }

    private fun handleCommand(command: String?) {
        when (command) {
            "alarm" -> playAlarmSound()
            "lock" -> showOverlay("DEVICE LOCKED")
            "wipe" -> showOverlay("WIPING DATA...")
            "location" -> handleLocationUpdate()
            else -> Log.d("OmniFCM", "Unknown command: $command")
        }
    }

    private fun handleLocationUpdate() {
        Log.d("OmniFCM", "Location update requested")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = getSharedPreferences("OmniPrefs", MODE_PRIVATE)
                val deviceId = prefs.getString("deviceId", null)
                val deviceSecret = prefs.getString("deviceSecret", null)
                
                if (deviceId != null && deviceSecret != null) {
                    // Check if permission is granted
                    if (ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            if (location != null) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    NetworkClient.postLocation(deviceId, deviceSecret, location.latitude, location.longitude)
                                }
                            } else {
                                postMockLocation(deviceId, deviceSecret)
                            }
                        }
                    } else {
                        Log.d("OmniFCM", "No location permission, sending mock location")
                        postMockLocation(deviceId, deviceSecret)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun postMockLocation(deviceId: String, deviceSecret: String) {
        CoroutineScope(Dispatchers.IO).launch {
            // Mock coordinates (Paris, France)
            NetworkClient.postLocation(deviceId, deviceSecret, 48.8566, 2.3522)
        }
    }

    private fun playAlarmSound() {
        try {
            val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val r = RingtoneManager.getRingtone(applicationContext, notification)
            r.play()
            // In a real app, you'd want to manage this sound (e.g. stop it after 5 mins)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showOverlay(message: String) {
        // For prototype purposes, we log it. A true overlay requires SYSTEM_ALERT_WINDOW permission
        // and starting an overlay service or bringing an Activity to the front.
        Log.d("OmniFCM", "OVERLAY REQUESTED: $message")
        
        // We can broadcast this to the MainActivity to show a dialog if it's open.
        val intent = android.content.Intent("com.example.omni.COMMAND_RECEIVED")
        intent.putExtra("message", message)
        sendBroadcast(intent)
    }
}
