package com.biospace.monitor.api

import retrofit2.http.GET
import retrofit2.http.Query

interface NoaaApi {
    @GET("products/noaa-planetary-k-index.json")
    suspend fun getKp(): List<List<Any>>
    @GET("products/solar-wind/plasma-7-day.json")
    suspend fun getSolarWindPlasma(): List<List<Any>>
    @GET("products/solar-wind/mag-7-day.json")
    suspend fun getSolarWindMag(): List<List<Any>>
    @GET("text/aurora-nowcast-hemi-power.txt")
    suspend fun getHemiPower(): String
}

interface DonkiApi {
    @GET("WS/get/FLR")
    suspend fun getFlares(@Query("startDate") start: String, @Query("endDate") end: String,
                          @Query("api_key") key: String = "DEMO_KEY"): List<Map<String, Any>>
    @GET("WS/get/CMEAnalysis")
    suspend fun getCME(@Query("startDate") start: String, @Query("endDate") end: String,
                       @Query("mostAccurateOnly") accurate: Boolean = true,
                       @Query("api_key") key: String = "DEMO_KEY"): List<Map<String, Any>>
    @GET("WS/get/GST")
    suspend fun getGST(@Query("startDate") start: String, @Query("endDate") end: String,
                       @Query("api_key") key: String = "DEMO_KEY"): List<Map<String, Any>>
    @GET("WS/get/IPS")
    suspend fun getIPS(@Query("startDate") start: String, @Query("endDate") end: String,
                       @Query("api_key") key: String = "DEMO_KEY"): List<Map<String, Any>>
    @GET("WS/get/HSS")
    suspend fun getHSS(@Query("startDate") start: String, @Query("endDate") end: String,
                       @Query("api_key") key: String = "DEMO_KEY"): List<Map<String, Any>>
    @GET("WS/get/MPC")
    suspend fun getMPC(@Query("startDate") start: String, @Query("endDate") end: String,
                       @Query("api_key") key: String = "DEMO_KEY"): List<Map<String, Any>>
    @GET("WS/get/RBE")
    suspend fun getRBE(@Query("startDate") start: String, @Query("endDate") end: String,
                       @Query("api_key") key: String = "DEMO_KEY"): List<Map<String, Any>>
    @GET("WS/get/SEP")
    suspend fun getSEP(@Query("startDate") start: String, @Query("endDate") end: String,
                       @Query("api_key") key: String = "DEMO_KEY"): List<Map<String, Any>>
}

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") lat: Double, @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,dew_point_2m,surface_pressure,wind_speed_10m,apparent_temperature,uv_index",
        @Query("hourly") hourly: String = "surface_pressure",
        @Query("past_days") pastDays: Int = 1, @Query("forecast_days") forecastDays: Int = 1,
        @Query("temperature_unit") tempUnit: String = "fahrenheit",
        @Query("wind_speed_unit") windUnit: String = "mph"
    ): Map<String, Any>
}

interface GeocodingApi {
    @GET("reverse")
    suspend fun reverse(@Query("format") fmt: String = "json",
                        @Query("lat") lat: Double, @Query("lon") lon: Double): Map<String, Any>
}
