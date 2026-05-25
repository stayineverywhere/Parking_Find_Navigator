package com.example.bigdata.data

import com.google.gson.annotations.SerializedName

data class RoutesRequest(
    val origin: Waypoint,
    val destination: Waypoint,
    val travelMode: String = "DRIVE",
    val routingPreference: String? = null
)

data class Waypoint(
    val location: Location
)

data class Location(
    val latLng: LatLngData
)

data class LatLngData(
    val latitude: Double,
    val longitude: Double
)

data class RoutesResponse(
    val routes: List<RouteData>? = null
)

data class RouteData(
    val distanceMeters: Int,
    val duration: String, // e.g., "123s"
    val polyline: PolylineData
)

data class PolylineData(
    val encodedPolyline: String
)
