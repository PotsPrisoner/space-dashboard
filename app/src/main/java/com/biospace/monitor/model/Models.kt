package com.biospace.monitor.model

import com.google.gson.annotations.SerializedName

// ── NOAA Kp Index ──────────────────────────────────────────────────────────────
data class KpEntry(
    @SerializedName("time_tag") val timeTag: String = "",
    @SerializedName("kp_index") val kpIndex: String = "0"
)

// ── Solar Wind Plasma ─────────────────────────────────────────────────────────
// Returns array of arrays: [time_tag, density, speed, temperature]
typealias PlasmaEntry = List<Any>

// ── Solar Wind Mag (IMF) ──────────────────────────────────────────────────────
// Returns array of arrays: [time_tag, bx, by, bz, phi, theta, bt]
typealias MagEntry = List<Any>

// ── X-Ray Flux ────────────────────────────────────────────────────────────────
data class XrayEntry(
    @SerializedName("time_tag") val timeTag: String = "",
    val flux: String = "0",
    @SerializedName("satellite") val satellite: Int = 0
)

// ── NOAA Alerts ───────────────────────────────────────────────────────────────
data class NoaaAlert(
    @SerializedName("product_id") val productId: String = "",
    val message: String = ""
)

// ── Hemispheric Power ─────────────────────────────────────────────────────────
data class HemisphericPower(
    @SerializedName("Hemispheric Power") val hp: HpValues? = null,
    @SerializedName("north_gw") val northGw: String? = null,
    @SerializedName("south_gw") val southGw: String? = null
)

data class HpValues(
    val north: String? = null,
    val North: String? = null,
    val south: String? = null,
    val South: String? = null
) {
    fun northVal() = (north ?: North)?.toDoubleOrNull() ?: Double.NaN
    fun southVal() = (south ?: South)?.toDoubleOrNull() ?: Double.NaN
}

// ── CME from NASA DONKI ───────────────────────────────────────────────────────
data class CmeEvent(
    val activityID: String = "",
    val startTime: String = "",
    val note: String = "",
    val cmeAnalyses: List<CmeAnalysis>? = null,
    val linkedEvents: List<LinkedEvent>? = null
)

data class CmeAnalysis(
    val speed: Double? = null,
    val type: String? = null,
    val isMostAccurate: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val halfAngle: Double? = null
)

data class LinkedEvent(
    val activityID: String = ""
)

// ── Open-Meteo Weather ────────────────────────────────────────────────────────
data class WeatherResponse(
    val current: CurrentWeather? = null,
    val hourly: HourlyWeather? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class CurrentWeather(
    val temperature_2m: Double = 0.0,
    val relative_humidity_2m: Int = 0,
    val surface_pressure: Double = 1013.0,
    val wind_speed_10m: Double = 0.0,
    val apparent_temperature: Double = 0.0,
    val weather_code: Int = 0
)

data class HourlyWeather(
    val time: List<String> = emptyList(),
    val surface_pressure: List<Double> = emptyList(),
    val temperature_2m: List<Double> = emptyList(),
    val relative_humidity_2m: List<Int> = emptyList()
)

// ── Geocoding ─────────────────────────────────────────────────────────────────
data class GeocodingResponse(
    val results: List<GeoResult>? = null
)

data class GeoResult(
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val admin1: String? = null,
    val country: String? = null
)

// ── App UI State ──────────────────────────────────────────────────────────────
data class SpaceWeatherState(
    val kp: Double = 0.0,
    val kpHistory: List<Double> = emptyList(),
    val speed: Double = 400.0,
    val density: Double = 5.0,
    val temperature: Double = 100000.0,
    val bz: Double = 0.0,
    val bt: Double = 5.0,
    val bx: Double = 0.0,
    val by: Double = 0.0,
    val phi: Double = 0.0,
    val theta: Double = 0.0,
    val bzHistory: List<Double> = emptyList(),
    val btHistory: List<Double> = emptyList(),
    val bxHistory: List<Double> = emptyList(),
    val byHistory: List<Double> = emptyList(),
    val speedHistory: List<Double> = emptyList(),
    val densityHistory: List<Double> = emptyList(),
    val xrayFlux: Double = 1e-9,
    val flareClass: String = "A0.0",
    val gScale: Int = 0,
    val sScale: Int = 0,
    val rScale: Int = 0,
    val hpNorth: Double = 0.0,
    val hpSouth: Double = 0.0,
    val hpNorthHistory: List<Double> = emptyList(),
    val hpSouthHistory: List<Double> = emptyList(),
    val alerts: List<NoaaAlert> = emptyList(),
    val cmeEvents: List<CmeEvent> = emptyList(),
    // TEC estimates (derived)
    val tecLocal: Double = 18.4,
    val tecMedian: Double = 16.3,
    val isLoading: Boolean = true,
    val lastUpdated: Long = 0L,
    val error: String? = null
)

data class WeatherState(
    val tempF: Double = 0.0,
    val humidity: Int = 0,
    val pressureHpa: Double = 1013.0,
    val pressureHistory: List<Double> = emptyList(),
    val windMph: Double = 0.0,
    val apparentTempF: Double = 0.0,
    val locationName: String = "West Monroe, Louisiana",
    val lat: Double = 32.5093,
    val lon: Double = -92.1482,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class SRMetrics(
    val f1: Double = 7.83,
    val drift: Double = 0.0,
    val qFactor: Double = 5.0,
    val amplitude: Double = 1.5,
    val intensity: Double = 0.5,
    val coherenceScore: Int = 60
)

data class ANSState(
    val loadIndex: Int = 0,
    val sympatheticBias: Double = 50.0,
    val hrvImpact: String = "NORMAL",
    val cortisolAxis: String = "NORMAL",
    val melatonin: String = "NORMAL",
    val coherencePct: Int = 60,
    val symptoms: List<SymptomPrediction> = emptyList(),
    val protocols: List<String> = emptyList()
)

data class SymptomPrediction(
    val name: String,
    val icon: String,
    val probability: Int,
    val severity: String, // "low", "moderate", "high"
    val mechanism: String,
    val drivers: List<String>
)

data class IntegratedAssessment(
    val score: Int = 0,
    val label: String = "COMPUTING",
    val spaceScore: Int = 0,
    val srScore: Int = 0,
    val envScore: Int = 0,
    val narrative: String = "",
    val protocols: List<String> = emptyList()
)

data class ChatMessage(
    val text: String = "",
    val nick: String = "",
    val mine: Boolean = false,
    val isSystem: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val id: String = ""
)

data class LocationState(
    val lat: Double = 32.5093,
    val lon: Double = -92.1482,
    val name: String = "West Monroe, Louisiana",
    val isGps: Boolean = false
)
