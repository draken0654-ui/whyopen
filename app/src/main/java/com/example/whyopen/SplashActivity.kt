package com.example.whyopen

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SplashScreen()
        }
    }

    @Composable
    fun SplashScreen() {
        var startAnimation by remember { mutableStateOf(false) }
        val alphaAnim = animateFloatAsState(
            targetValue = if (startAnimation) 1f else 0f,
            animationSpec = tween(durationMillis = 1500), label = "alpha"
        )

        LaunchedEffect(key1 = true) {
            startAnimation = true
            delay(2500)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }

        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "WhyOpen?",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.alpha(alphaAnim.value)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Intentional Usage",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.6f * alphaAnim.value),
                    modifier = Modifier.alpha(alphaAnim.value)
                )
            }
        }
    }
}
