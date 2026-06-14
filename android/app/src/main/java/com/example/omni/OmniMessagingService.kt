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
import android.content.Intent

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
        Log.d("OmniFCM", "Location update requested, forwarding to core service")
        val serviceIntent = Intent(this, OmniCoreService::class.java).apply {
            action = OmniCoreService.ACTION_UPDATE_LOCATION
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun playAlarmSound() {
        val serviceIntent = Intent(this, OmniCoreService::class.java).apply {
            action = OmniCoreService.ACTION_TOGGLE_ALARM
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
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
