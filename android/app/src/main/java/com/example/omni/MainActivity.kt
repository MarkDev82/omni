package com.example.omni

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.omni.ui.theme.OmniTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OmniTheme {
                EnrollmentScreen(
                    onEnrollSuccess = { _, _ ->
                        // Display success and in a real app, save to EncryptedSharedPreferences
                        Toast.makeText(this, "Device Linked Successfully!", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
}