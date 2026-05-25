package com.example.bigdata.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.bigdata.BuildConfig
import com.example.bigdata.data.LatLngData
import com.example.bigdata.data.Location
import com.example.bigdata.data.ParkingLot
import com.example.bigdata.data.ParkingRepository
import com.example.bigdata.data.GoogleRoutesService
import com.example.bigdata.data.OsrmService
import com.example.bigdata.data.RoutesRequest
import com.example.bigdata.data.Waypoint
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val DefaultLocation = LatLng(36.3504, 127.3845)

enum class ScreenState { SEARCHING, PICKING, ROUTE, NAVIGATING }

sealed class RouteUiState {
    object Idle : RouteUiState()
    object Loading : RouteUiState()
    data class Success(val driveRoute: List<LatLng>, val walkRoute: List<LatLng>, val durationMin: Int, val distanceKm: Double) : RouteUiState()
    data class Error(val message: String) : RouteUiState()
}

@Composable
fun ParkingFinderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF2D6BFF),
            surface = Color(0xFF1C1D21),
            background = Color(0xFF121316),
            onSurface = Color.White
        ),
        content = content
    )
}

@Composable
fun ParkingMapScreen() {
    ParkingFinderTheme {
        ParkingFinderApp()
    }
}

@Composable
fun ParkingFinderApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val repository = remember { ParkingRepository() }
    val routesService = remember { GoogleRoutesService.create() }
    val osrmService = remember { OsrmService.create() }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    // States
    var screenState by remember { mutableStateOf(ScreenState.SEARCHING) }
    var currentLocation by remember { mutableStateOf(DefaultLocation) }
    var searchQuery by remember { mutableStateOf("") }
    
    var startPoint by remember { mutableStateOf<LatLng?>(null) }
    var startName by remember { mutableStateOf("내 위치") }
    
    var endPoint by remember { mutableStateOf<LatLng?>(null) }
    var endName by remember { mutableStateOf("") }
    
    var pickedLocation by remember { mutableStateOf<LatLng?>(null) }
    var pickedName by remember { mutableStateOf("") }

    var selectedParkingLot by remember { mutableStateOf<ParkingLot?>(null) }
    var routeUiState by remember { mutableStateOf<RouteUiState>(RouteUiState.Idle) }

    var parkingLots by remember { mutableStateOf(emptyList<ParkingLot>()) }
    var filteredParkingLots by remember { mutableStateOf(emptyList<ParkingLot>()) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DefaultLocation, 14f)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) {
            fetchCurrentLocation(fusedLocationClient) { latLng ->
                currentLocation = latLng
                startPoint = latLng
            }
        }
    }

    LaunchedEffect(Unit) {
        parkingLots = withContext(Dispatchers.IO) { repository.fetchParkingLots() }
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            fetchCurrentLocation(fusedLocationClient) { latLng ->
                currentLocation = latLng
                startPoint = latLng
                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            }
        }
    }

    // Filter parking lots near destination
    LaunchedEffect(endPoint, parkingLots) {
        val dest = endPoint
        if (dest != null) {
            filteredParkingLots = parkingLots.filter { lot ->
                distanceKm(dest, LatLng(lot.location.latitude, lot.location.longitude)) < 2.0 // 2km radius
            }.sortedBy { distanceKm(dest, LatLng(it.location.latitude, it.location.longitude)) }
        } else {
            filteredParkingLots = emptyList()
            selectedParkingLot = null
        }
    }

    // Routes logic using Google Routes API with OSRM fallback for Korea
    LaunchedEffect(startPoint, endPoint, selectedParkingLot) {
        val start = startPoint
        val dest = endPoint
        val lot = selectedParkingLot
        
        if (start != null && dest != null) {
            routeUiState = RouteUiState.Loading
            runCatching {
                val packageName = context.packageName
                val certSha1 = getCertificateSHA1(context) ?: ""
                
                // Helper to get route from OSRM
                suspend fun fetchOsrmRoute(s: LatLng, e: LatLng, mode: String): Pair<List<LatLng>, Pair<Int, Double>>? {
                    return try {
                        val profile = if (mode == "WALK") "walking" else "driving"
                        val response = osrmService.getRoute(profile, "${s.longitude},${s.latitude};${e.longitude},${e.latitude}")
                        if (response.code == "Ok" && !response.routes.isNullOrEmpty()) {
                            val route = response.routes[0]
                            PolyUtil.decode(route.geometry) to (route.duration.toInt() to route.distance / 1000.0)
                        } else null
                    } catch (ex: Exception) {
                        Log.e("ParkingMap", "OSRM error: ${ex.message}")
                        null
                    }
                }

                // If a parking lot is selected, we need two segments: Car and Walk
                if (lot != null) {
                    var driveRes = routesService.computeRoutes(
                        apiKey = BuildConfig.ROUTES_API_KEY,
                        fieldMask = "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline",
                        packageName = packageName,
                        certSha1 = certSha1,
                        request = RoutesRequest(
                            origin = Waypoint(Location(LatLngData(start.latitude, start.longitude))),
                            destination = Waypoint(Location(LatLngData(lot.location.latitude, lot.location.longitude))),
                            travelMode = "DRIVE",
                            routingPreference = "TRAFFIC_AWARE"
                        )
                    )
                    
                    // Google DRIVE often fails in Korea (returns empty routes) -> Fallback to OSRM
                    var drivePoints: List<LatLng> = emptyList()
                    var driveSec = 0
                    var driveDist = 0.0

                    if (!driveRes.routes.isNullOrEmpty()) {
                        val r = driveRes.routes[0]
                        drivePoints = PolyUtil.decode(r.polyline.encodedPolyline)
                        driveSec = parseDuration(r.duration)
                        driveDist = r.distanceMeters / 1000.0
                    } else {
                        val osrm = fetchOsrmRoute(start, LatLng(lot.location.latitude, lot.location.longitude), "DRIVE")
                        if (osrm != null) {
                            drivePoints = osrm.first
                            driveSec = osrm.second.first
                            driveDist = osrm.second.second
                        }
                    }
                    
                    var walkRes = routesService.computeRoutes(
                        apiKey = BuildConfig.ROUTES_API_KEY,
                        fieldMask = "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline",
                        packageName = packageName,
                        certSha1 = certSha1,
                        request = RoutesRequest(
                            origin = Waypoint(Location(LatLngData(lot.location.latitude, lot.location.longitude))),
                            destination = Waypoint(Location(LatLngData(dest.latitude, dest.longitude))),
                            travelMode = "WALK",
                            routingPreference = null
                        )
                    )
                    
                    var walkPoints: List<LatLng> = emptyList()
                    var walkSec = 0
                    var walkDist = 0.0

                    if (!walkRes.routes.isNullOrEmpty()) {
                        val r = walkRes.routes[0]
                        walkPoints = PolyUtil.decode(r.polyline.encodedPolyline)
                        walkSec = parseDuration(r.duration)
                        walkDist = r.distanceMeters / 1000.0
                    } else {
                        val osrm = fetchOsrmRoute(LatLng(lot.location.latitude, lot.location.longitude), dest, "WALK")
                        if (osrm != null) {
                            walkPoints = osrm.first
                            walkSec = osrm.second.first
                            walkDist = osrm.second.second
                        }
                    }
                    
                    if (drivePoints.isNotEmpty() || walkPoints.isNotEmpty()) {
                        RouteUiState.Success(drivePoints, walkPoints, (driveSec + walkSec) / 60, driveDist + walkDist)
                    } else {
                        RouteUiState.Error("경로를 찾을 수 없습니다.")
                    }
                } else {
                    // Direct route (Drive)
                    val res = routesService.computeRoutes(
                        apiKey = BuildConfig.ROUTES_API_KEY,
                        fieldMask = "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline",
                        packageName = packageName,
                        certSha1 = certSha1,
                        request = RoutesRequest(
                            origin = Waypoint(Location(LatLngData(start.latitude, start.longitude))),
                            destination = Waypoint(Location(LatLngData(dest.latitude, dest.longitude))),
                            travelMode = "DRIVE",
                            routingPreference = "TRAFFIC_AWARE"
                        )
                    )
                    
                    if (!res.routes.isNullOrEmpty()) {
                        val r = res.routes[0]
                        RouteUiState.Success(PolyUtil.decode(r.polyline.encodedPolyline), emptyList(), parseDuration(r.duration) / 60, r.distanceMeters / 1000.0)
                    } else {
                        val osrm = fetchOsrmRoute(start, dest, "DRIVE")
                        if (osrm != null) {
                            RouteUiState.Success(osrm.first, emptyList(), osrm.second.first / 60, osrm.second.second)
                        } else {
                            RouteUiState.Error("경로를 찾을 수 없습니다.")
                        }
                    }
                }
            }.onSuccess {
                routeUiState = it
            }.onFailure { e ->
                Log.e("ParkingMap", "Route error", e)
                routeUiState = RouteUiState.Error("네트워크 오류: ${e.message}")
            }
        } else {
            routeUiState = RouteUiState.Idle
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission && screenState != ScreenState.NAVIGATING,
                mapStyleOptions = null
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false,
                mapToolbarEnabled = false,
                scrollGesturesEnabled = true,
                zoomGesturesEnabled = true
            ),
            onMapClick = { latLng ->
                if (screenState == ScreenState.SEARCHING || screenState == ScreenState.PICKING) {
                    scope.launch {
                        val name = reverseGeocode(context, latLng)
                        pickedLocation = latLng
                        pickedName = name
                        screenState = ScreenState.PICKING
                    }
                }
            }
        ) {
            // Picked location marker
            if (screenState == ScreenState.PICKING) {
                pickedLocation?.let {
                    Marker(
                        state = MarkerState(it),
                        title = pickedName,
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                }
            }

            // Route markers and line
            if (screenState == ScreenState.ROUTE || screenState == ScreenState.NAVIGATING) {
                startPoint?.let {
                    Marker(
                        state = MarkerState(it),
                        title = "출발: $startName",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }
                
                selectedParkingLot?.let {
                    Marker(
                        state = MarkerState(LatLng(it.location.latitude, it.location.longitude)),
                        title = "주차장: ${it.name}",
                        icon = BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_mylocation)
                    )
                }

                endPoint?.let {
                    Marker(
                        state = MarkerState(it),
                        title = "도착: $endName",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                }
                
                // Polyline rendering
                if (routeUiState is RouteUiState.Success) {
                    val state = routeUiState as RouteUiState.Success
                    // Drive segment
                    Polyline(
                        points = state.driveRoute,
                        color = Color(0xFF2D6BFF),
                        width = 16f,
                        jointType = JointType.ROUND
                    )
                    // Walk segment
                    if (state.walkRoute.isNotEmpty()) {
                        Polyline(
                            points = state.walkRoute,
                            color = Color.Green,
                            width = 12f,
                            jointType = JointType.ROUND,
                            pattern = listOf(Dash(20f), Gap(20f))
                        )
                    }
                }

                // Nearby parking markers
                filteredParkingLots.forEach { lot ->
                    if (lot.id != selectedParkingLot?.id) {
                        Marker(
                            state = MarkerState(LatLng(lot.location.latitude, lot.location.longitude)),
                            title = lot.name,
                            snippet = "빈자리 ${lot.availableSpots}/${lot.totalSpots}",
                            alpha = 0.7f,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET),
                            onClick = {
                                selectedParkingLot = lot
                                true
                            }
                        )
                    }
                }
            }
        }

        // Top Search Bar (Only when not navigating)
        if (screenState != ScreenState.NAVIGATING) {
            TopDualSearchBar(
                screenState = screenState,
                startName = startName,
                endName = endName,
                searchQuery = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = {
                    scope.launch {
                        val latLng = geocode(context, searchQuery)
                        if (latLng != null) {
                            val name = reverseGeocode(context, latLng)
                            pickedLocation = latLng
                            pickedName = name
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                            screenState = ScreenState.PICKING
                        } else {
                            Toast.makeText(context, "위치를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onBack = {
                    screenState = ScreenState.SEARCHING
                    pickedLocation = null
                    endPoint = null
                    endName = ""
                    selectedParkingLot = null
                    routeUiState = RouteUiState.Idle
                },
                onMyLocation = {
                    fetchCurrentLocation(fusedLocationClient) { latLng ->
                        currentLocation = latLng
                        scope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                        }
                    }
                },
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
            )
        }

        // In-App Navigation Overlay
        AnimatedVisibility(
            visible = screenState == ScreenState.NAVIGATING,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val (distText, timeText) = when (val s = routeUiState) {
                is RouteUiState.Success -> formatDistance(s.distanceKm) to "${s.durationMin}분"
                else -> "" to ""
            }
            InAppNavigationOverlay(
                endName = endName,
                distanceRemaining = distText,
                timeRemaining = timeText,
                onRecenter = {
                    scope.launch {
                        startPoint?.let {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.builder()
                                        .target(it)
                                        .zoom(18f)
                                        .bearing(30f)
                                        .tilt(45f)
                                        .build()
                                )
                            )
                        }
                    }
                },
                onExit = {
                    screenState = ScreenState.ROUTE
                    scope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomTo(14f))
                    }
                }
            )
        }

        // Bottom UI
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            when (screenState) {
                ScreenState.SEARCHING -> {}
                ScreenState.PICKING -> {
                    LocationDetailSheet(
                        name = pickedName,
                        onSetStart = {
                            startPoint = pickedLocation
                            startName = pickedName
                            pickedLocation = null
                            if (endPoint != null) screenState = ScreenState.ROUTE
                            else screenState = ScreenState.SEARCHING
                        },
                        onSetEnd = {
                            endPoint = pickedLocation
                            endName = pickedName
                            pickedLocation = null
                            if (startPoint != null) screenState = ScreenState.ROUTE
                            else screenState = ScreenState.SEARCHING
                        },
                        onClose = {
                            screenState = ScreenState.SEARCHING
                            pickedLocation = null
                        }
                    )
                }
                ScreenState.ROUTE -> {
                    RouteSummarySheet(
                        routeUiState = routeUiState,
                        parkingLots = filteredParkingLots,
                        selectedParkingLot = selectedParkingLot,
                        onParkingSelected = { selectedParkingLot = it },
                        destination = endPoint,
                        onStartNavigation = {
                            if (routeUiState is RouteUiState.Success) {
                                screenState = ScreenState.NAVIGATING
                                scope.launch {
                                    startPoint?.let {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newCameraPosition(
                                                CameraPosition.builder()
                                                    .target(it)
                                                    .zoom(18f)
                                                    .bearing(30f)
                                                    .tilt(45f)
                                                    .build()
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
                ScreenState.NAVIGATING -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF1C1D21),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("안내 중", color = Color.Green, fontWeight = FontWeight.Bold)
                                Text(endName, color = Color.White, style = MaterialTheme.typography.titleMedium)
                                if (selectedParkingLot != null) {
                                    Text("주차장 경유: ${selectedParkingLot!!.name}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Button(
                                onClick = { screenState = ScreenState.ROUTE },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("중단")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RouteSummarySheet(
    routeUiState: RouteUiState,
    parkingLots: List<ParkingLot>,
    selectedParkingLot: ParkingLot?,
    onParkingSelected: (ParkingLot) -> Unit,
    onStartNavigation: () -> Unit,
    destination: LatLng? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF121316),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column {
            // Summary Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    when (routeUiState) {
                        is RouteUiState.Loading -> {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF2D6BFF))
                            Text("경로 계산 중...", color = Color.Gray)
                        }
                        is RouteUiState.Success -> {
                            val label = if (selectedParkingLot != null) "주차장 경유 추천" else "실시간 추천"
                            Text(label, color = Color(0xFF2D6BFF), fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("${routeUiState.durationMin}분", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(formatDistance(routeUiState.distanceKm), color = Color.Gray)
                            }
                        }
                        is RouteUiState.Error -> {
                            Text("경로 오류", color = Color.Red, fontWeight = FontWeight.Bold)
                            Text(routeUiState.message, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                        else -> {}
                    }
                }
                Button(
                    onClick = onStartNavigation,
                    enabled = routeUiState is RouteUiState.Success,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6BFF))
                ) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("안내시작", fontWeight = FontWeight.Bold)
                }
            }

            if (selectedParkingLot != null && routeUiState is RouteUiState.Success) {
                Surface(
                    color = Color(0xFF1C1D21),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = Color.Green)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("주차 후 목적지까지 도보 이동이 포함됩니다.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF1C1D21))

            // Nearby Parking List
            Text(
                "도착지 주변 주차장 (${parkingLots.size})",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                modifier = Modifier
                    .height(250.dp)
                    .fillMaxWidth()
            ) {
                items(parkingLots) { lot ->
                    val dist = destination?.let { distanceKm(it, LatLng(lot.location.latitude, lot.location.longitude)) } ?: 0.0
                    ParkingLotItem(
                        lot = lot,
                        isSelected = lot.id == selectedParkingLot?.id,
                        distanceFromDest = dist,
                        onSelect = { onParkingSelected(lot) }
                    )
                }
            }
        }
    }
}

@Composable
fun TopDualSearchBar(
    screenState: ScreenState,
    startName: String,
    endName: String,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
    onMyLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF1C1D21),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (screenState == ScreenState.ROUTE) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color.Green, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(startName, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color.Red, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(endName, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.SwapVert, contentDescription = "Swap", tint = Color.Gray)
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (screenState == ScreenState.PICKING) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                    TextField(
                        value = searchQuery,
                        onValueChange = onQueryChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("장소, 주소 검색", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = { 
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    IconButton(onClick = onMyLocation) {
                        Icon(Icons.Default.MyLocation, contentDescription = "My Location", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun LocationDetailSheet(
    name: String,
    onSetStart: () -> Unit,
    onSetEnd: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1C1D21),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(name, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                }
            }
            Text("지번 주소 정보 등...", color = Color.Gray)
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onSetStart,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E4149))
                ) {
                    Text("출발")
                }
                Button(
                    onClick = onSetEnd,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6BFF))
                ) {
                    Text("도착")
                }
            }
        }
    }
}

@Composable
fun InAppNavigationOverlay(
    endName: String,
    distanceRemaining: String,
    timeRemaining: String,
    onRecenter: () -> Unit,
    onExit: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top TBT Card
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
                .clickable { onRecenter() },
            color = Color(0xFF2D6BFF),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.TurnRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("300m 앞 우회전", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(endName, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun ParkingLotItem(
    lot: ParkingLot,
    isSelected: Boolean = false,
    distanceFromDest: Double = 0.0,
    onSelect: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) Color(0xFF2D6BFF).copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onSelect() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            color = if (isSelected) Color(0xFF2D6BFF) else Color(0xFF1C1D21),
            shape = CircleShape
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = if (isSelected) Color.White else Color(0xFF2D6BFF), modifier = Modifier.padding(8.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(lot.name, color = Color.White, fontWeight = FontWeight.Bold)
            Text("빈자리 ${lot.availableSpots}/${lot.totalSpots}", color = if (lot.availableSpots > 0) Color.Green else Color.Red)
        }
        if (isSelected) {
            Text("선택됨", color = Color(0xFF2D6BFF), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        } else {
            Text(formatDistance(distanceFromDest), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// Helper functions
fun fetchCurrentLocation(fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient, onLocation: (LatLng) -> Unit) {
    try {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) onLocation(LatLng(location.latitude, location.longitude))
        }
    } catch (_: SecurityException) {}
}

suspend fun geocode(context: Context, query: String): LatLng? {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.KOREA)
            val addresses = geocoder.getFromLocationName(query, 1)
            if (!addresses.isNullOrEmpty()) {
                LatLng(addresses[0].latitude, addresses[0].longitude)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

suspend fun reverseGeocode(context: Context, latLng: LatLng): String {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.KOREA)
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                addresses[0].getAddressLine(0) ?: "알 수 없는 위치"
            } else "알 수 없는 위치"
        } catch (e: Exception) {
            "알 수 없는 위치"
        }
    }
}

fun distanceKm(a: LatLng, b: LatLng): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLng = Math.toRadians(b.longitude - a.longitude)
    val hav = Math.sin(dLat / 2).let { it * it } +
            Math.sin(dLng / 2).let { it * it } * Math.cos(Math.toRadians(a.latitude)) * Math.cos(Math.toRadians(b.latitude))
    return 2 * earthRadiusKm * Math.asin(Math.sqrt(hav))
}

fun formatDistance(km: Double): String = if (km < 1.0) "${(km * 1000).toInt()}m" else String.format("%.1fkm", km)

private fun parseDuration(duration: String): Int {
    return duration.replace("s", "").toIntOrNull() ?: 0
}

private fun getCertificateSHA1(context: Context): String? {
    return try {
        val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        }
        val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.signingCertificateHistory
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }
        val firstSig = signatures?.firstOrNull() ?: return null
        val md = MessageDigest.getInstance("SHA-1")
        val publicKey = md.digest(firstSig.toByteArray())
        publicKey.joinToString("") { "%02X".format(it) }
    } catch (e: Exception) {
        Log.e("ParkingMap", "Cert error", e)
        null
    }
}
