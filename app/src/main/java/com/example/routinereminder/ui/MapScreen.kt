package com.example.routinereminder.ui
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import com.example.routinereminder.data.model.SessionStats
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import android.app.ActivityManager
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
import com.example.routinereminder.data.model.ActiveRunState

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.example.routinereminder.ui.SessionStore
import com.example.routinereminder.ui.components.SettingsIconButton
import com.example.routinereminder.ui.theme.AppPalette
import com.google.android.gms.location.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import java.util.Calendar
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import android.content.BroadcastReceiver
import android.content.IntentFilter
import com.example.routinereminder.location.TrackingService
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.example.routinereminder.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request


enum class ActivityType(val label: String) {
    RUN_WALK("Run/Walk"),
    CYCLING("Cycling")
    ;

    companion object {
        fun fromLabel(label: String?): ActivityType {
            return when {
                label.equals("walking", ignoreCase = true) -> RUN_WALK
                label.equals("running", ignoreCase = true) -> RUN_WALK
                label.equals("run/walk", ignoreCase = true) -> RUN_WALK
                else -> values().firstOrNull { it.label.equals(label, ignoreCase = true) } ?: RUN_WALK
            }
        }
    }
}

private const val INACTIVITY_TIMEOUT_MS = 60_000L

enum class TrackingMode(val label: String, val value: String) {
    BALANCED("Balanced", TrackingService.MODE_BALANCED),
    HIGH_ACCURACY("High accuracy", TrackingService.MODE_HIGH_ACCURACY)
    ;

    companion object {
        fun fromValue(value: String?): TrackingMode {
            return values().firstOrNull { it.value == value } ?: BALANCED
        }
    }
}

private const val DEFAULT_WEIGHT_KG = 70.0
private const val DEFAULT_HEIGHT_CM = 175.0
private const val DEFAULT_AGE_YEARS = 30
private const val DEFAULT_GENDER = "Male"
private const val CALORIE_LOG_TAG = "CalorieProfile"
private const val WEATHER_REFRESH_MS = 10 * 60_000L
private const val WEATHER_MIN_DISTANCE_METERS = 250.0

private data class WeatherSnapshot(
    val temperatureC: Double,
    val windSpeedKmh: Double,
    val fetchedAtMs: Long
)

@Serializable
private data class OpenMeteoResponse(
    val current: OpenMeteoCurrent? = null
)

@Serializable
private data class OpenMeteoCurrent(
    @SerialName("temperature_2m") val temperatureC: Double? = null,
    @SerialName("wind_speed_10m") val windSpeedKmh: Double? = null
)

private val weatherJson = Json { ignoreUnknownKeys = true }
private val weatherClient = OkHttpClient()

private data class CalorieProfile(
    val weightKg: Double,
    val heightCm: Double,
    val ageYears: Int,
    val gender: String
)

private fun resolveCalorieProfile(
    weightKg: Double?,
    heightCm: Double?,
    ageYears: Int?,
    gender: String?,
    source: String
): CalorieProfile {
    val resolvedWeight = weightKg ?: run {
        Log.w(CALORIE_LOG_TAG, "$source: missing weight; using default $DEFAULT_WEIGHT_KG kg.")
        DEFAULT_WEIGHT_KG
    }
    val resolvedHeight = heightCm ?: run {
        Log.w(CALORIE_LOG_TAG, "$source: missing height; using default $DEFAULT_HEIGHT_CM cm.")
        DEFAULT_HEIGHT_CM
    }
    val resolvedAge = ageYears ?: run {
        Log.w(CALORIE_LOG_TAG, "$source: missing age; using default $DEFAULT_AGE_YEARS.")
        DEFAULT_AGE_YEARS
    }
    val resolvedGender = gender ?: run {
        Log.w(CALORIE_LOG_TAG, "$source: missing gender; using default $DEFAULT_GENDER.")
        DEFAULT_GENDER
    }
    return CalorieProfile(
        weightKg = resolvedWeight,
        heightCm = resolvedHeight,
        ageYears = resolvedAge,
        gender = resolvedGender
    )
}

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel(),
    calorieTrackerViewModel: CalorieTrackerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }
    // ------------------------------------------------------
    // RECEIVE GPS UPDATES FROM TrackingService (background)
    // ------------------------------------------------------


    // UI state
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var firstFix by remember { mutableStateOf(true) }
    var userZoom by remember { mutableStateOf(15.5) }
    var includeMapInShare by remember { mutableStateOf(true) }
    var selectedActivity by remember { mutableStateOf(ActivityType.RUN_WALK) }
    var permissionRequired by remember { mutableStateOf(false) }
    var showManualEntry by rememberSaveable { mutableStateOf(false) }
    var manualDistanceKm by rememberSaveable { mutableStateOf("") }
    var manualDurationMin by rememberSaveable { mutableStateOf("") }
    // live stats
    val runState by viewModel.activeRunState.collectAsState()
    val trailPoints by viewModel.trailPoints.collectAsState()
    val splitDurations by viewModel.splitDurations.collectAsState()
    val mapTrackingMode by viewModel.mapTrackingMode.collectAsState()
    val isRecording = runState?.isRecording == true
    val activity = runState?.activity?.let { ActivityType.fromLabel(it) } ?: selectedActivity
    val trackingMode = TrackingMode.fromValue(runState?.trackingMode ?: mapTrackingMode)
    val distanceMeters = runState?.distanceMeters ?: 0.0
    val durationSec = runState?.durationSec ?: 0L
    val calories = runState?.calories ?: 0.0

    // timer
    val scope = rememberCoroutineScope()
    var timerJob by remember { mutableStateOf<Job?>(null) }
    var inactivityJob by remember { mutableStateOf<Job?>(null) }
    var lastMovementAt by rememberSaveable { mutableStateOf<Long?>(null) }
    var splitStartDistance by rememberSaveable { mutableStateOf(0.0) }
    var splitStartDuration by rememberSaveable { mutableStateOf(0L) }
    val stopRecording = rememberUpdatedState {
        if (isRecording) {
            stopTracking(context)
            timerJob?.cancel()
            timerJob = null
            inactivityJob?.cancel()
            inactivityJob = null
            viewModel.stopRun()
        }
    }

    fun startRunTimers() {
        timerJob?.cancel()
        inactivityJob?.cancel()
        inactivityJob = null
        val calorieProfile = resolveCalorieProfile(
            weightKg = viewModel.currentUserWeightKgOrNull(),
            heightCm = viewModel.currentUserHeightCmOrNull(),
            ageYears = viewModel.currentUserAgeOrNull(),
            gender = viewModel.currentUserGenderOrNull(),
            source = "MapScreen"
        )
        timerJob = scope.launch {
            while (isActive) {
                delay(1000)
                val state = runState ?: continue
                if (!state.isRecording) continue
                val nextDuration = state.durationSec + 1
                val now = System.currentTimeMillis()
                val inactiveMs = lastMovementAt?.let { now - it } ?: 0L
                val isInactive = state.distanceMeters <= 0.0 || inactiveMs >= INACTIVITY_TIMEOUT_MS
                val nextCalories = calcActivityCalories(
                    activity = activity,
                    weightKg = calorieProfile.weightKg,
                    heightCm = calorieProfile.heightCm,
                    age = calorieProfile.ageYears,
                    gender = calorieProfile.gender,
                    distanceMeters = state.distanceMeters,
                    durationSec = nextDuration,
                    isInactive = isInactive
                )
                viewModel.updateRunStats(state.distanceMeters, nextDuration, nextCalories)
            }
        }
    }

    // weight (read from MainViewModel if you already expose it; otherwise prompt will be handled there)
    val weightKg by remember { mutableStateOf(viewModel.currentUserWeightKgOrNull()) }
    var effectiveWeightKg by remember { mutableStateOf(weightKg ?: DEFAULT_WEIGHT_KG) }


    //Kalman filter
    val kalman = remember { KalmanLatLong() }
    var smoothedLat by remember { mutableStateOf<Double?>(null) }
    var smoothedLng by remember { mutableStateOf<Double?>(null) }
    var weatherSnapshot by remember { mutableStateOf<WeatherSnapshot?>(null) }
    var weatherLoading by remember { mutableStateOf(false) }
    var lastWeatherPoint by remember { mutableStateOf<Point?>(null) }
    var lastWeatherFetchAt by remember { mutableStateOf(0L) }
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

    LaunchedEffect(runState?.sessionId, splitDurations.size) {
        splitStartDistance = splitDurations.size * 1000.0
        splitStartDuration = splitDurations.sum()
    }

    LaunchedEffect(runState?.sessionId, runState?.isRecording) {
        val state = runState ?: return@LaunchedEffect
        if (state.isRecording) {
            if (!startTrackingIfPermitted()) {
                stopRecording.value.invoke()
                return@LaunchedEffect
            }
            if (timerJob == null) {
                startRunTimers()
            }
        }
    }

    fun shouldFetchWeather(nextPoint: Point): Boolean {
        val lastPoint = lastWeatherPoint ?: return true
        val timeDelta = System.currentTimeMillis() - lastWeatherFetchAt
        val distanceDelta = haversineMeters(lastPoint, nextPoint)
        return timeDelta >= WEATHER_REFRESH_MS || distanceDelta >= WEATHER_MIN_DISTANCE_METERS
    }

    fun requestWeather(lat: Double, lng: Double) {
        val point = Point.fromLngLat(lng, lat)
        if (weatherLoading || !shouldFetchWeather(point)) return
        weatherLoading = true
        lastWeatherPoint = point
        lastWeatherFetchAt = System.currentTimeMillis()
        scope.launch {
            val snapshot = fetchWeatherSnapshot(lat, lng)
            if (snapshot != null) {
                weatherSnapshot = snapshot
            }
            weatherLoading = false
        }
    }

    // location callback implementation
    fun onLocation(location: Location) {

        // -----------------------------
        // 1) Accuracy filter
        // -----------------------------
        val maxAccuracyMeters = 100f
        if (location.accuracy > maxAccuracyMeters) {
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
        val last = trailPoints.lastOrNull()
        var allowTrailUpdate = true
        if (last != null) {
            val delta = haversineMeters(last, newPoint)

            val minMovementMeters = 0.5 + (location.accuracy.coerceAtMost(maxAccuracyMeters) * 0.01)

            // Ignore tiny drift below the movement threshold (still update marker)
            if (delta < minMovementMeters) allowTrailUpdate = false

            // Ignore giant GPS spikes
            if (delta > 120.0) allowTrailUpdate = false
        }

        // Continue with original code here
        val latLng = LatLng(lpLat, lpLng)
        val m = map ?: return
        val s = m.style ?: return
        requestWeather(lpLat, lpLng)


        // update marker
        val locationSource = s.getSourceAs("location-source") as? GeoJsonSource
        locationSource?.setGeoJson(
            Feature.fromGeometry(
                Point.fromLngLat(latLng.longitude, latLng.latitude)
            )
        )

        // trail update
        if (allowTrailUpdate && (last == null || last != newPoint)) {
            val newDistance = if (last != null) {
                distanceMeters + haversineMeters(last, newPoint)
            } else {
                distanceMeters
            }

            val updatedTrail = trailPoints + newPoint
            val splitUpdate = computeSplitUpdate(
                existingSplits = splitDurations,
                splitStartDistance = splitStartDistance,
                splitStartDuration = splitStartDuration,
                newDistance = newDistance,
                durationSec = durationSec
            )
            if (splitUpdate != null) {
                splitStartDistance = splitUpdate.splitStartDistance
                splitStartDuration = splitUpdate.splitStartDuration
                viewModel.setSplitDurations(splitUpdate.splits)
            }
            viewModel.updateRunStats(newDistance, durationSec, calories)
            viewModel.setTrailPoints(updatedTrail)
            lastMovementAt = System.currentTimeMillis()

            val line = LineString.fromLngLats(updatedTrail)
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

    DisposableEffect(lifecycleOwner, isRecording, trackingMode) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START && isRecording) {
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

    LaunchedEffect(mapTrackingMode, isRecording, runState?.trackingMode) {
        val state = runState ?: return@LaunchedEffect
        if (!isRecording) return@LaunchedEffect
        if (state.trackingMode != mapTrackingMode) {
            viewModel.updateTrackingMode(mapTrackingMode)
            if (!startTrackingIfPermitted()) {
                stopRecording.value.invoke()
            }
        }
    }

    LaunchedEffect(
        runState?.sessionId,
        runState?.distanceMeters,
        runState?.durationSec,
        runState?.calories,
        runState?.isRecording
    ) {
        val state = runState ?: return@LaunchedEffect
        if (!state.isRecording) return@LaunchedEffect
        updateTrackingNotification(context, state)
    }

    LaunchedEffect(map, trailPoints) {
        val m = map ?: return@LaunchedEffect
        val s = m.style ?: return@LaunchedEffect
        val source = s.getSourceAs("trail-source") as? GeoJsonSource ?: return@LaunchedEffect
        val line = LineString.fromLngLats(trailPoints)
        source.setGeoJson(Feature.fromGeometry(line))
    }

    if (showManualEntry) {
        val distanceKm = manualDistanceKm.toDoubleOrNull()
        val durationMin = manualDurationMin.toDoubleOrNull()
        val canSave = distanceKm != null && distanceKm > 0.0 && durationMin != null && durationMin > 0.0
        val calorieProfile = resolveCalorieProfile(
            weightKg = viewModel.currentUserWeightKgOrNull(),
            heightCm = viewModel.currentUserHeightCmOrNull(),
            ageYears = viewModel.currentUserAgeOrNull(),
            gender = viewModel.currentUserGenderOrNull(),
            source = "MapScreenManualEntryPreview"
        )
        val estimatedCalories = if (distanceKm != null && durationMin != null && durationMin > 0.0) {
            val durationSecManual = (durationMin * 60).roundToLong()
            calcActivityCalories(
                activity = selectedActivity,
                weightKg = calorieProfile.weightKg,
                heightCm = calorieProfile.heightCm,
                age = calorieProfile.ageYears,
                gender = calorieProfile.gender,
                distanceMeters = distanceKm * 1000.0,
                durationSec = durationSecManual
            ).roundToInt()
        } else {
            null
        }

        AlertDialog(
            onDismissRequest = { showManualEntry = false },
            title = { Text(text = "Add manual run") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter your estimated distance and duration.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.TextMuted
                    )
                    Text(
                        text = "Activity",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    ActivitySelector(
                        current = selectedActivity,
                        onChange = { selectedActivity = it }
                    )
                    OutlinedTextField(
                        value = manualDistanceKm,
                        onValueChange = { manualDistanceKm = it },
                        label = { Text("Distance (km)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = manualDurationMin,
                        onValueChange = { manualDurationMin = it },
                        label = { Text("Duration (minutes)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text(
                        text = "Estimated calories: ${estimatedCalories?.toString() ?: "--"} kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.TextMuted
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = canSave,
                    onClick = {
                        val distanceMeters = (distanceKm ?: 0.0) * 1000.0
                        val durationSecManual = ((durationMin ?: 0.0) * 60).roundToLong()
                        val now = System.currentTimeMillis()
                        val startTime = now - (durationSecManual * 1000)
                        val pace = avgPaceSecPerKm(distanceMeters, durationSecManual)
                        val splits = estimateSplits(distanceMeters, durationSecManual)

                        val calorieProfile = resolveCalorieProfile(
                            weightKg = viewModel.currentUserWeightKgOrNull(),
                            heightCm = viewModel.currentUserHeightCmOrNull(),
                            ageYears = viewModel.currentUserAgeOrNull(),
                            gender = viewModel.currentUserGenderOrNull(),
                            source = "MapScreenManualEntry"
                        )
                        val caloriesBurned = calcActivityCalories(
                            activity = selectedActivity,
                            weightKg = calorieProfile.weightKg,
                            heightCm = calorieProfile.heightCm,
                            age = calorieProfile.ageYears,
                            gender = calorieProfile.gender,
                            distanceMeters = distanceMeters,
                            durationSec = durationSecManual
                        )
                        val session = SessionStats(
                            id = now.toString(),
                            activity = selectedActivity.label,
                            startEpochMs = startTime,
                            endEpochMs = now,
                            durationSec = durationSecManual,
                            distanceMeters = distanceMeters,
                            calories = caloriesBurned,
                            avgPaceSecPerKm = pace,
                            splitPaceSecPerKm = splits,
                            polyline = emptyList()
                        )
                        SessionStore.saveSession(context, session)
                        manualDistanceKm = ""
                        manualDurationMin = ""
                        showManualEntry = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualEntry = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsIconButton(onClick = { navController.navigate("settings/map") })
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppPalette.SurfaceStrong)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatBlock(title = "Duration", value = formatHMS(durationSec))
                        StatBlock(title = "Distance (km)", value = "%.2f".format(distanceMeters / 1000.0))
                        StatBlock(title = "Avg. Pace", value = formatPace(distanceMeters, durationSec))
                        StatBlock(title = "Calories", value = calories.roundToInt().toString())
                    }
                }
            }
            if (weatherSnapshot != null || weatherLoading) {
                val temperatureText = weatherSnapshot?.let { "%.1f°".format(it.temperatureC) } ?: "--"
                val windText = weatherSnapshot?.let { "%.0f km/h".format(it.windSpeedKmh) } ?: "--"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AppPalette.SurfaceElevated)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.map_weather_title),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppPalette.TextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatBlock(
                                title = stringResource(R.string.map_weather_temperature),
                                value = temperatureText
                            )
                            StatBlock(
                                title = stringResource(R.string.map_weather_wind),
                                value = windText
                            )
                        }
                    }
                }
            }
            if (splitDurations.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AppPalette.SurfaceElevated)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Splits (per km)",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppPalette.TextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            splitDurations.forEachIndexed { index, split ->
                                Text(
                                    text = "${index + 1}km ${formatSplitPace(split)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppPalette.TextInverse
                                )
                            }
                        }
                    }
                }
            }

            if (permissionRequired) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AppPalette.SurfaceDanger)
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
                            color = AppPalette.DangerSoft,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = { requestLocationPermissions() },
                            colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Danger)
                        ) {
                            Text("Grant")
                        }
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
                                            org.maplibre.android.style.layers.PropertyFactory.circleColor(
                                                String.format("#%08X", AppPalette.MapLocation.toArgb())
                                            ),
                                            org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth(2f),
                                            org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor(
                                                String.format("#%08X", AppPalette.MapStroke.toArgb())
                                            )
                                        )
                                    )

                                    val trailSrc = GeoJsonSource("trail-source", GeoJsonOptions())
                                    style.addSource(trailSrc)
                                    style.addLayer(
                                        LineLayer("trail-layer", "trail-source").withProperties(
                                            org.maplibre.android.style.layers.PropertyFactory.lineColor(
                                                String.format("#%08X", AppPalette.MapTrail.toArgb())
                                            ),
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

                                    if (trailPoints.isNotEmpty()) {
                                        // Zoom to full trail if points exist
                                        zoomToTrailOnMapOpen(m, trailPoints)
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

                    if (!isRecording) {
                        // Show activity selector only before recording
                        ActivitySelector(
                            current = selectedActivity,
                            onChange = { selectedActivity = it }
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
                        if (!isRecording) {
                            Button(
                                onClick = { navController.navigate("history") },
                                colors = ButtonDefaults.buttonColors(containerColor = AppPalette.BorderStrong),
                                shape = RoundedCornerShape(30.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .padding(end = 8.dp)
                            ) {
                                Text("Log", color = AppPalette.TextInverse)
                            }
                            Button(
                                onClick = { showManualEntry = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(30.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .padding(end = 8.dp)
                            ) {
                                Text("Add", color = AppPalette.TextInverse)
                            }
                        }

                        // 🔹 Start / Stop button
                        Button(
                            onClick = {
                                if (!isRecording) {
                                    // --- Start logic ---
                                    lastMovementAt = System.currentTimeMillis()
                                    viewModel.startRun(selectedActivity.label, trackingMode.value)

                                    // Start Foreground GPS Tracking Service
                                    if (!startTrackingIfPermitted()) {
                                        return@Button
                                    }

                                    startRunTimers()

                                } else {
                                    // --- Stop logic ---
                                    stopTracking(context) // Stop foreground tracking
                                    timerJob?.cancel()
                                    timerJob = null
                                    inactivityJob?.cancel()
                                    inactivityJob = null
                                    viewModel.stopRun()

                                    // Avoid saving empty or invalid sessions
                                    if (distanceMeters < 5 || durationSec < 5) {
                                        viewModel.discardRun()
                                        return@Button
                                    }

                                    val now = System.currentTimeMillis()
                                    val startTime = runState?.startEpochMs ?: now
                                    val pace = avgPaceSecPerKm(distanceMeters, durationSec)

                                    val session = SessionStats(
                                        id = runState?.sessionId ?: now.toString(),
                                        activity = activity.label,
                                        startEpochMs = startTime,
                                        endEpochMs = now,
                                        durationSec = durationSec,
                                        distanceMeters = distanceMeters,
                                        calories = calories,
                                        avgPaceSecPerKm = pace,
                                        splitPaceSecPerKm = splitDurations,
                                        polyline = trailPoints
                                    )

                                    SessionStore.saveSession(context, session)

                                    mapView?.getMapAsync { map ->
                                        if (trailPoints.size < 2) {
                                            map.snapshot { bmp ->
                                                if (bmp != null) {
                                                    val path = SnapshotStorage.saveSnapshotForSession(context, session.id, bmp)
                                                    SessionStore.updateSessionSnapshotPath(context, session.id, path)
                                                }
                                                launchSharePreview(context, session, bmp)
                                            }
                                        } else {
                                            val boundsBuilder = org.maplibre.android.geometry.LatLngBounds.Builder()
                                            trailPoints.forEach { point ->
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
                                    viewModel.discardRun()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) AppPalette.Danger else AppPalette.Success
                            ),
                            shape = RoundedCornerShape(30.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp)
                                .padding(start = if (isRecording) 0.dp else 0.dp)
                        ) {
                            Text(
                                if (!isRecording) "Start" else "Stop & Share",
                                color = AppPalette.TextInverse
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

private fun isTrackingServiceRunning(context: Context): Boolean {
    val manager = context.getSystemService(ActivityManager::class.java) ?: return false
    return manager.getRunningServices(Int.MAX_VALUE).any { service ->
        service.service.className == TrackingService::class.java.name
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

private fun estimateSplits(distanceMeters: Double, durationSec: Long): List<Long> {
    if (distanceMeters <= 0.0 || durationSec <= 0L) return emptyList()
    val pace = avgPaceSecPerKm(distanceMeters, durationSec)
    val count = floor(distanceMeters / 1000.0).toInt()
    if (count <= 0) return emptyList()
    return List(count) { pace }
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

private fun formatSplitPace(paceSecPerKm: Long): String {
    if (paceSecPerKm <= 0L) return "--:--"
    val min = paceSecPerKm / 60
    val sec = paceSecPerKm % 60
    return "%d:%02d".format(min, sec)
}

private data class SplitUpdate(
    val splits: List<Long>,
    val splitStartDistance: Double,
    val splitStartDuration: Long
)

private fun computeSplitUpdate(
    existingSplits: List<Long>,
    splitStartDistance: Double,
    splitStartDuration: Long,
    newDistance: Double,
    durationSec: Long
): SplitUpdate? {
    val distanceDelta = newDistance - splitStartDistance
    if (distanceDelta < 1000.0) return null

    val durationDelta = (durationSec - splitStartDuration).coerceAtLeast(0)
    if (durationDelta <= 0L) return null

    val splits = existingSplits.toMutableList()
    var remainingDistance = distanceDelta
    var remainingDuration = durationDelta.toDouble()
    val secondsPerMeter = if (remainingDistance > 0.0) remainingDuration / remainingDistance else 0.0
    var nextSplitStartDistance = splitStartDistance
    var nextSplitStartDuration = splitStartDuration

    while (remainingDistance >= 1000.0) {
        val splitDuration = (1000.0 * secondsPerMeter).roundToLong().coerceAtLeast(1L)
        splits.add(splitDuration)
        remainingDistance -= 1000.0
        remainingDuration -= splitDuration
        nextSplitStartDistance += 1000.0
        nextSplitStartDuration += splitDuration
    }

    return SplitUpdate(
        splits = splits,
        splitStartDistance = nextSplitStartDistance,
        splitStartDuration = nextSplitStartDuration
    )
}

private fun calcRunWalkCalories(
    weightKg: Double,
    heightCm: Double,
    age: Int,
    gender: String,
    distanceMeters: Double,
    durationSec: Long
): Double {
    if (distanceMeters <= 0.0 || durationSec <= 0L) return 0.0
    val distanceKm = distanceMeters / 1000.0
    val hours = durationSec / 3600.0
    val speedKmh = if (hours > 0.0) distanceKm / hours else 0.0
    val paceMinPerKm = if (distanceKm > 0.0) (durationSec / 60.0) / distanceKm else Double.POSITIVE_INFINITY

    val caloriesPerKm = when {
        paceMinPerKm > 10.0 || speedKmh < 6.0 -> 0.5
        paceMinPerKm >= 7.5 || speedKmh <= 8.0 -> 0.7
        else -> 1.0
    }

    return caloriesPerKm * weightKg * distanceKm
}

private fun calcCyclingCalories(
    weightKg: Double,
    distanceMeters: Double,
    durationSec: Long
): Double {
    if (distanceMeters <= 0.0 || durationSec <= 0L) return 0.0
    val distanceKm = distanceMeters / 1000.0
    val hours = durationSec / 3600.0
    val speedKmh = if (hours > 0.0) distanceKm / hours else 0.0

    val caloriesPerKm = when {
        speedKmh < 15.0 -> 0.25
        speedKmh <= 20.0 -> 0.35
        else -> 0.5
    }

    return caloriesPerKm * weightKg * distanceKm
}

fun calcActivityCalories(
    activity: ActivityType,
    weightKg: Double,
    heightCm: Double,
    age: Int,
    gender: String,
    distanceMeters: Double,
    durationSec: Long,
    isInactive: Boolean = false
): Double {
    if (durationSec <= 0L) return 0.0
    if (isInactive || distanceMeters <= 0.0) return 0.0

    return when (activity) {
        ActivityType.RUN_WALK -> calcRunWalkCalories(
            weightKg = weightKg,
            heightCm = heightCm,
            age = age,
            gender = gender,
            distanceMeters = distanceMeters,
            durationSec = durationSec
        )
        ActivityType.CYCLING -> calcCyclingCalories(
            weightKg = weightKg,
            distanceMeters = distanceMeters,
            durationSec = durationSec
        )
    }
}

private fun isSameDay(firstEpochMs: Long, secondEpochMs: Long): Boolean {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = firstEpochMs
    val year = calendar.get(Calendar.YEAR)
    val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
    calendar.timeInMillis = secondEpochMs
    return year == calendar.get(Calendar.YEAR) && dayOfYear == calendar.get(Calendar.DAY_OF_YEAR)
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

private suspend fun fetchWeatherSnapshot(lat: Double, lng: Double): WeatherSnapshot? {
    val url = HttpUrl.Builder()
        .scheme("https")
        .host("api.open-meteo.com")
        .addPathSegments("v1/forecast")
        .addQueryParameter("latitude", lat.toString())
        .addQueryParameter("longitude", lng.toString())
        .addQueryParameter("current", "temperature_2m,wind_speed_10m")
        .addQueryParameter("wind_speed_unit", "kmh")
        .addQueryParameter("temperature_unit", "celsius")
        .build()
    val request = Request.Builder().url(url).build()
    return withContext(Dispatchers.IO) {
        val response = weatherClient.newCall(request).execute()
        response.use {
            if (!it.isSuccessful) {
                return@withContext null
            }
            val body = it.body?.string() ?: return@withContext null
            val decoded = weatherJson.decodeFromString<OpenMeteoResponse>(body)
            val current = decoded.current ?: return@withContext null
            val temp = current.temperatureC ?: return@withContext null
            val wind = current.windSpeedKmh ?: return@withContext null
            WeatherSnapshot(
                temperatureC = temp,
                windSpeedKmh = wind,
                fetchedAtMs = System.currentTimeMillis()
            )
        }
    }
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

        val splitsRaw = Regex(""""splits":\s*\[(.*?)]""", RegexOption.DOT_MATCHES_ALL)
            .find(obj)?.groups?.get(1)?.value ?: ""
        val splits = if (splitsRaw.isBlank()) {
            emptyList()
        } else {
            splitsRaw.split(",")
                .mapNotNull { it.trim().toLongOrNull() }
        }

        out.add(
            SessionStats(
                id = grab("id"),
                activity = grab("activity"),
                startEpochMs = grab("start").toLongOrNull() ?: 0L,
                endEpochMs = grab("end").toLongOrNull() ?: 0L,
                durationSec = grab("dur").toLongOrNull() ?: 0L,
                distanceMeters = grab("dist").toDoubleOrNull() ?: 0.0,
                calories = grab("calories").toDoubleOrNull() ?: 0.0,
                avgPaceSecPerKm = grab("pace").toLongOrNull() ?: 0L,
                splitPaceSecPerKm = splits,
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
    includeMap: Boolean,
    userWeightKg: Double?,
    userHeightCm: Double?,
    userAgeYears: Int?,
    userGender: String?
) {
    clearOldCache(context)
    val calorieProfile = resolveCalorieProfile(
        weightKg = userWeightKg,
        heightCm = userHeightCm,
        ageYears = userAgeYears,
        gender = userGender,
        source = "shareSessionImage"
    )

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
            color = AppPalette.MapStroke.toArgb()
            strokeWidth = 12f
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            alpha = 140
        }

        val trail = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AppPalette.MapTrail.toArgb()
            strokeWidth = 8f
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

        canvas.drawPath(path, halo)
        canvas.drawPath(path, trail)
    }

    fun overlayColor(baseColor: Int, alpha: Int): Int {
        return Color.argb(
            alpha,
            Color.red(baseColor),
            Color.green(baseColor),
            Color.blue(baseColor)
        )
    }

    fun finalizeAndShare(bmpBase: Bitmap?) {
        val width = 1080
        val height = 1920
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(AppPalette.MapBackground.toArgb())

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
            canvas.drawRect(mapRect, Paint().apply { color = AppPalette.SurfaceTrack.toArgb() })
        }

        // centered trail in the same mapRect
        drawTrailCentered(canvas, session.polyline, mapRect)

        // bottom overlay
        val baseOverlayColor = AppPalette.MapBackground.toArgb()
        val overlayPaint = Paint().apply {
            shader = LinearGradient(
                0f, overlayTop, 0f, height.toFloat(),
                overlayColor(baseOverlayColor, 180),
                overlayColor(baseOverlayColor, 255),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, overlayTop, width.toFloat(), height.toFloat(), overlayPaint)

        // Text paints
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AppPalette.TextInverse.toArgb()
            textSize = 90f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AppPalette.TextInverse.toArgb()
            textSize = 48f
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AppPalette.TextSubtle.toArgb()
            textSize = 36f
        }

        val centerY = height - 160f

        // Activity type
        canvas.drawText(session.activity.uppercase(), 60f, centerY - 70f, titlePaint)

        // Basic stats
        val spacing = width / 3f
        val distanceText = "%.2f km".format(session.distanceMeters / 1000.0)
        val durationText = formatHMS(session.durationSec)

        // Calories using activity-specific logic
        val caloriesVal = calcActivityCalories(
            activity = ActivityType.fromLabel(session.activity),
            weightKg = calorieProfile.weightKg,
            heightCm = calorieProfile.heightCm,
            age = calorieProfile.ageYears,
            gender = calorieProfile.gender,
            distanceMeters = session.distanceMeters,
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

private fun updateTrackingNotification(context: Context, state: ActiveRunState) {
    val intent = Intent(context, TrackingService::class.java).apply {
        action = TrackingService.ACTION_UPDATE_NOTIFICATION
        putExtra(TrackingService.EXTRA_DISTANCE_METERS, state.distanceMeters)
        putExtra(TrackingService.EXTRA_DURATION_SEC, state.durationSec)
        putExtra(TrackingService.EXTRA_CALORIES, state.calories)
        putExtra(TrackingService.EXTRA_ACTIVITY_LABEL, state.activity)
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
            .widthIn(min = 88.dp)
            .padding(horizontal = 4.dp)
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AppPalette.TextLight,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            color = AppPalette.TextMuted,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActivitySelector(current: ActivityType, onChange: (ActivityType) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModeChip(
            icon = Icons.AutoMirrored.Filled.DirectionsRun,
            label = "Run/Walk",
            selected = current == ActivityType.RUN_WALK
        ) {
            onChange(ActivityType.RUN_WALK)
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
    val bg = if (selected) MaterialTheme.colorScheme.primary else AppPalette.SurfaceAlt
    val fg = if (selected) AppPalette.TextInverse else AppPalette.TextSecondary
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
