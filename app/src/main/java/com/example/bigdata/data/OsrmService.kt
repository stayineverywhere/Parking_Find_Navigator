package com.example.bigdata.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OsrmService {
    @GET("route/v1/{profile}/{coords}")
    suspend fun getRoute(
        @Path("profile") profile: String, // driving, walking
        @Path("coords") coords: String,  // "lng,lat;lng,lat"
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "polyline"
    ): OsrmResponse

    companion object {
        private const val BASE_URL = "https://router.project-osrm.org/"

        fun create(): OsrmService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OsrmService::class.java)
        }
    }
}

data class OsrmResponse(
    val routes: List<OsrmRoute>? = null,
    val code: String? = null
)

data class OsrmRoute(
    val geometry: String,
    val distance: Double,
    val duration: Double
)
