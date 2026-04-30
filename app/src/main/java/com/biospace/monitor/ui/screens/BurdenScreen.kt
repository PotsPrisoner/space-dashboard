package com.biospace.monitor.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.biospace.monitor.data.AlertLevel
import com.biospace.monitor.data.BurdenComponent
import com.biospace.monitor.ui.MainViewModel
import com.biospace.monitor.ui.theme.*

@Composable
fun BurdenScreen(vm: MainViewModel) {
    val burden  by vm.burden.collectAsState()
    val loading by vm.loading.collectAsState()
    val error   by vm.error.collectAsState()
    val alertColor = Color(android.graphics.Color.parseColor(burden.alertLevel.colorHex))

    LazyColumn(
        Modifier.fillMaxSize().background(BgColor).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("BIOSPACE MONITOR", color = CyanColor, fontSize = 10.sp, letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
                    Text("ANS BURDEN ANALYSIS", color = BrightColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                if (loading) CircularProgressIndicator(color = CyanColor, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else TextButton(onClick = { vm.fetchAll() }) { Text("↻", color = DimColor, fontSize = 18.sp) }
            }
        }
        error?.let { item { Text("⚠ $it", color = RedColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace) } }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, alertColor.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ANS BURDEN SCORE", color = DimColor, fontSize = 9.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(8.dp))
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(160.dp)) {
                            val sweep = (burden.overall / 100f).coerceIn(0f,1f) * 270f
                            drawArc(color = BorderColor, startAngle = 135f, sweepAngle = 270f, useCenter = false,
                                style = Stroke(width = 20f, cap = StrokeCap.Round))
                            drawArc(color = alertColor, startAngle = 135f, sweepAngle = sweep, useCenter = false,
                                style = Stroke(width = 20f, cap = StrokeCap.Round))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${burden.overall.toInt()}%", color = alertColor, fontSize = 38.sp,
                                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(burden.alertLevel.name, color = alertColor.copy(0.7f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(alertColor.copy(0.12f)).border(1.dp, alertColor, RoundedCornerShape(8.dp)).padding(12.dp)) {
                        Column {
                            Text(burden.alertLevel.label, color = alertColor, fontSize = 13.sp,
                                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(burden.alertLevel.instruction, color = TextColor, fontSize = 11.sp,
                                lineHeight = 16.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MAGNITUDE", color = DimColor, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            Text("${burden.magnitude.toInt()}%", color = AmberColor, fontSize = 22.sp,
                                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("FLUCTUATION", color = DimColor, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            Text("${burden.fluctuation.toInt()}%", color = BlueColor, fontSize = 22.sp,
                                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Text("* Fluctuation weighted 60% — primary dysautonomia trigger", color = DimColor,
                        fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        item { Text("BREAKDOWN", color = DimColor, fontSize = 9.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace) }

        items(burden.breakdown.entries.sortedByDescending { it.value.combined }.toList()) { (name, comp) ->
            BurdenRow(name, comp)
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text("COLOR GUIDE", color = DimColor, fontSize = 9.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(6.dp))
            AlertLevel.values().forEach { lvl ->
                val c = Color(android.graphics.Color.parseColor(lvl.colorHex))
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(c))
                    Spacer(Modifier.width(8.dp))
                    Text("${lvl.name}  ${lvl.label}", color = c, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun BurdenRow(name: String, comp: BurdenComponent) {
    val barColor = when {
        comp.combined >= 50 -> BlueColor; comp.combined >= 25 -> RedColor
        comp.combined >= 7  -> AmberColor; else -> GreenColor
    }
    Card(colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, BorderColor), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(name, color = TextColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (comp.value.isNotBlank()) Text(comp.value, color = CyanColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("${comp.combined.toInt()}%", color = barColor, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
            Spacer(Modifier.height(5.dp))
            SmallBar("MAG", comp.magnitude, AmberColor)
            Spacer(Modifier.height(2.dp))
            SmallBar("FLUC", comp.fluctuation, BlueColor)
        }
    }
}

@Composable
fun SmallBar(label: String, value: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = DimColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(36.dp))
        Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(BorderColor)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth((value / 100f).coerceIn(0f,1f)).background(color))
        }
        Spacer(Modifier.width(4.dp))
        Text("${value.toInt()}%", color = DimColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(26.dp))
    }
}
