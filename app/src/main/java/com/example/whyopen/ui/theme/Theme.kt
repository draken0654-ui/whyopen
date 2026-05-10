package com.example.whyopen.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BlackAndWhiteScheme = darkColorScheme(
    primary = Color.White,
    secondary = Color.LightGray,
    tertiary = Color.Gray,
    background = Color.Black,
    surface = Color(0xFF111111),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun WhyopenTheme(
    darkTheme: Boolean = true, // Force dark
    dynamicColor: Boolean = false, // Disable dynamic color to maintain B&W
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BlackAndWhiteScheme,
        typography = Typography,
        content = content
    )
}
