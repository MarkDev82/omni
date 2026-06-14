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
import android.os.BatteryManager
import android.os.SystemClock
import android.content.IntentFilter
import android.os.PowerManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
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

        try {
            if (Build.VERSION.SDK_INT >= 34) { // UPSIDE_DOWN_CAKE
                try {
                    // Try to start with both LOCATION (8) and SPECIAL_USE (1073741824)
                    startForeground(1, notification, 8 or 1073741824)
                } catch (e: SecurityException) {
                    Log.e("OmniCore", "Cannot start location FGS from background, falling back to specialUse", e)
                    startForeground(1, notification, 1073741824)
                }
            } else if (Build.VERSION.SDK_INT >= 29) { // Q
                startForeground(1, notification, 8)
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) {
            Log.e("OmniCore", "Fallback startForeground failed", e)
            startForeground(1, notification)
        }

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
                                        // TELEMETRY GATHERING
                                        
                                        // 1. Battery
                                        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                                            applicationContext.registerReceiver(null, ifilter)
                                        }
                                        val batteryLevel: Int? = batteryStatus?.let { intent ->
                                            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                                            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                                            if (scale == 0) null else (level * 100 / scale.toFloat()).toInt()
                                        }
                                        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                                        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

                                        // 2. Screen State
                                        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                                        val screenOn = powerManager.isInteractive

                                        // 3. Uptime
                                        val uptimeSeconds = SystemClock.elapsedRealtime() / 1000

                                        // 4. Speed
                                        val speedMps = if (location.hasSpeed()) location.speed.toDouble() else 0.0

                                        // 5. Network
                                        var networkType = "offline"
                                        var wifiSsid: String? = null
                                        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                                        val activeNetwork = connectivityManager.activeNetwork
                                        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

                                        if (capabilities != null) {
                                            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                                                networkType = "wifi"
                                                val wifiManager = applicationContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                                                val info = wifiManager.connectionInfo
                                                if (info != null && info.ssid != null && info.ssid != "<unknown ssid>") {
                                                    wifiSsid = info.ssid.replace("\"", "")
                                                }
                                            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                                                networkType = "cellular"
                                            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                                                networkType = "ethernet"
                                            }
                                        }

                                        NetworkClient.postLocation(
                                            deviceId = deviceId,
                                            deviceSecret = deviceSecret,
                                            lat = location.latitude,
                                            lng = location.longitude,
                                            batteryLevel = batteryLevel,
                                            isCharging = isCharging,
                                            networkType = networkType,
                                            wifiSsid = wifiSsid,
                                            screenOn = screenOn,
                                            speedMps = speedMps,
                                            uptimeSeconds = uptimeSeconds
                                        )
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
