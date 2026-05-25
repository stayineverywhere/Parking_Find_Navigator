# Parking Map App

A Compose-based Android app that shows a map, lets users pick start/destination points, and recommends nearby parking lots with filters and ranking.

## Setup

Add your API values to `local.properties` (do not commit these):

```
PUBLIC_DATA_API_KEY=YOUR_DATA_GO_KR_SERVICE_KEY
PARKING_API_BASE_URL=https://apis.data.go.kr/6300000/pis/parkinglotIF
```

If `PARKING_API_BASE_URL` or `PUBLIC_DATA_API_KEY` is missing, the app falls back to sample parking data.

## Run

Open the project in Android Studio and run the `app` configuration.

## Notes

- The UI mirrors the ParkingLot web reference: start/end confirmation flow, radius filter, and congestion-colored list.
- Map rendering uses MapLibre with an OSM-based demo style (`https://demotiles.maplibre.org/style.json`). This does not require an API key but is intended for light usage.
- For production usage, switch to your own tile server or a provider with an API key and usage terms.
- Search and reverse-geocoding rely on Android Geocoder (network required).
- The parking API parser expects the XML schema from the `pis/parkinglotIF` endpoint. Adjust field mapping in `app/src/main/java/com/example/bigdata/data/ParkingRepository.kt` if the schema changes.
- Route navigation launches Google Maps intents.
