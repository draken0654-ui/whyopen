package com.example.whyopen

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.whyopen.ui.theme.WhyopenTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhyopenTheme {
                SettingsScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SettingsScreen() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                )
            },
            containerColor = Color.Black
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsSection("Interruption")
                    SettingsToggle("Lock Categories", "Group apps by category", true)
                    SettingsToggle("Relock Automatically", "Lock app after time expires", true)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    SettingsSection("Account")
                    SettingsItem("Sync Progress", "Backup your focus data")
                    SettingsItem("Emergency Bypass", "Set a 4-digit PIN for bypass")
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    SettingsSection("Diagnostics")
                    SplunkTestButton()
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }

    @Composable
    fun SplunkTestButton() {
        val context = LocalContext.current
        var testing by remember { mutableStateOf(false) }
        
        Button(
            onClick = {
                testing = true
                CoroutineScope(Dispatchers.IO).launch {
                    val result = testConnection()
                    withContext(Dispatchers.Main) {
                        testing = false
                        Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                    }
                }
            },
            enabled = !testing,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
        ) {
            Text(if (testing) "Testing Connection..." else "Test Splunk Connection", color = Color.White)
        }
    }

    private suspend fun testConnection(): String {
        val client = createUnsafeOkHttpClient()
        val url = "https://172.16.60.130:8088/services/collector/event"
        val token = "1f9bf740-006b-4e5c-9bac-d52b05ce0d61"
        
        return try {
            // Standard JSON format for Splunk
            val jsonPayload = "{\"sourcetype\": \"whyopen\", \"event\": \"App Connection Test - SUCCESS\"}"
            val body = jsonPayload.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Splunk $token")
                .header("X-Splunk-Request-Channel", java.util.UUID.randomUUID().toString())
                .post(body)
                .build()
                
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    "Success! Event received."
                } else {
                    "Failed ${response.code}: $responseBody"
                }
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun createUnsafeOkHttpClient(): OkHttpClient {
        try {
            val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(object : javax.net.ssl.X509TrustManager {
                override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            })
            val sslContext = javax.net.ssl.SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            return OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    @Composable
    fun SettingsSection(title: String) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))
    }

    @Composable
    fun SettingsToggle(title: String, desc: String, default: Boolean) {
        var checked by remember { mutableStateOf(default) }
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontWeight = FontWeight.Medium)
                    Text(desc, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
                Switch(
                    checked = checked, 
                    onCheckedChange = { checked = it }, 
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color.White.copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.Black
                    )
                )
            }
        }
    }

    @Composable
    fun SettingsItem(title: String, desc: String) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Medium)
                Text(desc, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }
    }
}
