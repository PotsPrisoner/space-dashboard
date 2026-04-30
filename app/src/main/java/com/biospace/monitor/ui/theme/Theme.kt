package com.biospace.monitor.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgColor     = Color(0xFF000000)
val CardBg      = Color(0xFF0A1628)
val BorderColor = Color(0xFF162230)
val CyanColor   = Color(0xFF00D9C8)
val GreenColor  = Color(0xFF00E87A)
val AmberColor  = Color(0xFFF5C842)
val OrangeColor = Color(0xFFFF9F1C)
val RedColor    = Color(0xFFFF3D5A)
val BlueColor   = Color(0xFF1A8FFF)
val DimColor    = Color(0xFF3A5570)
val TextColor   = Color(0xFFB8CEDD)
val BrightColor = Color(0xFFE8F8FF)

private val DarkColors = darkColorScheme(
    primary = CyanColor, secondary = GreenColor, background = BgColor,
    surface = CardBg, onPrimary = BgColor, onBackground = TextColor, onSurface = TextColor
)

@Composable
fun BioSpaceTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
