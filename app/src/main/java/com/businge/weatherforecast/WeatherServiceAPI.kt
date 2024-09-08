package com.businge.weatherforecast

import com.businge.weatherforecast.models.Weather
import com.businge.weatherforecast.models.WeatherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherServiceAPI {
    @GET("2.5/weather")
    fun getWeatherDetails(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appId") appId: String,
        @Query("units") metric: String
    ) : Call<WeatherResponse>
}