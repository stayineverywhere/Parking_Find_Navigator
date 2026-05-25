package com.example.bigdata.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GoogleRoutesService {
    @POST("directions/v2:computeRoutes")
    suspend fun computeRoutes(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String,
        @Header("X-Android-Package") packageName: String,
        @Header("X-Android-Cert") certSha1: String,
        @Body request: RoutesRequest
    ): RoutesResponse

    companion object {
        private const val BASE_URL = "https://routes.googleapis.com/"

        fun create(): GoogleRoutesService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GoogleRoutesService::class.java)
        }
    }
}
