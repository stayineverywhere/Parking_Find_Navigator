package com.example.bigdata

import com.example.bigdata.data.LatLngPoint
import com.example.bigdata.data.ParkingFilter
import com.example.bigdata.data.ParkingLot
import com.example.bigdata.data.ParkingRecommender
import com.example.bigdata.data.ParkingWeights
import org.junit.Assert.assertEquals
import org.junit.Test

class ParkingRecommenderTest {
    @Test
    fun scoreSortsByPriority() {
        val destination = LatLngPoint(36.35, 127.38)
        val lots = listOf(
            ParkingLot(
                id = "a",
                name = "LowUsage",
                location = LatLngPoint(36.35, 127.38),
                totalSpots = 100,
                availableSpots = 50,
                usageRate = 0.2,
                areaSqm = 1000.0,
                openDays = "Mon-Sun",
                openHours = "24H",
                hasEvCharging = true
            ),
            ParkingLot(
                id = "b",
                name = "HighUsage",
                location = LatLngPoint(36.3501, 127.3801),
                totalSpots = 100,
                availableSpots = 10,
                usageRate = 0.9,
                areaSqm = 2000.0,
                openDays = "Mon-Sun",
                openHours = "24H",
                hasEvCharging = true
            )
        )

        val recommender = ParkingRecommender(ParkingWeights())
        val results = recommender.filterAndScore(lots, destination, ParkingFilter(radiusKm = 2.0))

        assertEquals("LowUsage", results.first().lot.name)
    }
}

