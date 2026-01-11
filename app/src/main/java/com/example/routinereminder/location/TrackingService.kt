package com.example.routinereminder.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.routinereminder.R
import com.example.routinereminder.MainActivity
import com.google.android.gms.location.*
import kotlin.math.roundToInt

class TrackingService : Service() {

    private lateinit var fused: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var currentMode: String = MODE_BALANCED
    private var isIdle = false
    private var lastMovementTimeMs = 0L
    private var lastLocation: Location? = null
    private var lastRequestSignature: String? = null
    private var lastDistanceMeters = 0.0
    private var lastDurationSec = 0L
    private var lastCalories = 0.0
    private var lastActivityLabel = "Run"

    override fun onCreate() {
        super.onCreate()

        fused = LocationServices.getFusedLocationProviderClient(this)
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_UPDATE_NOTIFICATION) {
            updateNotificationFromIntent(intent)
            return START_STICKY
        }
        if (!hasLocationPermission()) {
            handleMissingPermissions()
            return START_NOT_STICKY
        }
        val requestedMode = intent?.getStringExtra(EXTRA_TRACKING_MODE) ?: currentMode
        if (requestedMode != currentMode) {
            currentMode = requestedMode
        }

        if (lastMovementTimeMs == 0L) {
            lastMovementTimeMs = System.currentTimeMillis()
        }

        updateLocationRequest(forceRestart = true)
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "tracking_channel"
        ensureNotificationChannel(channelId)
        startForeground(NOTIFICATION_ID, buildNotification(channelId))
    }

    private fun updateNotificationFromIntent(intent: Intent) {
        lastDistanceMeters = intent.getDoubleExtra(EXTRA_DISTANCE_METERS, lastDistanceMeters)
        lastDurationSec = intent.getLongExtra(EXTRA_DURATION_SEC, lastDurationSec)
        lastCalories = intent.getDoubleExtra(EXTRA_CALORIES, lastCalories)
        lastActivityLabel = intent.getStringExtra(EXTRA_ACTIVITY_LABEL) ?: lastActivityLabel
        val channelId = "tracking_channel"
        ensureNotificationChannel(channelId)
        startForeground(NOTIFICATION_ID, buildNotification(channelId))
    }

    private fun ensureNotificationChannel(channelId: String) {
        val chan = NotificationChannel(
            channelId,
            "Running Tracker",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(chan)
    }

    private fun buildNotification(channelId: String): Notification {
        val distanceKm = lastDistanceMeters / 1000.0
        val distanceText = if (distanceKm > 0) "%.2f km".format(distanceKm) else "0.00 km"
        val durationText = formatDuration(lastDurationSec)
        val paceText = formatPace(lastDistanceMeters, lastDurationSec)
        val caloriesText = lastCalories.roundToInt().toString()
        val contentText = "Time $durationText • $distanceText • $paceText • $caloriesText cal"
        val bigText = "Time $durationText\nDistance $distanceText\nPace $paceText\nCalories $caloriesText cal"

        val openMapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_MAP_TAB, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openMapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("$lastActivityLabel active")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun formatDuration(durationSec: Long): String {
        val hours = durationSec / 3600
        val minutes = (durationSec % 3600) / 60
        val seconds = durationSec % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    private fun formatPace(distanceMeters: Double, durationSec: Long): String {
        if (distanceMeters <= 0.0 || durationSec <= 0L) return "-- /km"
        val paceSecPerKm = (durationSec / (distanceMeters / 1000.0)).roundToInt()
        val minutes = paceSecPerKm / 60
        val seconds = paceSecPerKm % 60
        return "%d:%02d /km".format(minutes, seconds)
    }

    private fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            handleMissingPermissions()
            return
        }
        fused.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }

    private fun updateLocationRequest(forceRestart: Boolean = false) {
        val signature = "$currentMode-$isIdle"
        if (!forceRestart && signature == lastRequestSignature) return

        locationRequest = buildLocationRequest(currentMode, isIdle)
        lastRequestSignature = signature
        fused.removeLocationUpdates(locationCallback)
        startLocationUpdates()
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarse = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun handleMissingPermissions() {
        sendBroadcast(Intent(ACTION_PERMISSION_REQUIRED))
        stopForeground(true)
        stopSelf()
    }

    private fun buildLocationRequest(mode: String, idle: Boolean): LocationRequest {
        return if (mode == MODE_HIGH_ACCURACY && !idle) {
            LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                HIGH_ACCURACY_INTERVAL_MS
            )
                .setMinUpdateIntervalMillis(HIGH_ACCURACY_INTERVAL_MS)
                .setMinUpdateDistanceMeters(HIGH_ACCURACY_MIN_DISTANCE_M)
                .build()
        } else if (idle) {
            LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                IDLE_INTERVAL_MS
            )
                .setMinUpdateIntervalMillis(IDLE_INTERVAL_MS)
                .setMinUpdateDistanceMeters(IDLE_MIN_DISTANCE_M)
                .build()
        } else {
            LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                BALANCED_INTERVAL_MS
            )
                .setMinUpdateIntervalMillis(BALANCED_INTERVAL_MS)
                .setMinUpdateDistanceMeters(BALANCED_MIN_DISTANCE_M)
                .build()
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            for (loc: Location in result.locations) {
                val now = System.currentTimeMillis()
                val last = lastLocation
                if (last == null) {
                    lastMovementTimeMs = now
                } else {
                    val moved = last.distanceTo(loc)
                    if (moved >= MOVEMENT_THRESHOLD_M) {
                        lastMovementTimeMs = now
                    }
                }

                lastLocation = loc
                val idleNow = now - lastMovementTimeMs >= IDLE_TIMEOUT_MS
                if (idleNow != isIdle) {
                    isIdle = idleNow
                    updateLocationRequest()

                    val idleIntent = Intent(ACTION_TRACKING_IDLE)
                        .putExtra(EXTRA_IDLE, isIdle)
                    sendBroadcast(idleIntent)
                }

                // Broadcast to your ViewModel
                val intent = Intent(ACTION_TRACKING_LOCATION)
                intent.putExtra("lat", loc.latitude)
                intent.putExtra("lng", loc.longitude)
                sendBroadcast(intent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        fused.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    companion object {
        const val ACTION_TRACKING_LOCATION = "TRACKING_LOCATION"
        const val ACTION_TRACKING_IDLE = "TRACKING_IDLE"
        const val EXTRA_TRACKING_MODE = "tracking_mode"
        const val EXTRA_IDLE = "tracking_idle"
        const val MODE_HIGH_ACCURACY = "high_accuracy"
        const val MODE_BALANCED = "balanced"
        const val ACTION_PERMISSION_REQUIRED = "TRACKING_PERMISSION_REQUIRED"
        const val ACTION_UPDATE_NOTIFICATION = "TRACKING_UPDATE_NOTIFICATION"
        const val EXTRA_DISTANCE_METERS = "tracking_distance_meters"
        const val EXTRA_DURATION_SEC = "tracking_duration_sec"
        const val EXTRA_CALORIES = "tracking_calories"
        const val EXTRA_ACTIVITY_LABEL = "tracking_activity_label"
        const val NOTIFICATION_ID = 1

        private const val HIGH_ACCURACY_INTERVAL_MS = 2000L
        private const val HIGH_ACCURACY_MIN_DISTANCE_M = 2f
        private const val BALANCED_INTERVAL_MS = 7000L
        private const val BALANCED_MIN_DISTANCE_M = 7f
        private const val IDLE_INTERVAL_MS = 10000L
        private const val IDLE_MIN_DISTANCE_M = 10f
        private const val IDLE_TIMEOUT_MS = 2 * 60 * 1000L
        private const val MOVEMENT_THRESHOLD_M = 8f
    }
}
