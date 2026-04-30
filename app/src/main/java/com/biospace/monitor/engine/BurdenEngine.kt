package com.biospace.monitor.engine

import com.biospace.monitor.data.*
import kotlin.math.*

object BurdenEngine {

    private val kpHistory   = ArrayDeque<Float>(12)
    private val swHistory   = ArrayDeque<Float>(12)
    private val bzHistory   = ArrayDeque<Float>(12)
    private val hrHistory   = ArrayDeque<Int>(10)
    private val spO2History = ArrayDeque<Int>(10)
    private val bpHistory   = ArrayDeque<Int>(10)
    private val pressHistory= ArrayDeque<Float>(12)
    private val rmssdHistory= ArrayDeque<Float>(12)

    fun update(sw: SpaceWeather, wx: WeatherState, bio: Biometrics): BurdenScore {
        push(kpHistory, sw.kp); push(swHistory, sw.swSpeed); push(bzHistory, sw.imfBz)
        push(pressHistory, wx.pressure)
        if (bio.heartRate > 0) push(hrHistory, bio.heartRate)
        if (bio.spO2 > 0)     push(spO2History, bio.spO2)
        if (bio.bpSys > 0)    push(bpHistory, bio.bpSys)
        if (bio.rmssd > 0)    push(rmssdHistory, bio.rmssd)

        val c = mutableMapOf<String, BurdenComponent>()

        // SPACE WEATHER
        val kpMag  = (sw.kp / 9f).coerceIn(0f,1f) * 100f
        val kpFluc = fluc(kpHistory) * 15f
        c["Kp Index"] = comp("Kp Index", kpMag, kpFluc, "", sw.kp.toString())

        val swMag  = ((sw.swSpeed - 300f) / 500f).coerceIn(0f,1f) * 100f
        val swFluc = fluc(swHistory) * 12f
        c["Solar Wind"] = comp("Solar Wind", swMag, swFluc, "km/s", "${sw.swSpeed.toInt()}")

        val bzMag  = if (sw.imfBz < 0) (abs(sw.imfBz) / 20f).coerceIn(0f,1f) * 100f else 0f
        val bzFluc = fluc(bzHistory) * 20f
        c["IMF Bz"] = comp("IMF Bz", bzMag, bzFluc, "nT", "${"%.1f".format(sw.imfBz)}")

        val flareMag = sw.flares.fold(0f) { acc, f ->
            acc + when { f.flareClass.startsWith("X") -> 40f; f.flareClass.startsWith("M") -> 20f;
                         f.flareClass.startsWith("C") -> 8f; else -> 2f }
        }.coerceAtMost(100f)
        val flareFluc = if (sw.flares.any { it.hasCme }) 30f else if (sw.flares.isNotEmpty()) 10f else 0f
        c["Solar Flares"] = comp("Solar Flares", flareMag, flareFluc, "", "${sw.flares.size} events")

        val cmeMag  = ((sw.cmeSpeed - 300f) / 700f).coerceIn(0f,1f) * 100f
        val cmeFluc = if (sw.cmeArrivalHrs < 48) (if (sw.cmeAngle < 30) 45f else 25f) else 0f
        c["CME"] = comp("CME", cmeMag, cmeFluc, "km/s", "${sw.cmeSpeed.toInt()}")

        val gstMag = if (sw.gstActive) sw.kp / 9f * 80f else 0f
        c["Geomag Storm"] = comp("Geomag Storm", gstMag, if (sw.gstActive) 20f else 0f)

        val ipsMag = (sw.ipsCount * 25f).coerceAtMost(100f)
        c["Interplan. Shock"] = comp("Interplan. Shock", ipsMag, ipsMag * 0.3f)

        val hssMag = if (sw.hssActive) 30f else 0f
        c["High Speed Stream"] = comp("High Speed Stream", hssMag, if (sw.hssActive) 15f else 0f)

        val mpcMag = (sw.mpcCount * 20f).coerceAtMost(80f)
        c["Magnetopause"] = comp("Magnetopause", mpcMag, mpcMag * 0.4f)

        val rbeMag = (sw.rbeCount * 15f).coerceAtMost(60f)
        c["Radiation Belt"] = comp("Radiation Belt", rbeMag, rbeMag * 0.2f)

        val sepMag = if (sw.sepActive) 50f else 0f
        c["Solar En. Particles"] = comp("Solar En. Particles", sepMag, if (sw.sepActive) 25f else 0f)

        val hpMag  = ((sw.hemisphericPower - 10f) / 120f).coerceIn(0f,1f) * 100f
        val hpFluc = if (sw.fountainDumping == "ACTIVE") 35f else if (sw.fountainDumping == "MODERATE") 15f else 2f
        c["Hemispheric Power"] = comp("Hemispheric Power", hpMag, hpFluc, "GW", "${sw.hemisphericPower.toInt()}")

        val srMag  = (abs(sw.srDrift) * 30f + (sw.srAmplitude - 1f) * 5f).coerceIn(0f,100f)
        val srFluc = if (abs(sw.srDrift) > 0.15f) 20f else if (abs(sw.srDrift) > 0.05f) 8f else 2f
        c["Schumann Res."] = comp("Schumann Res.", srMag, srFluc, "Hz", "${"%.2f".format(sw.srFundamental)}")

        val poleMag = ((70f - sw.poleDist) / 70f).coerceIn(0f,1f) * 40f
        c["Pole Proximity"] = comp("Pole Proximity", poleMag, poleMag * 0.1f, "°", "${sw.poleDist.toInt()}")

        val emfMag = ((sw.localMagNt + sw.industrialNt - 20f) / 80f).coerceIn(0f,1f) * 60f
        c["Local EMF"] = comp("Local EMF", emfMag, emfMag * 0.15f, "nT", "${(sw.localMagNt + sw.industrialNt).toInt()}")

        // WEATHER
        val pressMag  = (abs(wx.pressureTrend) / 8f).coerceIn(0f,1f) * 50f
        val pressFluc = fluc(pressHistory) * 20f
        c["Barometric Press."] = comp("Barometric Press.", pressMag, pressFluc, "hPa", "${"%.1f".format(wx.pressure)}")

        val heatMag = (maxOf(0f, (wx.heatIndex - 75f) / 30f) +
            if (wx.humidity > 75f) 0.15f else if (wx.humidity > 60f) 0.05f else 0f).coerceIn(0f,1f) * 70f
        c["Heat/Humidity"] = comp("Heat/Humidity", heatMag, heatMag * 0.1f, "°F", "${wx.temp.toInt()}°/${wx.humidity.toInt()}%")

        val aqMag = ((wx.airQuality - 50f) / 150f).coerceIn(0f,1f) * 40f
        c["Air Quality"] = comp("Air Quality", aqMag, aqMag * 0.1f, "AQI", "${wx.airQuality}")

        // BIOMETRICS
        val hrMag = if (bio.heartRate > 0) when {
            bio.heartRate > 120 -> 80f; bio.heartRate > 100 -> 50f
            bio.heartRate in 1..54 -> 30f; else -> 5f
        } else 0f
        val hrFluc = fluc(hrHistory) * 25f
        c["Heart Rate"] = comp("Heart Rate", hrMag, hrFluc, "bpm",
            if (bio.heartRate > 0) "${bio.heartRate}" else "--")

        val spO2Mag = if (bio.spO2 > 0) when {
            bio.spO2 < 88 -> 90f; bio.spO2 < 92 -> 65f; bio.spO2 < 95 -> 30f; bio.spO2 < 97 -> 10f; else -> 0f
        } else 0f
        val spO2Fluc = fluc(spO2History) * 20f
        c["SpO2"] = comp("SpO2", spO2Mag, spO2Fluc, "%", if (bio.spO2 > 0) "${bio.spO2}%" else "--")

        val bpMag = if (bio.bpSys > 0) when {
            bio.bpSys > 160 -> 80f; bio.bpSys > 140 -> 50f; bio.bpSys > 130 -> 25f
            bio.bpSys < 90 -> 60f; bio.bpSys < 100 -> 35f; else -> 5f
        } else 0f
        val bpFluc = fluc(bpHistory) * 22f
        c["Blood Pressure"] = comp("Blood Pressure", bpMag, bpFluc, "mmHg",
            if (bio.bpSys > 0) "${bio.bpSys}/${bio.bpDia}" else "--")

        val hrvMag = if (bio.rmssd > 0) when {
            bio.rmssd < 15 -> 75f; bio.rmssd < 25 -> 45f; bio.rmssd < 40 -> 20f; else -> 5f
        } else 0f
        val hrvFluc = fluc(rmssdHistory) * 30f
        c["HRV (RMSSD)"] = comp("HRV (RMSSD)", hrvMag, hrvFluc, "ms",
            if (bio.rmssd > 0) "${"%.0f".format(bio.rmssd)}" else "--")

        val sleepMag = when {
            bio.sleepHours < 4 -> 60f; bio.sleepHours < 6 -> 35f
            bio.sleepQuality < 40 -> 40f; bio.sleepQuality < 60 -> 20f; else -> 5f
        }
        c["Sleep"] = comp("Sleep", sleepMag, 0f, "hrs",
            if (bio.sleepHours > 0) "${"%.1f".format(bio.sleepHours)}" else "--")

        val stressMag = (bio.stressScore / 100f).coerceIn(0f,1f) * 60f
        c["Stress Score"] = comp("Stress Score", stressMag, 0f, "",
            if (bio.stressScore > 0) "${bio.stressScore}" else "--")

        // WEIGHTED AGGREGATE
        val weights = mapOf(
            "Kp Index" to 1.4f, "Solar Wind" to 1.0f, "IMF Bz" to 1.6f,
            "Solar Flares" to 1.2f, "CME" to 1.3f, "Geomag Storm" to 1.5f,
            "Interplan. Shock" to 1.0f, "High Speed Stream" to 0.8f,
            "Magnetopause" to 0.9f, "Radiation Belt" to 0.7f, "Solar En. Particles" to 1.1f,
            "Hemispheric Power" to 1.0f, "Schumann Res." to 0.8f,
            "Pole Proximity" to 0.6f, "Local EMF" to 0.7f,
            "Barometric Press." to 1.2f, "Heat/Humidity" to 0.9f, "Air Quality" to 0.6f,
            "Heart Rate" to 1.8f, "SpO2" to 1.9f, "Blood Pressure" to 1.8f,
            "HRV (RMSSD)" to 1.6f, "Sleep" to 1.2f, "Stress Score" to 1.0f
        )
        var wSum = 0f; var wTotal = 0f
        c.forEach { (k, v) -> val w = weights[k] ?: 1f; wSum += v.combined * w; wTotal += w }
        val overall = (wSum / wTotal).coerceIn(0f, 100f)
        val mag  = c.values.map { it.magnitude }.average().toFloat()
        val fluc = c.values.map { it.fluctuation }.average().toFloat()

        val level = when {
            overall >= 50f -> AlertLevel.BLUE; overall >= 25f -> AlertLevel.RED
            overall >= 7f  -> AlertLevel.YELLOW; else -> AlertLevel.GREEN
        }

        val top = c.entries.sortedByDescending { it.value.combined }.take(3).map { it.key }
        val narrative = buildString {
            append("ANS burden ${overall.toInt()}% — ")
            if (overall < 7) append("Conditions favorable. ") else
            if (overall < 25) append("Mild environmental loading. ") else
            if (overall < 50) append("Moderate ANS stress. ") else append("HIGH burden — rest. ")
            append("Top drivers: ${top.joinToString(", ")}.")
            if (bio.heartRate > 100) append(" Tachycardia (${bio.heartRate} bpm).")
            if (bio.spO2 in 1..94) append(" Low SpO2 (${bio.spO2}%).")
            if (sw.imfBz < -5) append(" Southward IMF (${"%.1f".format(sw.imfBz)} nT).")
            if (abs(wx.pressureTrend) > 2f) append(" Rapid pressure change.")
        }

        return BurdenScore(overall, mag, fluc, level, c, narrative)
    }

    private fun comp(name: String, mag: Float, fluc: Float, unit: String = "", value: String = "") =
        BurdenComponent(name, mag, fluc, (mag * 0.4f + fluc * 0.6f).coerceIn(0f, 100f), unit, value)

    private fun <T : Number> push(dq: ArrayDeque<T>, v: T) {
        if (dq.size >= 12) dq.removeFirst(); dq.addLast(v)
    }
    private fun <T : Number> fluc(dq: ArrayDeque<T>): Float {
        if (dq.size < 3) return 0f
        val vals = dq.map { it.toFloat() }
        val diffs = (1 until vals.size).map { abs(vals[it] - vals[it-1]) }
        val mean = vals.average().toFloat()
        return if (mean == 0f) 0f else (diffs.average().toFloat() / (mean + 0.001f)).coerceIn(0f, 1f)
    }
}
