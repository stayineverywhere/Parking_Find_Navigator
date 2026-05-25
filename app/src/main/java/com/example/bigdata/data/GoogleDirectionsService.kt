package com.example.bigdata.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleDirectionsService {
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String,
        @Query("key") apiKey: String,
        @Query("language") language: String = "ko"
    ): DirectionsResponse

    companion object {
        private const val BASE_URL = "https://maps.googleapis.com/"

        fun create(): GoogleDirectionsService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GoogleDirectionsService::class.java)
        }
    }
}
