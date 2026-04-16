package com.biospace.monitor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// BioSpace color palette
val BgColor = Color(0xFF030910)
val CardColor = Color(0xFF060D1C)
val BorderColor = Color(0xFF112240)
val BorderGlowColor = Color(0xFF1A3A6A)
val CyanColor = Color(0xFF00E5FF)
val GreenColor = Color(0xFF26FF8C)
val OrangeColor = Color(0xFFFF6200)
val AmberColor = Color(0xFFFFB300)
val VioletColor = Color(0xFFD500F9)
val RedColor = Color(0xFFFF1744)
val DimColor = Color(0xFF4A5F80)
val MutedColor = Color(0xFF1E3050)
val TextColor = Color(0xFFB8CCE8)
val TextDimColor = Color(0xFF5A7090)
val SrGoldColor = Color(0xFFFFE066)
val SrTealColor = Color(0xFF00FFCC)
val SrRoseColor = Color(0xFFFF6EB4)

private val DarkColorScheme = darkColorScheme(
    primary = CyanColor,
    secondary = AmberColor,
    tertiary = GreenColor,
    background = BgColor,
    surface = CardColor,
    onPrimary = BgColor,
    onSecondary = BgColor,
    onBackground = TextColor,
    onSurface = TextColor,
    error = RedColor,
    outline = BorderColor
)

@Composable
fun BioSpaceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

fun kpColor(kp: Double): Color = when {
    kp >= 7 -> RedColor
    kp >= 5 -> OrangeColor
    kp >= 3 -> AmberColor
    else -> GreenColor
}

fun kpLabel(kp: Double): String = when {
    kp >= 7 -> "SEVERE STORM"
    kp >= 5 -> "GEOMAGNETIC STORM"
    kp >= 4 -> "ACTIVE"
    kp >= 3 -> "UNSETTLED"
    else -> "QUIET"
}

fun bzColor(bz: Double): Color = when {
    bz > 2 -> GreenColor
    bz < -2 -> RedColor
    else -> AmberColor
}

fun bzLabel(bz: Double): String = when {
    bz > 2 -> "↑ NORTHWARD FAVORABLE"
    bz < -2 -> "↓ SOUTHWARD GEOEFFECTIVE"
    else -> "⚠ NEAR-ZERO UNSTABLE"
}

fun loadColor(score: Int): Color = when {
    score > 70 -> RedColor
    score > 50 -> OrangeColor
    score > 30 -> AmberColor
    else -> GreenColor
}

fun hpColor(gw: Double): Color = when {
    gw >= 100 -> RedColor
    gw >= 50 -> OrangeColor
    gw >= 20 -> AmberColor
    gw >= 10 -> Color(0xFFA3FF26)
    else -> CyanColor
}
