package com.biospace.monitor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.biospace.monitor.ble.WatchBleManager.ConnectionState
import com.biospace.monitor.ble.WatchProtocol.WatchReading
import com.biospace.monitor.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

private val CardBg    = Color(0xFF1A1A2E)
private val AccentRed = Color(0xFFE94560)
private val AccentTeal = Color(0xFF00B4D8)
private val AccentGreen = Color(0xFF4CAF50)
private val AccentAmber = Color(0xFFFFAB00)
private val AccentPurple = Color(0xFF9C27B0)
private val AccentOrange = Color(0xFFFF5722)

@Composable
fun WatchScreen(vm: MainViewModel) {
    val connection by vm.watchConnectionState.collectAsState()
    val bp         by vm.bloodPressure.collectAsState()
    val hr         by vm.heartRate.collectAsState()
    val spo2       by vm.spO2.collectAsState()
    val steps      by vm.steps.collectAsState()
    val sleep      by vm.sleep.collectAsState()
    val stress     by vm.stress.collectAsState()
    val temp       by vm.temperature.collectAsState()
    val immunity   by vm.immunity.collectAsState()
    val battery    by vm.watchBattery.collectAsState()
    val device     by vm.watchDevice.collectAsState()

    val bpHistory    by vm.bpHistory.collectAsState()
    val hrHistory    by vm.hrHistory.collectAsState()
    val spo2History  by vm.spo2History.collectAsState()
    val sleepHistory by vm.sleepHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F1A))
            .padding(horizontal = 12.dp)
    ) {
        // ── Header ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("BP Doctor Watch", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    device?.address ?: "Not paired",
                    color = Color.Gray, fontSize = 12.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (battery >= 0) {
                    Text("🔋 $battery%", color = Color.White, fontSize = 13.sp)
                }
                ConnectionButton(connection, vm)
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ── Blood Pressure ────────────────────────────────────────────
            item {
                MetricCard(
                    title = "Blood Pressure",
                    icon = "🩸",
                    color = AccentRed
                ) {
                    bp?.let {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
                            BigValue("${it.systolic}", "mmHg", "SYS")
                            Text("/", color = Color.White, fontSize = 28.sp)
                            BigValue("${it.diastolic}", "mmHg", "DIA")
                        }
                        Spacer(Modifier.height(4.dp))
                        BpCategory(it.systolic, it.diastolic)
                        Text(formatTime(it.timestampMs), color = Color.Gray, fontSize = 11.sp)
                    } ?: EmptyState("Waiting for reading…")

                    if (bpHistory.size > 1) {
                        Spacer(Modifier.height(8.dp))
                        Text("History", color = Color.Gray, fontSize = 11.sp)
                        MiniHistoryRow(bpHistory.takeLast(8).map { "${it.systolic}/${it.diastolic}" })
                    }
                }
            }

            // ── Heart Rate ────────────────────────────────────────────────
            item {
                MetricCard("Heart Rate", "❤️", AccentRed) {
                    hr?.let {
                        BigValue("${it.bpm}", "BPM", "Heart Rate")
                        HrCategory(it.bpm)
                        Text(formatTime(it.timestampMs), color = Color.Gray, fontSize = 11.sp)
                    } ?: EmptyState("Waiting for reading…")

                    if (hrHistory.size > 1) {
                        Spacer(Modifier.height(8.dp))
                        MiniHistoryRow(hrHistory.takeLast(8).map { "${it.bpm} bpm" })
                    }
                }
            }

            // ── SpO2 ──────────────────────────────────────────────────────
            item {
                MetricCard("Blood Oxygen (SpO₂)", "💨", AccentTeal) {
                    spo2?.let {
                        BigValue("${it.percent}", "%", "SpO₂")
                        Spo2Category(it.percent)
                        Text(formatTime(it.timestampMs), color = Color.Gray, fontSize = 11.sp)
                    } ?: EmptyState("Waiting for reading…")

                    if (spo2History.size > 1) {
                        Spacer(Modifier.height(8.dp))
                        MiniHistoryRow(spo2History.takeLast(8).map { "${it.percent}%" })
                    }
                }
            }

            // ── Steps + Calories ──────────────────────────────────────────
            item {
                MetricCard("Activity", "👟", AccentGreen) {
                    steps?.let {
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            BigValue("${it.count}", "steps", "Steps")
                            BigValue("${it.kcal}", "kcal", "Calories")
                        }
                        Text(formatTime(it.timestampMs), color = Color.Gray, fontSize = 11.sp)
                    } ?: EmptyState("Waiting for reading…")
                }
            }

            // ── Sleep ─────────────────────────────────────────────────────
            item {
                MetricCard("Sleep", "🌙", AccentPurple) {
                    if (sleepHistory.isNotEmpty()) {
                        val deepMins  = sleepHistory.filter { it.type == 2 }.sumOf { it.durationMinutes }
                        val lightMins = sleepHistory.filter { it.type == 1 }.sumOf { it.durationMinutes }
                        val awakeMins = sleepHistory.filter { it.type == 0 }.sumOf { it.durationMinutes }
                        val total     = deepMins + lightMins
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            BigValue("${total / 60}h ${total % 60}m", "", "Total")
                            BigValue("${deepMins}m", "", "Deep")
                            BigValue("${lightMins}m", "", "Light")
                            BigValue("${awakeMins}m", "", "Awake")
                        }
                    } else {
                        sleep?.let {
                            val typeName = when (it.type) { 2 -> "Deep Sleep" 1 -> "Light Sleep" else -> "Awake" }
                            BigValue("${it.durationMinutes}m", "", typeName)
                        } ?: EmptyState("Waiting for reading…")
                    }
                }
            }

            // ── Stress ────────────────────────────────────────────────────
            item {
                MetricCard("Stress", "🧠", AccentAmber) {
                    stress?.let {
                        BigValue("${it.score}", "/100", "Stress Index")
                        StressCategory(it.score)
                        Text(formatTime(it.timestampMs), color = Color.Gray, fontSize = 11.sp)
                    } ?: EmptyState("Waiting for reading…")
                }
            }

            // ── Temperature ───────────────────────────────────────────────
            item {
                MetricCard("Body Temperature", "🌡️", AccentOrange) {
                    temp?.let {
                        BigValue("${"%.1f".format(it.celsius)}°C", "", "Temp")
                        val f = it.celsius * 9f / 5f + 32f
                        Text("${"%.1f".format(f)}°F", color = Color.Gray, fontSize = 13.sp)
                        Text(formatTime(it.timestampMs), color = Color.Gray, fontSize = 11.sp)
                    } ?: EmptyState("Waiting for reading…")
                }
            }

            // ── Immunity ──────────────────────────────────────────────────
            item {
                MetricCard("Immunity", "🛡️", AccentTeal) {
                    immunity?.let {
                        BigValue("${it.score}", "/100", "Immunity Score")
                        ImmunityCategory(it.score)
                        Text(formatTime(it.timestampMs), color = Color.Gray, fontSize = 11.sp)
                    } ?: EmptyState("Waiting for reading…")
                }
            }
        }
    }
}

// ─── Connection button ───────────────────────────────────────────────────────
@Composable
private fun ConnectionButton(state: ConnectionState, vm: MainViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val (label, color) = when (state) {
        ConnectionState.DISCONNECTED -> Pair("Connect", AccentTeal)
        ConnectionState.SCANNING     -> Pair("Scanning…", AccentAmber)
        ConnectionState.CONNECTING   -> Pair("Connecting…", AccentAmber)
        ConnectionState.CONNECTED    -> Pair("Connected ✓", AccentGreen)
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) vm.connectWatch()
    }
    Button(
        onClick = {
            when (state) {
                ConnectionState.DISCONNECTED -> {
                    val perms = mutableListOf(
                        android.Manifest.permission.BLUETOOTH_SCAN,
                        android.Manifest.permission.BLUETOOTH_CONNECT,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    val needed = perms.filter {
                        androidx.core.content.ContextCompat.checkSelfPermission(context, it) !=
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                    }
                    if (needed.isEmpty()) vm.connectWatch()
                    else permLauncher.launch(needed.toTypedArray())
                }
                ConnectionState.CONNECTED -> vm.disconnectWatch()
                else -> {}
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 12.sp, color = Color.White)
    }
}

// ─── Reusable card ────────────────────────────────────────────────────────────
@Composable
private fun MetricCard(title: String, icon: String, color: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text(icon, fontSize = 18.sp) }
                Spacer(Modifier.width(10.dp))
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun BigValue(value: String, unit: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(4.dp))
                Text(unit, color = Color.Gray, fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 6.dp))
            }
        }
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
private fun MiniHistoryRow(items: List<String>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(items) { label ->
            Text(
                label,
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier
                    .background(Color(0xFF2A2A3E), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun EmptyState(msg: String) {
    Text(msg, color = Color.Gray, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center)
}

// ─── Category labels ──────────────────────────────────────────────────────────
@Composable
private fun BpCategory(sys: Int, dia: Int) {
    val (label, color) = when {
        sys < 120 && dia < 80  -> "Normal" to AccentGreen
        sys < 130 && dia < 80  -> "Elevated" to AccentAmber
        sys < 140 || dia < 90  -> "High Stage 1" to AccentOrange
        else                   -> "High Stage 2" to AccentRed
    }
    CategoryChip(label, color)
}

@Composable
private fun HrCategory(bpm: Int) {
    val (label, color) = when {
        bpm < 60  -> "Bradycardia" to AccentTeal
        bpm <= 100 -> "Normal" to AccentGreen
        bpm <= 150 -> "Elevated" to AccentAmber
        else       -> "High" to AccentRed
    }
    CategoryChip(label, color)
}

@Composable
private fun Spo2Category(pct: Int) {
    val (label, color) = when {
        pct >= 95  -> "Normal" to AccentGreen
        pct >= 90  -> "Low" to AccentAmber
        else       -> "Critical" to AccentRed
    }
    CategoryChip(label, color)
}

@Composable
private fun StressCategory(score: Int) {
    val (label, color) = when {
        score <= 29 -> "Relaxed" to AccentGreen
        score <= 59 -> "Normal" to AccentTeal
        score <= 79 -> "Elevated" to AccentAmber
        else        -> "High Stress" to AccentRed
    }
    CategoryChip(label, color)
}

@Composable
private fun ImmunityCategory(score: Int) {
    val (label, color) = when {
        score >= 80 -> "Strong" to AccentGreen
        score >= 60 -> "Average" to AccentAmber
        else        -> "Low" to AccentRed
    }
    CategoryChip(label, color)
}

@Composable
private fun CategoryChip(label: String, color: Color) {
    Text(
        label,
        color = color,
        fontSize = 11.sp,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

private fun formatTime(ms: Long): String {
    if (ms == 0L) return ""
    return SimpleDateFormat("MMM d, h:mm a", Locale.US).format(Date(ms))
}
