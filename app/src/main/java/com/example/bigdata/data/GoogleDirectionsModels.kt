package com.example.bigdata.data

import com.google.gson.annotations.SerializedName

data class DirectionsResponse(
    @SerializedName("routes") val routes: List<DirectionsRoute>,
    @SerializedName("status") val status: String
)

data class DirectionsRoute(
    @SerializedName("overview_polyline") val overviewPolyline: OverviewPolyline,
    @SerializedName("legs") val legs: List<DirectionsLeg>
)

data class OverviewPolyline(
    @SerializedName("points") val points: String
)

data class DirectionsLeg(
    @SerializedName("distance") val distance: TextValue,
    @SerializedName("duration") val duration: TextValue
)

data class TextValue(
    @SerializedName("text") val text: String,
    @SerializedName("value") val value: Int
)
