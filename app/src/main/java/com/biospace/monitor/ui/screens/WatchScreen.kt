package com.biospace.monitor.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.biospace.monitor.ble.*
import com.biospace.monitor.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WatchScreen(repository: WatchRepository) {
    val context      = LocalContext.current
    val connState    by repository.ble.state.collectAsStateWithLifecycle()
    val latest       by repository.latest.collectAsStateWithLifecycle()
    val history      by repository.history.collectAsStateWithLifecycle()
    val sessions     by repository.sessions.collectAsStateWithLifecycle()
    val isEventMode  by repository.isEventMode.collectAsStateWithLifecycle()
    val logMessages  = remember { mutableStateListOf<String>() }
    var selectedTab  by remember { mutableStateOf(0) } // 0=Live, 1=History, 2=Reports

    LaunchedEffect(Unit) {
        repository.ble.log.collect { msg ->
            logMessages.add(0, msg)
            if (logMessages.size > 100) logMessages.removeLast()
        }
    }

    val blePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        rememberMultiplePermissionsState(listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        ))
    } else {
        rememberMultiplePermissionsState(listOf(Manifest.permission.ACCESS_FINE_LOCATION))
    }

    val notifPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberMultiplePermissionsState(listOf(Manifest.permission.POST_NOTIFICATIONS))
    } else null

    // Outer Column — NO verticalScroll here, just fills available space
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        Spacer(Modifier.height(11.dp))

        // ── Event mode banner ─────────────────────────────────────────────
        if (isEventMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A0A00))
                    .border(1.dp, Color(0xFFFF8C00), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    "⚡ SPACE WEATHER EVENT — CONTINUOUS RECORDING ACTIVE",
                    color = Color(0xFFFF8C00),
                    fontSize = 9.sp, letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── Connection card ───────────────────────────────────────────────
        WCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = when (connState) {
                            ConnectionState.DISCONNECTED -> "NOT CONNECTED"
                            ConnectionState.SCANNING     -> "SCANNING…"
                            ConnectionState.CONNECTING   -> "CONNECTING…"
                            ConnectionState.CONNECTED    -> "BP DOCTOR FIT"
                        },
                        color = when (connState) {
                            ConnectionState.CONNECTED    -> CyanColor
                            ConnectionState.DISCONNECTED -> Color(0xFFFF4444)
                            else                         -> Color(0xFFFFAA00)
                        },
                        fontSize = 11.sp, letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                    )
                    if (latest.battery != null) {
                        Text("Battery ${latest.battery}%", color = DimColor, fontSize = 9.sp, letterSpacing = 1.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (connState == ConnectionState.CONNECTED) {
                        WSmallButton("DISCONNECT", danger = true) { repository.stop() }
                    } else {
                        WSmallButton("CONNECT") {
                            if (blePermissions.allPermissionsGranted) {
                                repository.start()
                                repository.ble.scanAndConnect()
                            } else {
                                blePermissions.launchMultiplePermissionRequest()
                            }
                        }
                    }
                    notifPermissions?.let {
                        if (!it.allPermissionsGranted) {
                            WSmallButton("ENABLE ALERTS") { it.launchMultiplePermissionRequest() }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Sub-tab row ───────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("LIVE", "HISTORY", "REPORTS").forEachIndexed { i, label ->
                val active = selectedTab == i
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) Color(0xFF071828) else CardColor)
                        .border(1.dp, if (active) CyanColor else BorderColor, RoundedCornerShape(6.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { selectedTab = i }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (active) CyanColor else DimColor,
                        fontSize = 9.sp, letterSpacing = 2.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Each tab gets its own scrollable Column with fillMaxSize + weight
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> LiveTab(latest, logMessages)
                1 -> HistoryTab(history, repository)
                2 -> ReportsTab(sessions, repository, context)
            }
        }
    }
}

// ── Live Tab ──────────────────────────────────────────────────────────────────

@Composable
private fun LiveTab(latest: WatchSnapshot, logMessages: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        WLabel("CURRENT READINGS")
        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WTile(Modifier.weight(1f), Icons.Default.Favorite,      "HEART",       latest.heartRate?.toString() ?: "--",      "BPM",  Color(0xFFFF6B6B))
            WTile(Modifier.weight(1f), Icons.Default.MonitorHeart,  "BLOOD PRESS",
                if (latest.systolic != null && latest.diastolic != null) "${latest.systolic}/${latest.diastolic}" else "--",
                "mmHg", Color(0xFFFF8C42))
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WTile(Modifier.weight(1f), Icons.Default.Air,           "SPO2",        latest.spo2?.let { "$it" } ?: "--",        "%",    Color(0xFF64B5F6))
            WTile(Modifier.weight(1f), Icons.Default.Thermostat,    "TEMP",        latest.temperature?.let { "$it" } ?: "--", "°C",   Color(0xFFFFD54F))
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WTile(Modifier.weight(1f), Icons.Default.DirectionsWalk,"STEPS",       latest.steps?.toString() ?: "--",          "TODAY",Color(0xFF81C784))
            WTile(Modifier.weight(1f), Icons.Default.Psychology,    "STRESS",      latest.stress?.let { "$it" } ?: "--",      "/100", Color(0xFFCE93D8))
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WTile(Modifier.weight(1f), Icons.Default.Air,           "RESP RATE",   latest.respiratoryRate?.let { "$it" } ?: "--", "RPM", Color(0xFF80DEEA))
            Spacer(Modifier.weight(1f))
        }

        if (logMessages.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            WLabel("CONNECTION LOG")
            Spacer(Modifier.height(8.dp))
            WCard {
                logMessages.take(8).forEach { msg ->
                    Text("> $msg", color = DimColor, fontSize = 9.sp,
                        letterSpacing = 1.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

// ── History Tab ───────────────────────────────────────────────────────────────

@Composable
private fun HistoryTab(history: List<WatchSnapshot>, repository: WatchRepository) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            WLabel("HISTORY  (${history.size} readings)")
            if (history.isNotEmpty()) {
                WSmallButton("EXPORT CSV") {
                    val csv  = repository.exportCsv()
                    val file = java.io.File(context.getExternalFilesDir(null),
                        "biospace_watch_${System.currentTimeMillis()}.csv")
                    file.writeText(csv)
                    android.widget.Toast.makeText(context, "Saved: ${file.name}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        if (history.size >= 2) {
            WLabel("HR TIMELINE  (bpm)")
            Spacer(Modifier.height(6.dp))
            KpStyleGraph(
                values = history.takeLast(96).map { it.heartRate?.toFloat() ?: 0f },
                maxVal = 120f, minVal = 40f,
                color  = Color(0xFFFF6B6B)
            )
            Spacer(Modifier.height(10.dp))

            WLabel("BP TIMELINE  (systolic mmHg)")
            Spacer(Modifier.height(6.dp))
            KpStyleGraph(
                values = history.takeLast(96).map { it.systolic?.toFloat() ?: 0f },
                maxVal = 180f, minVal = 80f,
                color  = Color(0xFFFF8C42)
            )
            Spacer(Modifier.height(10.dp))

            WLabel("SPO2 TIMELINE  (%)")
            Spacer(Modifier.height(6.dp))
            KpStyleGraph(
                values = history.takeLast(96).map { it.spo2?.toFloat() ?: 0f },
                maxVal = 100f, minVal = 85f,
                color  = Color(0xFF64B5F6)
            )
            Spacer(Modifier.height(14.dp))
        }

        WLabel("RECENT SNAPSHOTS")
        Spacer(Modifier.height(8.dp))
        if (history.isEmpty()) {
            WCard { Text("No history yet. Connect your watch to start recording.", color = DimColor, fontSize = 10.sp) }
        } else {
            history.takeLast(10).reversed().forEach { snap ->
                HistoryRow(snap)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// ── Kp-style bar graph ────────────────────────────────────────────────────────

@Composable
private fun KpStyleGraph(values: List<Float>, maxVal: Float, minVal: Float, color: Color) {
    val nonZero = values.filter { it > 0f }
    if (nonZero.isEmpty()) return

    WCard {
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            values.forEach { v ->
                val fraction = if (v <= 0f) 0f else ((v - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                        .background(if (v > 0f) color.copy(alpha = 0.8f) else Color.Transparent)
                )
            }
        }
    }
}

// ── Reports Tab ───────────────────────────────────────────────────────────────

@Composable
private fun ReportsTab(sessions: List<CorrelationSession>, repository: WatchRepository, context: android.content.Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        WLabel("CORRELATION SESSIONS  (${sessions.size})")
        Spacer(Modifier.height(8.dp))

        if (sessions.isEmpty()) {
            WCard {
                Text(
                    "No correlation sessions yet. Sessions are recorded automatically when space weather events or biometric alerts occur.",
                    color = DimColor, fontSize = 10.sp, letterSpacing = 1.sp
                )
            }
        } else {
            val fmt = SimpleDateFormat("MM/dd HH:mm", Locale.US)
            sessions.reversed().forEach { session ->
                WCard {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(session.triggerDescription, color = CyanColor,
                                fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Text(fmt.format(Date(session.startTime)), color = DimColor, fontSize = 9.sp)
                            Text("${session.snapshots.size} readings", color = DimColor, fontSize = 9.sp)
                        }
                        WSmallButton("EXPORT") {
                            val report = repository.exportSessionReport(session)
                            val file   = java.io.File(context.getExternalFilesDir(null),
                                "correlation_${session.id}.txt")
                            file.writeText(report)
                            android.widget.Toast.makeText(context, "Saved: ${file.name}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun WLabel(text: String) {
    Text(text, color = DimColor, fontSize = 9.sp, letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
}

@Composable
private fun WCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardColor)
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun WatchCard(content: @Composable ColumnScope.() -> Unit) = WCard(content)

@Composable
private fun WTile(modifier: Modifier, icon: ImageVector, label: String, value: String, unit: String, color: Color) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CardColor)
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = DimColor, fontSize = 8.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(unit, color = DimColor, fontSize = 8.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun WSmallButton(label: String, danger: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (danger) Color(0xFF2A0A0A) else Color(0xFF071828))
            .border(1.dp, if (danger) Color(0xFFFF4444) else CyanColor, RoundedCornerShape(6.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (danger) Color(0xFFFF4444) else CyanColor,
            fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HistoryRow(snap: WatchSnapshot) {
    val fmt = remember { SimpleDateFormat("MM/dd HH:mm", Locale.US) }
    val triggerColor = when (snap.triggerType) {
        TriggerType.SPACE_WEATHER_EVENT -> Color(0xFFFF8C00)
        TriggerType.BIOMETRIC_ALERT     -> Color(0xFFFF4444)
        TriggerType.SCHEDULED           -> DimColor
    }
    WCard {
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(fmt.format(Date(snap.timestamp)), color = DimColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text(snap.triggerType.name, color = triggerColor, fontSize = 7.sp, letterSpacing = 1.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                snap.heartRate?.let  { MiniStat("HR",   "$it",                      Color(0xFFFF6B6B)) }
                if (snap.systolic != null && snap.diastolic != null)
                                     MiniStat("BP",  "${snap.systolic}/${snap.diastolic}", Color(0xFFFF8C42))
                snap.spo2?.let       { MiniStat("O2",   "$it%",                     Color(0xFF64B5F6)) }
                snap.stress?.let     { MiniStat("STR",  "$it",                      Color(0xFFCE93D8)) }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = DimColor, fontSize = 7.sp, letterSpacing = 1.sp)
        Text(value, color = color, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}
