package com.example.bigdata.data

import kotlin.math.*

class ParkingRecommender(
    private val weights: ParkingWeights = ParkingWeights()
) {
    fun filterAndScore(
        parkingLots: List<ParkingLot>,
        destination: LatLngPoint,
        filter: ParkingFilter
    ): List<ScoredParkingLot> {
        val maxArea = parkingLots.maxOfOrNull { it.areaSqm } ?: 1.0
        val candidates = parkingLots.filter { lot ->
            val distanceKm = haversineKm(destination, lot.location)
            distanceKm <= filter.radiusKm && (!filter.onlyWithEvCharging || lot.hasEvCharging)
        }

        return candidates.map { lot ->
            val usageScore = 1.0 - normalizePercentage(lot.usageRate)
            val availabilityScore = if (lot.totalSpots > 0) {
                lot.availableSpots.toDouble() / lot.totalSpots
            } else {
                0.0
            }
            val areaScore = (lot.areaSqm / maxArea).coerceIn(0.0, 1.0)
            val openScore = if (lot.openHours.contains("24")) 1.0 else 0.5
            val score = (usageScore * weights.usageRateWeight) +
                (availabilityScore * weights.availableSpotsWeight) +
                (areaScore * weights.areaWeight) +
                (openScore * weights.openHoursWeight)
            ScoredParkingLot(lot, score)
        }.sortedByDescending { it.score }
    }

    private fun normalizePercentage(value: Double): Double {
        val normalized = if (value > 1.0) value / 100.0 else value
        return normalized.coerceIn(0.0, 1.0)
    }

    private fun haversineKm(a: LatLngPoint, b: LatLngPoint): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val sinLat = sin(dLat / 2)
        val sinLon = sin(dLon / 2)
        val h = sinLat * sinLat + cos(lat1) * cos(lat2) * sinLon * sinLon
        val c = 2 * asin(min(1.0, sqrt(h)))
        return earthRadiusKm * c
    }
}

data class ScoredParkingLot(
    val lot: ParkingLot,
    val score: Double
)

