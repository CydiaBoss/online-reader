package com.wang.twkanviewer.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkGreenPrimary,
    secondary = DarkGreenSecondary,
    tertiary = DarkGreenTertiary,
    background = Color(0xFF1A1C1A),
    surface = Color(0xFF1A1C1A),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE2E3DE),
    onSurface = Color(0xFFE2E3DE)
)

private val LightColorScheme = lightColorScheme(
    primary = DarkGreenPrimary, // Keep primary as #047900 even in light mode if requested "everything" to be green
    secondary = LightGreenSecondary,
    tertiary = LightGreenTertiary,
    background = Color(0xFFFBFDF8),
    surface = Color(0xFFFBFDF8),
    onPrimary = Color.White,
    onSecondary = Color(0xFF1D1C1B),
    onTertiary = Color(0xFF1D1C1B),
    onBackground = Color(0xFF1A1C1A),
    onSurface = Color(0xFF1A1C1A)
)

@Composable
fun TWKANViewerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true, // Disabled to enforce the green theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
