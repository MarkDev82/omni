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
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri

object OmniAlarmState {
    var mediaPlayer: MediaPlayer? = null
}

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
            "lock" -> lockDevice()
            "wipe" -> wipeDevice()
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
                        val cancellationTokenSource = CancellationTokenSource()
                        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
                            .addOnSuccessListener { location ->
                                if (location != null) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        NetworkClient.postLocation(deviceId, deviceSecret, location.latitude, location.longitude)
                                    }
                                } else {
                                    Log.d("OmniFCM", "Location is still null even with getCurrentLocation, sending mock")
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
            if (OmniAlarmState.mediaPlayer?.isPlaying == true) {
                // Stop the alarm if it's already playing
                OmniAlarmState.mediaPlayer?.stop()
                OmniAlarmState.mediaPlayer?.release()
                OmniAlarmState.mediaPlayer = null
                Log.d("OmniFCM", "Alarm stopped via remote command")
                return
            }

            // Force volume to max for the alarm stream
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

            val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            OmniAlarmState.mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, notification)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
            Log.d("OmniFCM", "Alarm started via remote command")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun lockDevice() {
        Log.d("OmniFCM", "LOCK REQUESTED")
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, OmniDeviceAdminReceiver::class.java)
        if (dpm.isAdminActive(adminComponent)) {
            dpm.lockNow()
        } else {
            Log.w("OmniFCM", "Cannot lock device: Not a Device Admin")
        }
    }

    private fun wipeDevice() {
        Log.d("OmniFCM", "WIPE REQUESTED")
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, OmniDeviceAdminReceiver::class.java)
        if (dpm.isAdminActive(adminComponent)) {
            // 0 flag specifies to wipe data
            dpm.wipeData(0)
        } else {
            Log.w("OmniFCM", "Cannot wipe device: Not a Device Admin")
        }
    }
}
