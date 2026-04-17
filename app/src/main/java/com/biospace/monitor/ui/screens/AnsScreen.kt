package com.biospace.monitor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
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
import com.biospace.monitor.model.ANSState
import com.biospace.monitor.model.SymptomPrediction
import com.biospace.monitor.ui.components.*
import com.biospace.monitor.ui.theme.*

@Composable
fun AnsScreen(ans: ANSState) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF060516))
            .border(1.dp, VioletColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        // Title
        Text("ANS LOAD ENGINE", color = VioletColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
        Text("AUTONOMIC NERVOUS SYSTEM · REAL-TIME BURDEN ANALYSIS", color = DimColor, fontSize = 8.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(14.dp))

        // Score + ring
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularGauge(
                fraction = ans.loadIndex / 100f,
                color = loadColor(ans.loadIndex),
                size = 100.dp,
                strokeWidth = 8f
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${ans.loadIndex}",
                        color = loadColor(ans.loadIndex), fontSize = 32.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black
                    )
                    Text("/100", color = DimColor, fontSize = 9.sp)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    when {
                        ans.loadIndex > 70 -> "CRITICAL LOAD"
                        ans.loadIndex > 50 -> "HIGH LOAD"
                        ans.loadIndex > 30 -> "MODERATE LOAD"
                        else -> "LOW LOAD"
                    },
                    color = loadColor(ans.loadIndex), fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text("ANS LOAD INDEX", color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                Text("Coherence: ${ans.coherencePct}%", color = DimColor, fontSize = 9.sp)
                Text("Sympathetic bias: ${ans.sympatheticBias.toInt()}%", color = DimColor, fontSize = 9.sp)
            }
        }

        Spacer(Modifier.height(14.dp))

        // Coherence field quality
        Text("ENVIRONMENTAL COHERENCE FIELD QUALITY", color = DimColor, fontSize = 9.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(8.dp))
        val cohColor = if (ans.coherencePct > 65) GreenColor else if (ans.coherencePct > 45) AmberColor else RedColor
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF040214))
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column {
                Text(
                    when {
                        ans.coherencePct > 75 -> "HIGH COHERENCE"
                        ans.coherencePct > 55 -> "ADEQUATE"
                        ans.coherencePct > 35 -> "DEGRADED"
                        else -> "CRITICAL DEGRADATION"
                    },
                    color = cohColor, fontSize = 12.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(4.dp))
                Text("FIELD QUALITY STATE", color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(6.dp))
                BioProgressBar(ans.coherencePct / 100f, cohColor)
            }
        }

        Spacer(Modifier.height(14.dp))

        // ANS Balance bar
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("◀ PARA (REST)", color = CyanColor, fontSize = 8.sp)
                Text("ANS BALANCE", color = AmberColor, fontSize = 8.sp)
                Text("SYMPATHETIC ▶", color = RedColor, fontSize = 8.sp)
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0A1222))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ans.sympatheticBias.toFloat() / 100f)
                        .fillMaxHeight()
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(CyanColor, AmberColor, RedColor)
                            )
                        )
                )
                Box(modifier = Modifier.align(Alignment.Center).width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.3f)))
            }
        }

        Spacer(Modifier.height(14.dp))

        // HRV / Cortisol / Melatonin metrics
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnsMetricBox("↓", "HRV IMPACT", ans.hrvImpact,
                if (ans.hrvImpact.contains("SUPPRESSED")) OrangeColor else if (ans.hrvImpact == "REDUCED") AmberColor else GreenColor,
                Modifier.weight(1f))
            AnsMetricBox("↑", "CORTISOL AXIS", ans.cortisolAxis,
                if (ans.cortisolAxis.contains("HIGH")) RedColor else if (ans.cortisolAxis == "ELEVATED") OrangeColor else GreenColor,
                Modifier.weight(1f))
            AnsMetricBox("↓", "MELATONIN", ans.melatonin,
                if (ans.melatonin == "SUPPRESSED") OrangeColor else if (ans.melatonin == "REDUCED") AmberColor else CyanColor,
                Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))

        // Symptoms
        Text("PROBABLE SYMPTOMS TODAY · RANKED BY DRIVER LOAD",
            color = DimColor, fontSize = 9.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(8.dp))

        ans.symptoms.take(6).forEach { symptom ->
            SymptomCard(symptom)
            Spacer(Modifier.height(6.dp))
        }

        Spacer(Modifier.height(8.dp))

        // Protocol
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF030C14))
                .border(1.dp, CyanColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column {
                Text("▸ MITIGATION PROTOCOL", color = CyanColor, fontSize = 9.sp, letterSpacing = 3.sp)
                Spacer(Modifier.height(8.dp))
                ans.protocols.forEach { item ->
                    Row(
                        modifier = Modifier.padding(bottom = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("▸", color = CyanColor, fontSize = 9.sp)
                        Text(item, color = DimColor, fontSize = 9.sp, lineHeight = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnsMetricBox(arrow: String, label: String, status: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF040214))
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(arrow, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = DimColor, fontSize = 7.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(2.dp))
        Text(status, color = color, fontSize = 7.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun SymptomCard(symptom: SymptomPrediction) {
    var expanded by remember { mutableStateOf(false) }
    val sevColor = when (symptom.severity) {
        "high" -> RedColor
        "moderate" -> AmberColor
        else -> GreenColor
    }
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF040214))
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                symptom.icon, fontSize = 16.sp,
                modifier = Modifier.width(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(symptom.name, color = TextColor, fontSize = 11.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    symptom.drivers.take(2).forEach { driver ->
                        Text(driver, color = DimColor, fontSize = 7.sp, letterSpacing = 1.sp)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${symptom.probability}%",
                    color = sevColor, fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                )
                Text(
                    symptom.severity.uppercase(),
                    color = sevColor, fontSize = 7.sp, letterSpacing = 1.sp
                )
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null, tint = DimColor, modifier = Modifier.size(16.dp)
            )
        }
        if (expanded) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(BorderColor))
            Text(
                symptom.mechanism,
                color = DimColor, fontSize = 8.sp, lineHeight = 13.sp,
                modifier = Modifier.padding(9.dp)
            )
            Row(modifier = Modifier.padding(9.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                symptom.drivers.forEach { driver ->
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(CyanColor.copy(alpha = 0.08f))
                            .border(1.dp, CyanColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(driver, color = CyanColor, fontSize = 7.sp)
                    }
                }
            }
        }
    }
}
