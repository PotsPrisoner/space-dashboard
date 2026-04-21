package com.biospace.monitor.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.biospace.monitor.ble.WatchBleManager.ConnectionState
import com.biospace.monitor.ui.MainViewModel
import com.biospace.monitor.ui.components.NeonSpeedometer
import com.biospace.monitor.ui.components.CircularGauge
import com.biospace.monitor.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── DataStore ────────────────────────────────────────────────────────────────
private val Context.vitalsStore by preferencesDataStore("manual_vitals")
private val KEY_SYS    = intPreferencesKey("sys")
private val KEY_DIA    = intPreferencesKey("dia")
private val KEY_HR     = intPreferencesKey("hr")
private val KEY_SPO2   = intPreferencesKey("spo2")
private val KEY_STRESS = intPreferencesKey("stress")
private val KEY_RESP   = intPreferencesKey("resp")

@Composable
fun WatchScreen(vm: MainViewModel) {
    val context      = LocalContext.current
    val scope        = rememberCoroutineScope()
    val connection   by vm.watchConnectionState.collectAsState()
    val bp           by vm.bloodPressure.collectAsState()
    val hr           by vm.heartRate.collectAsState()
    val spo2         by vm.spO2.collectAsState()
    val steps        by vm.steps.collectAsState()
    val sleep        by vm.sleep.collectAsState()
    val stress       by vm.stress.collectAsState()
    val respiration  by vm.respiration.collectAsState()
    val battery      by vm.watchBattery.collectAsState()
    val device       by vm.watchDevice.collectAsState()
    val sleepHistory by vm.sleepHistory.collectAsState()

    val isConnected = connection == ConnectionState.CONNECTED

    // ── DataStore manual values ──────────────────────────────────────────────
    val prefs by context.vitalsStore.data.collectAsState(initial = null)
    var manualSys    by remember { mutableStateOf(0) }
    var manualDia    by remember { mutableStateOf(0) }
    var manualHr     by remember { mutableStateOf(0) }
    var manualSpo2   by remember { mutableStateOf(0) }
    var manualStress by remember { mutableStateOf(0) }
    var manualResp   by remember { mutableStateOf(0) }

    LaunchedEffect(prefs) {
        prefs?.let {
            manualSys    = it[KEY_SYS]    ?: 0
            manualDia    = it[KEY_DIA]    ?: 0
            manualHr     = it[KEY_HR]     ?: 0
            manualSpo2   = it[KEY_SPO2]   ?: 0
            manualStress = it[KEY_STRESS] ?: 0
            manualResp   = it[KEY_RESP]   ?: 0
        }
    }

    // ── Effective values — watch always takes priority ───────────────────────
    val hasWatchBp     = isConnected && bp != null
    val hasWatchHr     = isConnected && hr != null
    val hasWatchSpo2   = isConnected && spo2 != null
    val hasWatchStress = isConnected && stress != null
    val hasWatchResp   = isConnected && respiration != null

    val effSys    = if (hasWatchBp)     bp!!.systolic               else manualSys
    val effDia    = if (hasWatchBp)     bp!!.diastolic              else manualDia
    val effHr     = if (hasWatchHr)     hr!!.bpm                    else manualHr
    val effSpo2   = if (hasWatchSpo2)   spo2!!.percent              else manualSpo2
    val effStress = if (hasWatchStress) stress!!.score              else manualStress
    val effResp   = if (hasWatchResp)   respiration!!.breathsPerMin else manualResp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(horizontal = 12.dp)
    ) {
        // ── Header ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "WATCH",
                    color = CyanColor,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 4.sp
                )
                Text(
                    device?.address ?: "NOT PAIRED",
                    color = DimColor,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (battery >= 0) {
                    Text(
                        "$battery%",
                        color = if (battery > 20) GreenColor else RedColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if (isConnected) {
                    TextButton(
                        onClick = { vm.refreshWatch() },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "SYNC",
                            color = CyanColor,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                    }
                }
                ConnectionButton(connection, vm)
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            // ── Manual input panel (disconnected only) ───────────────────────
            if (!isConnected) {
                item {
                    WatchCard("MANUAL INPUT") {
                        Text(
                            "WATCH DISCONNECTED — ENTER VALUES MANUALLY",
                            color = AmberColor,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ManualField("SYS", manualSys, Modifier.weight(1f)) { v ->
                                manualSys = v
                                scope.launch { context.vitalsStore.edit { it[KEY_SYS] = v } }
                            }
                            ManualField("DIA", manualDia, Modifier.weight(1f)) { v ->
                                manualDia = v
                                scope.launch { context.vitalsStore.edit { it[KEY_DIA] = v } }
                            }
                            ManualField("HR", manualHr, Modifier.weight(1f)) { v ->
                                manualHr = v
                                scope.launch { context.vitalsStore.edit { it[KEY_HR] = v } }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ManualField("SPO2 %", manualSpo2, Modifier.weight(1f)) { v ->
                                manualSpo2 = v
                                scope.launch { context.vitalsStore.edit { it[KEY_SPO2] = v } }
                            }
                            ManualField("STRESS", manualStress, Modifier.weight(1f)) { v ->
                                manualStress = v
                                scope.launch { context.vitalsStore.edit { it[KEY_STRESS] = v } }
                            }
                            ManualField("RESP", manualResp, Modifier.weight(1f)) { v ->
                                manualResp = v
                                scope.launch { context.vitalsStore.edit { it[KEY_RESP] = v } }
                            }
                        }
                    }
                }
            }

            // ── Blood Pressure ───────────────────────────────────────────────
            item {
                WatchCard("BLOOD PRESSURE") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeonSpeedometer(
                            fraction = ((effSys - 80) / 120f).coerceIn(0f, 1f),
                            value = if (effSys > 0) "$effSys" else "—",
                            unit = "mmHg",
                            color = bpColor(effSys),
                            size = 110.dp,
                            minLabel = "80",
                            maxLabel = "200"
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "/",
                                color = DimColor,
                                fontSize = 24.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(Modifier.height(4.dp))
                            SourceBadge(hasWatchBp)
                        }
                        NeonSpeedometer(
                            fraction = ((effDia - 40) / 80f).coerceIn(0f, 1f),
                            value = if (effDia > 0) "$effDia" else "—",
                            unit = "mmHg",
                            color = bpColor(effDia),
                            size = 110.dp,
                            minLabel = "40",
                            maxLabel = "120"
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text("SYS", color = DimColor, fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                        Text("DIA", color = DimColor, fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                    }
                    if (hasWatchBp) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            formatTime(bp!!.timestampMs),
                            color = DimColor,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // ── Heart Rate ───────────────────────────────────────────────────
            item {
                WatchCard("HEART RATE") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeonSpeedometer(
                            fraction = ((effHr - 30) / 170f).coerceIn(0f, 1f),
                            value = if (effHr > 0) "$effHr" else "—",
                            unit = "bpm",
                            color = hrColor(effHr),
                            size = 130.dp,
                            minLabel = "30",
                            maxLabel = "200"
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(start = 16.dp)
                        ) {
                            SourceBadge(hasWatchHr)
                            if (effHr > 0) {
                                Spacer(Modifier.height(8.dp))
                                HrCategoryLabel(effHr)
                            }
                            if (hasWatchHr) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    formatTime(hr!!.timestampMs),
                                    color = DimColor,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // ── SpO2 ─────────────────────────────────────────────────────────
            item {
                WatchCard("BLOOD OXYGEN  SpO\u2082") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeonSpeedometer(
                            fraction = ((effSpo2 - 80) / 20f).coerceIn(0f, 1f),
                            value = if (effSpo2 > 0) "$effSpo2" else "—",
                            unit = "%",
                            color = spo2Color(effSpo2),
                            size = 130.dp,
                            minLabel = "80",
                            maxLabel = "100"
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(start = 16.dp)
                        ) {
                            SourceBadge(hasWatchSpo2)
                            if (effSpo2 > 0) {
                                Spacer(Modifier.height(8.dp))
                                Spo2CategoryLabel(effSpo2)
                            }
                            if (hasWatchSpo2) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    formatTime(spo2!!.timestampMs),
                                    color = DimColor,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // ── Stress ───────────────────────────────────────────────────────
            item {
                WatchCard("STRESS INDEX") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularGauge(
                            fraction = (effStress / 100f).coerceIn(0f, 1f),
                            value = if (effStress > 0) "$effStress" else "—",
                            label = "/ 100",
                            color = stressColor(effStress),
                            size = 120.dp
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(start = 16.dp)
                        ) {
                            SourceBadge(hasWatchStress)
                            if (effStress > 0) {
                                Spacer(Modifier.height(8.dp))
                                StressCategoryLabel(effStress)
                            }
                        }
                    }
                }
            }

            // ── Respiration ──────────────────────────────────────────────────
            item {
                WatchCard("RESPIRATION RATE") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                if (effResp > 0) "$effResp" else "—",
                                color = CyanColor,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                "brpm",
                                color = DimColor,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SourceBadge(hasWatchResp)
                            if (!hasWatchResp) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "MANUAL ONLY",
                                    color = DimColor,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }

            // ── Steps + Calories ─────────────────────────────────────────────
            item {
                WatchCard("ACTIVITY") {
                    steps?.let {
                        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            MetricPair("${it.count}", "STEPS")
                            MetricPair("${it.kcal}", "KCAL")
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            formatTime(it.timestampMs),
                            color = DimColor,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } ?: Text(
                        "— NO DATA",
                        color = DimColor,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // ── Sleep ────────────────────────────────────────────────────────
            item {
                WatchCard("SLEEP") {
                    if (sleepHistory.isNotEmpty()) {
                        val deepMins  = sleepHistory.filter { it.type == 2 }.sumOf { it.durationMinutes }
                        val lightMins = sleepHistory.filter { it.type == 1 }.sumOf { it.durationMinutes }
                        val awakeMins = sleepHistory.filter { it.type == 0 }.sumOf { it.durationMinutes }
                        val total     = deepMins + lightMins
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            MetricPair("${total / 60}h ${total % 60}m", "TOTAL")
                            MetricPair("${deepMins}m", "DEEP")
                            MetricPair("${lightMins}m", "LIGHT")
                            MetricPair("${awakeMins}m", "AWAKE")
                        }
                    } else {
                        sleep?.let {
                            val typeName = when (it.type) {
                                2    -> "DEEP SLEEP"
                                1    -> "LIGHT SLEEP"
                                else -> "AWAKE"
                            }
                            MetricPair("${it.durationMinutes}m", typeName)
                        } ?: Text(
                            "— NO DATA",
                            color = DimColor,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// ── Connection button — unchanged ────────────────────────────────────────────
@Composable
private fun ConnectionButton(state: ConnectionState, vm: MainViewModel) {
    val context = LocalContext.current
    val (label, color) = when (state) {
        ConnectionState.DISCONNECTED -> "CONNECT"    to CyanColor
        ConnectionState.SCANNING     -> "SCANNING"   to AmberColor
        ConnectionState.CONNECTING   -> "CONNECTING" to AmberColor
        ConnectionState.CONNECTED    -> "CONNECTED"  to GreenColor
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) vm.connectWatchTo("C0:29:AB:60:4D:10")
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
                    if (needed.isEmpty()) vm.connectWatchTo("C0:29:AB:60:4D:10")
                    else permLauncher.launch(needed.toTypedArray())
                }
                ConnectionState.CONNECTED -> vm.disconnectWatch()
                else -> {}
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            fontSize = 11.sp,
            color = color,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
    }
}

// ── Card shell ────────────────────────────────────────────────────────────────
@Composable
private fun WatchCard(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardColor, RoundedCornerShape(4.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
            .padding(16.dp)
    ) {
        Text(
            label,
            color = DimColor,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 3.sp
        )
        Spacer(Modifier.height(12.dp))
        content()
    }
}

// ── Source badge ──────────────────────────────────────────────────────────────
@Composable
private fun SourceBadge(fromWatch: Boolean) {
    val color = if (fromWatch) CyanColor else AmberColor
    val label = if (fromWatch) "WATCH" else "MANUAL"
    Text(
        label,
        color = color,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 2.sp,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
}

// ── Manual input field ────────────────────────────────────────────────────────
@Composable
private fun ManualField(label: String, value: Int, modifier: Modifier, onSave: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(if (value > 0) "$value" else "") }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            color = DimColor,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { s ->
                text = s.filter { it.isDigit() }.take(3)
                text.toIntOrNull()?.let { onSave(it) }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanColor,
                unfocusedBorderColor = BorderColor,
                focusedTextColor = TextColor,
                unfocusedTextColor = TextColor,
                cursorColor = CyanColor
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ── Metric display pair ───────────────────────────────────────────────────────
@Composable
private fun MetricPair(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            color = TextColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            label,
            color = DimColor,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
    }
}

// ── Category labels ───────────────────────────────────────────────────────────
@Composable
private fun HrCategoryLabel(bpm: Int) {
    val (label, color) = when {
        bpm < 60   -> "BRADYCARDIA" to CyanColor
        bpm <= 100 -> "NORMAL"      to GreenColor
        bpm <= 150 -> "ELEVATED"    to AmberColor
        else       -> "HIGH"        to RedColor
    }
    CategoryChip(label, color)
}

@Composable
private fun Spo2CategoryLabel(pct: Int) {
    val (label, color) = when {
        pct >= 95 -> "NORMAL"   to GreenColor
        pct >= 90 -> "LOW"      to AmberColor
        else      -> "CRITICAL" to RedColor
    }
    CategoryChip(label, color)
}

@Composable
private fun StressCategoryLabel(score: Int) {
    val (label, color) = when {
        score <= 29 -> "RELAXED"     to GreenColor
        score <= 59 -> "NORMAL"      to CyanColor
        score <= 79 -> "ELEVATED"    to AmberColor
        else        -> "HIGH STRESS" to RedColor
    }
    CategoryChip(label, color)
}

@Composable
private fun CategoryChip(label: String, color: Color) {
    Text(
        label,
        color = color,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 2.sp,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
}

// ── Color helpers ─────────────────────────────────────────────────────────────
private fun bpColor(mmhg: Int): Color = when {
    mmhg <= 0  -> DimColor
    mmhg < 120 -> GreenColor
    mmhg < 130 -> AmberColor
    mmhg < 140 -> OrangeColor
    else       -> RedColor
}

private fun hrColor(bpm: Int): Color = when {
    bpm <= 0   -> DimColor
    bpm < 60   -> CyanColor
    bpm <= 100 -> GreenColor
    bpm <= 150 -> AmberColor
    else       -> RedColor
}

private fun spo2Color(pct: Int): Color = when {
    pct <= 0  -> DimColor
    pct >= 95 -> GreenColor
    pct >= 90 -> AmberColor
    else      -> RedColor
}

private fun stressColor(score: Int): Color = when {
    score <= 0  -> DimColor
    score <= 29 -> GreenColor
    score <= 59 -> CyanColor
    score <= 79 -> AmberColor
    else        -> RedColor
}

private fun formatTime(ms: Long): String {
    if (ms == 0L) return ""
    return SimpleDateFormat("MMM d  HH:mm", Locale.US).format(Date(ms))
}
