package com.biospace.monitor.ui

import android.app.Application
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.biospace.monitor.api.ApiClient
import com.biospace.monitor.engine.ANSEngine
import com.biospace.monitor.engine.SREngine
import com.biospace.monitor.model.*
import com.google.gson.JsonArray
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import com.biospace.monitor.ble.WatchRepository

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "biospace_prefs")

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.dataStore

    private val _spaceWeather = MutableStateFlow(SpaceWeatherState())
    val spaceWeather: StateFlow<SpaceWeatherState> = _spaceWeather.asStateFlow()

    private val _weather = MutableStateFlow(WeatherState())
    val weather: StateFlow<WeatherState> = _weather.asStateFlow()

    private val _srMetrics = MutableStateFlow(SRMetrics())
    val srMetrics: StateFlow<SRMetrics> = _srMetrics.asStateFlow()

    private val _ansState = MutableStateFlow(ANSState())
    val ansState: StateFlow<ANSState> = _ansState.asStateFlow()

    private val _assessment = MutableStateFlow(IntegratedAssessment())
    val assessment: StateFlow<IntegratedAssessment> = _assessment.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _chatConnected = MutableStateFlow(false)
    val chatConnected: StateFlow<Boolean> = _chatConnected.asStateFlow()

    private val db = FirebaseDatabase.getInstance()
    private val chatRef = db.getReference("biospace_chat")

    private val _location = MutableStateFlow(LocationState())
    val location: StateFlow<LocationState> = _location.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var refreshJob: Job? = null
    private var srRefreshJob: Job? = null

    companion object {
        private val PREF_LAT = doublePreferencesKey("saved_lat")
        private val PREF_LON = doublePreferencesKey("saved_lon")
        private val PREF_NAME = stringPreferencesKey("saved_location_name")
        private const val TAG = "BioSpace"
    }

    init {
        addSystemMessage("BIOSPACE MONITOR · INITIALIZING")
        viewModelScope.launch {
            loadSavedLocation()
            fetchAll()
            startAutoRefresh()
        }
        startChatListener()
    }

    private suspend fun loadSavedLocation() {
        dataStore.data.first().let { prefs ->
            val lat = prefs[PREF_LAT] ?: 32.5093
            val lon = prefs[PREF_LON] ?: -92.1482
            val name = prefs[PREF_NAME] ?: "West Monroe, Louisiana"
            _location.value = LocationState(lat, lon, name)
            _weather.value = _weather.value.copy(lat = lat, lon = lon, locationName = name)
        }
    }

    private suspend fun saveLocation(lat: Double, lon: Double, name: String) {
        dataStore.edit { prefs ->
            prefs[PREF_LAT] = lat
            prefs[PREF_LON] = lon
            prefs[PREF_NAME] = name
        }
    }

    fun setLocation(lat: Double, lon: Double, name: String) {
        _location.value = LocationState(lat, lon, name, false)
        _weather.value = _weather.value.copy(lat = lat, lon = lon, locationName = name, isLoading = true)
        viewModelScope.launch {
            saveLocation(lat, lon, name)
            fetchWeather()
        }
    }

    fun setGpsLocation(location: Location) {
        val lat = location.latitude
        val lon = location.longitude
        _location.value = LocationState(lat, lon, "GPS Location", true)
        _weather.value = _weather.value.copy(lat = lat, lon = lon, locationName = "GPS Location", isLoading = true)
        viewModelScope.launch {
            // Reverse geocode
            try {
                val result = ApiClient.geocoding.searchCity("$lat,$lon")
                val name = result.results?.firstOrNull()?.let {
                    "${it.name}${if (it.admin1 != null) ", ${it.admin1}" else ""}"
                } ?: "GPS Location"
                _location.value = _location.value.copy(name = name)
                _weather.value = _weather.value.copy(locationName = name)
                saveLocation(lat, lon, name)
            } catch (e: Exception) {
                Log.w(TAG, "Reverse geocode failed", e)
            }
            fetchWeather()
        }
    }

    suspend fun searchCity(query: String): List<GeoResult> {
        return try {
            ApiClient.geocoding.searchCity(query).results ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchAll()
            _isRefreshing.value = false
        }
    }

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000)
                fetchAll()
            }
        }
        srRefreshJob?.cancel()
        srRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(4_000)
                updateDerivedEngines()
            }
        }
    }

    private suspend fun fetchAll() {
        coroutineScope {
            launch { fetchSpaceWeather() }
            launch { fetchHemisphericPower() }
            launch { fetchIMF() }
            launch { fetchCME() }
            launch { fetchWeather() }
        }
        updateDerivedEngines()
        addSystemMessage("AUTO ▸ Kp=${String.format("%.2f", _spaceWeather.value.kp)} · ${
            SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        }")
    }

    private suspend fun fetchSpaceWeather() {
        try {
            // Kp
            val kpData = ApiClient.noaa.getKpIndex()
            val kpHistory = kpData.takeLast(180).mapNotNull { it.kpIndex.toDoubleOrNull() }
            val kp = kpHistory.lastOrNull() ?: 0.0

            // X-ray
            val xray = ApiClient.noaa.getXray()
            val lastXray = xray.lastOrNull()
            val flux = lastXray?.flux?.toDoubleOrNull() ?: 1e-9
            val flareClass = when {
                flux >= 1e-4 -> "X${String.format("%.1f", flux / 1e-4)}"
                flux >= 1e-5 -> "M${String.format("%.1f", flux / 1e-5)}"
                flux >= 1e-6 -> "C${String.format("%.1f", flux / 1e-6)}"
                flux >= 1e-7 -> "B${String.format("%.1f", flux / 1e-7)}"
                else -> "A${String.format("%.1f", flux / 1e-8)}"
            }

            // Storm scales
            val gScale = when {
                kp >= 9.0 -> 5; kp >= 8.0 -> 4; kp >= 7.0 -> 3
                kp >= 6.0 -> 2; kp >= 5.0 -> 1; else -> 0
            }
            val sScale = when {
                flux >= 1e-4 -> 5; flux >= 1e-5 -> 4; flux >= 1e-6 -> 3
                flux >= 1e-7 -> 2; flux >= 1e-8 -> 1; else -> 0
            }
            val rScale = sScale

            // Alerts
            val alerts = try { ApiClient.noaa.getAlerts() } catch (e: Exception) { emptyList() }

            // Plasma
            val plasma = try { ApiClient.noaa.getPlasma() } catch (e: Exception) { JsonArray() }
            var speed = 400.0; var density = 5.0; var temperature = 100000.0
            val speeds = mutableListOf<Double>()
            val densities = mutableListOf<Double>()
            if (plasma.size() > 0) {
                val slice = plasma.asList().takeLast(180)
                for (entry in slice) {
                    val arr = entry.asJsonArray
                    arr.getOrNull(2)?.asString?.toDoubleOrNull()?.let { speeds.add(it) }
                    arr.getOrNull(1)?.asString?.toDoubleOrNull()?.let { densities.add(it) }
                }
                speed = speeds.lastOrNull() ?: 400.0
                density = densities.lastOrNull() ?: 5.0
                val last = plasma.asList().lastOrNull()?.asJsonArray
                temperature = last?.getOrNull(3)?.asString?.toDoubleOrNull() ?: 100000.0
            }

            // TEC estimate from Kp and speed
            val tecMedian = 16.3
            val tecLocal = tecMedian + kp * 0.3 + (speed - 400) * 0.005

            _spaceWeather.value = _spaceWeather.value.copy(
                kp = kp,
                kpHistory = kpHistory,
                speed = speed,
                density = density,
                temperature = temperature,
                xrayFlux = flux,
                flareClass = flareClass,
                gScale = gScale,
                sScale = sScale,
                rScale = rScale,
                alerts = alerts,
                speedHistory = speeds,
                densityHistory = densities,
                tecLocal = tecLocal,
                tecMedian = tecMedian,
                isLoading = false,
                lastUpdated = System.currentTimeMillis(),
                error = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "fetchSpaceWeather failed", e)
            _spaceWeather.value = _spaceWeather.value.copy(
                isLoading = false,
                error = "Space weather fetch failed: ${e.message}"
            )
        }
    }

    private suspend fun fetchIMF() {
        try {
            val mag = ApiClient.noaa.getMag()
            val slice = mag.asList().takeLast(180)
            val bzArr = mutableListOf<Double>()
            val btArr = mutableListOf<Double>()
            val bxArr = mutableListOf<Double>()
            val byArr = mutableListOf<Double>()
            for (entry in slice) {
                val arr = entry.asJsonArray
                arr.getOrNull(1)?.asString?.toDoubleOrNull()?.let { bxArr.add(it) }
                arr.getOrNull(2)?.asString?.toDoubleOrNull()?.let { byArr.add(it) }
                arr.getOrNull(3)?.asString?.toDoubleOrNull()?.let { bzArr.add(it) }
                arr.getOrNull(6)?.asString?.toDoubleOrNull()?.let { btArr.add(it) }
            }
            val last = mag.asList().lastOrNull()?.asJsonArray
            val bz = last?.getOrNull(3)?.asString?.toDoubleOrNull() ?: _spaceWeather.value.bz
            val bt = last?.getOrNull(6)?.asString?.toDoubleOrNull() ?: _spaceWeather.value.bt
            val bx = last?.getOrNull(1)?.asString?.toDoubleOrNull() ?: _spaceWeather.value.bx
            val by = last?.getOrNull(2)?.asString?.toDoubleOrNull() ?: _spaceWeather.value.by
            val phi = last?.getOrNull(4)?.asString?.toDoubleOrNull() ?: _spaceWeather.value.phi
            val theta = last?.getOrNull(5)?.asString?.toDoubleOrNull() ?: _spaceWeather.value.theta

            _spaceWeather.value = _spaceWeather.value.copy(
                bz = bz, bt = bt, bx = bx, by = by, phi = phi, theta = theta,
                bzHistory = bzArr, btHistory = btArr, bxHistory = bxArr, byHistory = byArr
            )
        } catch (e: Exception) {
            Log.w(TAG, "fetchIMF failed", e)
        }
    }

    private suspend fun fetchHemisphericPower() {
        try {
            val body = ApiClient.noaa.getHemisphericPower().string()
            var north = Double.NaN; var south = Double.NaN
            // Parse last non-comment data line
            val lastLine = body.lines()
                .filter { !it.startsWith("#") && it.isNotBlank() }
                .lastOrNull()
            if (lastLine != null) {
                val parts = lastLine.trim().split(Regex("\\s+"))
                if (parts.size >= 4) {
                    north = parts[2].toDoubleOrNull() ?: Double.NaN
                    south = parts[3].toDoubleOrNull() ?: Double.NaN
                }
            }
            if (north.isNaN() || south.isNaN()) {
                val kp = _spaceWeather.value.kp
                north = kp * kp * 4.2 + 2
                south = kp * kp * 0.9 * 4.2 + 2
            }
            val kpHistory = _spaceWeather.value.kpHistory
            val northHistory = kpHistory.map { k -> k * k * 4.2 + 2 }
            val southHistory = kpHistory.map { k -> k * k * 0.9 * 4.2 + 2 }
            _spaceWeather.value = _spaceWeather.value.copy(
                hpNorth = north, hpSouth = south,
                hpNorthHistory = northHistory, hpSouthHistory = southHistory
            )
        } catch (e: Exception) {
            Log.w(TAG, "fetchHP failed", e)
        }
    }

    private suspend fun fetchCME() {
        try {
            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val endDate = sdf.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, -7)
            val startDate = sdf.format(cal.time)
            val cmes = ApiClient.nasa.getCme(startDate, endDate)
            _spaceWeather.value = _spaceWeather.value.copy(cmeEvents = cmes.takeLast(6))
        } catch (e: Exception) {
            Log.w(TAG, "fetchCME failed: ${e.message}")
        }
    }

    private suspend fun fetchWeather() {
        val loc = _location.value
        try {
            val response = ApiClient.weather.getWeather(loc.lat, loc.lon)
            val current = response.current ?: return
            val hourly = response.hourly

            val pressureHistory = hourly?.surface_pressure?.takeLast(24) ?: emptyList()

            _weather.value = WeatherState(
                tempF = current.temperature_2m,
                humidity = current.relative_humidity_2m,
                pressureHpa = current.surface_pressure,
                pressureHistory = pressureHistory,
                windMph = current.wind_speed_10m,
                apparentTempF = current.apparent_temperature,
                locationName = loc.name,
                lat = loc.lat,
                lon = loc.lon,
                isLoading = false,
                error = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "fetchWeather failed", e)
            _weather.value = _weather.value.copy(
                isLoading = false,
                error = "Weather fetch failed"
            )
        }
    }

    private fun updateDerivedEngines() {
        val sw = _spaceWeather.value
        val sr = SREngine.computeSRMetrics(sw)
        val ans = ANSEngine.computeANSLoad(sw, sr)
        val assessment = ANSEngine.computeIntegratedAssessment(sw, sr, _weather.value)
        _srMetrics.value = sr
        _ansState.value = ans
        _assessment.value = assessment
    }

    // ── Firebase Chat ──────────────────────────────────────────────────────────

    private fun startChatListener() {
        // Keep only last 200 messages in the query to avoid unlimited growth
        val query = chatRef.orderByChild("timestamp").limitToLast(200)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val msgs = snapshot.children.mapNotNull { child ->
                    try {
                        ChatMessage(
                            id        = child.key ?: "",
                            text      = child.child("text").getValue(String::class.java) ?: "",
                            nick      = child.child("nick").getValue(String::class.java) ?: "ANON",
                            mine      = false,   // remote; local is set at send time
                            isSystem  = child.child("isSystem").getValue(Boolean::class.java) ?: false,
                            timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                        )
                    } catch (e: Exception) { null }
                }
                _chatMessages.value = msgs
                _chatConnected.value = true
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Chat listener cancelled: ${error.message}")
                _chatConnected.value = false
            }
        })

        // Connection state indicator
        db.getReference(".info/connected").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _chatConnected.value = snapshot.getValue(Boolean::class.java) ?: false
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun sendChatMessage(text: String, nick: String) {
        val trimText = text.trim()
        val trimNick = nick.trim().uppercase().take(12).ifBlank { "ANON" }
        if (trimText.isBlank()) return

        val key = chatRef.push().key ?: return
        val payload = mapOf(
            "text"      to trimText,
            "nick"      to trimNick,
            "isSystem"  to false,
            "timestamp" to System.currentTimeMillis()
        )
        chatRef.child(key).setValue(payload)

        // Prune to last 300 entries to prevent the DB growing forever
        chatRef.orderByChild("timestamp").limitToFirst(1).addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val count = _chatMessages.value.size
                    if (count > 300) snapshot.children.forEach { it.ref.removeValue() }
                }
                override fun onCancelled(error: DatabaseError) {}
            }
        )
    }

    private fun addSystemMessage(text: String) {
        // System messages are local-only (init/status lines), not pushed to Firebase
        val msg = ChatMessage(text = text, nick = "SYSTEM", mine = false, isSystem = true)
        _chatMessages.value = (_chatMessages.value + msg).takeLast(200)
    }

    private fun JsonArray.getOrNull(index: Int) = try { this[index] } catch (e: Exception) { null }

    // ── Watch BLE (auto-patched) ─────────────────────────────────────
    private val watchRepo by lazy { WatchRepository.getInstance(getApplication()) }

    val watchConnectionState = watchRepo.connectionState
    val watchDevice          = watchRepo.lastKnownDevice
    val watchBattery         = watchRepo.battery

    val bloodPressure = watchRepo.bloodPressure
    val heartRate     = watchRepo.heartRate
    val spO2          = watchRepo.spO2
    val steps         = watchRepo.steps
    val sleep         = watchRepo.sleep
    val stress        = watchRepo.stress
    val hourlyBundle  = watchRepo.hourlyBundle
    val oneKeyBundle  = watchRepo.oneKeyBundle

    val bpHistory     = watchRepo.bpHistory
    val hrHistory     = watchRepo.hrHistory
    val spo2History   = watchRepo.spo2History
    val sleepHistory  = watchRepo.sleepHistory
    val stressHistory = watchRepo.stressHistory
    val tempHistory   = watchRepo.tempHistory
    val immHistory    = watchRepo.immHistory
    val stepsHistory  = watchRepo.stepsHistory
    val respiration   = watchRepo.respiration
    fun refreshWatch() = watchRepo.sendRefresh()

    fun connectWatch()              = watchRepo.connect()
    fun disconnectWatch()           = watchRepo.disconnect()
    fun connectWatchTo(mac: String) = watchRepo.connectTo(mac)

}
