package com.biospace.monitor.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biospace.monitor.model.WeatherState
import com.biospace.monitor.ui.components.*
import com.biospace.monitor.ui.theme.*
import kotlin.math.*

@Composable
fun EnvironmentScreen(
    weather: WeatherState,
    onRequestLocation: () -> Unit,
    onSearchCity: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF060D1E))
            .border(1.dp, CyanColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        // Title
        Text("LOCAL ENVIRONMENT", color = CyanColor, fontSize = 13.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Text("LOCAL WEATHER · AUTONOMIC DYSFUNCTION RISK FACTORS",
            color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(12.dp))

        // Location bar
        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF040A1A))
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = CyanColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(weather.locationName, color = TextColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    String.format("%.4f°, %.4f°", weather.lat, weather.lon),
                    color = DimColor, fontSize = 8.sp, letterSpacing = 1.sp
                )
            }
            IconButton(onClick = onRequestLocation, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "Update location", tint = CyanColor, modifier = Modifier.size(16.dp))
            }
        }
        TextButton(onClick = onSearchCity) {
            Text("SEARCH CITY", color = CyanColor, fontSize = 9.sp, letterSpacing = 2.sp)
        }

        Spacer(Modifier.height(10.dp))

        // 4-metric grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallMetricBox(
                value = String.format("%.0f", weather.tempF),
                unit = "°F",
                label = "TEMPERATURE",
                status = when {
                    weather.tempF > 95 -> "▲ CRITICAL HEAT"
                    weather.tempF > 85 -> "▲ ELEVATED"
                    weather.tempF > 75 -> "WARM"
                    else -> "COOL"
                },
                color = when {
                    weather.tempF > 95 -> RedColor
                    weather.tempF > 85 -> OrangeColor
                    weather.tempF > 75 -> AmberColor
                    else -> CyanColor
                },
                modifier = Modifier.weight(1f)
            )
            SmallMetricBox(
                value = "${weather.humidity}",
                unit = "% RH",
                label = "HUMIDITY",
                status = when {
                    weather.humidity > 80 -> "▲ HIGH STRESS"
                    weather.humidity > 65 -> "ELEVATED"
                    else -> "NORMAL"
                },
                color = when {
                    weather.humidity > 80 -> OrangeColor
                    weather.humidity > 65 -> AmberColor
                    else -> CyanColor
                },
                modifier = Modifier.weight(1f)
            )
            SmallMetricBox(
                value = String.format("%.0f", weather.pressureHpa),
                unit = "hPa",
                label = "PRESSURE",
                status = pressureTrend(weather.pressureHistory),
                color = if (isPressureDroppingFast(weather.pressureHistory)) OrangeColor else AmberColor,
                modifier = Modifier.weight(1f)
            )
            SmallMetricBox(
                value = String.format("%.0f", weather.windMph),
                unit = "mph",
                label = "WIND",
                status = if (weather.windMph > 20) "▲ STRONG" else if (weather.windMph > 10) "MODERATE" else "LIGHT",
                color = GreenColor,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))

        // Barometric trend
        BaroTrendChart(weather.pressureHistory)

        Spacer(Modifier.height(14.dp))

        // Trigger grid
        Text("AUTONOMIC DYSFUNCTION TRIGGER ASSESSMENT",
            color = DimColor, fontSize = 9.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TriggerBox(
                    icon = "🌡️",
                    name = "HEAT LOAD",
                    value = String.format("%.0f°F / HI %.0f°F", weather.tempF, weather.apparentTempF),
                    risk = when {
                        weather.tempF > 95 || weather.apparentTempF > 100 -> "CRITICAL TRIGGER"
                        weather.tempF > 85 || weather.apparentTempF > 90 -> "HIGH RISK"
                        weather.tempF > 75 -> "MODERATE"
                        else -> "LOW RISK"
                    },
                    riskColor = when {
                        weather.tempF > 95 -> RedColor
                        weather.tempF > 85 -> OrangeColor
                        weather.tempF > 75 -> AmberColor
                        else -> GreenColor
                    },
                    modifier = Modifier.weight(1f)
                )
                TriggerBox(
                    icon = "💧",
                    name = "HUMIDITY STRESS",
                    value = "${weather.humidity}% RH",
                    risk = when {
                        weather.humidity > 80 -> "HIGH STRESS"
                        weather.humidity > 65 -> "MODERATE"
                        else -> "LOW"
                    },
                    riskColor = when {
                        weather.humidity > 80 -> OrangeColor
                        weather.humidity > 65 -> AmberColor
                        else -> GreenColor
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val baroDelta = if (weather.pressureHistory.size >= 2)
                    weather.pressureHistory.last() - weather.pressureHistory.first() else 0.0
                TriggerBox(
                    icon = "📉",
                    name = "BARO DROP RATE",
                    value = "${if (baroDelta >= 0) "+" else ""}${String.format("%.1f", baroDelta)} hPa/24hr",
                    risk = when {
                        baroDelta < -5 -> "CRITICAL DROP"
                        baroDelta < -2 -> "SIGNIFICANT DROP"
                        baroDelta < 0 -> "FALLING"
                        else -> "STABLE / RISING"
                    },
                    riskColor = when {
                        baroDelta < -5 -> RedColor
                        baroDelta < -2 -> OrangeColor
                        baroDelta < 0 -> AmberColor
                        else -> GreenColor
                    },
                    modifier = Modifier.weight(1f)
                )
                // Dewpoint gap
                val dewpointF = dewpoint(weather.tempF, weather.humidity)
                val dewGap = weather.tempF - dewpointF
                TriggerBox(
                    icon = "🌫️",
                    name = "DEWPOINT GAP",
                    value = "${String.format("%.0f", dewpointF)}°F DP · Δ${String.format("%.0f", dewGap)}°",
                    risk = when {
                        dewGap < 5 -> "NEAR SATURATION"
                        dewGap < 15 -> "HUMID"
                        else -> "DRY COMFORTABLE"
                    },
                    riskColor = when {
                        dewGap < 5 -> OrangeColor
                        dewGap < 15 -> AmberColor
                        else -> GreenColor
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BaroTrendChart(history: List<Double>) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("BAROMETRIC PRESSURE · 24HR TREND (hPa)", color = DimColor, fontSize = 8.sp, letterSpacing = 1.sp)
            if (history.size >= 2) {
                val delta = history.last() - history.first()
                Text(
                    "${if (delta >= 0) "+" else ""}${String.format("%.1f", delta)} hPa",
                    color = if (delta < -2) OrangeColor else if (delta > 2) GreenColor else AmberColor,
                    fontSize = 8.sp, fontFamily = FontFamily.Monospace
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(60.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF020812))
                .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                .padding(4.dp)
        ) {
            if (history.size >= 2) {
                Sparkline(
                    values = history,
                    color = AmberColor,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("LOADING HISTORY...", color = DimColor, fontSize = 8.sp,
                    modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun TriggerBox(
    icon: String, name: String, value: String,
    risk: String, riskColor: Color, modifier: Modifier
) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF040A18))
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(icon, fontSize = 14.sp)
            Text(name, color = DimColor, fontSize = 8.sp, letterSpacing = 1.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(value, color = TextColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(2.dp))
        Text(risk, color = riskColor, fontSize = 8.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun pressureTrend(history: List<Double>): String {
    if (history.size < 2) return "LOADING"
    val delta = history.last() - history.first()
    return when {
        delta < -5 -> "▼ DROPPING FAST"
        delta < -2 -> "▼ DROPPING"
        delta < 0 -> "▼ SLIGHT DROP"
        delta < 2 -> "STABLE"
        else -> "▲ RISING"
    }
}

private fun isPressureDroppingFast(history: List<Double>): Boolean {
    if (history.size < 2) return false
    return (history.last() - history.first()) < -2
}

private fun dewpoint(tempF: Double, humidity: Int): Double {
    val tempC = (tempF - 32) * 5 / 9
    val a = 17.27
    val b = 237.7
    val alpha = ((a * tempC) / (b + tempC)) + ln(humidity / 100.0)
    val dewC = (b * alpha) / (a - alpha)
    return dewC * 9 / 5 + 32
}
