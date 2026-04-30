package com.biospace.monitor.data

data class SpaceWeather(
    val kp: Float = 0f, val kpTrend: Int = 0,
    val swSpeed: Float = 400f, val swDensity: Float = 5f, val swTemp: Float = 100000f, val swTrend: String = "STEADY",
    val imfBz: Float = 0f, val imfBt: Float = 5f, val imfPhi: Float = 150f, val imfTrend: String = "NORTHWARD",
    val flares: List<Flare> = emptyList(),
    val cmeSpeed: Float = 300f, val cmeArrivalHrs: Int = 999, val cmeAngle: Float = 60f, val cmeDirection: String = "Non-Halo",
    val hemisphericPower: Float = 20f, val tec: Float = 18f, val tecDelta: Float = 0f,
    val fountainDumping: String = "QUIET",
    val srFundamental: Float = 7.83f, val srAmplitude: Float = 1f, val srDrift: Float = 0f, val srQFactor: Float = 3f,
    val gstActive: Boolean = false, val ipsCount: Int = 0, val hssActive: Boolean = false,
    val mpcCount: Int = 0, val rbeCount: Int = 0, val sepActive: Boolean = false,
    val poleDist: Float = 55f, val localMagNt: Float = 35f, val industrialNt: Float = 12f,
    val timestamp: Long = 0L
)

data class Flare(
    val flareClass: String = "B1.0", val start: String = "--:--", val end: String = "--:--",
    val direction: String = "Limb", val angle: Int = 50, val hasCme: Boolean = false
)

data class WeatherState(
    val temp: Float = 72f, val humidity: Float = 55f, val dewpoint: Float = 60f,
    val pressure: Float = 1013f, val pressureTrend: Float = 0f,
    val wind: Float = 8f, val heatIndex: Float = 72f, val uvIndex: Float = 3f,
    val airQuality: Int = 50, val locationName: String = "",
    val lat: Double = 0.0, val lon: Double = 0.0
)

data class Biometrics(
    val heartRate: Int = 0, val hrSource: String = "MANUAL",
    val spO2: Int = 0, val spO2Source: String = "MANUAL",
    val bpSys: Int = 0, val bpDia: Int = 0, val bpSource: String = "MANUAL",
    val rmssd: Float = 0f, val sdnn: Float = 0f, val pnn50: Float = 0f, val hrvSource: String = "MANUAL",
    val steps: Int = 0, val calories: Int = 0,
    val sleepHours: Float = 0f, val sleepQuality: Int = 0,
    val stressScore: Int = 0, val respirationRate: Int = 0,
    val isWatchConnected: Boolean = false
)

data class BurdenScore(
    val overall: Float = 0f,
    val magnitude: Float = 0f,
    val fluctuation: Float = 0f,
    val alertLevel: AlertLevel = AlertLevel.GREEN,
    val breakdown: Map<String, BurdenComponent> = emptyMap(),
    val narrativeLine: String = ""
)

data class BurdenComponent(
    val name: String, val magnitude: Float, val fluctuation: Float,
    val combined: Float, val unit: String = "", val value: String = ""
)

enum class AlertLevel(val label: String, val instruction: String, val colorHex: String) {
    GREEN("FREE TO GO",
        "Space weather and biometrics are within normal range. You are free to go about your day.",
        "#00E87A"),
    YELLOW("PROCEED WITH CAUTION",
        "Elevated environmental or biometric stress detected. Go about your day but pay attention to how you feel.",
        "#F5C842"),
    RED("STOP AND ASSESS",
        "High ANS burden detected. Stop what you are doing. Sit down, assess how you feel, and do what is appropriate for your body.",
        "#FF3D5A"),
    BLUE("LAY DOWN IMMEDIATELY",
        "Severe ANS burden with rapid fluctuation detected. Stop all activity immediately. Lie down, breathe slowly, stay calm and relaxed.",
        "#1A8FFF")
}

data class SymptomLog(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val lightheadedness: Int = 0, val heartPounding: Int = 0, val fatigue: Int = 0,
    val brainFog: Int = 0, val chestPain: Int = 0, val nausea: Int = 0,
    val shortBreath: Int = 0, val tremors: Int = 0, val blurredVision: Int = 0,
    val headache: Int = 0, val notes: String = "",
    val kpAtLog: Float = 0f, val burdenAtLog: Float = 0f
)

data class ChatMessage(
    val id: String = "", val userId: String = "", val username: String = "",
    val message: String = "", val timestamp: Long = 0L,
    val kp: Float = 0f, val burden: Float = 0f
)
