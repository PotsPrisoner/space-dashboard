package com.biospace.monitor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biospace.monitor.model.SpaceWeatherState
import com.biospace.monitor.ui.components.*
import com.biospace.monitor.ui.theme.*
import kotlin.math.abs

@Composable
fun DashboardScreen(sw: SpaceWeatherState) {
    Column {
        // ── Kp Index ──────────────────────────────────────────────────────────
        BioCard {
            CardTitle("PLANETARY K-INDEX", "// NOAA SWPC")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column {
                    Text(
                        text = String.format("%.1f", sw.kp),
                        color = kpColor(sw.kp),
                        fontSize = 44.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        lineHeight = 44.sp
                    )
                    Text(
                        text = kpLabel(sw.kp),
                        color = kpColor(sw.kp),
                        fontSize = 11.sp,
                        letterSpacing = 3.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    BioProgressBar(
                        fraction = (sw.kp / 9.0).toFloat(),
                        color = kpColor(sw.kp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0 QUIET", color = DimColor, fontSize = 8.sp)
                        Text("3", color = DimColor, fontSize = 8.sp)
                        Text("5 ACTIVE", color = DimColor, fontSize = 8.sp)
                        Text("7", color = DimColor, fontSize = 8.sp)
                        Text("9 G5", color = DimColor, fontSize = 8.sp)
                    }
                    if (sw.kpHistory.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        SmoothSparkline(
                            values = sw.kpHistory.takeLast(60),
                            color = kpColor(sw.kp),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        )
                        Text("KP HISTORY · 1HR", color = DimColor, fontSize = 7.sp, letterSpacing = 2.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(11.dp))

        // ── Storm Scales ──────────────────────────────────────────────────────
        BioCard {
            CardTitle("NOAA STORM SCALES")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScaleBox(
                    "STORM INDEX", "G${sw.gScale}",
                    stormScaleColor(sw.gScale), Modifier.weight(1f)
                )
                ScaleBox(
                    "SOLAR RAD", "S${sw.sScale}",
                    stormScaleColor(sw.sScale), Modifier.weight(1f)
                )
                ScaleBox(
                    "RADIO", "R${sw.rScale}",
                    stormScaleColor(sw.rScale), Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(11.dp))

        // ── Solar Wind ────────────────────────────────────────────────────────
        BioCard {
            CardTitle("SOLAR WIND", "// DSCOVR / ACE")
                // Speed + Density as neon speedometers
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        NeonSpeedometer(
                            fraction = ((sw.speed - 200) / 800f).coerceIn(0f, 1f),
                            value = "${sw.speed.toInt()}",
                            unit = "km/s",
                            color = speedColor(sw.speed),
                            size = 130.dp,
                            minLabel = "200",
                            maxLabel = "1000"
                        )
                        Text("SPEED", color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        NeonSpeedometer(
                            fraction = (sw.density / 30f).coerceIn(0f, 1f),
                            value = String.format("%.1f", sw.density),
                            unit = "p/cm³",
                            color = GreenColor,
                            size = 130.dp,
                            minLabel = "0",
                            maxLabel = "30"
                        )
                        Text("DENSITY", color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace)
                    }
                }
            MetricRow(
                "TEMPERATURE",
                "${String.format("%.0f", sw.temperature / 1000)} kK"
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("IMF Bz DIRECTION", color = DimColor, fontSize = 10.sp, letterSpacing = 2.sp, fontFamily = FontFamily.SansSerif)
                BzBadge(sw.bz)
            }
            if (sw.speedHistory.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        SmoothSparkline(
                            values = sw.speedHistory.takeLast(60),
                            color = CyanColor,
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        )
                        Text("SPEED 1HR", color = DimColor, fontSize = 7.sp, letterSpacing = 2.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        SmoothSparkline(
                            values = sw.densityHistory.takeLast(60),
                            color = GreenColor,
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        )
                        Text("DENSITY 1HR", color = DimColor, fontSize = 7.sp, letterSpacing = 2.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(11.dp))

        // ── X-Ray Flux ────────────────────────────────────────────────────────
        BioCard {
            CardTitle("GOES X-RAY FLUX", "// SOLAR FLARES")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = String.format("%.2e", sw.xrayFlux),
                        color = OrangeColor,
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text("W/m²", color = DimColor, fontSize = 8.sp, letterSpacing = 1.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = sw.flareClass,
                        color = flareClassColor(sw.flareClass),
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text("FLARE CLASS", color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp)
                }
            }
        }

        Spacer(Modifier.height(11.dp))

        // ── Hemispheric Power ─────────────────────────────────────────────────
        BioCard {
            CardTitle("HEMISPHERIC POWER", "// OVATION AURORA")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF040A18)).border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("NORTH", color = DimColor, fontSize = 9.sp, letterSpacing = 3.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        String.format("%.0f", sw.hpNorth),
                        color = hpColor(sw.hpNorth), fontSize = 26.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                    )
                    Text("GW", color = DimColor, fontSize = 10.sp, letterSpacing = 2.sp)
                    Text(hpLabel(sw.hpNorth), color = hpColor(sw.hpNorth), fontSize = 8.sp, letterSpacing = 2.sp)
                }
                Column(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF040A18)).border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("SOUTH", color = DimColor, fontSize = 9.sp, letterSpacing = 3.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        String.format("%.0f", sw.hpSouth),
                        color = hpColor(sw.hpSouth), fontSize = 26.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                    )
                    Text("GW", color = DimColor, fontSize = 10.sp, letterSpacing = 2.sp)
                    Text(hpLabel(sw.hpSouth), color = hpColor(sw.hpSouth), fontSize = 8.sp, letterSpacing = 2.sp)
                }
            }
            if (sw.hpNorthHistory.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                SmoothSparkline(
                    values = sw.hpNorthHistory.takeLast(60),
                    color = CyanColor,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                )
                Text("NORTH HP HISTORY", color = DimColor, fontSize = 7.sp, letterSpacing = 2.sp)
            }
        }
    }
}

@Composable
fun BzBadge(bz: Double) {
    val color = bzColor(bz)
    val label = bzLabel(bz)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            "${if (bz >= 0) "+" else ""}${String.format("%.1f", bz)} nT $label",
            color = color, fontSize = 9.sp, letterSpacing = 1.sp, fontFamily = FontFamily.Monospace
        )
    }
}

private fun stormScaleColor(level: Int): Color = when {
    level >= 4 -> RedColor
    level >= 3 -> OrangeColor
    level >= 2 -> AmberColor
    level >= 1 -> GreenColor
    else -> Color(0xFF2A4A7A)
}

private fun speedColor(speed: Double): Color = when {
    speed > 600 -> RedColor
    speed > 500 -> OrangeColor
    speed > 450 -> AmberColor
    else -> CyanColor
}

private fun flareClassColor(cls: String): Color = when {
    cls.startsWith("X") -> RedColor
    cls.startsWith("M") -> OrangeColor
    cls.startsWith("C") -> AmberColor
    cls.startsWith("B") -> GreenColor
    else -> DimColor
}

private fun hpLabel(gw: Double) = when {
    gw >= 100 -> "▲ STORM LEVEL"
    gw >= 50 -> "▲ ACTIVE AURORA"
    gw >= 20 -> "▲ UNSETTLED"
    gw >= 5 -> "● MODERATE"
    else -> "● QUIET"
}
