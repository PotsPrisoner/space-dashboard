package com.biospace.monitor.ble

import java.util.UUID
import java.util.Calendar

object WatchProtocol {

    val NUS_SERVICE: UUID   = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    val NUS_TX_WRITE: UUID  = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    val NUS_RX_NOTIFY: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
    val CCCD: UUID          = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // Category bytes (byte[4] in packet)
    const val CAT_SINGLE_MEASURE = 0x31
    const val CAT_ONE_KEY        = 0x32
    const val CAT_HISTORY        = 0x51
    const val CAT_SLEEP          = 0x52
    const val CAT_SPORT          = 0xB7
    const val CAT_VITALS_BUNDLE  = 0x92
    const val CAT_STATUS         = 0x91
    const val CAT_MEAS_ACK       = 0x20
    const val CAT_MEAS_RESULT    = 0x21
    const val CAT_PPG_HISTORY    = 0x9E

    // Sub-commands under CAT_SINGLE_MEASURE (0x31)
    const val SUB_SINGLE_HR      = 0x09
    const val SUB_CURRENT_HR     = 0x0A
    const val SUB_SINGLE_SPO2    = 0x11
    const val SUB_CURRENT_SPO2   = 0x12
    const val SUB_SINGLE_BP      = 0x21
    const val SUB_CURRENT_BP     = 0x22
    const val SUB_SINGLE_IMM     = 0x41
    const val SUB_SINGLE_TEMP    = 0x81

    // Sub-commands under CAT_HISTORY (0x51)
    const val SUB_CURRENT_STEPS  = 0x08
    const val SUB_STRESS         = 0x0B
    const val SUB_MEASURE_IMM    = 0x18
    const val SUB_HOURLY_1       = 0x20
    const val SUB_HOURLY_2       = 0x21
    const val SUB_MEASURE_HR     = 0x11
    const val SUB_MEASURE_SPO2   = 0x12
    const val SUB_MEASURE_TEMP   = 0x13
    const val SUB_MEASURE_BP     = 0x14

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
            val type: Int,
            val durationMinutes: Int
        ) : WatchReading()

        data class Stress(
            val timestampMs: Long,
            val score: Int
        ) : WatchReading()


        data class Respiration(
            val timestampMs: Long,
            val breathsPerMin: Int
        ) : WatchReading()
        data class Temperature(
            val timestampMs: Long,
            val celsius: Float
        ) : WatchReading()

        data class Immunity(
            val timestampMs: Long,
            val score: Int
        ) : WatchReading()

        data class HourlyBundle(
            val timestampMs: Long,
            val steps: Int,
            val kcal: Int,
            val heartRate: Int,
            val spO2: Int,
            val systolic: Int,
            val diastolic: Int
        ) : WatchReading()

        data class OneKeyBundle(
            val timestampMs: Long,
            val heartRate: Int,
            val spO2: Int,
            val systolic: Int,
            val diastolic: Int
        ) : WatchReading()

        data class Battery(val percent: Int) : WatchReading()

        object SyncDone : WatchReading()

        data class Unknown(
            val cmdByte: Int,
            val subByte: Int = -1,
            val raw: ByteArray
        ) : WatchReading()
    }

    fun parse(raw: ByteArray, nowMs: Long = System.currentTimeMillis()): WatchReading {
        val bytes = raw.map { it.toInt() and 0xFF }

        if (bytes.size < 5) return WatchReading.Unknown(-1, -1, raw)
        // Handle 0xFF-header BP result packets as well as standard 0xAB packets
        if (bytes[0] == 0xFF && bytes.size >= 15) {
            // FF-00-0C-00-01-01-YY-MM-DD-HH-MIN-SS-SYS-DIA-??
            val sys = bytes[12]
            val dia = bytes[13]
            if (sys in 60..250 && dia in 40..150)
                return WatchReading.BloodPressure(nowMs, sys, dia)
        }
        if (bytes[0] != 0xAB) return WatchReading.Unknown(-1, -1, raw)

        val cat = bytes[4]
        val sub = if (bytes.size > 5) bytes[5] else -1

        return when (cat) {

            CAT_SINGLE_MEASURE -> when (sub) {
                SUB_SINGLE_HR, SUB_CURRENT_HR -> {
                    if (bytes.size < 7) return WatchReading.Unknown(cat, sub, raw)
                    val bpm = bytes[6]
                    if (bpm <= 0) return WatchReading.Unknown(cat, sub, raw)
                    WatchReading.HeartRate(nowMs, bpm)
                }
                SUB_SINGLE_SPO2, SUB_CURRENT_SPO2 -> {
                    if (bytes.size < 7) return WatchReading.Unknown(cat, sub, raw)
                    val pct = bytes[6]
                    if (pct <= 0) return WatchReading.Unknown(cat, sub, raw)
                    WatchReading.SpO2(nowMs, pct)
                }
                SUB_SINGLE_BP, SUB_CURRENT_BP -> {
                    if (bytes.size < 8) return WatchReading.Unknown(cat, sub, raw)
                    val sys = bytes[6]
                    val dia = bytes[7]
                    if (sys <= 0 || dia <= 0) return WatchReading.Unknown(cat, sub, raw)
                    WatchReading.BloodPressure(nowMs, sys, dia)
                }
                SUB_SINGLE_IMM -> {
                    if (bytes.size < 7) return WatchReading.Unknown(cat, sub, raw)
                    val score = minOf(bytes[6], 100)
                    if (score == 0) return WatchReading.Unknown(cat, sub, raw)
                    WatchReading.Immunity(nowMs, score)
                }
                SUB_SINGLE_TEMP -> {
                    if (bytes.size < 8) return WatchReading.Unknown(cat, sub, raw)
                    val tempC = "${bytes[6]}.${bytes[7]}".toFloatOrNull()
                        ?: return WatchReading.Unknown(cat, sub, raw)
                    val safeTemp = if (tempC >= 50f) 36.5f else tempC
                    if (safeTemp == 0f) return WatchReading.Unknown(cat, sub, raw)
                    WatchReading.Temperature(nowMs, safeTemp)
                }
                else -> WatchReading.Unknown(cat, sub, raw)
            }

            CAT_ONE_KEY -> {
                if (bytes.size < 10) return WatchReading.Unknown(cat, sub, raw)
                WatchReading.OneKeyBundle(
                    timestampMs = nowMs,
                    heartRate   = bytes[6],
                    spO2        = bytes[7],
                    systolic    = bytes[8],
                    diastolic   = bytes[9]
                )
            }

            CAT_HISTORY -> when (sub) {
                SUB_MEASURE_HR -> {
                    if (bytes.size < 12) return WatchReading.Unknown(cat, sub, raw)
                    val bpm = bytes[11]
                    if (bpm <= 0) return WatchReading.Unknown(cat, sub, raw)
                    WatchReading.HeartRate(timestampFromBytes(bytes, 6), bpm)
                }
                SUB_MEASURE_SPO2 -> {
                    if (bytes.size < 12) return WatchReading.Unknown(cat, sub, raw)
                    val pct = bytes[11]
                    if (pct <= 0) return WatchReading.Unknown(cat, sub, raw)
                    WatchReading.SpO2(timestampFromBytes(bytes, 6), pct)
                }
                SUB_MEASURE_BP -> {
                    if (bytes.size < 13) return WatchReading.Unknown(cat, sub, raw)
                    val sys = bytes[11]; val dia = bytes[12]
                    if (sys <= 0 || dia <= 0) return WatchReading.Unknown(cat, sub, raw)
                    WatchReading.BloodPressure(timestampFromBytes(bytes, 6), sys, dia)
                }
                SUB_MEASURE_TEMP -> {
                    if (bytes.size < 13) return WatchReading.Unknown(cat, sub, raw)
                    val tempC = "${bytes[11]}.${bytes[12]}".toFloatOrNull()
                        ?: return WatchReading.Unknown(cat, sub, raw)
                    WatchReading.Temperature(timestampFromBytes(bytes, 6),
                        if (tempC >= 50f) 36.5f else tempC)
                }
                SUB_CURRENT_STEPS -> {
                    if (bytes.size < 12) return WatchReading.Unknown(cat, sub, raw)
                    val steps = bytes[8] + (bytes[7] shl 8) + (bytes[6] shl 16)
                    val kcal  = bytes[11] + (bytes[10] shl 8) + (bytes[9] shl 16)
                    WatchReading.Steps(nowMs, steps, kcal)
                }
                SUB_STRESS -> {
                    if (bytes.size < 12) return WatchReading.Unknown(cat, sub, raw)
                    val score = bytes[11]
                    if (score <= 0) return WatchReading.Unknown(cat, sub, raw)
                    WatchReading.Stress(timestampFromBytes(bytes, 6), score)
                }
                SUB_MEASURE_IMM -> {
                    if (bytes.size < 12) return WatchReading.Unknown(cat, sub, raw)
                    val score = minOf(bytes[11], 100)
                    if (score == 0) return WatchReading.Unknown(cat, sub, raw)
                    WatchReading.Immunity(timestampFromBytes(bytes, 6), score)
                }
                SUB_HOURLY_1, SUB_HOURLY_2 -> {
                    if (bytes.size < 20) return WatchReading.Unknown(cat, sub, raw)
                    WatchReading.HourlyBundle(
                        timestampMs = timestampFromBytes(bytes, 6),
                        steps       = bytes[12] + (bytes[11] shl 8) + (bytes[10] shl 16),
                        kcal        = bytes[15] + (bytes[14] shl 8) + (bytes[13] shl 16),
                        heartRate   = bytes[16],
                        spO2        = bytes[17],
                        systolic    = bytes[18],
                        diastolic   = bytes[19]
                    )
                }
                else -> WatchReading.Unknown(cat, sub, raw)
            }

            CAT_SLEEP -> {
                if (bytes.size < 14) return WatchReading.Unknown(cat, sub, raw)
                WatchReading.Sleep(
                    timestampMs     = timestampFromBytes(bytes, 6),
                    type            = bytes[11],
                    durationMinutes = bytes[12] + bytes[13]
                )
            }

            CAT_VITALS_BUNDLE -> {
                // 0x92 packet is device state broadcast — only HR is reliable here
                // BP comes via separate 0xFF-header packets
                if (bytes.size < 8) return WatchReading.Unknown(cat, sub, raw)
                val hr = bytes[7]
                if (hr in 30..220) WatchReading.HeartRate(nowMs, hr)
                else WatchReading.Unknown(cat, sub, raw)
            }
            CAT_STATUS -> {
                if (bytes.size < 8) return WatchReading.Unknown(cat, sub, raw)
                val batt = bytes[7]
                if (batt in 0..100) WatchReading.Battery(batt)
                else WatchReading.Unknown(cat, sub, raw)
            }
            CAT_MEAS_ACK, CAT_MEAS_RESULT, CAT_PPG_HISTORY ->
                WatchReading.Unknown(cat, sub, raw)
            else -> WatchReading.Unknown(cat, sub, raw)
        }
    }

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
