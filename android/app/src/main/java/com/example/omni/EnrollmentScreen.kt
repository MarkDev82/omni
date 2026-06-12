package com.example.omni

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrollmentScreen(onEnrollSuccess: (String, String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Omni Colors to match the web dashboard
    val omniBackground = Color(0xFFFDFBF7)
    val omniDark = Color(0xFF1C1C1B)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = omniBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "omni",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = omniDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "device recovery",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6) pin = it },
                label = { Text("6-Digit PIN") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = omniDark,
                    cursorColor = omniDark
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (pin.length == 6) {
                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = NetworkClient.redeemPin(pin)
                            isLoading = false
                            result.onSuccess { (deviceId, deviceSecret) ->
                                onEnrollSuccess(deviceId, deviceSecret)
                            }.onFailure { error ->
                                errorMessage = "Invalid or expired PIN"
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = pin.length == 6 && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = omniDark,
                    disabledContainerColor = Color.LightGray
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = omniBackground,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Link Device", color = omniBackground, fontSize = 16.sp)
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
