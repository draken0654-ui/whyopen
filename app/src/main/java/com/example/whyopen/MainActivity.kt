package com.example.whyopen

import android.accessibilityservice.AccessibilityService
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whyopen.ui.theme.WhyopenTheme

class MainActivity : ComponentActivity() {
    private lateinit var settingsManager: SettingsManager

    private var accessibilityEnabled by mutableStateOf(false)
    private var overlayEnabled by mutableStateOf(false)
    private var usageAccessEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsManager = SettingsManager(this)
        enableEdgeToEdge()
        
        setContent {
            WhyopenTheme {
                MainDashboardScreen(
                    isAccessibilityEnabled = accessibilityEnabled,
                    isOverlayEnabled = overlayEnabled,
                    isUsageAccessEnabled = usageAccessEnabled
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions() {
        accessibilityEnabled = isAccessibilityServiceEnabled(this, AppAccessibilityService::class.java)
        overlayEnabled = Settings.canDrawOverlays(this)
        usageAccessEnabled = hasUsageAccessPermission(this)
    }

    @Composable
    fun MainDashboardScreen(
        isAccessibilityEnabled: Boolean,
        isOverlayEnabled: Boolean,
        isUsageAccessEnabled: Boolean
    ) {
        Scaffold(
            containerColor = Color.Black
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    HeaderSection()
                    Spacer(modifier = Modifier.height(32.dp))
                }

                item {
                    PrivacyDisclaimerCard()
                    Spacer(modifier = Modifier.height(32.dp))
                }

                item {
                    StatsGrid()
                    Spacer(modifier = Modifier.height(32.dp))
                }

                item {
                    Text(
                        text = "System Status",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    StatusCard(
                        title = "Accessibility Service",
                        isEnabled = isAccessibilityEnabled,
                        icon = Icons.Default.AccessibilityNew,
                        onClick = { 
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            startActivity(intent) 
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    StatusCard(
                        title = "Overlay Permission",
                        isEnabled = isOverlayEnabled,
                        icon = Icons.Default.Layers,
                        onClick = {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                            startActivity(intent)
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    StatusCard(
                        title = "Usage Access",
                        isEnabled = isUsageAccessEnabled,
                        icon = Icons.Default.History,
                        onClick = { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }

                item {
                    MainActions()
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }

    @Composable
    fun HeaderSection() {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Welcome back,", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.6f))
                Text("Focused Mind", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
            IconButton(
                onClick = { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) },
                modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
            }
        }
    }

    @Composable
    fun PrivacyDisclaimerCard() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Privacy & Functionality", style = MaterialTheme.typography.titleSmall, color = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "WhyOpen requires permissions to detect app launches and help you build healthier digital habits. We do not monitor or collect your personal data. Your usage information stays secure on your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    lineHeight = 16.sp
                )
            }
        }
    }

    @Composable
    fun StatsGrid() {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(modifier = Modifier.weight(1f), label = "Saved Time", value = "${settingsManager.getTotalSavedTime()}m", icon = Icons.Default.Timer, color = Color.White)
                StatCard(modifier = Modifier.weight(1f), label = "Streak", value = "${settingsManager.getFocusStreak()} Days", icon = Icons.Default.Whatshot, color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(modifier = Modifier.weight(1f), label = "Blocked Today", value = "${settingsManager.getBlockedAttemptsToday()}", icon = Icons.Default.Block, color = Color.White)
                StatCard(modifier = Modifier.weight(1f), label = "Focus Score", value = "${settingsManager.getDailyFocusScore()}", icon = Icons.Default.AutoGraph, color = Color.White)
            }
        }
    }

    @Composable
    fun StatCard(modifier: Modifier, label: String, value: String, icon: ImageVector, color: Color) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
            }
        }
    }

    @Composable
    fun StatusCard(title: String, isEnabled: Boolean, icon: ImageVector, onClick: () -> Unit) {
        Card(
            onClick = { if (!isEnabled) onClick() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = title, color = Color.White, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (isEnabled) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }

    @Composable
    fun MainActions() {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { startActivity(Intent(this@MainActivity, AppSelectorActivity::class.java)) },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Text("Select Protected Apps", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { startActivity(Intent(this@MainActivity, StatsActivity::class.java)) },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Detailed Analytics")
            }
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
        val expectedComponentName = android.content.ComponentName(context, service)
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains(expectedComponentName.flattenToString()) ?: false
    }

    private fun hasUsageAccessPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
