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

    override fun onCreate() {
        super.onCreate()

        fused = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 2000L
        ).setMinUpdateDistanceMeters(1f).build()

        startForegroundService()
        startLocationUpdates()
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

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            for (loc: Location in result.locations) {
                // Broadcast to your ViewModel
                val intent = Intent("TRACKING_LOCATION")
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
}
