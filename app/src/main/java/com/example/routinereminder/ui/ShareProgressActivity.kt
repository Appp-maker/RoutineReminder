package com.example.routinereminder.ui

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.layers.RasterLayer
import android.view.Gravity
import android.view.ViewGroup
import android.graphics.Color

class ShareProgressActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var mapLibreMap: MapLibreMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a FrameLayout to hold the map and attribution
        val container = FrameLayout(this)

        // Create the MapView
        mapView = MapView(this)
        container.addView(
            mapView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // Create the attribution TextView
        val attributionText = TextView(this).apply {
            text = "Imagery © NASA Earth Observatory (GIBS)"
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(8, 8, 8, 8)
            setBackgroundColor(Color.parseColor("#66000000")) // semi-transparent black
        }

        // Position it bottom-right
        val attributionParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            setMargins(0, 0, 12, 12)
        }

        container.addView(attributionText, attributionParams)
        setContentView(container)

        // Initialize MapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onMapReady(map: MapLibreMap) {
        mapLibreMap = map

        // NASA GIBS BlueMarble satellite imagery (public domain)
        val nasaSourceId = "nasa-satellite"
        val nasaUrlTemplate =
            "https://gibs.earthdata.nasa.gov/wmts/epsg3857/best/BlueMarble_ShadedRelief/default/{z}/{y}/{x}.jpg"

        mapLibreMap.setStyle(
            Style.Builder()
                .fromUri("https://demotiles.maplibre.org/style.json")
                .withSource(RasterSource(nasaSourceId, nasaUrlTemplate, 256))
                .withLayerBelow(RasterLayer("nasa-layer", nasaSourceId), "water")
        ) {
            // Move camera to a default position (Eiffel Tower)
            val location = LatLng(48.8584, 2.2945)
            mapLibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 3.5))

            // Add a sample marker
            mapLibreMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .title("Example Marker")
            )
        }
    }

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { mapView.onPause(); super.onPause() }
    override fun onStop() { mapView.onStop(); super.onStop() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onDestroy() { mapView.onDestroy(); super.onDestroy() }
}
