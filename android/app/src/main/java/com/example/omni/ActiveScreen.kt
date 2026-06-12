package com.example.omni

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ActiveScreen() {
    val omniBackground = Color(0xFFFDFBF7)
    val omniDark = Color(0xFF1C1C1B)
    val omniGreen = Color(0xFF00A046)
    val omniGreenBg = Color(0x1A00B450)

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
            
            // Shield Icon / Status Indicator
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(omniGreenBg),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(omniGreen)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "omni is active",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = omniDark,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Text(
                text = "Your device is securely linked and protected. You can now track or recover it from your web dashboard.",
                fontSize = 15.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
