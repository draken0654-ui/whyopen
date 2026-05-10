package com.example.whyopen

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.whyopen.ui.theme.WhyopenTheme

data class AppItem(
    val name: String,
    val packageName: String,
    val info: ApplicationInfo,
    var isSelected: Boolean = false
)

class AppSelectorActivity : ComponentActivity() {
    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsManager = SettingsManager(this)

        setContent {
            WhyopenTheme {
                AppSelectorScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppSelectorScreen() {
        val pm = packageManager
        var searchQuery by remember { mutableStateOf("") }
        
        val apps = remember {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
                .map {
                    AppItem(
                        name = it.loadLabel(pm).toString(),
                        packageName = it.packageName,
                        info = it,
                        isSelected = settingsManager.getBlockedApps().contains(it.packageName)
                    )
                }
                .sortedBy { it.name }
        }

        val filteredApps = apps.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.packageName.contains(searchQuery, ignoreCase = true) 
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Protect Apps", color = Color.White) },
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
            Column(modifier = Modifier.padding(padding)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                    )
                )

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredApps) { app ->
                        var selected by remember { mutableStateOf(app.isSelected) }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Image(
                                    bitmap = remember { app.info.loadIcon(pm).toBitmap().asImageBitmap() },
                                    contentDescription = null,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = app.name, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                Text(text = app.packageName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Switch(
                                checked = selected,
                                onCheckedChange = { isChecking ->
                                    selected = isChecking
                                    settingsManager.setAppBlocked(app.packageName, isChecking)
                                },
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
            }
        }
    }
}
