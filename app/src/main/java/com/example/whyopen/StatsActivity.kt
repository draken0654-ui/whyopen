package com.example.whyopen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whyopen.ui.theme.WhyopenTheme

class StatsActivity : ComponentActivity() {
    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsManager = SettingsManager(this)
        setContent {
            WhyopenTheme {
                StatsScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun StatsScreen() {
        val weeklyActivity = remember { settingsManager.getWeeklyActivity() }
        val distractingApps = remember { settingsManager.getMostDistractingApps() }
        var selectedDay by remember { mutableStateOf(weeklyActivity.lastOrNull()) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Performance Insights", color = Color.White) },
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
                    selectedDay?.let { day ->
                        AnalyticsCard("Focus Score", "${day.focusScore}/100", day.fullDate)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                item {
                    Text("Weekly Activity", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    ActivityChart(weeklyActivity, selectedDay) { selectedDay = it }
                    Spacer(modifier = Modifier.height(32.dp))
                }
                
                item {
                    Text("Most Distracting Apps", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (distractingApps.isEmpty()) {
                    item {
                        Text("No data yet.", color = Color.White.copy(alpha = 0.4f))
                    }
                } else {
                    items(distractingApps) { (pkg, count) ->
                        DistractionItem(pkg, count)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }

    @Composable
    fun AnalyticsCard(title: String, value: String, subtitle: String? = null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                Text(value, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                if (subtitle != null) {
                    Text(subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                } else {
                    val message = if (value.split("/")[0].toIntOrNull() ?: 0 > 70) "You're doing great!" else "Keep trying!"
                    Text(message, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                }
            }
        }
    }

    @Composable
    fun ActivityChart(
        activity: List<DayStats>,
        selectedDay: DayStats?,
        onDaySelected: (DayStats) -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            activity.forEach { stat ->
                val isSelected = stat == selectedDay
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .fillMaxHeight()
                        .clickable { onDaySelected(stat) }
                ) {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .fillMaxHeight(stat.intensity.coerceAtLeast(0.05f))
                            .weight(1f, fill = false)
                            .background(
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stat.dayLabel,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    Text(
                        text = stat.dateLabel,
                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.4f),
                        fontSize = 8.sp
                    )
                }
            }
        }
    }

    @Composable
    fun DistractionItem(pkg: String, count: Int) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pkg.split(".").last().replaceFirstChar { it.uppercase() },
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Text("$count blocks", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
