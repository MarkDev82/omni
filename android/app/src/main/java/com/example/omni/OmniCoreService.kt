package com.example.omni

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OmniCoreService : Service() {
    companion object {
        const val CHANNEL_ID = "OmniCoreChannel"
        const val ACTION_START_CORE = "ACTION_START_CORE"
        const val ACTION_TOGGLE_ALARM = "ACTION_TOGGLE_ALARM"
        const val ACTION_UPDATE_LOCATION = "ACTION_UPDATE_LOCATION"
        
        var mediaPlayer: MediaPlayer? = null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Omni is active")
            .setContentText("Protecting your device in the background")
            .setSmallIcon(R.mipmap.ic_launcher) // We use launcher icon temporarily
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)

        when (intent?.action) {
            ACTION_TOGGLE_ALARM -> toggleAlarm()
            ACTION_START_CORE -> Log.d("OmniCore", "Core service started/verified")
            ACTION_UPDATE_LOCATION -> fetchAndSendLocation()
        }

        return START_STICKY
    }

    private fun fetchAndSendLocation() {
        Log.d("OmniCore", "Fetching location in foreground service")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = getSharedPreferences("OmniPrefs", MODE_PRIVATE)
                val deviceId = prefs.getString("deviceId", null)
                val deviceSecret = prefs.getString("deviceSecret", null)

                if (deviceId != null && deviceSecret != null) {
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
                                    Log.d("OmniCore", "Location null, sending mock (Paris)")
                                    postMockLocation(deviceId, deviceSecret)
                                }
                            }
                    } else {
                        Log.d("OmniCore", "No location permission, sending mock")
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
            NetworkClient.postLocation(deviceId, deviceSecret, 48.8566, 2.3522)
        }
    }

    private fun toggleAlarm() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            Log.d("OmniCore", "Alarm stopped")
        } else {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

            val notificationUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, notificationUri)
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
            Log.d("OmniCore", "Alarm started")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Omni Protection Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
