package com.biospace.monitor.ble

import android.content.Context
import com.biospace.monitor.model.SpaceWeatherState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// ── Data models ───────────────────────────────────────────────────────────────

data class WatchSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val triggerType: TriggerType = TriggerType.SCHEDULED,
    val heartRate: Int? = null,
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val spo2: Int? = null,
    val respiratoryRate: Int? = null,
    val temperature: Int? = null,
    val steps: Int? = null,
    val stress: Int? = null,
    val battery: Int? = null,
    // Space weather at time of recording
    val kp: Double? = null,
    val bz: Double? = null,
    val speed: Double? = null,
    val xrayFlux: Double? = null,
    val gScale: Int? = null
)

enum class TriggerType {
    SCHEDULED,          // normal 15-min window
    BIOMETRIC_ALERT,    // abnormal biometric detected
    SPACE_WEATHER_EVENT // solar storm / Bz drop / flare
}

data class CorrelationSession(
    val id: String = System.currentTimeMillis().toString(),
    val startTime: Long = System.currentTimeMillis(),
    var endTime: Long? = null,
    val trigger: TriggerType = TriggerType.SPACE_WEATHER_EVENT,
    val triggerDescription: String = "",
    val snapshots: MutableList<WatchSnapshot> = mutableListOf()
)

data class WatchAlert(
    val timestamp: Long = System.currentTimeMillis(),
    val type: AlertType,
    val message: String,
    val value: String = ""
)

enum class AlertType { HR_HIGH, HR_LOW, BP_HIGH, SPO2_LOW, RESP_ABNORMAL, STRESS_HIGH,
    KP_STORM, BZ_SOUTHWARD, SOLAR_FLARE, CME_DETECTED }

// ── Thresholds (configurable) ─────────────────────────────────────────────────

data class BiometricThresholds(
    val hrHigh: Int = 100,
    val hrLow: Int = 50,
    val sysHigh: Int = 140,
    val diaHigh: Int = 90,
    val spo2Low: Int = 95,
    val respHigh: Int = 20,
    val respLow: Int = 12,
    val stressHigh: Int = 70
)

data class SpaceThresholds(
    val kpStorm: Double = 5.0,
    val bzSouth: Double = -10.0,
    val solarWindHigh: Double = 600.0,
    val flareMinClass: String = "M"  // M or X
)

// ── Repository ────────────────────────────────────────────────────────────────

class WatchRepository(context: Context) {

    val ble = WatchBleManager(context)

    var bioThresholds = BiometricThresholds()
    var spaceThresholds = SpaceThresholds()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── State flows ──────────────────────────────────────────────────────────
    private val _history = MutableStateFlow<List<WatchSnapshot>>(emptyList())
    val history: StateFlow<List<WatchSnapshot>> = _history.asStateFlow()

    private val _latest = MutableStateFlow(WatchSnapshot())
    val latest: StateFlow<WatchSnapshot> = _latest.asStateFlow()

    private val _alerts = MutableSharedFlow<WatchAlert>(replay = 8, extraBufferCapacity = 32)
    val alerts: SharedFlow<WatchAlert> = _alerts.asSharedFlow()

    private val _sessions = MutableStateFlow<List<CorrelationSession>>(emptyList())
    val sessions: StateFlow<List<CorrelationSession>> = _sessions.asStateFlow()

    private val _isEventMode = MutableStateFlow(false)
    val isEventMode: StateFlow<Boolean> = _isEventMode.asStateFlow()

    // ── Internal state ───────────────────────────────────────────────────────
    private var syncJob: Job? = null
    private var collectJob: Job? = null
    private var activeSession: CorrelationSession? = null

    // Pending pairing for systolic→diastolic matching


    // Latest live readings accumulator
    private var live = WatchSnapshot()

    // Latest space weather (updated by ViewModel observer)
    private var currentSw: SpaceWeatherState? = null

    // ── Start / Stop ─────────────────────────────────────────────────────────

    fun start() {
        startCollecting()
        startSyncScheduler()
    }

    fun stop() {
        syncJob?.cancel()
        collectJob?.cancel()
        ble.disconnect()
        _isEventMode.value = false
    }

    // ── Space weather observer (call from ViewModel) ──────────────────────────

    fun onSpaceWeatherUpdate(sw: SpaceWeatherState) {
        currentSw = sw
        checkSpaceWeatherThresholds(sw)
    }

    // ── Sync scheduler ───────────────────────────────────────────────────────

    private fun startSyncScheduler() {
        syncJob?.cancel()
        syncJob = scope.launch {
            while (isActive) {
                // Align to next :00, :15, :30, :45
                val now = System.currentTimeMillis()
                val msInHour = now % 3_600_000
                val slot = (msInHour / 900_000) * 900_000
                val nextSlot = slot + 900_000
                val delay = nextSlot - msInHour
                delay(delay)

                if (!_isEventMode.value) {
                    performSyncWindow(TriggerType.SCHEDULED)
                }
                // In event mode the connection stays open — no action needed
            }
        }
    }

    private suspend fun performSyncWindow(trigger: TriggerType) {
        ble.scanAndConnect()
        delay(120_000) // 2-minute collection window
        val snapshot = live.copy(
            timestamp   = System.currentTimeMillis(),
            triggerType = trigger,
            kp          = currentSw?.kp,
            bz          = currentSw?.bz,
            speed       = currentSw?.speed,
            xrayFlux    = currentSw?.xrayFlux,
            gScale      = currentSw?.gScale
        )
        appendSnapshot(snapshot)
        ble.disconnect()
        live = WatchSnapshot() // reset accumulator
    }

    // ── BLE reading collector ─────────────────────────────────────────────────

    private fun startCollecting() {
        collectJob?.cancel()
        collectJob = scope.launch {
            ble.readings.collect { reading ->
                processReading(reading)
            }
        }
    }

    private fun processReading(reading: WatchReading) {
        live = when (reading) {
            is WatchReading.Battery          -> live.copy(battery = reading.percent)
            is WatchReading.Temperature      -> live.copy(temperature = reading.celsius)
            is WatchReading.StepsSummary     -> live.copy(steps = reading.steps)
            is WatchReading.StepsHourly      -> live.copy(steps = (live.steps ?: 0) + reading.steps)
            is WatchReading.SpO2             -> live.copy(spo2 = reading.percent)
            is WatchReading.RespiratoryRate  -> live.copy(respiratoryRate = reading.rpm)
            is WatchReading.Stress           -> live.copy(stress = reading.score)
            is WatchReading.Sleep            -> live // log but don't overwrite primary fields
            is WatchReading.HeartRate        -> live.copy(heartRate = reading.bpm)
            is WatchReading.BloodPressure    -> live.copy(systolic = reading.systolic, diastolic = reading.diastolic)

            // Pair systolic + diastolic arriving separately
            is WatchReading.BloodPressure -> {
                live.copy(systolic = reading.systolic, diastolic = reading.diastolic, heartRate = reading.heartRate)
            }
        }
        _latest.value = live

        // Check biometric thresholds on every update
        checkBiometricThresholds(live)

        // Feed into active correlation session if running
        activeSession?.snapshots?.add(live.copy(timestamp = System.currentTimeMillis()))
    }

    // ── Threshold checks ─────────────────────────────────────────────────────

    private fun checkBiometricThresholds(snap: WatchSnapshot) {
        snap.heartRate?.let { hr ->
            when {
                hr > bioThresholds.hrHigh -> fireAlert(AlertType.HR_HIGH, "Heart rate elevated", "$hr bpm")
                hr < bioThresholds.hrLow  -> fireAlert(AlertType.HR_LOW,  "Heart rate low",      "$hr bpm")
            }
        }
        snap.systolic?.let { sys ->
            snap.diastolic?.let { dia ->
                if (sys >= bioThresholds.sysHigh || dia >= bioThresholds.diaHigh)
                    fireAlert(AlertType.BP_HIGH, "Blood pressure elevated", "$sys/$dia mmHg")
            }
        }
        snap.spo2?.let { o2 ->
            if (o2 < bioThresholds.spo2Low)
                fireAlert(AlertType.SPO2_LOW, "SpO2 low", "$o2%")
        }
        snap.respiratoryRate?.let { rr ->
            if (rr > bioThresholds.respHigh || rr < bioThresholds.respLow)
                fireAlert(AlertType.RESP_ABNORMAL, "Respiratory rate abnormal", "$rr rpm")
        }
        snap.stress?.let { s ->
            if (s > bioThresholds.stressHigh)
                fireAlert(AlertType.STRESS_HIGH, "Stress level high", "$s/100")
        }
    }

    private fun checkSpaceWeatherThresholds(sw: SpaceWeatherState) {
        var eventTriggered = false
        var description = ""

        if (sw.kp >= spaceThresholds.kpStorm) {
            fireAlert(AlertType.KP_STORM, "Geomagnetic storm", "Kp ${String.format("%.1f", sw.kp)} (G${sw.gScale})")
            description = "G${sw.gScale} geomagnetic storm (Kp=${String.format("%.1f", sw.kp)})"
            eventTriggered = true
        }
        if (sw.bz <= spaceThresholds.bzSouth) {
            fireAlert(AlertType.BZ_SOUTHWARD, "Bz strongly southward", "${String.format("%.1f", sw.bz)} nT")
            if (description.isEmpty()) description = "Bz strongly southward (${String.format("%.1f", sw.bz)} nT)"
            eventTriggered = true
        }
        if (sw.xrayFlux >= 1e-5) {
            val cls = sw.flareClass
            fireAlert(AlertType.SOLAR_FLARE, "Solar flare detected", cls)
            if (description.isEmpty()) description = "$cls solar flare"
            eventTriggered = true
        }
        if (sw.cmeEvents.isNotEmpty() && sw.cmeEvents.first().activityID != (currentSw?.cmeEvents?.firstOrNull()?.activityID ?: "")) {
            fireAlert(AlertType.CME_DETECTED, "New CME detected", sw.cmeEvents.first().activityID)
            if (description.isEmpty()) description = "CME event: ${sw.cmeEvents.first().activityID}"
            eventTriggered = true
        }

        if (eventTriggered && !_isEventMode.value) {
            enterEventMode(description)
        } else if (!eventTriggered && _isEventMode.value) {
            exitEventMode()
        }
    }

    // ── Event mode ────────────────────────────────────────────────────────────

    private fun enterEventMode(description: String) {
        _isEventMode.value = true
        val session = CorrelationSession(
            trigger = TriggerType.SPACE_WEATHER_EVENT,
            triggerDescription = description
        )
        activeSession = session
        scope.launch {
            if (ble.state.value == ConnectionState.DISCONNECTED) {
                ble.scanAndConnect()
            }
        }
    }

    private fun exitEventMode() {
        _isEventMode.value = false
        activeSession?.let { session ->
            session.endTime = System.currentTimeMillis()
            _sessions.value = (_sessions.value + session).takeLast(50)
            activeSession = null
        }
        scope.launch {
            delay(5_000)
            ble.disconnect()
            live = WatchSnapshot()
        }
    }

    // ── Alert emitter (deduplicated — 5 min cooldown per type) ───────────────

    private val lastAlertTime = mutableMapOf<AlertType, Long>()
    private fun fireAlert(type: AlertType, message: String, value: String = "") {
        val now = System.currentTimeMillis()
        if ((now - (lastAlertTime[type] ?: 0)) < 300_000) return // 5-min cooldown
        lastAlertTime[type] = now
        _alerts.tryEmit(WatchAlert(now, type, message, value))
    }

    // ── History management ────────────────────────────────────────────────────

    private fun appendSnapshot(snap: WatchSnapshot) {
        _history.value = (_history.value + snap).takeLast(2000)
    }

    // ── Export ────────────────────────────────────────────────────────────────

    fun exportCsv(): String {
        val sb = StringBuilder()
        sb.appendLine("timestamp,trigger,hr_bpm,systolic,diastolic,spo2_pct,resp_rpm,temp_c,steps,stress,battery,kp,bz,solar_wind,xray_flux,g_scale")
        _history.value.forEach { s ->
            val ts = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                .format(java.util.Date(s.timestamp))
            sb.appendLine("$ts,${s.triggerType},${s.heartRate ?: ""},${s.systolic ?: ""},${s.diastolic ?: ""}," +
                "${s.spo2 ?: ""},${s.respiratoryRate ?: ""},${s.temperature ?: ""},${s.steps ?: ""}," +
                "${s.stress ?: ""},${s.battery ?: ""},${s.kp ?: ""},${s.bz ?: ""},${s.speed ?: ""}," +
                "${s.xrayFlux ?: ""},${s.gScale ?: ""}")
        }
        return sb.toString()
    }

    fun exportSessionReport(session: CorrelationSession): String {
        val sb = StringBuilder()
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        sb.appendLine("=== CORRELATION SESSION REPORT ===")
        sb.appendLine("Trigger: ${session.triggerDescription}")
        sb.appendLine("Start:   ${fmt.format(java.util.Date(session.startTime))}")
        session.endTime?.let { sb.appendLine("End:     ${fmt.format(java.util.Date(it))}") }
        sb.appendLine()
        sb.appendLine("BIOMETRICS DURING EVENT:")
        val hrs  = session.snapshots.mapNotNull { it.heartRate }
        val syss = session.snapshots.mapNotNull { it.systolic }
        val dias = session.snapshots.mapNotNull { it.diastolic }
        val o2s  = session.snapshots.mapNotNull { it.spo2 }
        val resp = session.snapshots.mapNotNull { it.respiratoryRate }
        val str  = session.snapshots.mapNotNull { it.stress }
        if (hrs.isNotEmpty())  sb.appendLine("HR:    avg=${hrs.average().toInt()} min=${hrs.min()} max=${hrs.max()} bpm")
        if (syss.isNotEmpty()) sb.appendLine("BP:    avg ${syss.average().toInt()}/${dias.average().toInt()} mmHg")
        if (o2s.isNotEmpty())  sb.appendLine("SpO2:  avg=${o2s.average().toInt()}% min=${o2s.min()}%")
        if (resp.isNotEmpty()) sb.appendLine("Resp:  avg=${resp.average().toInt()} rpm")
        if (str.isNotEmpty())  sb.appendLine("Stress: avg=${str.average().toInt()}/100")
        sb.appendLine()
        sb.appendLine("SPACE WEATHER DURING EVENT:")
        session.snapshots.lastOrNull()?.let { s ->
            s.kp?.let    { sb.appendLine("Kp:     ${String.format("%.2f", it)}") }
            s.bz?.let    { sb.appendLine("Bz:     ${String.format("%.1f", it)} nT") }
            s.speed?.let { sb.appendLine("Wind:   ${it.toInt()} km/s") }
            s.gScale?.let { if (it > 0) sb.appendLine("G-Scale: G$it") }
        }
        return sb.toString()
    }
}
