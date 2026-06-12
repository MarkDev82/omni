package com.example.omni

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.omni.ui.theme.OmniTheme

import androidx.compose.runtime.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OmniTheme {
                var isEnrolled by remember { mutableStateOf(false) }

                if (isEnrolled) {
                    ActiveScreen()
                } else {
                    EnrollmentScreen(
                        onEnrollSuccess = { _, _ ->
                            // Display success and switch to active screen
                            Toast.makeText(this, "Device Linked Successfully!", Toast.LENGTH_LONG).show()
                            isEnrolled = true
                        }
                    )
                }
            }
        }
    }
}