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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.biospace.monitor.model.*
import com.biospace.monitor.ui.components.*
import com.biospace.monitor.ui.theme.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// ═══════════════════════════════════════════════════════
//  CME TRACKER
// ═══════════════════════════════════════════════════════
@Composable
fun CmeScreen(cmeEvents: List<CmeEvent>) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF060510))
            .border(1.dp, OrangeColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text("CME TRACKER", color = OrangeColor, fontSize = 13.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Text("CORONAL MASS EJECTIONS · SOLAR ENERGETIC PARTICLE EVENTS · DIRECT ANS INFLUENCE",
            color = DimColor, fontSize = 8.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(14.dp))

        if (cmeEvents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("NO CME EVENTS IN PAST 7 DAYS", color = DimColor, fontSize = 11.sp, letterSpacing = 2.sp)
            }
        } else {
            cmeEvents.forEach { cme ->
                CmeEventCard(cme)
                Spacer(Modifier.height(10.dp))
            }
        }

        SectionDivider()

        // Influence pathways
        Text("▸ CME / SOLAR STORM → ANS INFLUENCE PATHWAYS",
            color = OrangeColor, fontSize = 9.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(10.dp))

        val pathways = listOf(
            Triple("ROUTE 1 · MAGNETIC", "IMF DISRUPTION",
                "CME compresses magnetosphere → Sudden Commencement → Rapid Bz southward swing → SR cavity disruption → ANS sympathetic loading"),
            Triple("ROUTE 2 · IONOSPHERIC", "TEC SURGE",
                "CME proton flux → Ionospheric ionization spike → TEC +15–40 TECU → f₁ upshift → Accelerated ELF → Disrupted brainwave entrainment"),
            Triple("ROUTE 3 · GEOMAGNETIC", "Kp ≥ 5 STORM",
                "Kp spike triggers documented melatonin suppression, cortisol elevation, HRV decline within 2-4 hrs. Effect peaks at 6-12hr post-onset."),
            Triple("ROUTE 4 · BARIC", "SEP PRESSURE",
                "Solar Energetic Particles penetrate stratosphere → Odd nitrogen production → Ozone chemistry changes → Atmospheric pressure coupling → Barometric POTS triggers")
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            pathways.chunked(2).forEach { pair ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { (label, value, desc) ->
                        Column(
                            modifier = Modifier.weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF040A18))
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(label, color = DimColor, fontSize = 8.sp, letterSpacing = 1.sp)
                            Text(value, color = OrangeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(desc, color = DimColor, fontSize = 7.sp, lineHeight = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CmeEventCard(cme: CmeEvent) {
    val analysis = cme.cmeAnalyses?.firstOrNull { it.isMostAccurate } ?: cme.cmeAnalyses?.firstOrNull()
    val speed = analysis?.speed
    val type = analysis?.type ?: "CME"
    val color = when {
        speed != null && speed > 1500 -> RedColor
        speed != null && speed > 800 -> OrangeColor
        else -> CyanColor
    }
    val hasSEP = cme.linkedEvents?.any { it.activityID.contains("SEP") } == true

    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF040A18))
            .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .border(leftBorderWidth = 3.dp, color = color, shape = RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(11.dp)) {
            Text(
                cme.activityID,
                color = color, fontSize = 9.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(type, color = TextColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (hasSEP) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                        .background(RedColor.copy(alpha = 0.15f))
                        .border(1.dp, RedColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)) {
                        Text("SEP", color = RedColor, fontSize = 7.sp)
                    }
                }
            }
            Spacer(Modifier.height(5.dp))
            if (speed != null) Text("Speed: ${speed.toInt()} km/s", color = DimColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text("Start: ${cme.startTime.take(16)}", color = DimColor, fontSize = 9.sp)
            if (cme.note.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(cme.note.take(200), color = DimColor, fontSize = 8.sp, lineHeight = 13.sp)
            }
        }
    }
}

// Extension for left border only — workaround
@Composable
private fun Modifier.border(leftBorderWidth: androidx.compose.ui.unit.Dp, color: Color, shape: RoundedCornerShape): Modifier = this

// ═══════════════════════════════════════════════════════
//  ALERTS SCREEN
// ═══════════════════════════════════════════════════════
@Composable
fun AlertsScreen(alerts: List<NoaaAlert>) {
    BioCard {
        CardTitle("NOAA SPACE WEATHER ALERTS")
        if (alerts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("NO ACTIVE SPACE WEATHER ALERTS", color = DimColor, fontSize = 11.sp, letterSpacing = 2.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                alerts.take(8).forEach { alert ->
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF040910))
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(alert.productId.ifBlank { "ALERT" },
                            color = AmberColor, fontSize = 10.sp, letterSpacing = 1.sp)
                        Spacer(Modifier.height(5.dp))
                        Text(alert.message.take(320),
                            color = DimColor, fontSize = 8.sp, lineHeight = 14.sp)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
//  INTEGRATED ASSESSMENT SCREEN
// ═══════════════════════════════════════════════════════
@Composable
fun AssessmentScreen(assessment: IntegratedAssessment) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF060216))
            .border(1.dp, VioletColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text("INTEGRATED ASSESSMENT", color = VioletColor, fontSize = 12.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
        Text("SYNTHESIZED BODY BURDEN ANALYSIS · ALL DATA SOURCES",
            color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(14.dp))

        // Threat score
        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF04021A))
                .border(1.dp, VioletColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "${assessment.score}",
                color = loadColor(assessment.score), fontSize = 52.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black
            )
            Column {
                Text(assessment.label, color = loadColor(assessment.score), fontSize = 13.sp,
                    letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
                Text("INTEGRATED BODY BURDEN INDEX", color = DimColor, fontSize = 8.sp,
                    letterSpacing = 2.sp, lineHeight = 14.sp)
                Text("Space + SR + Environment", color = DimColor, fontSize = 8.sp)
            }
        }

        Spacer(Modifier.height(14.dp))

        // Driver breakdown
        Text("LOAD DRIVERS", color = DimColor, fontSize = 9.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DriverBox("SPACE\nWEATHER", assessment.spaceScore, 40, CyanColor, Modifier.weight(1f))
            DriverBox("SCHUMANN\nRESONANCE", assessment.srScore, 30, SrGoldColor, Modifier.weight(1f))
            DriverBox("LOCAL\nENVIRONMENT", assessment.envScore, 30, GreenColor, Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))

        // Narrative
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF050318))
                .border(1.dp, VioletColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(13.dp)
        ) {
            Column {
                Text("▸ CLINICAL NARRATIVE", color = VioletColor, fontSize = 9.sp, letterSpacing = 3.sp)
                Spacer(Modifier.height(8.dp))
                Text(assessment.narrative, color = TextColor, fontSize = 10.sp, lineHeight = 17.sp)
            }
        }

        Spacer(Modifier.height(14.dp))

        // Protocols
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF030C14))
                .border(1.dp, CyanColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column {
                Text("▸ MANAGEMENT PROTOCOL", color = CyanColor, fontSize = 9.sp, letterSpacing = 3.sp)
                Spacer(Modifier.height(8.dp))
                assessment.protocols.forEach { item ->
                    Row(modifier = Modifier.padding(bottom = 5.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("▸", color = CyanColor, fontSize = 9.sp)
                        Text(item, color = DimColor, fontSize = 9.sp, lineHeight = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DriverBox(label: String, score: Int, maxScore: Int, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF040214))
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = DimColor, fontSize = 7.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        Text("$score", color = loadColor(score * 100 / maxScore.coerceAtLeast(1)),
            fontSize = 20.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Text("/$maxScore", color = DimColor, fontSize = 8.sp)
        Spacer(Modifier.height(4.dp))
        BioProgressBar((score.toFloat() / maxScore), color, height = 3.dp)
    }
}

// ═══════════════════════════════════════════════════════
//  SOLAR IMAGES SCREEN
// ═══════════════════════════════════════════════════════
private data class SolarImage(val url: String, val label: String, val desc: String)

private val SOLAR_IMAGES = listOf(
    SolarImage("https://sdo.gsfc.nasa.gov/assets/img/latest/latest_1024_0171.jpg", "SDO AIA 171Å", "Coronal loops · 600,000K · EUV"),
    SolarImage("https://sdo.gsfc.nasa.gov/assets/img/latest/latest_1024_0304.jpg", "SDO AIA 304Å", "Chromosphere · 50,000K · Filaments"),
    SolarImage("https://sdo.gsfc.nasa.gov/assets/img/latest/latest_1024_0193.jpg", "SDO AIA 193Å", "Corona · Flare locations · 1.2MK"),
    SolarImage("https://sdo.gsfc.nasa.gov/assets/img/latest/latest_1024_0094.jpg", "SDO AIA 094Å", "Hot coronal loops · Flare plasma · 6.3MK"),
    SolarImage("https://sdo.gsfc.nasa.gov/assets/img/latest/latest_1024_HMIi.jpg", "SDO HMI Intensitygram", "Photospheric sunspot structure"),
    SolarImage("https://sdo.gsfc.nasa.gov/assets/img/latest/latest_1024_HMIB.jpg", "SDO HMI Magnetogram", "Magnetic field polarity map"),
    SolarImage("https://soho.nascom.nasa.gov/data/realtime/c2/1024/latest.jpg", "LASCO C2", "White light coronagraph · 2–6 R☉"),
    SolarImage("https://soho.nascom.nasa.gov/data/realtime/c3/1024/latest.jpg", "LASCO C3", "Wide field coronagraph · CME detection"),
    SolarImage("https://sdo.gsfc.nasa.gov/assets/img/latest/latest_1024_0211.jpg", "SDO AIA 211Å", "Active regions · 2MK coronal plasma")
)

@Composable
fun SolarImagesScreen() {
    BioCard {
        CardTitle("SOLAR IMAGERY", "// LIVE SDO · SOHO")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SOLAR_IMAGES.chunked(2).forEach { pair ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { img ->
                        Column(
                            modifier = Modifier.weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CardColor)
                                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                        ) {
                            AsyncImage(
                                model = img.url,
                                contentDescription = img.label,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                            )
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(img.label, color = TextColor, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                Text(img.desc, color = DimColor, fontSize = 7.sp)
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
//  CHAT SCREEN
// ═══════════════════════════════════════════════════════
@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    onSend: (text: String, nick: String) -> Unit
) {
    var nick by remember { mutableStateOf("ANON") }
    var text by remember { mutableStateOf("") }

    BioCard {
        CardTitle("BIOSPACE CHANNEL", "// LOCAL DEVICE")
        
        // Messages
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 200.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            messages.takeLast(20).forEach { msg ->
                ChatBubble(msg)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Input
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = nick,
                onValueChange = { nick = it.uppercase().take(12) },
                modifier = Modifier.width(80.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = CyanColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                placeholder = { Text("NICK", color = DimColor, fontSize = 9.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanColor.copy(alpha = 0.55f),
                    unfocusedBorderColor = BorderColor
                )
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(color = TextColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                placeholder = { Text("TYPE MESSAGE...", color = DimColor, fontSize = 9.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanColor.copy(alpha = 0.55f),
                    unfocusedBorderColor = BorderColor
                )
            )
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onSend(text, nick)
                        text = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF071828)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanColor.copy(alpha = 0.55f))
            ) {
                Text("SEND", color = CyanColor, fontSize = 9.sp, letterSpacing = 2.sp)
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage) {
    val timeStr = remember(msg.timestamp) {
        java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date(msg.timestamp))
    }
    if (msg.isSystem) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(msg.text, color = DimColor, fontSize = 8.sp, letterSpacing = 1.sp)
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (msg.mine) Arrangement.End else Arrangement.Start
        ) {
            Column(horizontalAlignment = if (msg.mine) Alignment.End else Alignment.Start) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (msg.mine) CyanColor.copy(alpha = 0.12f) else Color(0xFF071828)
                        )
                        .border(1.dp, if (msg.mine) CyanColor.copy(alpha = 0.3f) else BorderColor,
                            RoundedCornerShape(10.dp))
                        .padding(horizontal = 11.dp, vertical = 7.dp)
                ) {
                    Text(msg.text, color = TextColor, fontSize = 10.sp)
                }
                Text(
                    "${if (!msg.mine) "${msg.nick} · " else ""}$timeStr",
                    color = DimColor, fontSize = 7.sp, letterSpacing = 1.sp
                )
            }
        }
    }
}
