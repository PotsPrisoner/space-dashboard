package com.biospace.monitor.ble

import java.util.UUID

object WatchProtocol {

    // ── GATT UUIDs (confirmed from nRF Connect logs) ──────────────────────────
    val SERVICE_UUID     = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    val CHAR_NOTIFY_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
    val DESCRIPTOR_UUID  = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

    // ── Known packet type bytes (byte[4]) ─────────────────────────────────────
    private const val TYPE_STEPS    = 0x51
    private const val TYPE_BP_HIST  = 0x52
    private const val TYPE_SPO2     = 0x73
    private const val TYPE_TEMP     = 0x87
    private const val TYPE_BATTERY  = 0x91
    private const val TYPE_RESP     = 0x9E
    private const val TYPE_STRESS   = 0x74
    private const val TYPE_SLEEP    = 0x9B

    // ── Step / HR / BP sub-types (byte[5]) ───────────────────────────────────
    private const val STEPS_DAILY   = 0x08
    private const val STEPS_HOURLY  = 0x20
    private const val HR_READING    = 0x0B
    private const val SYS_READING   = 0x11
    private const val DIA_READING   = 0x12

    fun parse(raw: ByteArray): WatchReading? {
        if (raw.size < 5) return null
        val hdr  = raw[0].u8()
        if (hdr != 0xAB) return null
        val type = raw[4].u8()
        val sub  = if (raw.size > 5) raw[5].u8() else 0

        return when (type) {

            // Battery: AB-00-05-FF-91-80-00-2D → byte[7]
            TYPE_BATTERY -> if (raw.size >= 8) WatchReading.Battery(raw[7].u8()) else null

            // Temperature: AB-00-04-FF-87-80-01 → byte[6]
            TYPE_TEMP -> if (raw.size >= 7) WatchReading.Temperature(raw[6].u8()) else null

            // Steps / HR / BP subtypes
            TYPE_STEPS -> when (sub) {
                STEPS_DAILY -> if (raw.size >= 10)
                    WatchReading.StepsSummary((raw[8].u8() shl 8) or raw[9].u8()) else null

                STEPS_HOURLY -> if (raw.size >= 14)
                    WatchReading.StepsHourly(
                        year  = 2000 + raw[6].u8(), month = raw[7].u8(),
                        day   = raw[8].u8(),         hour  = raw[9].u8(),
                        steps = (raw[12].u8() shl 8) or raw[13].u8()
                    ) else null

                HR_READING -> if (raw.size >= 12)
                    WatchReading.HeartRate(
                        year = 2000 + raw[6].u8(), month = raw[7].u8(),
                        day  = raw[8].u8(),         hour  = raw[9].u8(),
                        minute = raw[10].u8(),       bpm   = raw[11].u8()
                    ) else null

                SYS_READING -> if (raw.size >= 12)
                    WatchReading.Systolic(
                        year = 2000 + raw[6].u8(), month = raw[7].u8(),
                        day  = raw[8].u8(),         hour  = raw[9].u8(),
                        minute = raw[10].u8(),       mmHg  = raw[11].u8()
                    ) else null

                DIA_READING -> if (raw.size >= 12)
                    WatchReading.Diastolic(
                        year = 2000 + raw[6].u8(), month = raw[7].u8(),
                        day  = raw[8].u8(),         hour  = raw[9].u8(),
                        minute = raw[10].u8(),       mmHg  = raw[11].u8()
                    ) else null

                else -> null
            }

            // BP history: AB-00-0B-FF-52-80-YY-MM-DD-HH-mm-SYS-DIA
            TYPE_BP_HIST -> if (raw.size >= 13)
                WatchReading.BloodPressure(
                    year = 2000 + raw[6].u8(), month = raw[7].u8(),
                    day  = raw[8].u8(),         hour  = raw[9].u8(),
                    minute = raw[10].u8(),       systolic  = raw[11].u8(),
                    diastolic = raw[12].u8()
                ) else null

            // SpO2 hourly: byte[10-11] = value
            TYPE_SPO2 -> if (raw.size >= 12) {
                val raw16 = (raw[10].u8() shl 8) or raw[11].u8()
                WatchReading.SpO2(if (raw16 > 100) raw16 / 100 else raw16)
            } else null

            // Respiratory rate: byte[10-11] = value
            TYPE_RESP -> if (raw.size >= 12) {
                val raw16 = (raw[10].u8() shl 8) or raw[11].u8()
                WatchReading.RespiratoryRate(if (raw16 > 200) raw16 / 100 else raw16)
            } else null

            // Stress score: byte[6]
            TYPE_STRESS -> if (raw.size >= 7) WatchReading.Stress(raw[6].u8()) else null

            // Sleep: byte[6]=light minutes, byte[7]=deep minutes
            TYPE_SLEEP -> if (raw.size >= 8)
                WatchReading.Sleep(raw[6].u8(), raw[7].u8()) else null

            else -> null
        }
    }

    private fun Byte.u8() = this.toInt() and 0xFF
}

sealed class WatchReading {
    data class Battery(val percent: Int) : WatchReading()
    data class Temperature(val celsius: Int) : WatchReading()
    data class StepsSummary(val steps: Int) : WatchReading()
    data class StepsHourly(val year: Int, val month: Int, val day: Int, val hour: Int, val steps: Int) : WatchReading()
    data class HeartRate(val year: Int, val month: Int, val day: Int, val hour: Int, val minute: Int, val bpm: Int) : WatchReading()
    data class Systolic(val year: Int, val month: Int, val day: Int, val hour: Int, val minute: Int, val mmHg: Int) : WatchReading()
    data class Diastolic(val year: Int, val month: Int, val day: Int, val hour: Int, val minute: Int, val mmHg: Int) : WatchReading()
    data class BloodPressure(val year: Int, val month: Int, val day: Int, val hour: Int, val minute: Int, val systolic: Int, val diastolic: Int) : WatchReading()
    data class SpO2(val percent: Int) : WatchReading()
    data class RespiratoryRate(val rpm: Int) : WatchReading()
    data class Stress(val score: Int) : WatchReading()
    data class Sleep(val lightMinutes: Int, val deepMinutes: Int) : WatchReading()
}
