package com.example.routinereminder.location

import android.app.*
import android.content.Intent
import android.location.Location
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.routinereminder.R
import com.google.android.gms.location.*

class TrackingService : Service() {

    private lateinit var fused: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var currentMode: String = MODE_BALANCED
    private var isIdle = false
    private var lastMovementTimeMs = 0L
    private var lastLocation: Location? = null
    private var lastRequestSignature: String? = null

    override fun onCreate() {
        super.onCreate()

        fused = LocationServices.getFusedLocationProviderClient(this)
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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

        val chan = NotificationChannel(
            channelId,
            "Running Tracker",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(chan)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Running active")
            .setContentText("Tracking your run in background")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun startLocationUpdates() {
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
