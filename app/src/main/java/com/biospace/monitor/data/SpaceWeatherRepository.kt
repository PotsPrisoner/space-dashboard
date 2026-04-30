package com.biospace.monitor.data

import com.biospace.monitor.api.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class SpaceWeatherRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()

    private fun <T> build(base: String, cls: Class<T>): T = Retrofit.Builder()
        .baseUrl(base).client(client).addConverterFactory(GsonConverterFactory.create()).build().create(cls)

    private val noaa  = build("https://services.swpc.noaa.gov/", NoaaApi::class.java)
    private val donki = build("https://kauai.ccmc.gsfc.nasa.gov/DONKI/", DonkiApi::class.java)
    private val meteo = build("https://api.open-meteo.com/", OpenMeteoApi::class.java)
    private val geo   = build("https://nominatim.openstreetmap.org/", GeocodingApi::class.java)

    private fun dateStr(daysAgo: Int = 0): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return sdf.format(cal.time)
    }

    suspend fun fetchAll(lat: Double, lon: Double): SpaceWeather = withContext(Dispatchers.IO) {
        val today = dateStr(0); val week = dateStr(7)
        val kpR    = async { runCatching { noaa.getKp() } }
        val plaR   = async { runCatching { noaa.getSolarWindPlasma() } }
        val magR   = async { runCatching { noaa.getSolarWindMag() } }
        val hpR    = async { runCatching { noaa.getHemiPower() } }
        val flrR   = async { runCatching { donki.getFlares(week, today) } }
        val cmeR   = async { runCatching { donki.getCME(week, today) } }
        val gstR   = async { runCatching { donki.getGST(week, today) } }
        val ipsR   = async { runCatching { donki.getIPS(week, today) } }
        val hssR   = async { runCatching { donki.getHSS(week, today) } }
        val mpcR   = async { runCatching { donki.getMPC(week, today) } }
        val rbeR   = async { runCatching { donki.getRBE(week, today) } }
        val sepR   = async { runCatching { donki.getSEP(week, today) } }

        val kpData   = kpR.await().getOrNull()
        val plasma   = plaR.await().getOrNull()
        val mag      = magR.await().getOrNull()
        val hpTxt    = hpR.await().getOrNull()
        val flares   = flrR.await().getOrNull()
        val cmeData  = cmeR.await().getOrNull()
        val gst      = gstR.await().getOrNull()
        val ips      = ipsR.await().getOrNull()
        val hss      = hssR.await().getOrNull()
        val mpc      = mpcR.await().getOrNull()
        val rbe      = rbeR.await().getOrNull()
        val sep      = sepR.await().getOrNull()

        var kp = 1f
        kpData?.drop(1)?.lastOrNull()?.let { row ->
            (row as? List<*>)?.getOrNull(1)?.toString()?.toFloatOrNull()?.let { kp = it }
        }

        var swSpeed = 400f; var swDensity = 5f
        plasma?.drop(1)?.lastOrNull()?.let { row ->
            val r = row as? List<*>
            swSpeed = r?.getOrNull(2)?.toString()?.toFloatOrNull() ?: swSpeed
            swDensity = r?.getOrNull(1)?.toString()?.toFloatOrNull() ?: swDensity
        }

        var imfBz = 0f; var imfBt = 5f
        mag?.drop(1)?.lastOrNull()?.let { row ->
            val r = row as? List<*>
            imfBz = r?.getOrNull(3)?.toString()?.toFloatOrNull() ?: imfBz
            imfBt = r?.getOrNull(6)?.toString()?.toFloatOrNull() ?: imfBt
        }

        var hp = 20f
        hpTxt?.split("\n")?.filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith(":") }
            ?.lastOrNull()?.trim()?.split("\\s+".toRegex())?.getOrNull(4)?.toFloatOrNull()
            ?.let { if (it > 0) hp = it }

        val parsedFlares = flares?.takeLast(5)?.reversed()?.mapNotNull {
            val f = it as? Map<*, *> ?: return@mapNotNull null
            val cls = f["classType"]?.toString() ?: "B1.0"
            val linked = f["linkedEvents"] as? List<*>
            val hasCme = linked?.any { e -> (e as? Map<*, *>)?.get("activityID")?.toString()?.contains("CME") == true } == true
            val loc = f["sourceLocation"]?.toString() ?: ""
            val angle = Regex("[EW](\\d+)").find(loc)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 50
            Flare(cls, f["beginTime"]?.toString()?.let { t -> if (t.length >= 16) t.substring(11,16) else "--:--" } ?: "--:--",
                  f["endTime"]?.toString()?.let { t -> if (t.length >= 16) t.substring(11,16) else "--:--" } ?: "--:--",
                  if (angle < 20) "Earth-directed" else if (angle < 45) "Partial" else "Limb", angle, hasCme)
        } ?: emptyList()

        var cmeSpeed = 300f; var cmeAngle = 60f; var cmeArrival = 999; var cmeDir = "Non-Halo"
        cmeData?.lastOrNull()?.let {
            val cm = it as? Map<*, *>
            cmeSpeed = cm?.get("speed")?.toString()?.toFloatOrNull() ?: cmeSpeed
            cmeAngle = cm?.get("halfAngle")?.toString()?.toFloatOrNull() ?: cmeAngle
            cmeDir = if (cmeAngle < 20) "Full Halo" else if (cmeAngle < 40) "Partial Halo" else "Non-Halo"
            cm?.get("time21_5")?.toString()?.let { t ->
                runCatching {
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    val diff = sdf.parse(t)!!.time - System.currentTimeMillis()
                    cmeArrival = maxOf(0, (diff / 3_600_000).toInt())
                }
            }
        }

        val poleDist = (Math.abs(lat - 80.65) + Math.abs(lon + 72.68) * 0.5).toFloat().coerceAtMost(90f)
        val fountain = if (hp > 100) "ACTIVE" else if (hp > 50) "MODERATE" else "QUIET"

        SpaceWeather(
            kp = kp, swSpeed = swSpeed, swDensity = swDensity,
            imfBz = imfBz, imfBt = imfBt,
            imfTrend = if (imfBz < -2) "SOUTHWARD" else if (imfBz > 2) "NORTHWARD" else "FLUCTUATING",
            flares = parsedFlares,
            cmeSpeed = cmeSpeed, cmeArrivalHrs = cmeArrival, cmeAngle = cmeAngle, cmeDirection = cmeDir,
            hemisphericPower = hp, tec = 10f + hp * 0.35f, tecDelta = hp / 20f - 1f,
            fountainDumping = fountain,
            gstActive = (gst?.size ?: 0) > 0, ipsCount = ips?.size ?: 0,
            hssActive = (hss?.size ?: 0) > 0, mpcCount = mpc?.size ?: 0,
            rbeCount = rbe?.size ?: 0, sepActive = (sep?.size ?: 0) > 0,
            poleDist = poleDist, localMagNt = 35f, industrialNt = 12f,
            timestamp = System.currentTimeMillis()
        )
    }

    suspend fun fetchWeather(lat: Double, lon: Double): WeatherState = withContext(Dispatchers.IO) {
        val data = meteo.getWeather(lat, lon)
        val cur = data["current"] as? Map<*, *> ?: return@withContext WeatherState(lat = lat, lon = lon)
        val temp     = cur["temperature_2m"]?.toString()?.toFloatOrNull() ?: 72f
        val humidity = cur["relative_humidity_2m"]?.toString()?.toFloatOrNull() ?: 55f
        val dewpoint = cur["dew_point_2m"]?.toString()?.toFloatOrNull() ?: 60f
        val pressure = cur["surface_pressure"]?.toString()?.toFloatOrNull() ?: 1013f
        val wind     = cur["wind_speed_10m"]?.toString()?.toFloatOrNull() ?: 8f
        val heat     = cur["apparent_temperature"]?.toString()?.toFloatOrNull() ?: temp
        val uv       = cur["uv_index"]?.toString()?.toFloatOrNull() ?: 3f

        val locName = runCatching {
            val r = geo.reverse(lat = lat, lon = lon)
            val addr = r["address"] as? Map<*, *>
            val city = (addr?.get("city") ?: addr?.get("town") ?: addr?.get("village") ?: addr?.get("county") ?: "").toString()
            val state = (addr?.get("state_code") ?: "").toString()
            if (state.isNotBlank()) "$city, $state".uppercase() else city.uppercase()
        }.getOrDefault("")

        WeatherState(temp, humidity, dewpoint, pressure, 0f, wind, heat, uv, 50, locName, lat, lon)
    }
}
