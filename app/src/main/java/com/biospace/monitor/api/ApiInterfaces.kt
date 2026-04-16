package com.biospace.monitor.api

import com.biospace.monitor.model.*
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query

// ── NOAA SWPC ─────────────────────────────────────────────────────────────────
interface NoaaApi {
    @GET("json/planetary_k_index_1m.json")
    suspend fun getKpIndex(): List<KpEntry>

    @GET("products/solar-wind/plasma-7-day.json")
    suspend fun getPlasma(): JsonArray

    @GET("products/solar-wind/mag-7-day.json")
    suspend fun getMag(): JsonArray

    @GET("json/goes/primary/xrays-6-hour.json")
    suspend fun getXray(): List<XrayEntry>

    @GET("products/alerts.json")
    suspend fun getAlerts(): List<NoaaAlert>

    @GET("json/ovation_aurora_latest.json")
    suspend fun getHemisphericPower(): JsonElement
}

// ── NASA DONKI CME ────────────────────────────────────────────────────────────
interface NasaApi {
    @GET("DONKI/CME")
    suspend fun getCme(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("api_key") apiKey: String = "DEMO_KEY"
    ): List<CmeEvent>
}

// ── Open-Meteo Weather ────────────────────────────────────────────────────────
interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,surface_pressure,wind_speed_10m,apparent_temperature,weather_code",
        @Query("hourly") hourly: String = "surface_pressure,temperature_2m,relative_humidity_2m",
        @Query("temperature_unit") tempUnit: String = "fahrenheit",
        @Query("wind_speed_unit") windUnit: String = "mph",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 2,
        @Query("past_days") pastDays: Int = 1
    ): WeatherResponse
}

// ── Open-Meteo Geocoding ──────────────────────────────────────────────────────
interface GeocodingApi {
    @GET("v1/search")
    suspend fun searchCity(
        @Query("name") name: String,
        @Query("count") count: Int = 5,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): GeocodingResponse
}
