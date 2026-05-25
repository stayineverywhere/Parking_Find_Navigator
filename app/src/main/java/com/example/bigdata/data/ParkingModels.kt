package com.example.bigdata.data

data class LatLngPoint(
    val latitude: Double,
    val longitude: Double
)

data class ParkingLot(
    val id: String,
    val name: String,
    val location: LatLngPoint,
    val totalSpots: Int,
    val availableSpots: Int,
    val usageRate: Double,
    val areaSqm: Double,
    val openDays: String,
    val openHours: String,
    val hasEvCharging: Boolean,
    val address: String = "",
    val phone: String = ""
)

data class ParkingFilter(
    val radiusKm: Double = 1.0,
    val onlyOpenNow: Boolean = false,
    val onlyWithEvCharging: Boolean = false
)

data class ParkingWeights(
    val usageRateWeight: Double = 0.40,
    val availableSpotsWeight: Double = 0.30,
    val areaWeight: Double = 0.20,
    val openHoursWeight: Double = 0.10
)
