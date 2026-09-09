package com.goldsilver.livecalc.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

val GoldPrimary: Color
    get() = if (isSystemDarkThemeGlobal) Color(0xFFD4AF37) else Color(0xFF9E7815)

val GoldLight = Color(0xFFFFD700)

val SilverPrimary: Color
    get() = if (isSystemDarkThemeGlobal) Color(0xFFC0C0C0) else Color(0xFF4A5568)

val SilverLight = Color(0xFFE0E0E0)

var selectedThemeGlobal by mutableStateOf("Night")
var isSystemDarkThemeGlobal by mutableStateOf(true)

val DarkBackground: Color
    get() = when (selectedThemeGlobal) {
        "Ivory" -> Color(0xFFFAF9F6)
        "Cream" -> Color(0xFFF9F6EE)
        "Mist" -> Color(0xFFF0F4F8)
        "Sage" -> Color(0xFFF1F4F2)
        "Lavender" -> Color(0xFFF5F3F7)
        "Peach" -> Color(0xFFFAF4F0)
        "Sky Blue" -> Color(0xFFEDF4F9)
        "Sand" -> Color(0xFFF5EFEB)
        else -> Color(0xFF0D0E15) // Night / Default dark
    }

val DarkSurface: Color
    get() = when (selectedThemeGlobal) {
        "Ivory", "Cream", "Mist", "Sage", "Lavender", "Peach", "Sky Blue", "Sand" -> Color(0xFFFFFFFF)
        else -> Color(0xFF161722) // Night surface
    }

val DarkSurfaceElevated: Color
    get() = when (selectedThemeGlobal) {
        "Ivory" -> Color(0xFFF4F1EA)
        "Cream" -> Color(0xFFF0EBE1)
        "Mist" -> Color(0xFFE2E8F0)
        "Sage" -> Color(0xFFE2E9E5)
        "Lavender" -> Color(0xFFEAE6ED)
        "Peach" -> Color(0xFFEFE3DB)
        "Sky Blue" -> Color(0xFFDFECF5)
        "Sand" -> Color(0xFFE9DDD5)
        else -> Color(0xFF222332) // Night surface elevated
    }

val AccentGreen: Color
    get() = if (isSystemDarkThemeGlobal) Color(0xFF4CAF50) else Color(0xFF2E7D32)

val AccentRed: Color
    get() = if (isSystemDarkThemeGlobal) Color(0xFFF44336) else Color(0xFFC62828)

val TextPrimary: Color
    get() = if (isSystemDarkThemeGlobal) Color(0xFFFFFFFF) else Color(0xFF1C1C1E)

val TextSecondary: Color
    get() = if (isSystemDarkThemeGlobal) Color(0xFFB0B0B0) else Color(0xFF48484A)

val TextMuted: Color
    get() = if (isSystemDarkThemeGlobal) Color(0xFF757575) else Color(0xFF636366)

val GoldGradientStart = Color(0xFFF39C12)
val GoldGradientEnd = Color(0xFFF1C40F)
val SilverGradientStart = Color(0xFFBDC3C7)
val SilverGradientEnd = Color(0xFF95A5A6)

