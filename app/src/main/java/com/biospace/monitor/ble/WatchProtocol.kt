package com.biospace.monitor.ble

import java.util.UUID
import java.util.Calendar

// ─── NUS UUIDs (confirmed from BpDoctor decompile) ───────────────────────────
object WatchProtocol {

    val NUS_SERVICE: UUID   = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    val NUS_TX_WRITE: UUID  = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    val NUS_RX_NOTIFY: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
    val CCCD: UUID          = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // ─── CONFIRMED packet structure from BpDoctor HealthDataAnalyzer.kt ──────
    // All packets start with byte[0] = 0xAB (171)
    // byte[4] = category command
    // byte[5] = sub-command (when category is 0x31 or 0x51)
    // Data starts at byte[6]
    //
    // Category 0x31 (49) sub-commands:
    //   0x09 (9)  → saveSingleHR      (DataSyncMgr.q)
    //   0x0A (10) → saveCurrentHR     (DataSyncMgr.f)
    //   0x11 (17) → saveSingleBO/SpO2 (DataSyncMgr.o)
    //   0x12 (18) → saveCurrentBO     (DataSyncMgr.d)
    //   0x21 (33) → saveSingleBP      (DataSyncMgr.p)
    //   0x22 (34) → saveCurrentBP     (DataSyncMgr.e)  ← CONFIRMED real cmd for live BP
    //   0x41 (65) → saveSingleIMM     (DataSyncMgr.r)
    //   0x81(129) → saveSingleTemp    (DataSyncMgr.s)
    //
    // Category 0x32 (50) → saveOnceKey / one-key bundle (DataSyncMgr.m)
    //
    // Category 0x51 (81) sub-commands:
    //   0x08 (8)  → saveCurrentStep   (DataSyncMgr.g)
    //   0x0B (11) → savePressure/stress(DataSyncMgr.n)
    //   0x18 (24) → saveMeasureIMM    (DataSyncMgr.k)
    //   0x20 (32) → hourlyMeasure1    (DataSyncMgr.a)
    //   0x21 (33) → hourlyMeasure2    (DataSyncMgr.b)
    //   0x11 (17) → saveMeasureHR     (DataSyncMgr.j)
    //   0x12 (18) → saveMeasureBO     (DataSyncMgr.h)
    //   0x13 (19) → saveMeasureTemp   (DataSyncMgr.l)
    //   0x14 (20) → saveMeasureBP     (DataSyncMgr.i)
    //
    // Category 0x52 (82) → saveSleep  (DataSyncMgr.t)
    // Category 0xB7(183) → saveSport  (DataSyncMgr.u)

    // Category bytes
    const val CAT_SINGLE_MEASURE = 0x31   // 49  — single/realtime readings
    const val CAT_ONE_KEY        = 0x32   // 50  — one-key bundle (HR+SpO2+BP)
    const val CAT_HISTORY        = 0x51   // 81  — historical/hourly/measured
    const val CAT_SLEEP          = 0x52   // 82  — sleep segments
    const val CAT_SPORT          = 0xB7   // 183 — sport/activity

    // Sub-commands under CAT_SINGLE_MEASURE (0x31)
    const val SUB_SINGLE_HR      = 0x09   // 9
    const val SUB_CURRENT_HR     = 0x0A   // 10
    const val SUB_SINGLE_SPO2    = 0x11   // 17
    const val SUB_CURRENT_SPO2   = 0x12   // 18
    const val SUB_SINGLE_BP      = 0x21   // 33
    const val SUB_CURRENT_BP     = 0x22   // 34  ← CONFIRMED live BP command
    const val SUB_SINGLE_IMM     = 0x41   // 65
    const val SUB_SINGLE_TEMP    = 0x81   // 129

    // Sub-commands under CAT_HISTORY (0x51)
    const val SUB_CURRENT_STEPS  = 0x08   // 8
    const val SUB_STRESS         = 0x0B   // 11
    const val SUB_MEASURE_IMM    = 0x18   // 24
    const val SUB_HOURLY_1       = 0x20   // 32
    const val SUB_HOURLY_2       = 0x21   // 33
    const val SUB_MEASURE_HR     = 0x11   // 17
    const val SUB_MEASURE_SPO2   = 0x12   // 18
    const val SUB_MEASURE_TEMP   = 0x13   // 19
    const val SUB_MEASURE_BP     = 0x14   // 20

    // ─── Sealed hierarchy of every readable metric ────────────────────────────
    sealed class WatchReading {
        data class BloodPressure(
            val timestampMs: Long,
            val systolic: Int,
            val diastolic: Int
        ) : WatchReading()

        data class HeartRate(
            val timestampMs: Long,
            val bpm: Int
        ) : WatchReading()

        data class SpO2(
            val timestampMs: Long,
            val percent: Int
        ) : WatchReading()

        data class Steps(
            val timestampMs: Long,
            val count: Int,
            val kcal: Int
        ) : WatchReading()

        data class Sleep(
            val timestampMs: Long,
            val type: Int,           // 0=awake 1=light 2=deep
            val durationMinutes: Int
        ) : WatchReading()

        data class Stress(
            val timestampMs: Long,
            val score: Int           // 0-100
        ) : WatchReading()

        data class Temperature(
            val timestampMs: Long,
            val celsius: Float
        ) : WatchReading()

        data class Immunity(
            val timestampMs: Long,
            val score: Int           // 0-100
        ) : WatchReading()

        data class OneKeyBundle(
            val timestampMs: Long,
            val heartRate: Int,
            val spO2: Int,
            val systolic: Int,
            val diastolic: Int
        ) : WatchReading()

        /** Raw packet that didn't match any known command — logged for debugging */
        data class Unknown(val cat: Int, val sub: Int, val raw: ByteArray) : WatchReading()
    }

    // ─── Main packet parser ───────────────────────────────────────────────────
    // Confirmed structure:
    //   byte[0] = 0xAB header
    //   byte[1..3] = ignored routing/length bytes
    //   byte[4] = category
    //   byte[5] = sub-command (for cat 0x31 and 0x51)
    //   byte[6..] = data payload
    fun parse(raw: ByteArray, nowMs: Long = System.currentTimeMillis()): WatchReading {
        val bytes = raw.map { it.toInt() and 0xFF }

        if (bytes.size < 5) return WatchReading.Unknown(-1, -1, raw)
        if (bytes[0] != 0xAB) return WatchReading.Unknown(-1, -1, raw)

        val cat = bytes[4]
        val sub = if (bytes.size > 5) bytes[5] else -1

        return when (cat) {

            CAT_SINGLE_MEASURE -> when (sub) {
                // ── Heart Rate (single + realtime) ───────────────────────────
                SUB_SINGLE_HR, SUB_CURRENT_HR -> {
                    if (bytes.size < 7) return WatchReading.Unknown(cat, sub, raw)
                    val bpm = bytes[6]
                    if (bpm <= 0) return WatchReading.Unknown(cat, sub, raw)
                    WatchReading.HeartRate(nowMs, bpm)
                }
                // ── SpO2 / Blood Oxygen (single + realtime) ──────────────────
                SUB_SINGLE_SPO2, SUB_CURRENT_SPO2 -> {
                    if (bytes.size < 7) return WatchReading.Unknown(cat, sub, raw)
                    val pct = bytes[6]
                    if (pct <= 0) return WatchReading.Unknown(cat, sub, raw)
                    WatchReading.SpO2(nowMs, pct)
                }
                // ── Blood Pressure (single + realtime) ───────────────────────
                // CONFIRMED: systolic=list[6], diastolic=list[7]
                SUB_SINGLE_BP, SUB_CURRENT_BP -> {
                    if (bytes.size < 8) return WatchReading.Unknown(cat, sub, raw)
                    val sys = bytes[6]
                    val dia = bytes[7]
                    if (sys <= 0 || dia <= 0) return WatchReading.Unknown(cat, sub, raw)
                    WatchReading.BloodPressure(nowMs, sys, dia)
                }
                // ── Immunity (single) ────────────────────────────────────────
                SUB_SINGLE_IMM -> {
                    if (bytes.size < 7) return WatchReading.Unknown(cat, sub, raw)
                    val score = minOf(bytes[6], 100)
                    if (score == 0) return WatchReading.Unknown(cat, sub, raw)
                    WatchReading.Immunity(nowMs, score)
                }
                // ── Temperature (single) ─────────────────────────────────────
                // CONFIRMED: integer part=list[6], decimal part=list[7]
                // formatted as "${list[6]}.${list[7]}" then parsed to float
                SUB_SINGLE_TEMP -> {
                    if (bytes.size < 8) return WatchReading.Unknown(cat, sub, raw)
                    val tempC = "${bytes[6]}.${bytes[7]}".toFloatOrNull()
                        ?: return WatchReading.Unknown(cat, sub, raw)
                    // Sanity check: BpDoctor rejects >= 50.0 and replaces with 36.5
                    val safeTemp = if (tempC >= 50f) 36.5f else tempC
                    if (safeTemp == 0f) return WatchReading.Unknown(cat, sub, raw)
                    WatchReading.Temperature(nowMs, safeTemp)
                }
                else -> WatchReading.Unknown(cat, sub, raw)
            }

            // ── One-key bundle: HR + SpO2 + BP ───────────────────────────────
            // CONFIRMED: hr=list[6], spO2=list[7], sys=list[8], dia=list[9]
            CAT_ONE_KEY -> {
                if (bytes.size < 10) return WatchReading.Unknown(cat, sub, raw)
                val hr   = bytes[6]
                val spo2 = bytes[7]
                val sys  = bytes[8]
                val dia  = bytes[9]
                WatchReading.OneKeyBundle(nowMs, hr, spo2, sys, dia)
            }

            CAT_HISTORY -> when (sub) {
                // ── Measured HR (with timestamp) ─────────────────────────────
                SUB_MEASURE_HR -> {
                    if (bytes.size < 12) return WatchReading.Unknown(cat, sub, raw)
                    val ts  = timestampFromBytes(bytes, 6)
                    val bpm = bytes[11]
                    if (bpm <= 0) return WatchReading.Unknown(cat, sub, raw)
                    WatchReading.HeartRate(ts, bpm)
                }
                // ── Measured SpO2 (with timestamp) ───────────────────────────
                SUB_MEASURE_SPO2 -> {
                    if (bytes.size < 12) return WatchReading.Unknown(cat, sub, raw)
                    val ts  = timestampFromBytes(bytes, 6)
                    val pct = bytes[11]
                    if (pct <= 0) return WatchReading.Unknown(cat, sub, raw)
                    WatchReading.SpO2(ts, pct)
                }
                // ── Measured BP (with timestamp) ─────────────────────────────
                // CONFIRMED: year=list[6]+2000, month=list[7], day=list[8],
                //            hour=list[9], min=list[10], sys=list[11], dia=list[12]
                SUB_MEASURE_BP -> {
                    if (bytes.size < 13) return WatchReading.Unknown(cat, sub, raw)
                    val ts  = timestampFromBytes(bytes, 6)
                    val sys = bytes[11]
                    val dia = bytes[12]
                    if (sys <= 0 || dia <= 0) return WatchReading.Unknown(cat, sub, raw)
                    WatchReading.BloodPressure(ts, sys, dia)
                }
                // ── Measured Temperature (with timestamp) ────────────────────
                SUB_MEASURE_TEMP -> {
                    if (bytes.size < 13) return WatchReading.Unknown(cat, sub, raw)
                    val ts     = timestampFromBytes(bytes, 6)
                    val tempC  = "${bytes[11]}.${bytes[12]}".toFloatOrNull()
                        ?: return WatchReading.Unknown(cat, sub, raw)
                    val safeTemp = if (tempC >= 50f) 36.5f else tempC
                    WatchReading.Temperature(ts, safeTemp)
                }
                // ── Current Steps (live 24-bit) ──────────────────────────────
                // CONFIRMED: steps = list[8] + (list[7]<<8) + (list[6]<<16)
                //            kcal  = list[11]+ (list[10]<<8)+ (list[9]<<16)
                SUB_CURRENT_STEPS -> {
                    if (bytes.size < 17) return WatchReading.Unknown(cat, sub, raw)
                    val steps = bytes[8] + (bytes[7] shl 8) + (bytes[6] shl 16)
                    val kcal  = bytes[11] + (bytes[10] shl 8) + (bytes[9] shl 16)
                    WatchReading.Steps(nowMs, steps, kcal)
                }
                // ── Stress / Pressure score ──────────────────────────────────
                // CONFIRMED: year=list[6]+2000..min=list[10], score=list[11]
                SUB_STRESS -> {
                    if (bytes.size < 12) return WatchReading.Unknown(cat, sub, raw)
                    val ts    = timestampFromBytes(bytes, 6)
                    val score = bytes[11]
                    if (score <= 0) return WatchReading.Unknown(cat, sub, raw)
                    WatchReading.Stress(ts, score)
                }
                // ── Measured Immunity ────────────────────────────────────────
                SUB_MEASURE_IMM -> {
                    if (bytes.size < 12) return WatchReading.Unknown(cat, sub, raw)
                    val ts    = timestampFromBytes(bytes, 6)
                    val score = minOf(bytes[11], 100)
                    if (score == 0) return WatchReading.Unknown(cat, sub, raw)
                    WatchReading.Immunity(ts, score)
                }
                // ── Hourly bundles (steps+kcal summary) ─────────────────────
                SUB_HOURLY_1, SUB_HOURLY_2 -> {
                    // These contain steps/kcal/sleep aggregates — parse as Steps
                    if (bytes.size < 12) return WatchReading.Unknown(cat, sub, raw)
                    val steps = bytes[8] + (bytes[7] shl 8) + (bytes[6] shl 16)
                    val kcal  = bytes[11] + (bytes[10] shl 8) + (bytes[9] shl 16)
                    WatchReading.Steps(nowMs, steps, kcal)
                }
                else -> WatchReading.Unknown(cat, sub, raw)
            }

            // ── Sleep segment ─────────────────────────────────────────────────
            // CONFIRMED: year=list[6]+2000..min=list[10], sleepType=list[11]
            //            sleepDuration = ByteArrayExpand.e(bytes 12..13) in minutes
            CAT_SLEEP -> {
                if (bytes.size < 14) return WatchReading.Unknown(cat, sub, raw)
                val ts       = timestampFromBytes(bytes, 6)
                val sleepType = bytes[11]
                // 2-byte big-endian duration in minutes at index 12-13
                val durMins  = (bytes[12] shl 8) + bytes[13]
                WatchReading.Sleep(ts, sleepType, durMins)
            }

            else -> WatchReading.Unknown(cat, sub, raw)
        }
    }

    // ─── Timestamp helper ─────────────────────────────────────────────────────
    // CONFIRMED layout: year-2000, month, day, hour, minute at bytes[offset..offset+4]
    private fun timestampFromBytes(bytes: List<Int>, offset: Int): Long {
        val year   = bytes[offset] + 2000
        val month  = bytes[offset + 1]
        val day    = bytes[offset + 2]
        val hour   = bytes[offset + 3]
        val minute = bytes[offset + 4]
        return Calendar.getInstance().apply {
            set(year, month - 1, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
