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
import android.app.AlertDialog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request Location Permissions at startup
        checkAndRequestPermissions()

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

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 100)
        } else {
            checkBackgroundLocation()
        }
    }

    private fun checkBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            AlertDialog.Builder(this)
                .setTitle("Permiso Maestro Necesario")
                .setMessage("Para que Omni pueda proteger este dispositivo en todo momento (incluso tras un reinicio o con la pantalla apagada), necesitas seleccionar 'Permitir todo el tiempo' en la siguiente pantalla.")
                .setPositiveButton("Entendido") { _, _ ->
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 101)
                }
                .setCancelable(false)
                .show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Foreground granted, now immediately ask for background
            checkBackgroundLocation()
        }
    }
}