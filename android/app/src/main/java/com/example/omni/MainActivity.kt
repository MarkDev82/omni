package com.example.omni

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.omni.ui.theme.OmniTheme

import androidx.compose.runtime.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Build
import android.content.Intent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request Location Permissions at startup
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 100)
        }

        enableEdgeToEdge()
        setContent {
            OmniTheme {
                val prefs = getSharedPreferences("OmniPrefs", MODE_PRIVATE)
                val hasDevice = prefs.getString("deviceId", null) != null
                var isEnrolled by remember { mutableStateOf(hasDevice) }

                if (isEnrolled) {
                    startOmniCore()
                    ActiveScreen()
                } else {
                    EnrollmentScreen(
                        onEnrollSuccess = { deviceId, deviceSecret ->
                            // Save to SharedPreferences for background services
                            val prefs = getSharedPreferences("OmniPrefs", MODE_PRIVATE)
                            prefs.edit().putString("deviceId", deviceId).putString("deviceSecret", deviceSecret).apply()

                            // Display success and switch to active screen
                            Toast.makeText(this@MainActivity, "Device Linked Successfully!", Toast.LENGTH_LONG).show()
                            isEnrolled = true
                            startOmniCore()
                        }
                    )
                }
            }
        }
    }

    private fun startOmniCore() {
        val serviceIntent = Intent(this, OmniCoreService::class.java).apply {
            action = OmniCoreService.ACTION_START_CORE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}