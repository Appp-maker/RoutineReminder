package com.example.routinereminder.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.routinereminder.ui.theme.RoutineReminderTheme
import com.example.routinereminder.ui.theme.AppPalette
import org.maplibre.android.maps.MapView
import androidx.core.content.FileProvider
import java.util.UUID
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SharePreviewActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionId = intent.getStringExtra("sessionId") ?: return
        val snapshotPath = intent.getStringExtra("snapshotPath")

        val mapView = MapView(this)

        setContent {
            RoutineReminderTheme(darkTheme = true, dynamicColor = false) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppPalette.SurfaceOverlay),
                    color = AppPalette.SurfaceOverlay
                ) {
                    SessionSharePreviewScreen(
                        sessionId = sessionId,
                        mapView = mapView,
                        onShare = { bitmap -> shareBitmap(this, bitmap) },
                        onCancel = { finish() }
                    )
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        overrideOverrideAnimations()
    }


    override fun onStart() {
        super.onStart()
        overrideOverrideAnimations()
    }

    @Suppress("DEPRECATION")
    private fun overrideOverrideAnimations() {
        val opts = android.app.ActivityOptions.makeCustomAnimation(
            this,
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }}
