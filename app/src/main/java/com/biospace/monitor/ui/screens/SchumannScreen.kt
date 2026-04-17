package com.biospace.monitor.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biospace.monitor.model.SRMetrics
import com.biospace.monitor.model.SpaceWeatherState
import com.biospace.monitor.ui.components.*
import com.biospace.monitor.ui.theme.*
import kotlin.math.*

@Composable
fun SchumannScreen(sr: SRMetrics, sw: SpaceWeatherState) {
    Column {
        // ── Main SR Card ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF06080F))
                .border(1.dp, SrGoldColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            // Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "SCHUMANN RESONANCE",
                        color = SrGoldColor, fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                    Text(
                        "EARTH–IONOSPHERE CAVITY · GLOBAL ELF MONITOR",
                        color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LiveDot(SrGoldColor)
                    Text("LIVE", color = SrGoldColor, fontSize = 8.sp, letterSpacing = 2.sp)
                }
            }

            Spacer(Modifier.height(14.dp))

            // Primary freq readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        String.format("%.2f", sr.f1),
                        color = SrGoldColor, fontSize = 36.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black
                    )
                    Text("Hz", color = DimColor, fontSize = 10.sp)
                    Text("FUNDAMENTAL (f₁)", color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp)
                }
                Box(modifier = Modifier.width(1.dp).height(60.dp).background(BorderColor))
                Column {
                    Text(
                        "${if (sr.drift >= 0) "+" else ""}${String.format("%.3f", sr.drift)}",
                        color = if (abs(sr.drift) > 0.3) OrangeColor else AmberColor,
                        fontSize = 18.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                    )
                    Text("Hz DRIFT", color = DimColor, fontSize = 8.sp, letterSpacing = 1.sp)
                    Text(
                        if (abs(sr.drift) < 0.1) "STABLE" else if (abs(sr.drift) < 0.3) "⚠ ABOVE BASELINE" else "⚠ ELEVATED",
                        color = if (abs(sr.drift) > 0.3) OrangeColor else AmberColor, fontSize = 8.sp
                    )
                }
                Box(modifier = Modifier.width(1.dp).height(60.dp).background(BorderColor))
                Column {
                    Text(
                        String.format("%.2f", sr.amplitude),
                        color = SrGoldColor, fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                    )
                    Text("pT AMPLITUDE", color = DimColor, fontSize = 8.sp, letterSpacing = 1.sp)
                    Text(
                        if (sr.amplitude > 3) "▲ ELEVATED" else if (sr.amplitude > 2) "▲ ABOVE NORMAL" else "NORMAL",
                        color = if (sr.amplitude > 3) OrangeColor else AmberColor, fontSize = 8.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Animated waveform
            SrWaveform(sr, modifier = Modifier.fillMaxWidth().height(70.dp))
            Text(
                "AMPLITUDE HISTORY · 3HR  ·  SIMULATED CAVITY SIGNAL",
                color = DimColor, fontSize = 7.sp, letterSpacing = 1.sp
            )

            Spacer(Modifier.height(14.dp))

            // Three metrics
            Text(
                "CORE METRICS · ANS IMPACT QUANTIFICATION",
                color = DimColor, fontSize = 9.sp, letterSpacing = 3.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SrMetricCard(
                    "INTENSITY", String.format("%.2f", sr.intensity), "pT²/Hz",
                    bar = (sr.intensity / 5f).toFloat(),
                    statusText = if (sr.intensity > 3) "▲ HIGH" else if (sr.intensity > 1.5) "▲ ELEVATED" else "NORMAL",
                    color = if (sr.intensity > 3) OrangeColor else AmberColor,
                    modifier = Modifier.weight(1f)
                )
                SrMetricCard(
                    "FREQ DRIFT", "${if (sr.drift >= 0) "+" else ""}${String.format("%.3f", sr.drift)}", "Hz ABOVE 7.83",
                    bar = (abs(sr.drift) / 0.8f).toFloat(),
                    statusText = if (abs(sr.drift) > 0.5) "DISRUPTED" else if (abs(sr.drift) > 0.2) "THETA→α EDGE" else "STABLE",
                    color = if (abs(sr.drift) > 0.5) OrangeColor else AmberColor,
                    modifier = Modifier.weight(1f)
                )
                SrMetricCard(
                    "Q-FACTOR", String.format("%.1f", sr.qFactor), "COHERENCE",
                    bar = (sr.qFactor / 7f).toFloat(),
                    statusText = if (sr.qFactor < 3) "⚠ CRITICAL" else if (sr.qFactor < 4.5) "⚠ BROADBAND" else "SHARP",
                    color = if (sr.qFactor < 3) RedColor else if (sr.qFactor < 4.5) AmberColor else GreenColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))

            // TEC coupling
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF04081A))
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    .padding(11.dp)
            ) {
                Column {
                    Text("▸ TEC → CAVITY COUPLING", color = SrTealColor, fontSize = 9.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    val tecAnomaly = sw.tecLocal - sw.tecMedian
                    TecRow("LOCAL TEC", "${String.format("%.1f", sw.tecLocal)} TECU",
                        "${if (tecAnomaly >= 0) "+" else ""}${String.format("%.1f", tecAnomaly)} FROM MEDIAN →")
                    TecRow("CAVITY HEIGHT", if (tecAnomaly > 0) "COMPRESSED" else "NORMAL",
                        if (tecAnomaly > 0) "↑ f₁ SHIFT →" else "NEUTRAL →")
                    TecRow("SR COUPLING", if (sw.kp > 3) "ACTIVE" else "PASSIVE",
                        "Kp ${String.format("%.1f", sw.kp)} ${if (sw.kp > 3) "→ Q REDUCTION →" else "→ MINIMAL IMPACT →"}")
                }
            }

            Spacer(Modifier.height(14.dp))

            // Coherence score
            SrCoherenceBlock(sr, sw)
        }

        Spacer(Modifier.height(11.dp))

        // ── Biological Table ──────────────────────────────────────────────────
        BioCard {
            CardTitle("HARMONIC → ANS PATHWAY TABLE")
            HarmonicTable()
        }
    }
}

@Composable
private fun SrWaveform(sr: SRMetrics, modifier: Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "phase"
    )
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val midY = h / 2f
        val amp1 = (h * 0.35f) * (sr.amplitude / 3.0).toFloat().coerceIn(0.3f, 1f)
        val amp2 = amp1 * 0.5f
        val freq1 = sr.f1.toFloat() / 7.83f

        // f2 line (rose)
        val path2 = Path()
        for (i in 0..size.width.toInt()) {
            val x = i.toFloat()
            val y = midY - amp2 * sin(freq1 * 2 * (x / w) * 4 * PI.toFloat() + phase * 2)
            if (i == 0) path2.moveTo(x, y) else path2.lineTo(x, y)
        }
        drawPath(path2, SrRoseColor.copy(alpha = 0.6f), style = Stroke(1.2f, cap = StrokeCap.Round))

        // f1 line (gold)
        val path1 = Path()
        for (i in 0..size.width.toInt()) {
            val x = i.toFloat()
            val y = midY - amp1 * sin(freq1 * (x / w) * 4 * PI.toFloat() + phase)
            if (i == 0) path1.moveTo(x, y) else path1.lineTo(x, y)
        }
        drawPath(path1, SrGoldColor, style = Stroke(2f, cap = StrokeCap.Round))

        // Baseline
        drawLine(Color.White.copy(alpha = 0.1f), Offset(0f, midY), Offset(w, midY), strokeWidth = 0.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 5f)))
    }
}

@Composable
private fun SrMetricCard(
    label: String, value: String, unit: String,
    bar: Float, statusText: String, color: Color, modifier: Modifier
) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF04021A))
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .padding(9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = DimColor, fontSize = 7.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = color, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Text(unit, color = DimColor, fontSize = 7.sp)
        Spacer(Modifier.height(4.dp))
        BioProgressBar(bar.coerceIn(0f, 1f), color, height = 3.dp)
        Spacer(Modifier.height(4.dp))
        Text(statusText, color = color, fontSize = 7.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun TecRow(param: String, value: String, arrow: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(param, color = DimColor, fontSize = 9.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
        Text(value, color = GreenColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.width(8.dp))
        Text(arrow, color = DimColor, fontSize = 8.sp)
    }
}

@Composable
private fun SrCoherenceBlock(sr: SRMetrics, sw: SpaceWeatherState) {
    val cohColor = if (sr.coherenceScore > 65) GreenColor else if (sr.coherenceScore > 45) AmberColor else RedColor
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF040214))
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .padding(11.dp)
    ) {
        Text("▸ COHERENCE SCORE · FIELD QUALITY VS. ANS COMPENSATION THRESHOLD",
            color = SrGoldColor, fontSize = 8.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CircularGauge(sr.coherenceScore / 100f, cohColor, size = 80.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${sr.coherenceScore}", color = cohColor, fontSize = 22.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("/100", color = DimColor, fontSize = 8.sp)
                }
            }
            Column {
                Text(
                    when {
                        sr.coherenceScore > 75 -> "HIGH COHERENCE"
                        sr.coherenceScore > 55 -> "MODERATE COHERENCE"
                        sr.coherenceScore > 35 -> "DEGRADED · MODERATE ANS LOAD"
                        else -> "LOW COHERENCE · HIGH ANS LOAD"
                    }, color = cohColor, fontSize = 10.sp, letterSpacing = 2.sp
                )
                Spacer(Modifier.height(6.dp))
                Text("Intensity: ${String.format("%.2f", sr.intensity)} pT²/Hz", color = DimColor, fontSize = 8.sp)
                Text("Freq drift: ${if (sr.drift >= 0) "+" else ""}${String.format("%.3f", sr.drift)} Hz", color = DimColor, fontSize = 8.sp)
                Text("Q-factor: ${String.format("%.1f", sr.qFactor)}", color = DimColor, fontSize = 8.sp)
                Text("Bz stability: ${if (abs(sw.bz) < 2) "UNSTABLE" else "STABLE"}", color = DimColor, fontSize = 8.sp)
            }
        }
        if (sr.coherenceScore < 50) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(AmberColor.copy(alpha = 0.05f))
                    .border(1.dp, AmberColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(10.dp)
            ) {
                Text(
                    "⚠ COHERENCE THRESHOLD BREACH: Environmental noise (Low Q + Bz instability + TEC compression) currently exceeds estimated ANS compensation baseline. Expect elevated fatigue, reduced cognitive filtering, and heightened sympathetic tone.",
                    color = AmberColor, fontSize = 8.sp, lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
private fun HarmonicTable() {
    val rows = listOf(
        listOf("7.83", "7.0–8.5", "Theta/α", "HRV coherence · Sleep architecture · Parasympathetic baseline · Circadian rhythm anchor"),
        listOf("14.3", "13–15", "Low Beta/SMR", "Motor cortex excitability · Cortisol axis sensitization · Jaw clenching · Muscle tension"),
        listOf("20.8", "19–22", "Mid Beta", "Hypervigilance · Thought loop amplification · Sensory filter degradation · Anxiety tone"),
        listOf("26.4", "25–28", "High Beta", "Adrenaline axis · Tachycardia · Startle amplification · Vagal tone suppression"),
        listOf("33.0", "32–35", "Gamma boundary", "Sensory binding disruption · Tinnitus · Visual processing artifacts · Temporal disorientation"),
        listOf("39.5", "38–41", "Gamma", "Pineal axis · Melatonin suppression · Circadian phase shift · Compounded sleep debt")
    )
    val headers = listOf("f (Hz)", "RANGE", "BRAIN", "PRIMARY ANS EFFECT")
    val colors = listOf(SrGoldColor, SrRoseColor, CyanColor, GreenColor, VioletColor, AmberColor)

    Column {
        // Header
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp)) {
            Text(headers[0], color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp, modifier = Modifier.width(40.dp))
            Text(headers[1], color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp, modifier = Modifier.width(55.dp))
            Text(headers[2], color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp, modifier = Modifier.width(70.dp))
            Text(headers[3], color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(BorderColor))
        Spacer(Modifier.height(4.dp))
        rows.forEachIndexed { i, row ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text(row[0], color = colors[i], fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(40.dp))
                Text(row[1], color = DimColor, fontSize = 9.sp, modifier = Modifier.width(55.dp))
                Text(row[2], color = TextDimColor, fontSize = 8.sp, modifier = Modifier.width(70.dp))
                Text(row[3], color = DimColor, fontSize = 8.sp, lineHeight = 13.sp, modifier = Modifier.weight(1f))
            }
            if (i < rows.size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(BorderColor.copy(alpha = 0.5f)))
        }
    }
}
