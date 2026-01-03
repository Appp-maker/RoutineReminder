package com.example.routinereminder.ui
import android.os.Handler
import android.os.Looper
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import com.example.routinereminder.data.model.SessionStats
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import android.Manifest
import android.annotation.SuppressLint
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.geojson.Point
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Location
import android.net.Uri
import com.example.routinereminder.MainActivity
import com.example.routinereminder.data.SnapshotStorage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.example.routinereminder.ui.SessionStore
import com.google.android.gms.location.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt
import android.content.BroadcastReceiver
import android.content.IntentFilter
import com.example.routinereminder.location.TrackingService
import androidx.compose.ui.platform.LocalLifecycleOwner


private enum class ActivityType(val label: String, val met: Double) {
    WALKING("Walking", 3.8),
    RUNNING("Running", 9.8),
    CYCLING("Cycling", 8.0)
}

private enum class TrackingMode(val label: String, val value: String) {
    BALANCED("Balanced", TrackingService.MODE_BALANCED),
    HIGH_ACCURACY("High accuracy", TrackingService.MODE_HIGH_ACCURACY)
}

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }
    // ------------------------------------------------------
    // RECEIVE GPS UPDATES FROM TrackingService (background)
    // ------------------------------------------------------


    // UI state
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var recording by remember { mutableStateOf(false) }
    var firstFix by remember { mutableStateOf(true) }
    var userZoom by remember { mutableStateOf(15.5) }
    var includeMapInShare by remember { mutableStateOf(true) }
    var activity by remember { mutableStateOf(ActivityType.RUNNING) }
    var trackingMode by remember { mutableStateOf(TrackingMode.BALANCED) }
    var trackingMenuExpanded by remember { mutableStateOf(false) }
    var permissionRequired by remember { mutableStateOf(false) }

    // live stats
    var distanceMeters by remember { mutableStateOf(0.0) }
    var durationSec by remember { mutableStateOf(0L) }
    var calories by remember { mutableStateOf(0.0) }
    val trail = remember { mutableStateListOf<Point>() }

    // timer
    val scope = rememberCoroutineScope()
    var timerJob by remember { mutableStateOf<Job?>(null) }
    var inactivityJob by remember { mutableStateOf<Job?>(null) }
    var lastMovementAt by remember { mutableStateOf<Long?>(null) }
    val stopRecording = rememberUpdatedState {
        if (recording) {
            stopTracking(context)
            timerJob?.cancel()
            timerJob = null
            inactivityJob?.cancel()
            inactivityJob = null
            recording = false
        }
    }
    val noMovementTimeoutMs = remember { 2 * 60 * 1000L }

    // weight (read from MainViewModel if you already expose it; otherwise prompt will be handled there)
    val weightKg by remember { mutableStateOf(viewModel.currentUserWeightKgOrNull()) } // implement in your MainViewModel; return null if unknown
    var effectiveWeightKg by remember { mutableStateOf(weightKg ?: 70.0) } // default 70 if not provided yet


    //Kalman filter
    val kalman = remember { KalmanLatLong() }
    var smoothedLat by remember { mutableStateOf<Double?>(null) }
    var smoothedLng by remember { mutableStateOf<Double?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun requestLocationPermissions() {
        (context as? MainActivity)?.requestLocationPermissions()
    }

    fun startTrackingIfPermitted(): Boolean {
        if (!hasLocationPermission(context)) {
            permissionRequired = true
            requestLocationPermissions()
            return false
        }
        permissionRequired = false
        startTracking(context, trackingMode)
        return true
    }

    // ask once if weight missing
    LaunchedEffect(weightKg) {
        if (weightKg == null) {
            viewModel.promptUserForWeightOnce(context) // show your own dialog flow wired to DB
        }
    }

    // location callback implementation
    fun onLocation(location: Location) {

        // -----------------------------
        // 1) Accuracy filter
        // -----------------------------
        if (location.accuracy > 50f) {
            return  // ignore noisy GPS
        }

        // -----------------------------
        // 2) Run through Kalman Filter
        // -----------------------------
        val (kalLat, kalLng) = kalman.process(
            location.latitude,
            location.longitude,
            location.accuracy,
            System.currentTimeMillis()
        )

        // -----------------------------
        // 3) Apply Low-Pass Smoothing
        // -----------------------------
        val alpha = 0.35 // lower = smoother, higher = more reactive

        val lpLat = if (smoothedLat == null) kalLat
        else (alpha * kalLat + (1 - alpha) * smoothedLat!!)

        val lpLng = if (smoothedLng == null) kalLng
        else (alpha * kalLng + (1 - alpha) * smoothedLng!!)

        smoothedLat = lpLat
        smoothedLng = lpLng

        // Use filtered coordinates
        val newPoint = Point.fromLngLat(lpLng, lpLat)

        // -----------------------------
        // 4) Min-movement filter (removes jitter)
        // -----------------------------
        val last = trail.lastOrNull()
        var allowTrailUpdate = true
        if (last != null) {
            val delta = haversineMeters(last, newPoint)

            // Ignore tiny drift below 2m (still update marker)
            if (delta < 2.0) allowTrailUpdate = false

            // Ignore giant GPS spikes
            if (delta > 100.0) allowTrailUpdate = false
        }

        // Continue with original code here
        val latLng = LatLng(lpLat, lpLng)
        val m = map ?: return
        val s = m.style ?: return


        // update marker
        val locationSource = s.getSourceAs("location-source") as? GeoJsonSource
        locationSource?.setGeoJson(
            Feature.fromGeometry(
                Point.fromLngLat(latLng.longitude, latLng.latitude)
            )
        )

        // trail update
        if (allowTrailUpdate && (last == null || last != newPoint)) {
            // distance increment
            if (last != null) distanceMeters += haversineMeters(last, newPoint)

            trail.add(newPoint)
            lastMovementAt = System.currentTimeMillis()

            val line = LineString.fromLngLats(trail.toList())
            val trailSource = s.getSourceAs("trail-source") as? GeoJsonSource
            trailSource?.setGeoJson(Feature.fromGeometry(line))
        }

        // follow user but keep zoom
        val cp = CameraPosition.Builder().target(latLng).zoom(userZoom).build()
        m.animateCamera(CameraUpdateFactory.newCameraPosition(cp))
    }
    var receiver by remember { mutableStateOf<BroadcastReceiver?>(null) }

    DisposableEffect(Unit) {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == TrackingService.ACTION_PERMISSION_REQUIRED) {
                    permissionRequired = true
                    stopRecording.value.invoke()
                    return
                }
                val lat = intent?.getDoubleExtra("lat", 0.0) ?: return
                val lng = intent?.getDoubleExtra("lng", 0.0) ?: return

                // Convert broadcast into Location for your existing logic
                val loc = Location("tracking").apply {
                    latitude = lat
                    longitude = lng
                    accuracy = 5f
                }
                onLocation(loc)
            }
        }

        receiver = r
        val filter = IntentFilter().apply {
            addAction("TRACKING_LOCATION")
            addAction(TrackingService.ACTION_PERMISSION_REQUIRED)
        }
        context.registerReceiver(r, filter, Context.RECEIVER_NOT_EXPORTED)


        onDispose {
            context.unregisterReceiver(r)
        }
    }

    DisposableEffect(lifecycleOwner, recording, trackingMode) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && recording) {
                stopTracking(context)
            } else if (event == Lifecycle.Event.ON_START && recording) {
                if (!startTrackingIfPermitted()) {
                    stopRecording.value.invoke()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {

            // ----------- STATS HEADER -----------
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF121212))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatBlock(title = "Duration", value = formatHMS(durationSec))
                    StatBlock(title = "Distance (km)", value = "%.2f".format(distanceMeters / 1000.0))
                    StatBlock(title = "Avg. Pace", value = formatPace(distanceMeters, durationSec))
                    StatBlock(title = "Calories", value = calories.roundToInt().toString())
                }
            }

            if (permissionRequired) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF3B1B1B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Location permission required to track.",
                            color = ComposeColor(0xFFFFB4B4),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = { requestLocationPermissions() },
                            colors = ButtonDefaults.buttonColors(containerColor = ComposeColor(0xFFB3261E))
                        ) {
                            Text("Grant")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tracking: ${trackingMode.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ComposeColor.Gray
                )
                IconButton(onClick = { trackingMenuExpanded = true }) {
                    Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Tracking mode")
                }
                DropdownMenu(
                    expanded = trackingMenuExpanded,
                    onDismissRequest = { trackingMenuExpanded = false }
                ) {
                    TrackingMode.values().forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.label) },
                            onClick = {
                                trackingMode = mode
                                trackingMenuExpanded = false
                                if (recording) {
                                    if (!startTrackingIfPermitted()) {
                                        stopRecording.value.invoke()
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // ----------- MAP VIEW -----------
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // MAP fills all space
                AndroidView(
                    factory = { ctx ->
                        val mv = MapView(ctx)
                        mapView = mv
                        mv.getMapAsync(object : OnMapReadyCallback {
                            override fun onMapReady(m: MapLibreMap) {
                                map = m
                                m.setStyle(Style.Builder().fromUri("asset://esri-hybrid-style.json")) { style ->
                                    val locSrc = GeoJsonSource("location-source", GeoJsonOptions())
                                    style.addSource(locSrc)
                                    style.addLayer(
                                        CircleLayer("location-layer", "location-source").withProperties(
                                            org.maplibre.android.style.layers.PropertyFactory.circleRadius(6f),
                                            org.maplibre.android.style.layers.PropertyFactory.circleColor("#2196F3"),
                                            org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth(2f),
                                            org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor("#FFFFFF")
                                        )
                                    )

                                    val trailSrc = GeoJsonSource("trail-source", GeoJsonOptions())
                                    style.addSource(trailSrc)
                                    style.addLayer(
                                        LineLayer("trail-layer", "trail-source").withProperties(
                                            org.maplibre.android.style.layers.PropertyFactory.lineColor("#FF4081"),
                                            org.maplibre.android.style.layers.PropertyFactory.lineWidth(4f),
                                            org.maplibre.android.style.layers.PropertyFactory.lineJoin("round"),
                                            org.maplibre.android.style.layers.PropertyFactory.lineCap("round")
                                        )
                                    )

                                    m.uiSettings.isZoomGesturesEnabled = true
                                    m.uiSettings.isScrollGesturesEnabled = true
                                    m.uiSettings.isRotateGesturesEnabled = true
                                    m.uiSettings.isTiltGesturesEnabled = true

                                    m.addOnCameraMoveListener { userZoom = m.cameraPosition.zoom }

                                    if (trail.isNotEmpty()) {
                                        // Zoom to full trail if points exist
                                        zoomToTrailOnMapOpen(m, trail.toList())
                                    } else {
                                        // Safe LocationComponent setup — run only after style is loaded
                                        try {
                                            val fine = ActivityCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.ACCESS_FINE_LOCATION
                                            )
                                            val coarse = ActivityCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )

                                            if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
                                                val locationComponent = m.locationComponent
                                                val options =
                                                    org.maplibre.android.location.LocationComponentActivationOptions
                                                        .builder(context, style)
                                                        .useDefaultLocationEngine(true)
                                                        .build()

                                                locationComponent.activateLocationComponent(options)
                                                locationComponent.isLocationComponentEnabled = true

                                                val lastLoc =
                                                    try {
                                                        locationComponent.lastKnownLocation
                                                    } catch (_: Exception) {
                                                        null
                                                    }

                                                if (lastLoc != null) {
                                                    val latLng = org.maplibre.android.geometry.LatLng(
                                                        lastLoc.latitude,
                                                        lastLoc.longitude
                                                    )
                                                    m.animateCamera(
                                                        org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                                                            latLng,
                                                            16.5
                                                        )
                                                    )
                                                } else {
                                                    val defaultPos =
                                                        org.maplibre.android.geometry.LatLng(48.137, 11.575)
                                                    m.animateCamera(
                                                        org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                                                            defaultPos,
                                                            5.0
                                                        )
                                                    )
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }
                        })
                        mv
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // FLOATING CONTROLS ON TOP OF MAP
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Spacer(Modifier.height(12.dp))

                    if (!recording) {
                        // Show activity selector only before recording
                        ActivitySelector(
                            current = activity,
                            onChange = { activity = it }
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 🔹 Show "Log" button only when NOT recording
                        if (!recording) {
                            Button(
                                onClick = { navController.navigate("history") },
                                colors = ButtonDefaults.buttonColors(containerColor = ComposeColor(0xFF424242)),
                                shape = RoundedCornerShape(30.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .padding(end = 8.dp)
                            ) {
                                Text("Log", color = ComposeColor.White)
                            }
                        }

                        // 🔹 Start / Stop button
                        Button(
                            onClick = {
                                if (!recording) {
                                    // --- Start logic ---
                                    distanceMeters = 0.0
                                    durationSec = 0L
                                    calories = 0.0
                                    trail.clear()
                                    firstFix = true
                                    lastMovementAt = System.currentTimeMillis()

                                    // Start Foreground GPS Tracking Service
                                    if (!startTrackingIfPermitted()) {
                                        return@Button
                                    }

                                    // Start timer job
                                    timerJob?.cancel()
                                    timerJob = scope.launch {
                                        while (isActive) {
                                            delay(1000L)
                                            durationSec += 1

                                            val met = activity.met
                                            val heightCm = 175.0
                                            val ageYears = 30
                                            val gender = "Male"
                                            calories = calcCalories(
                                                met = met,
                                                weightKg = effectiveWeightKg,
                                                heightCm = heightCm,
                                                age = ageYears,
                                                gender = gender,
                                                durationSec = durationSec
                                            )
                                        }
                                    }
                                    inactivityJob?.cancel()
                                    inactivityJob = scope.launch {
                                        while (isActive) {
                                            delay(5000L)
                                            val lastMove = lastMovementAt
                                            if (lastMove != null &&
                                                System.currentTimeMillis() - lastMove > noMovementTimeoutMs
                                            ) {
                                                stopRecording.value.invoke()
                                                break
                                            }
                                        }
                                    }

                                    recording = true

                                } else {
                                    // --- Stop logic ---
                                    stopTracking(context) // Stop foreground tracking
                                    timerJob?.cancel()
                                    timerJob = null
                                    inactivityJob?.cancel()
                                    inactivityJob = null
                                    recording = false

                                    // Avoid saving empty or invalid sessions
                                    if (distanceMeters < 5 || durationSec < 5) return@Button

                                    val now = System.currentTimeMillis()
                                    val startTime = now - (durationSec * 1000).coerceAtLeast(1000L)
                                    val pace = avgPaceSecPerKm(distanceMeters, durationSec)

                                    val session = SessionStats(
                                        id = now.toString(),
                                        activity = activity.label,
                                        startEpochMs = startTime,
                                        endEpochMs = now,
                                        durationSec = durationSec,
                                        distanceMeters = distanceMeters,
                                        avgPaceSecPerKm = pace,
                                        polyline = trail.toList()
                                    )

                                    SessionStore.saveSession(context, session)

                                    mapView?.getMapAsync { map ->
                                        if (trail.size < 2) {
                                            map.snapshot { bmp ->
                                                if (bmp != null) {
                                                    val path = SnapshotStorage.saveSnapshotForSession(context, session.id, bmp)
                                                    SessionStore.updateSessionSnapshotPath(context, session.id, path)
                                                }
                                                launchSharePreview(context, session, bmp)
                                            }
                                        } else {
                                            val boundsBuilder = org.maplibre.android.geometry.LatLngBounds.Builder()
                                            trail.forEach { point ->
                                                boundsBuilder.include(
                                                    org.maplibre.android.geometry.LatLng(point.latitude(), point.longitude())
                                                )
                                            }
                                            val bounds = boundsBuilder.build()
                                            val update = CameraUpdateFactory.newLatLngBounds(bounds, 100)

                                            map.animateCamera(update, 1500, object : MapLibreMap.CancelableCallback {
                                                override fun onFinish() {
                                                    mapView?.postDelayed({
                                                        map.snapshot { bmp ->
                                                            if (bmp != null) {
                                                                val path = SnapshotStorage.saveSnapshotForSession(context, session.id, bmp)
                                                                SessionStore.updateSessionSnapshotPath(context, session.id, path)
                                                            }
                                                            launchSharePreview(context, session, bmp)
                                                        }
                                                    }, 300)
                                                }

                                                override fun onCancel() {
                                                    map.snapshot { bmp ->
                                                        if (bmp != null) {
                                                            val path = SnapshotStorage.saveSnapshotForSession(context, session.id, bmp)
                                                            SessionStore.updateSessionSnapshotPath(context, session.id, path)
                                                        }
                                                        launchSharePreview(context, session, bmp)
                                                    }
                                                }
                                            })
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (recording) ComposeColor.Red else ComposeColor(0xFF00C853)
                            ),
                            shape = RoundedCornerShape(30.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp)
                                .padding(start = if (recording) 0.dp else 8.dp)
                        ) {
                            Text(
                                if (!recording) "Start" else "Stop & Share",
                                color = ComposeColor.White
                            )
                        }

                    }
                }
            }
        }
    }
}

/* ---------------- Location handling ---------------- */

private var globalLocationCallback: LocationCallback? = null

private fun launchSharePreview(context: Context, session: SessionStats, bmp: Bitmap?) {
    if (bmp != null && !bmp.isRecycled) {
        val cacheFile = File(context.cacheDir, "latest_map_snapshot.png")
        FileOutputStream(cacheFile).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val intent = Intent(context, SharePreviewActivity::class.java).apply {
            putExtra("sessionId", session.id)
            putExtra("snapshotPath", cacheFile.absolutePath)
        }
        context.startActivity(intent)
    } else {
        val intent = Intent(context, SharePreviewActivity::class.java).apply {
            putExtra("sessionId", session.id)
        }
        context.startActivity(intent)
    }
}

//@SuppressLint("MissingPermission")
//private fun startLocationUpdates(
//    context: Context,
//    fused: FusedLocationProviderClient,
//    map: MapLibreMap?,
//    onLocation: (Location) -> Unit,
//    onFirstFix: (LatLng) -> Unit
//) {
//    if (
//        ActivityCompat.checkSelfPermission(
//            context,
//            Manifest.permission.ACCESS_FINE_LOCATION
//        ) != PackageManager.PERMISSION_GRANTED
//    ) return
//
//    val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
//        .setMinUpdateDistanceMeters(2f)
//        .build()
//
//    var first = true
//    globalLocationCallback = object : LocationCallback() {
//        override fun onLocationResult(result: LocationResult) {
//            val loc = result.lastLocation ?: return
//            if (first) {
//                first = false
//                onFirstFix(LatLng(loc.latitude, loc.longitude))
//            }
//            onLocation(loc) // delegate back to composable
//        }
//    }
//
//    fused.requestLocationUpdates(
//        req,
//        globalLocationCallback as LocationCallback,
//        Looper.getMainLooper()
//    )
//}

//private fun stopLocationUpdates(fused: FusedLocationProviderClient) {
//    globalLocationCallback?.let { fused.removeLocationUpdates(it) }
//    globalLocationCallback = null
//}

/* ---------------- Calculations & Helpers ---------------- */

fun formatHMS(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

private fun zoomToTrailOnMapOpen(map: MapLibreMap, trail: List<Point>) {
    if (trail.size < 2) return
    try {
        val boundsBuilder = org.maplibre.android.geometry.LatLngBounds.Builder()
        trail.forEach { point ->
            boundsBuilder.include(
                org.maplibre.android.geometry.LatLng(point.latitude(), point.longitude())
            )
        }
        val bounds = boundsBuilder.build()
        val padding = 100
        val update = org.maplibre.android.camera.CameraUpdateFactory.newLatLngBounds(bounds, padding)
        map.animateCamera(update)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun avgPaceSecPerKm(distanceMeters: Double, durationSec: Long): Long {
    if (distanceMeters <= 0.0) return 0L
    val km = distanceMeters / 1000.0
    return (durationSec / km).roundToInt().toLong()
}

private fun formatPace(distanceMeters: Double, durationSec: Long): String {
    val pace = avgPaceSecPerKm(distanceMeters, durationSec)
    if (pace <= 0L) return "--:--"
    val min = pace / 60
    val sec = pace % 60
    return "%02d:%02d".format(min, sec)
}

fun calcCalories(
    met: Double,
    weightKg: Double,
    heightCm: Double,
    age: Int,
    gender: String,
    durationSec: Long
): Double {
    val hours = durationSec / 3600.0

    // Basal Metabolic Rate (Mifflin-St Jeor)
    val genderConstant = if (gender.equals("Male", ignoreCase = true)) 5 else -161
    val bmr = (10 * weightKg) + (6.25 * heightCm) - (5 * age) + genderConstant

    // Calories burned
    return (bmr / 24.0) * met * hours
}

private fun haversineMeters(a: Point, b: Point): Double {
    val R = 6371000.0
    val lat1 = Math.toRadians(a.latitude())
    val lat2 = Math.toRadians(b.latitude())
    val dLat = lat2 - lat1
    val dLon = Math.toRadians(b.longitude() - a.longitude())
    val sinDLat = Math.sin(dLat / 2)
    val sinDLon = Math.sin(dLon / 2)
    val c = 2 * Math.asin(
        Math.sqrt(
            sinDLat * sinDLat +
                    Math.cos(lat1) * Math.cos(lat2) * sinDLon * sinDLon
        )
    )
    return R * c
}

/* ---------------- Tiny JSON codec (no external libs) ---------------- */

fun decodeList(json: String): List<SessionStats> {
    // extremely small/forgiving parser for our own format
    if (json.isBlank() || json == "[]") return emptyList()
    val blocks = json.trim().removePrefix("[").removeSuffix("]")
        .split(Regex("\\},\\s*\\{"))
        .mapIndexed { i, s ->
            var t = s
            if (i == 0) t = t.removePrefix("{")
            if (i == (s.length - 1)) t = t.removeSuffix("}")
            t
        }

    val out = mutableListOf<SessionStats>()
    for (b in blocks) {
        val obj = ("{$b}").trim()
        fun grab(key: String): String {
            val match = Regex(""""$key"\s*:\s*("?)([^",\r\n}]*)\1""").find(obj)
            return match?.groups?.get(2)?.value ?: ""
        }

        val ptsRaw = Regex(""""pts":\s*\[(.*)]""", RegexOption.DOT_MATCHES_ALL)
            .find(obj)?.groups?.get(1)?.value ?: ""
        val pts = if (ptsRaw.isBlank()) {
            emptyList()
        } else {
            ptsRaw
                .split("},")
                .map { it.trim().trim('{', '}') }
                .mapNotNull {
                    val lng =
                        Regex("lng:([\\-0-9.]+)").find(it)?.groupValues?.get(1)?.toDoubleOrNull()
                    val lat =
                        Regex("lat:([\\-0-9.]+)").find(it)?.groupValues?.get(1)?.toDoubleOrNull()
                    if (lng != null && lat != null) Point.fromLngLat(lng, lat) else null
                }
        }

        out.add(
            SessionStats(
                id = grab("id"),
                activity = grab("activity"),
                startEpochMs = grab("start").toLongOrNull() ?: 0L,
                endEpochMs = grab("end").toLongOrNull() ?: 0L,
                durationSec = grab("dur").toLongOrNull() ?: 0L,
                distanceMeters = grab("dist").toDoubleOrNull() ?: 0.0,
                avgPaceSecPerKm = grab("pace").toLongOrNull() ?: 0L,
                polyline = pts
            )
        )
    }
    return out
}

/* ---------------- Sharing: build an image of map (optional) + trail + stats ---------------- */

fun shareSessionImage(
    context: Context,
    mapView: MapView?,
    session: SessionStats,
    includeMap: Boolean
) {
    clearOldCache(context)

    fun drawTrailCentered(
        canvas: Canvas,
        poly: List<Point>,
        mapRect: RectF
    ) {
        if (poly.size < 2) return

        val pad = 32f
        val availW = mapRect.width() - pad * 2
        val bottomPadding = 80f // leave room above the black stats bar
        val availH = mapRect.height() - pad * 2 - bottomPadding

        val xs = poly.map { it.longitude() }
        val ys = poly.map { it.latitude() }
        val minX = xs.minOrNull() ?: return
        val maxX = xs.maxOrNull() ?: return
        val minY = ys.minOrNull() ?: return
        val maxY = ys.maxOrNull() ?: return

        val spanX = (maxX - minX).coerceAtLeast(1e-9)
        val spanY = (maxY - minY).coerceAtLeast(1e-9)

        val scale = minOf((availW / spanX).toFloat(), (availH / spanY).toFloat())

        // Remaining space after scaling (for centering)
        val extraX = (availW - spanX * scale) / 2f
        val extraY = (availH - spanY * scale) / 2f

        // Shift trail slightly down (~40px) to compensate for perceived offset
        val left = mapRect.left + pad + extraX
        val top = mapRect.top + pad + extraY +40f

        val path = Path()
        poly.forEachIndexed { i, p ->
            val x = (left + ((p.longitude() - minX) * scale)).toFloat()
            val y = (top + ((maxY - p.latitude()) * scale)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeWidth = 12f
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            alpha = 140
        }

        val trail = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF4081")
            strokeWidth = 8f
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

        canvas.drawPath(path, halo)
        canvas.drawPath(path, trail)
    }

    fun finalizeAndShare(bmpBase: Bitmap?) {
        val width = 1080
        val height = 1920
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.BLACK)

        val overlayTop = height - 300f
        val mapRect = RectF(0f, 0f, width.toFloat(), overlayTop - 80f)

        // draw the map image into mapRect
        if (includeMap && bmpBase != null) {
            val scaled = Bitmap.createScaledBitmap(
                bmpBase,
                mapRect.width().toInt(),
                mapRect.height().toInt(),
                true
            )
            canvas.drawBitmap(scaled, mapRect.left, mapRect.top, null)
        } else {
            canvas.drawRect(mapRect, Paint().apply { color = Color.DKGRAY })
        }

        // centered trail in the same mapRect
        drawTrailCentered(canvas, session.polyline, mapRect)

        // bottom overlay
        val overlayPaint = Paint().apply {
            shader = LinearGradient(
                0f, overlayTop, 0f, height.toFloat(),
                Color.argb(180, 0, 0, 0),
                Color.argb(255, 0, 0, 0),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, overlayTop, width.toFloat(), height.toFloat(), overlayPaint)

        // Text paints
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 90f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 48f
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            textSize = 36f
        }

        val centerY = height - 160f

        // Activity type
        canvas.drawText(session.activity.uppercase(), 60f, centerY - 70f, titlePaint)

        // Basic stats
        val spacing = width / 3f
        val distanceText = "%.2f km".format(session.distanceMeters / 1000.0)
        val durationText = formatHMS(session.durationSec)

        // Calories using calcCalories with default values
        val met = when (session.activity.lowercase()) {
            "walking" -> ActivityType.WALKING.met
            "running" -> ActivityType.RUNNING.met
            "cycling" -> ActivityType.CYCLING.met
            else -> ActivityType.RUNNING.met
        }
        val defaultWeightKg = 70.0
        val defaultHeightCm = 175.0
        val defaultAge = 30
        val defaultGender = "Male"
        val caloriesVal = calcCalories(
            met = met,
            weightKg = defaultWeightKg,
            heightCm = defaultHeightCm,
            age = defaultAge,
            gender = defaultGender,
            durationSec = session.durationSec
        ).roundToInt()

        canvas.drawText(distanceText, 60f, centerY, textPaint)
        canvas.drawText("Distance", 60f, centerY + 40f, subPaint)

        canvas.drawText(durationText, spacing, centerY, textPaint)
        canvas.drawText("Duration", spacing, centerY + 40f, subPaint)

        canvas.drawText(
            "$caloriesVal cal",
            spacing * 2,
            centerY,
            textPaint
        )
        canvas.drawText("Calories", spacing * 2, centerY + 40f, subPaint)

        // Save and share
        val cacheFile = File(context.cacheDir, "session_${session.id}.png")
        FileOutputStream(cacheFile).use { out ->
            result.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share workout"))
    }

    if (includeMap && mapView != null) {
        try {
            mapView.getMapAsync { m ->
                m.snapshot { bitmap ->
                    Handler(Looper.getMainLooper()).post {
                        finalizeAndShare(bitmap)
                    }




                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            finalizeAndShare(null)
        }
    } else {
        finalizeAndShare(null)
    }
}

private fun startTracking(context: Context, trackingMode: TrackingMode) {
    val intent = Intent(context, TrackingService::class.java).apply {
        putExtra(TrackingService.EXTRA_TRACKING_MODE, trackingMode.value)
    }
    context.startForegroundService(intent)
}

fun stopTracking(context: Context) {
    val intent = Intent(context, TrackingService::class.java)
    context.stopService(intent)
}

private fun clearOldCache(context: Context, maxAgeMs: Long = 7L * 24 * 3600 * 1000) {
    val now = System.currentTimeMillis()
    context.cacheDir.listFiles()?.forEach {
        if (now - it.lastModified() > maxAgeMs) it.delete()
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    val coarse = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}

/* ---------------- Small UI pieces ---------------- */

@Composable
private fun StatBlock(title: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(min = 0.dp)
            .padding(horizontal = 4.dp)
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(title, style = MaterialTheme.typography.bodySmall, color = ComposeColor.Gray)
    }
}

@Composable
private fun ActivitySelector(current: ActivityType, onChange: (ActivityType) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModeChip(
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            label = "Walk",
            selected = current == ActivityType.WALKING
        ) {
            onChange(ActivityType.WALKING)
        }
        Spacer(Modifier.width(8.dp))
        ModeChip(
            icon = Icons.AutoMirrored.Filled.DirectionsRun,
            label = "Run",
            selected = current == ActivityType.RUNNING
        ) {
            onChange(ActivityType.RUNNING)
        }
        Spacer(Modifier.width(8.dp))
        ModeChip(
            icon = Icons.AutoMirrored.Filled.DirectionsBike,
            label = "Bike",
            selected = current == ActivityType.CYCLING
        ) {
            onChange(ActivityType.CYCLING)
        }
    }
}

@Composable
private fun ModeChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) ComposeColor(0xFF1E88E5) else ComposeColor(0xFF2A2A2A)
    val fg = if (selected) ComposeColor.White else ComposeColor.LightGray
    Row(
        modifier = Modifier
            .background(bg, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = fg)
        Spacer(Modifier.width(6.dp))
        Text(label, color = fg)
    }
}

/* --------- Hooks to your ViewModel for weight prompt (no-op defaults) --------- */
// Implement these in your MainViewModel to read & store user weight from Room.
// For now, these no-op fallbacks keep this file compile-safe.

private fun MainViewModel.currentUserWeightKgOrNull(): Double? = null
private fun MainViewModel.promptUserForWeightOnce(context: Context) {
    /* show your dialog & save to DB */
}
    // ------------ Kalman Filter for GPS ------------
    class KalmanLatLong(private var qMetersPerSecond: Float = 3f) {

        private var lat: Double = 0.0
        private var lng: Double = 0.0
        private var variance = -1.0 // -1 means uninitialized

        fun process(
            newLat: Double,
            newLng: Double,
            accuracy: Float,
            timestampMs: Long
        ): Pair<Double, Double> {

            val accuracySq = accuracy * accuracy

            if (variance < 0) {
                // First measurement
                lat = newLat
                lng = newLng
                variance = accuracySq.toDouble()
            } else {
                val dt = 1.0 // assume 1 sec since your update interval is 1s
                variance += dt * qMetersPerSecond * qMetersPerSecond

                // Kalman gain
                val k = variance / (variance + accuracySq)

                lat += k * (newLat - lat)
                lng += k * (newLng - lng)

                variance *= (1 - k)
            }
            return lat to lng

    }

}
