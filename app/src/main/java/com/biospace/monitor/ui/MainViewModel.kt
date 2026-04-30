package com.biospace.monitor.ui

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.biospace.monitor.ble.WatchRepository
import com.biospace.monitor.data.*
import com.biospace.monitor.engine.BurdenEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import com.google.gson.Gson
import java.util.UUID
import java.util.concurrent.TimeUnit

private val Context.store by preferencesDataStore("settings")
private val K_LAT  = doublePreferencesKey("lat")
private val K_LON  = doublePreferencesKey("lon")
private val K_GPS  = booleanPreferencesKey("gps")
private val K_NAME = stringPreferencesKey("name")
private val K_GEM  = stringPreferencesKey("gemini")
private val K_USER = stringPreferencesKey("user")

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SpaceWeatherRepository()
    val watchRepo = WatchRepository(app)

    private val _sw      = MutableStateFlow(SpaceWeather())
    private val _wx      = MutableStateFlow(WeatherState())
    private val _burden  = MutableStateFlow(BurdenScore())
    private val _loading = MutableStateFlow(false)
    private val _error   = MutableStateFlow<String?>(null)
    private val _symptoms   = MutableStateFlow<List<SymptomLog>>(emptyList())
    private val _chat       = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _geminiKey  = MutableStateFlow("")
    private val _username   = MutableStateFlow("")
    private val _useGps     = MutableStateFlow(true)
    private val _lat        = MutableStateFlow(32.5093)
    private val _lon        = MutableStateFlow(-92.1482)
    private val _locName    = MutableStateFlow("")
    private val _chatInput  = MutableStateFlow("")
    private val _report     = MutableStateFlow("")
    private val _reportLoad = MutableStateFlow(false)

    val sw: StateFlow<SpaceWeather>   = _sw
    val wx: StateFlow<WeatherState>   = _wx
    val burden: StateFlow<BurdenScore>= _burden
    val bio: StateFlow<Biometrics>    = watchRepo.bio
    val loading: StateFlow<Boolean>   = _loading
    val error: StateFlow<String?>     = _error
    val symptoms: StateFlow<List<SymptomLog>> = _symptoms
    val chatMessages: StateFlow<List<ChatMessage>> = _chat
    val geminiKey: StateFlow<String>  = _geminiKey
    val username: StateFlow<String>   = _username
    val useGps: StateFlow<Boolean>    = _useGps
    val lat: StateFlow<Double>        = _lat
    val lon: StateFlow<Double>        = _lon
    val locName: StateFlow<String>    = _locName
    val chatInput: StateFlow<String>  = _chatInput
    val reportOutput: StateFlow<String>  = _report
    val reportLoading: StateFlow<Boolean>= _reportLoad

    private var userId = ""
    private var chatRef: DatabaseReference? = null

    init {
        viewModelScope.launch { loadSettings() }
        viewModelScope.launch { watchRepo.bio.collect { _burden.value = BurdenEngine.update(_sw.value, _wx.value, it) } }
        Firebase.auth.signInAnonymously().addOnSuccessListener { userId = it.user?.uid ?: UUID.randomUUID().toString() }
        startChatListener()
        viewModelScope.launch { fetchAll() }
        viewModelScope.launch { while(true) { delay(300_000); fetchAll() } }
    }

    private fun startChatListener() {
        chatRef = FirebaseDatabase.getInstance().getReference("biospace_chat")
        chatRef?.limitToLast(50)?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                _chat.value = snap.children.mapNotNull { it.getValue(ChatMessage::class.java) }
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    fun fetchAll() {
        viewModelScope.launch {
            _loading.value = true; _error.value = null
            try {
                val sw = repo.fetchAll(_lat.value, _lon.value)
                val wx = repo.fetchWeather(_lat.value, _lon.value)
                _sw.value = sw; _wx.value = wx
                if (wx.locationName.isNotBlank()) _locName.value = wx.locationName
                _burden.value = BurdenEngine.update(sw, wx, watchRepo.bio.value)
            } catch (e: Exception) { _error.value = "Fetch error: ${e.message}" }
            _loading.value = false
        }
    }

    fun setLocation(lat: Double, lon: Double, name: String = "") {
        _lat.value = lat; _lon.value = lon
        if (name.isNotBlank()) _locName.value = name
        viewModelScope.launch { saveSettings(); fetchAll() }
    }

    fun setUseGps(v: Boolean) { _useGps.value = v; viewModelScope.launch { saveSettings() } }
    fun setGeminiKey(k: String) { _geminiKey.value = k; viewModelScope.launch { saveSettings() } }
    fun setUsername(v: String) { _username.value = v; viewModelScope.launch { saveSettings() } }
    fun setChatInput(v: String) { _chatInput.value = v }
    fun logSymptom(s: SymptomLog) { _symptoms.value = (listOf(s) + _symptoms.value).take(100) }

    fun sendChatMessage() {
        val text = _chatInput.value.trim(); if (text.isBlank() || userId.isBlank()) return
        val msg = ChatMessage(UUID.randomUUID().toString(), userId,
            _username.value.ifBlank { "Anonymous" }, text,
            System.currentTimeMillis(), _sw.value.kp, _burden.value.overall)
        chatRef?.child(msg.id)?.setValue(msg)
        _chatInput.value = ""
    }

    fun generateReport(clinical: Boolean) {
        viewModelScope.launch {
            val key = _geminiKey.value.trim()
            if (key.isBlank()) { _report.value = "⚠ Enter your Gemini API key below."; return@launch }
            _reportLoad.value = true; _report.value = ""
            try {
                val sw = _sw.value; val wx = _wx.value; val bio = watchRepo.bio.value; val b = _burden.value
                val style = if (clinical) "clinical with physiological mechanisms and research citations" else "plain language, easy for anyone to understand"
                val prompt = """You are a heliobiological ANS health analyst. Generate a $style report.

⚠ DISCLAIMER: Begin with this exact text: "⚠ DISCLAIMER: This is an AI-generated informational report. It is NOT medical advice and was NOT produced by a licensed medical professional. Always consult your physician."

CURRENT DATA (${java.util.Date()}):
ANS Burden: ${b.overall.toInt()}% | Alert: ${b.alertLevel.name} | Magnitude: ${b.magnitude.toInt()}% | Fluctuation: ${b.fluctuation.toInt()}%
Kp: ${sw.kp} | Solar Wind: ${sw.swSpeed.toInt()} km/s | IMF Bz: ${"%.1f".format(sw.imfBz)} nT (${sw.imfTrend})
Flares: ${sw.flares.size} | CME: ${sw.cmeSpeed.toInt()} km/s arrival ${sw.cmeArrivalHrs}hrs | GST: ${sw.gstActive}
IPS: ${sw.ipsCount} | HSS: ${sw.hssActive} | MPC: ${sw.mpcCount} | RBE: ${sw.rbeCount} | SEP: ${sw.sepActive}
Hemispheric Power: ${sw.hemisphericPower.toInt()} GW (${sw.fountainDumping}) | Schumann: ${sw.srFundamental} Hz
Weather: ${wx.temp.toInt()}°F | Humidity: ${wx.humidity.toInt()}% | Pressure: ${wx.pressure.toInt()} hPa
Heart Rate: ${if (bio.heartRate > 0) "${bio.heartRate} bpm (${bio.hrSource})" else "not recorded"}
BP: ${if (bio.bpSys > 0) "${bio.bpSys}/${bio.bpDia} mmHg" else "not recorded"}
SpO2: ${if (bio.spO2 > 0) "${bio.spO2}%" else "not recorded"}
HRV RMSSD: ${if (bio.rmssd > 0f) "${bio.rmssd.toInt()} ms" else "not recorded"}
Sleep: ${if (bio.sleepHours > 0f) "${bio.sleepHours} hrs" else "not recorded"}
Top burden drivers: ${b.breakdown.entries.sortedByDescending { it.value.combined }.take(5).joinToString(", ") { "${it.key} (${it.value.combined.toInt()}%)" }}

Write sections: 1) Overall ANS Assessment  2) Space & Environmental Drivers  3) Biometric Findings  4) Fluctuation Analysis (critical for dysautonomia — note that oscillation is more harmful than stable highs or lows)  5) Expected Symptoms Next 12-24hrs  6) Mitigation Strategies  7) 48hr Outlook"""

                _report.value = callGemini(key, prompt)
            } catch (e: Exception) { _report.value = "⚠ Report failed: ${e.message}" }
            _reportLoad.value = false
        }
    }

    private suspend fun callGemini(key: String, prompt: String): String = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).build()
        val body = RequestBody.create(MediaType.parse("application/json"),
            Gson().toJson(mapOf("contents" to listOf(mapOf("parts" to listOf(mapOf("text" to prompt)))),
                "generationConfig" to mapOf("temperature" to 0.3, "maxOutputTokens" to 2000))))
        val req = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$key")
            .post(body).build()
        val resp = client.newCall(req).execute()
        val json = Gson().fromJson(resp.body()?.string(), Map::class.java)
        val candidates = json["candidates"] as? List<*>
        val content = (candidates?.firstOrNull() as? Map<*, *>)?.get("content") as? Map<*, *>
        val parts = content?.get("parts") as? List<*>
        (parts?.firstOrNull() as? Map<*, *>)?.get("text")?.toString() ?: "No response."
    }

    private suspend fun loadSettings() {
        getApplication<Application>().store.data.first().let {
            _lat.value = it[K_LAT] ?: 32.5093; _lon.value = it[K_LON] ?: -92.1482
            _useGps.value = it[K_GPS] ?: true; _locName.value = it[K_NAME] ?: ""
            _geminiKey.value = it[K_GEM] ?: ""; _username.value = it[K_USER] ?: ""
        }
    }
    private suspend fun saveSettings() {
        getApplication<Application>().store.edit {
            it[K_LAT] = _lat.value; it[K_LON] = _lon.value; it[K_GPS] = _useGps.value
            it[K_NAME] = _locName.value; it[K_GEM] = _geminiKey.value; it[K_USER] = _username.value
        }
    }
}
