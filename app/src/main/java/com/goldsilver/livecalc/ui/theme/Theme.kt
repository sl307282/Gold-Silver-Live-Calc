package com.goldsilver.livecalc.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun GoldSilverLiveCalcTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isSystemDarkThemeGlobal) {
        darkColorScheme(
            primary = GoldPrimary,
            secondary = SilverPrimary,
            background = DarkBackground,
            surface = DarkSurface,
            onPrimary = DarkBackground,
            onSecondary = DarkBackground,
            onBackground = Color(0xFFFFFFFF),
            onSurface = Color(0xFFFFFFFF),
            surfaceVariant = DarkSurfaceElevated,
            onSurfaceVariant = Color(0xFFB0B0B0)
        )
    } else {
        lightColorScheme(
            primary = GoldPrimary,
            secondary = SilverPrimary,
            background = DarkBackground,
            surface = DarkSurface,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFF1C1C1E),
            onSurface = Color(0xFF1C1C1E),
            surfaceVariant = DarkSurfaceElevated,
            onSurfaceVariant = Color(0xFF8E8E93)
        )
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isSystemDarkThemeGlobal
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
