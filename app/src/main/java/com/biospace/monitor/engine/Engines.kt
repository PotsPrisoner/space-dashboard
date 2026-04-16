package com.biospace.monitor.engine

import com.biospace.monitor.model.*
import kotlin.math.*

object SREngine {

    fun computeSRMetrics(sw: SpaceWeatherState): SRMetrics {
        val tecAnomaly = sw.tecLocal - sw.tecMedian
        val tecFreqShift = maxOf(0.0, tecAnomaly) * 0.065

        val bzAbs = abs(sw.bz)
        val bzNoiseShift = if (bzAbs < 2) (2 - bzAbs) * 0.04 else 0.0

        val speedExcess = maxOf(0.0, sw.speed - 350)
        val speedFreqShift = speedExcess * 0.00008

        val f1 = 7.83 + tecFreqShift + bzNoiseShift + speedFreqShift
        val drift = f1 - 7.83

        // Q-Factor
        var qBase = 6.0
        if (bzAbs < 2) qBase -= 2.0
        if (bzAbs < 5 && sw.bz < 0) qBase -= 0.8
        if (sw.speed > 450) qBase -= 0.6
        if (sw.speed > 550) qBase -= 0.8
        if (sw.kp > 3) qBase -= (sw.kp - 3) * 0.4
        val qFactor = qBase.coerceIn(1.0, 7.0)

        // Amplitude
        val ampBase = 1.5 + (if (tecAnomaly > 0) tecAnomaly * 0.15 else 0.0) +
                (if (sw.speed > 400) (sw.speed - 400) * 0.003 else 0.0)
        val amplitude = ampBase.coerceIn(0.8, 8.0)

        // Intensity
        val intensity = (amplitude * amplitude) / qFactor

        // Coherence score
        var loadUnits = 0
        if (intensity > 1.0) loadUnits += 1
        if (intensity > 2.0) loadUnits += 1
        if (abs(drift) > 0.15) loadUnits += 2
        if (abs(drift) > 0.4) loadUnits += 2
        if (qFactor < 4) loadUnits += 3
        if (qFactor < 2.5) loadUnits += 2
        if (bzAbs < 2) loadUnits += 2
        if (tecAnomaly > 2) loadUnits += 1
        val coherenceScore = (100 - loadUnits * 8).coerceIn(0, 100)

        return SRMetrics(
            f1 = f1,
            drift = drift,
            qFactor = qFactor,
            amplitude = amplitude,
            intensity = intensity,
            coherenceScore = coherenceScore
        )
    }
}

object ANSEngine {

    fun computeANSLoad(sw: SpaceWeatherState, sr: SRMetrics): ANSState {
        val driftF = sr.drift
        val qF = sr.qFactor
        val intensityF = sr.intensity

        val symptoms = buildSymptomList(sw, sr, driftF, qF, intensityF)
            .sortedByDescending { it.probability }

        val topProbs = symptoms.take(4).map { it.probability.toDouble() }
        val avgLoad = (topProbs.sum() / topProbs.size).roundToInt()

        val sympatheticBias = (50 + (7 - qF) * 4 + driftF * 8 + (if (abs(sw.bz) < 2) 6.0 else 0.0))
            .coerceIn(10.0, 95.0)

        val hrvImpact = when {
            qF < 3 -> "SEVERELY SUPPRESSED"
            qF < 4 -> "SUPPRESSED"
            sw.kp > 4 -> "REDUCED"
            else -> "NORMAL"
        }
        val cortisolAxis = when {
            sw.kp > 5 || sw.speed > 550 -> "HIGH ELEVATION"
            sw.kp > 3 || sw.speed > 450 -> "ELEVATED"
            else -> "NORMAL"
        }
        val melatonin = when {
            sr.amplitude > 3 && sw.kp > 3 -> "SUPPRESSED"
            sr.amplitude > 2 -> "REDUCED"
            else -> "VARIABLE"
        }

        val protocols = buildProtocol(avgLoad, sw, sr)

        return ANSState(
            loadIndex = avgLoad,
            sympatheticBias = sympatheticBias,
            hrvImpact = hrvImpact,
            cortisolAxis = cortisolAxis,
            melatonin = melatonin,
            coherencePct = (100 - avgLoad).coerceIn(0, 100),
            symptoms = symptoms,
            protocols = protocols
        )
    }

    private fun buildSymptomList(
        sw: SpaceWeatherState, sr: SRMetrics,
        driftF: Double, qF: Double, intensityF: Double
    ): List<SymptomPrediction> {
        val bzAbs = abs(sw.bz)

        return listOf(
            // 1. Orthostatic Tachycardia
            run {
                var p = 30
                if (sw.kp > 2) p += 15
                if (sw.kp > 4) p += 15
                if (sw.bz < -2) p += 20
                if (sw.speed > 450) p += 10
                if (intensityF > 1.5) p += 10
                val sev = if (sw.kp > 4 || sw.bz < -5) "high" else if (sw.kp > 2) "moderate" else "low"
                SymptomPrediction(
                    "Orthostatic Tachycardia / POTS Flare", "💓",
                    p.coerceAtMost(95), sev,
                    "Geomagnetic Kp elevation directly suppresses vagal tone via magnetoreception pathways in the sinoatrial node. Bz southward excursions compound this through rapid IMF-driven baroreceptor desensitization. High solar wind dynamic pressure increases cardiac preload requirements in upright posture.",
                    listOf("Kp=${String.format("%.1f", sw.kp)}", "Bz=${String.format("%.1f", sw.bz)}nT", "Speed=${sw.speed.toInt()}km/s")
                )
            },
            // 2. Fatigue / Crash
            run {
                var p = 25
                if (qF < 4) p += 20
                if (qF < 3) p += 15
                if (sw.kp > 3) p += 15
                if (intensityF > 2) p += 10
                if (driftF > 0.2) p += 10
                val sev = if (qF < 3 || sw.kp > 4) "high" else if (qF < 4) "moderate" else "low"
                SymptomPrediction(
                    "Post-Exertional Fatigue / ANS Crash", "⚡",
                    p.coerceAtMost(93), sev,
                    "Low SR Q-factor creates broadband ELF noise that forces continuous compensatory mitochondrial ATP expenditure to maintain cellular membrane potentials. This metabolic cost is invisible but cumulative — the result is accelerated energy depletion even at rest.",
                    listOf("Q-factor=${String.format("%.1f", qF)}", "SR intensity=${String.format("%.2f", intensityF)}", "Kp=${String.format("%.1f", sw.kp)}")
                )
            },
            // 3. Sleep Disruption
            run {
                var p = 25
                if (sr.amplitude > 2) p += 20
                if (sr.amplitude > 3.5) p += 10
                if (driftF > 0.15) p += 15
                if (sw.bz < -2) p += 15
                if (sw.kp > 3) p += 10
                val sev = if (sr.amplitude > 3 || sw.kp > 3) "high" else if (driftF > 0.2) "moderate" else "low"
                SymptomPrediction(
                    "Sleep Disruption & Insomnia", "🌙",
                    p.coerceAtMost(90), sev,
                    "Elevated SR amplitude at Mode 6 (39.5 Hz Gamma) interferes with pineal melatonin synthesis. f₁ drift away from deep Theta disrupts sleep-onset architecture. HRV coherence reduction during NREM stages compounds the effect.",
                    listOf("SR amplitude=${String.format("%.2f", sr.amplitude)}pT", "f₁ drift=+${String.format("%.3f", driftF)}Hz", "Bz=${String.format("%.1f", sw.bz)}nT")
                )
            },
            // 4. Cognitive Fog
            run {
                var p = 20
                if (qF < 4) p += 20
                if (qF < 3) p += 15
                if (sw.tecLocal - sw.tecMedian > 1.5) p += 10
                if (sw.speed > 400) p += 10
                if (driftF > 0.2) p += 10
                val sev = if (qF < 3) "high" else if (qF < 4) "moderate" else "low"
                SymptomPrediction(
                    "Cognitive Fog & Impaired Focus", "🧠",
                    p.coerceAtMost(88), sev,
                    "Broadband ELF noise (low Q) forces the prefrontal cortex to filter conflicting electromagnetic input signals. Mode 3 (20.8 Hz Mid Beta) amplifies hypervigilance pathways, crowding out executive function bandwidth.",
                    listOf("Q-factor=${String.format("%.1f", qF)}", "TEC +${String.format("%.1f", sw.tecLocal - sw.tecMedian)}TECU", "Speed=${sw.speed.toInt()}km/s")
                )
            },
            // 5. Headache
            run {
                var p = 15
                if (sw.bz < -2) p += 25
                if (sw.bz < -5) p += 20
                if (sw.speed > 450) p += 20
                if (sw.kp > 3) p += 10
                if (bzAbs < 2 && sw.speed > 400) p += 10
                val sev = if (sw.bz < -5 || sw.kp > 4) "high" else if (sw.bz < -2 || sw.speed > 450) "moderate" else "low"
                SymptomPrediction(
                    "Headache & Pressure Sensitivity", "🎯",
                    p.coerceAtMost(85), sev,
                    "Magnetopause compression from CH HSS CIR arrival creates transient intracranial EM environment changes. Pc5 pulsations (0.2–5 Hz) produce fluctuating magnetospheric pressure perceived as frontal or temporal pressure by sensitized individuals.",
                    listOf("Bz=${String.format("%.1f", sw.bz)}nT", "Speed=${sw.speed.toInt()}km/s", "Kp=${String.format("%.1f", sw.kp)}")
                )
            },
            // 6. HRV Suppression
            run {
                var p = 25
                if (qF < 4) p += 20
                if (qF < 3) p += 15
                if (intensityF > 1.5) p += 10
                if (sw.kp > 2) p += 10
                if (sw.bz < -2) p += 10
                val sev = if (qF < 3) "high" else if (qF < 4) "moderate" else "low"
                SymptomPrediction(
                    "Heart Rate Variability Suppression", "📉",
                    p.coerceAtMost(88), sev,
                    "HRV coherence strongly correlates with SR amplitude stability (McCraty et al., 2017). Low Q-factor disrupts the 0.1 Hz autonomic oscillation cycle (Mayer wave frequency), pushing ANS toward sympathetic dominance.",
                    listOf("Q-factor=${String.format("%.1f", qF)}", "SR intensity=${String.format("%.2f", intensityF)}", "Kp=${String.format("%.1f", sw.kp)}")
                )
            },
            // 7. Sensory Hypersensitivity
            run {
                var p = 15
                if (qF < 3.5) p += 20
                if (intensityF > 2) p += 15
                if (driftF > 0.3) p += 10
                if (sw.kp > 3) p += 15
                val sev = if (qF < 2.5 || sw.kp > 4) "high" else if (qF < 3.5) "moderate" else "low"
                SymptomPrediction(
                    "Sensory Hypersensitivity", "🔊",
                    p.coerceAtMost(80), sev,
                    "Mode 5 (33 Hz Gamma boundary) interferes with sensory binding — the neural process integrating discrete sensory streams into coherent perception. Effect is heightened sensitivity to light, sound, and tactile input.",
                    listOf("Q-factor=${String.format("%.1f", qF)}", "SR intensity=${String.format("%.2f", intensityF)}", "f₁ drift=+${String.format("%.3f", driftF)}Hz")
                )
            },
            // 8. Muscle Tension
            run {
                var p = 20
                if (sw.speed > 420) p += 15
                if (intensityF > 1.5) p += 15
                if (sw.kp > 2) p += 10
                if (qF < 3.5) p += 10
                val sev = if (sw.speed > 500 || sw.kp > 3) "moderate" else "low"
                SymptomPrediction(
                    "Muscle Tension & Restlessness", "🦴",
                    p.coerceAtMost(78), sev,
                    "The 14.3 Hz second SR harmonic overlaps the sensorimotor rhythm (SMR, ~12–15 Hz). Amplitude elevation at this mode elevates motor cortex excitability, producing involuntary muscle activation patterns and jaw clenching.",
                    listOf("Speed=${sw.speed.toInt()}km/s", "SR intensity=${String.format("%.2f", intensityF)}", "Kp=${String.format("%.1f", sw.kp)}")
                )
            }
        )
    }

    private fun buildProtocol(total: Int, sw: SpaceWeatherState, sr: SRMetrics): List<String> {
        val items = mutableListOf<String>()
        if (sw.speed > 450 || sw.kp > 3) {
            items.add("GEOMAGNETIC: Elevated solar wind activity warrants postponing cognitively and physically demanding activities. Peak ANS vulnerability typically 2–8 hours after Kp spike onset.")
        }
        if (abs(sw.bz) < 2) {
            items.add("IMF WATCH: Bz near-zero — watch for sudden southward excursion. Diaphragmatic breathing at 0.1 Hz (6 breaths/min, 5s in / 5s out) provides direct HRV stabilization.")
        }
        if (sw.bz < -2) {
            items.add("Bz SOUTHWARD: Active geoeffective IMF. Increase oral salt and fluid loading now, before orthostatic symptoms manifest. Consider compression garments.")
        }
        if (sr.qFactor < 4 || sr.intensity > 2) {
            items.add("SR FIELD: Ground exposure (direct skin-earth contact, grounding/earthing) for 20+ minutes provides direct electron transfer and may compensate for low Q-factor. Best morning or evening.")
        }
        items.add("PACING: Combined load of $total/100 warrants planned rest windows. Autonomic banking — resting before activity rather than after — is more effective during high-load windows.")
        items.add("HYDRATION: 2–3L fluid + adequate sodium chloride. Dysautonomia increases sweat sodium losses. Electrolyte replacement (not plain water) is essential during elevated geomagnetic windows.")
        if (total > 50) {
            items.add("MONITORING: HRV tracking via wearable is the most accessible real-time ANS proxy. A sudden HRV drop >15% from personal baseline indicates compensatory threshold crossed — initiate rest protocol.")
        }
        return items.take(6)
    }

    fun computeIntegratedAssessment(
        sw: SpaceWeatherState, sr: SRMetrics, weather: WeatherState
    ): IntegratedAssessment {
        // Space score
        var spaceScore = 0
        if (sw.kp > 2) spaceScore += (sw.kp * 3).toInt()
        if (sw.kp > 5) spaceScore += 10
        if (sw.bz < -2) spaceScore += (abs(sw.bz) * 2).toInt()
        if (sw.speed > 400) spaceScore += ((sw.speed - 400) / 20).toInt()
        if (sw.density > 10) spaceScore += 5
        spaceScore = spaceScore.coerceAtMost(40)

        // SR score
        var srScore = 0
        if (abs(sr.drift) > 0.1) srScore += 5
        if (abs(sr.drift) > 0.3) srScore += 5
        if (sr.qFactor < 5) srScore += ((5 - sr.qFactor) * 3).toInt()
        if (sr.intensity > 1.5) srScore += 5
        srScore = srScore.coerceAtMost(30)

        // Env score
        var envScore = 0
        if (weather.tempF > 85) envScore += ((weather.tempF - 85) / 3).toInt()
        if (weather.humidity > 65) envScore += 5
        if (weather.humidity > 80) envScore += 5
        val pressureDelta = if (weather.pressureHistory.size >= 2)
            weather.pressureHistory.last() - weather.pressureHistory.first() else 0.0
        if (pressureDelta < -2) envScore += 8
        if (pressureDelta < -5) envScore += 7
        envScore = envScore.coerceAtMost(30)

        val total = (spaceScore + srScore + envScore).coerceAtMost(100)

        val label = when {
            total > 70 -> "CRITICAL LOAD"
            total > 50 -> "HIGH LOAD"
            total > 30 -> "MODERATE LOAD"
            else -> "LOW LOAD"
        }

        val narrative = buildNarrative(total, spaceScore, srScore, envScore, sw, sr, weather)

        return IntegratedAssessment(
            score = total,
            label = label,
            spaceScore = spaceScore,
            srScore = srScore,
            envScore = envScore,
            narrative = narrative,
            protocols = buildProtocol(total, sw, sr)
        )
    }

    private fun buildNarrative(
        total: Int, space: Int, sr: Int, env: Int,
        sw: SpaceWeatherState, srM: SRMetrics, weather: WeatherState
    ): String {
        val spacePart = when {
            sw.kp > 5 -> "a geomagnetic storm (Kp ${String.format("%.1f", sw.kp)}) is actively compressing the magnetosphere"
            sw.kp > 3 -> "elevated geomagnetic activity (Kp ${String.format("%.1f", sw.kp)}) is disturbing the inner magnetosphere"
            else -> "geomagnetic conditions are relatively quiet (Kp ${String.format("%.1f", sw.kp)})"
        }
        val srPart = when {
            srM.qFactor < 3 -> "Schumann Resonance coherence is severely degraded (Q=${String.format("%.1f", srM.qFactor)}) — the Earth-ionosphere cavity is acting as a broadband noise source"
            srM.qFactor < 4.5 -> "SR field coherence is reduced (Q=${String.format("%.1f", srM.qFactor)}), creating moderate ELF background noise"
            else -> "SR field quality is adequate (Q=${String.format("%.1f", srM.qFactor)})"
        }
        val envPart = when {
            weather.tempF > 90 -> "local heat load (${weather.tempF.toInt()}°F) is at a critical dysautonomia trigger threshold"
            weather.tempF > 80 -> "local temperature (${weather.tempF.toInt()}°F) is elevated and adds meaningful thermal stress"
            else -> "local environmental conditions are within manageable range (${weather.tempF.toInt()}°F)"
        }
        val overall = when {
            total > 70 -> "This represents a CRITICAL BURDEN window. Significant dysautonomic flares should be anticipated."
            total > 50 -> "This represents an elevated burden window. Geosensitive individuals should expect noticeable dysautonomic activity."
            total > 30 -> "This is a moderate background load window. Those with poor baseline reserve may notice symptom clustering."
            else -> "Conditions are in a relatively low-burden window — a favorable period for autonomic recovery."
        }
        return "As of this reading, $spacePart. Simultaneously, $srPart. On the ground, $envPart. $overall The integrated load index of $total/100 reflects the combined electromagnetic, ionospheric, and local environmental pressure on the autonomic nervous system at this moment."
    }
}

private fun Double.roundToInt(): Int = kotlin.math.round(this).toInt()
