package com.example.bigdata.data

import android.net.Uri
import com.example.bigdata.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class ParkingRepository(
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    suspend fun fetchParkingLots(): List<ParkingLot> {
        val baseUrl = BuildConfig.PARKING_API_BASE_URL
        val apiKey = BuildConfig.PUBLIC_DATA_API_KEY
        if (baseUrl.isBlank() || apiKey.isBlank()) {
            return sampleParkingLots()
        }

        val url = Uri.parse(baseUrl)
            .buildUpon()
            .appendQueryParameter("serviceKey", apiKey)
            .appendQueryParameter("numOfRows", "400")
            .appendQueryParameter("pageNo", "1")
            .appendQueryParameter("type", "xml")
            .build()
            .toString()

        return runCatching {
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@runCatching sampleParkingLots()
                }
                val body = response.body?.string().orEmpty()
                if (body.trimStart().startsWith("<")) {
                    parseParkingLotsXml(body)
                } else {
                    sampleParkingLots()
                }
            }
        }.getOrElse { sampleParkingLots() }
    }

    private fun parseParkingLotsXml(xml: String): List<ParkingLot> {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(xml.reader())

        val lots = mutableListOf<ParkingLot>()
        var currentTag: String? = null
        var currentItem: MutableMap<String, String>? = null
        var index = 0

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag == "item") {
                        currentItem = mutableMapOf()
                    }
                }
                XmlPullParser.TEXT -> {
                    val tag = currentTag
                    val item = currentItem
                    if (tag != null && item != null) {
                        item[tag] = parser.text.trim()
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item") {
                        currentItem?.let { item ->
                            val name = item["name"].orEmpty()
                            val lat = item["lat"]?.toDoubleOrNull()
                            val lon = item["lon"]?.toDoubleOrNull()
                            if (name.isNotBlank() && lat != null && lon != null) {
                                val total = item["totalQty"]?.toIntOrNull() ?: 0
                                val available = item["resQty"]?.toIntOrNull() ?: 0
                                val usage = if (total > 0) {
                                    1.0 - (available.toDouble() / total.toDouble())
                                } else {
                                    0.0
                                }
                                val openDays = item["operDay"].orEmpty()
                                val openHours = buildString {
                                    append(item["weekdayOpenTime"].orEmpty())
                                    append("-")
                                    append(item["weekdayCloseTime"].orEmpty())
                                }
                                val address = item["address"].orEmpty()
                                val phone = item["tel"].orEmpty()

                                lots.add(
                                    ParkingLot(
                                        id = "${name}_${index++}",
                                        name = name,
                                        location = LatLngPoint(lat, lon),
                                        totalSpots = total,
                                        availableSpots = available,
                                        usageRate = usage,
                                        areaSqm = 0.0,
                                        openDays = if (openDays.isBlank()) "N/A" else openDays,
                                        openHours = if (openHours == "-") "N/A" else openHours,
                                        hasEvCharging = false,
                                        address = address,
                                        phone = phone
                                    )
                                )
                            }
                        }
                        currentItem = null
                    }
                    currentTag = null
                }
            }
            parser.next()
        }

        return if (lots.isNotEmpty()) lots else sampleParkingLots()
    }

    private fun sampleParkingLots(): List<ParkingLot> {
        return listOf(
            ParkingLot(
                id = "sample_1",
                name = "Daejeon City Hall",
                location = LatLngPoint(36.3504, 127.3845),
                totalSpots = 300,
                availableSpots = 120,
                usageRate = 0.6,
                areaSqm = 5400.0,
                openDays = "Mon-Sun",
                openHours = "24H",
                hasEvCharging = true,
                address = "Daejeon Jung-gu",
                phone = ""
            ),
            ParkingLot(
                id = "sample_2",
                name = "Government Complex Daejeon",
                location = LatLngPoint(36.3540, 127.3636),
                totalSpots = 2600,
                availableSpots = 300,
                usageRate = 0.85,
                areaSqm = 12000.0,
                openDays = "Mon-Sun",
                openHours = "06-23",
                hasEvCharging = true,
                address = "Daejeon Seo-gu",
                phone = ""
            ),
            ParkingLot(
                id = "sample_3",
                name = "Expo Park",
                location = LatLngPoint(36.3731, 127.3878),
                totalSpots = 800,
                availableSpots = 420,
                usageRate = 0.45,
                areaSqm = 8800.0,
                openDays = "Mon-Sun",
                openHours = "24H",
                hasEvCharging = false,
                address = "Daejeon Yuseong-gu",
                phone = ""
            ),
            ParkingLot(
                id = "sample_4",
                name = "Dunsan Public Parking",
                location = LatLngPoint(36.3510, 127.3771),
                totalSpots = 450,
                availableSpots = 180,
                usageRate = 0.6,
                areaSqm = 3900.0,
                openDays = "Mon-Fri",
                openHours = "08-20",
                hasEvCharging = false,
                address = "Daejeon Seo-gu",
                phone = ""
            )
        )
    }
}
