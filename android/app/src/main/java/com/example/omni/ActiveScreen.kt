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
import androidx.compose.ui.platform.LocalContext
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun ActiveScreen() {
    val context = LocalContext.current
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val adminComponent = ComponentName(context, OmniDeviceAdminReceiver::class.java)
    
    var isAdminActive by remember { mutableStateOf(dpm.isAdminActive(adminComponent)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAdminActive = dpm.isAdminActive(adminComponent)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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

            if (!isAdminActive) {
                Spacer(modifier = Modifier.height(32.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E5)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Action Required", fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                        Text(
                            "Please enable Advanced Protection to allow remote lock and erase from the web dashboard.", 
                            color = Color(0xFFD97706), 
                            fontSize = 14.sp, 
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                        Button(
                            onClick = {
                                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable Omni Advanced Protection to allow remote tracking, locking, and data erasing.")
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                        ) {
                            Text("Enable Protection")
                        }
                    }
                }
            }
        }
    }
}
