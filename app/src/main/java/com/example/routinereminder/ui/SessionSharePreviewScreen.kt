@file:Suppress("DEPRECATION")

package com.example.routinereminder.ui
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.maps.Style
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import androidx.core.content.FileProvider
import java.util.UUID
import android.content.Intent

import androidx.compose.ui.viewinterop.AndroidView

import kotlinx.coroutines.isActive
import kotlin.coroutines.resumeWithException
import com.example.routinereminder.data.model.SessionStats
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.maps.MapView
import com.example.routinereminder.ui.SessionStore
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

enum class ShareMode { SATELLITE, STATIC }

@Composable
fun SessionSharePreviewScreen(
    sessionId: String,
    mapView: MapView?,
    onShare: (Bitmap) -> Unit,
    onCancel: () -> Unit
        ) {
    val viewModel: MainViewModel = hiltViewModel()
    // --- REAL USER DATA FROM VIEWMODEL ---
    val weight = viewModel.userWeightKg ?: 70.0
    val height = viewModel.userHeightCm ?: 175.0
    val age = viewModel.userAge ?: 30
    val gender = viewModel.userGender ?: "Male"
    var cachedBackground by remember { mutableStateOf<Bitmap?>(null) }
    var cachedBgUri by remember { mutableStateOf<Uri?>(null) }


    val context = LocalContext.current
    val session = remember(sessionId) {
        SessionStore.getSession(context, sessionId)
    }

    if (session == null) {
        Text("Session not found")
        return
    }
    val effectiveMapView = remember(mapView) { mapView ?: MapView(context) }

    DisposableEffect(effectiveMapView) {
        effectiveMapView.onCreate(null)
        effectiveMapView.onStart()
        effectiveMapView.onResume()
        onDispose {
            effectiveMapView.onPause()
            effectiveMapView.onStop()
            effectiveMapView.onDestroy()
        }
    }

    // keep it attached but invisible (1dp) so MapLibre actually draws
    AndroidView(factory = { effectiveMapView }, modifier = Modifier.size(1.dp))


    var shareMode by remember { mutableStateOf(ShareMode.SATELLITE) }
    var backgroundHue by remember { mutableStateOf(200f) }
    var trailHue by remember { mutableStateOf(340f) }
    var includeStats by remember { mutableStateOf(true) }
    var bgUri by remember { mutableStateOf<Uri?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Hold temporary file URI for the camera
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    // 📸 Camera launcher (take a picture)
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri?.let { uri -> bgUri = uri }
        }
    }
    //request permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(tempCameraUri)
        }
    }


    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        bgUri = uri
    }

    // Re-render preview whenever relevant state changes
    LaunchedEffect(shareMode, backgroundHue, trailHue, bgUri, effectiveMapView, includeStats) {
        previewBitmap = withContext(Dispatchers.Default) {
            generatePreviewBitmap(
                context = context,
                session = session,
                mapView = effectiveMapView,
                mode = shareMode,
                bgHue = backgroundHue,
                trailHue = trailHue,
                bgUri = bgUri,
                includeStats = includeStats,
                weight = weight,
                height = height,
                age = age,
                gender = gender
            )
        }

    }







// Keep it attached (invisible)




    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor(0xFF0E0E0E)),
        color = ComposeColor(0xFF0E0E0E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🖼 Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ComposeColor(0xFF1A1A1A))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                previewBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                    )
                } ?: Text("Generating preview...", color = ComposeColor.Gray)
            }

            Spacer(Modifier.height(20.dp))

            // 📦 Mode Selection
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth()
            ) {
                ModeBox(
                    label = "Satellite Image",
                    selected = shareMode == ShareMode.SATELLITE,
                    onClick = { shareMode = ShareMode.SATELLITE },
                    modifier = Modifier.weight(1f)
                )
                ModeBox(
                    label = "Static Background",
                    selected = shareMode == ShareMode.STATIC,
                    onClick = { shareMode = ShareMode.STATIC },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Show stats", color = ComposeColor.White)
                Switch(
                    checked = includeStats,
                    onCheckedChange = { includeStats = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ComposeColor.White,
                        checkedTrackColor = ComposeColor(0xFF00C853)
                    )
                )
            }


            AnimatedVisibility(visible = shareMode == ShareMode.STATIC) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(16.dp))

                    if (bgUri == null) {
                        // Show color picker when no image is selected
                        Text("Background Color", color = ComposeColor.LightGray)
                        HueSlider(hue = backgroundHue, onHueChange = { backgroundHue = it })
                        Spacer(Modifier.height(12.dp))
                    }

                    Text("Trail Color", color = ComposeColor.LightGray)
                    HueSlider(hue = trailHue, onHueChange = { trailHue = it })
                    Spacer(Modifier.height(12.dp))

                    if (bgUri == null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Pick from gallery
                            OutlinedButton(
                                onClick = { galleryPicker.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ComposeColor.White)
                            ) {
                                Text("Pick from Gallery")
                            }

                            // Take a picture
                            OutlinedButton(
                                onClick = {
                                    val photoFile = File(context.cacheDir, "camera_${UUID.randomUUID()}.jpg")
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        photoFile
                                    )
                                    tempCameraUri = uri

                                    val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                                    val resolveInfoList = context.packageManager.queryIntentActivities(cameraIntent, 0)
                                    for (resolveInfo in resolveInfoList) {
                                        val packageName = resolveInfo.activityInfo.packageName
                                        context.grantUriPermission(
                                            packageName,
                                            uri,
                                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        )
                                    }

                                    // 🚀 Launch or request permission
                                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.CAMERA
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    ) {
                                        cameraLauncher.launch(uri)
                                    } else {
                                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ComposeColor.White)
                            ) {
                                Text("Take Picture")
                            }

                        }
                    } else {
                        // Remove image button
                        OutlinedButton(
                            onClick = { bgUri = null },
                            border = BorderStroke(1.dp, ComposeColor(0xFFFF5252)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ComposeColor(0xFFFF5252)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Remove Image")
                        }
                    }

                }
            }


            Spacer(Modifier.height(20.dp))

            // 🧭 Bottom Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    border = BorderStroke(1.dp, ComposeColor(0xFF9E9E9E)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ComposeColor(0xFF9E9E9E)
                    )
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { previewBitmap?.let(onShare) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ComposeColor(0xFF00C853)
                    ),
                    modifier = Modifier
                        .height(50.dp)
                        .width(120.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Share", color = ComposeColor.Black, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }


}

@Composable
private fun ModeBox(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) ComposeColor(0xFF1F1F1F) else ComposeColor(0xFF151515))
            .border(
                width = 2.dp,
                color = if (selected) ComposeColor(0xFF00C853) else ComposeColor(0xFF333333),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp)
            .height(50.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) ComposeColor.White else ComposeColor(0xFFBBBBBB),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
@Composable
private fun HueSlider(hue: Float, onHueChange: (Float) -> Unit) {
    val adjustedHue = hue.coerceIn(0f, 360f)

    Slider(
        value = adjustedHue,
        onValueChange = onHueChange,
        valueRange = 0f..360f,
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp), // thin like before
        colors = SliderDefaults.colors(
            thumbColor = ComposeColor.White,
            activeTrackColor = when {
                adjustedHue <= 0f -> ComposeColor.Black
                adjustedHue >= 360f -> ComposeColor.White
                else -> ComposeColor.hsv(adjustedHue, 1f, 1f)
            },
            inactiveTrackColor = ComposeColor.DarkGray
        )
    )
}




/* ---------------- Bitmap Generation ---------------- */
private suspend fun captureSnapshot(
    mapView: MapView,
    map: org.maplibre.android.maps.MapLibreMap,
    trail: List<Point>
): Bitmap? = suspendCancellableCoroutine { cont ->

    if (trail.size < 2) {
        map.snapshot { bmp -> cont.resume(bmp) }
        return@suspendCancellableCoroutine
    }

    // Compute bounds for trail
    val boundsBuilder = org.maplibre.android.geometry.LatLngBounds.Builder()
    trail.forEach { point ->
        boundsBuilder.include(org.maplibre.android.geometry.LatLng(point.latitude(), point.longitude()))
    }
    val bounds = boundsBuilder.build()

    // Prepare camera update
    val padding = 100
    val update = org.maplibre.android.camera.CameraUpdateFactory.newLatLngBounds(bounds, padding)

    // Animate camera, then snapshot after completion
    map.animateCamera(update, 1500, object : org.maplibre.android.maps.MapLibreMap.CancelableCallback {
        override fun onFinish() {
            mapView.postDelayed({
                try {
                    map.snapshot { bmp ->
                        cont.resume(bmp)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    cont.resume(null)
                }
            }, 300) // small buffer after animation ends
        }

        override fun onCancel() {
            cont.resume(null)
        }
    })
}
private suspend fun generateStaticMapSnapshotViaMapLibre(
    context: Context,
    mapView: MapView,
    session: SessionStats,
    bgHue: Float,
    trailHue: Float
): Bitmap? = suspendCancellableCoroutine { cont ->

    mapView.getMapAsync { mapLibreMap ->

        val bgColor = android.graphics.Color.HSVToColor(
            floatArrayOf(bgHue, 1f, 1f)
        )
        val trailColor = android.graphics.Color.HSVToColor(
            floatArrayOf(trailHue, 1f, 1f)
        )

        val style = Style.Builder()
            .withSource(
                GeoJsonSource(
                    "trail-source",
                    LineString.fromLngLats(session.polyline)
                )
            )
            .withLayer(
                LineLayer("trail-layer", "trail-source").withProperties(
                    org.maplibre.android.style.layers.PropertyFactory.lineColor(trailColor),
                    org.maplibre.android.style.layers.PropertyFactory.lineWidth(6f),
                    org.maplibre.android.style.layers.PropertyFactory.lineJoin("round"),
                    org.maplibre.android.style.layers.PropertyFactory.lineCap("round")
                )
            )

        mapLibreMap.setStyle(style) { loaded ->

            zoomToTrail(mapView, mapLibreMap, session.polyline)

            val idleListener = object : org.maplibre.android.maps.MapLibreMap.OnCameraIdleListener {
                override fun onCameraIdle() {
                    mapLibreMap.removeOnCameraIdleListener(this)

                    mapView.postDelayed({
                        try {
                            mapLibreMap.snapshot { bmp ->
                                if (bmp != null) {
                                    val final = Bitmap.createBitmap(
                                        bmp.width, bmp.height, Bitmap.Config.ARGB_8888
                                    )
                                    val canvas = Canvas(final)
                                    canvas.drawColor(bgColor)
                                    canvas.drawBitmap(bmp, 0f, 0f, null)
                                    cont.resume(final)
                                } else cont.resume(null)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            cont.resume(null)
                        }
                    }, 600)
                }
            }

            mapLibreMap.addOnCameraIdleListener(idleListener)
        }
    }
}

private suspend fun MapView.getMapAsyncBlocking(): org.maplibre.android.maps.MapLibreMap =
    suspendCancellableCoroutine { cont ->
        getMapAsync { map -> cont.resume(map) }
    }
private fun hueToColor(hue: Float): ComposeColor {
    return when {
        hue <= 0f -> ComposeColor.Black
        hue >= 360f -> ComposeColor.White
        else -> ComposeColor.hsv(hue, 1f, 1f)
    }
}

private suspend fun generatePreviewBitmap(
    context: Context,
    session: SessionStats,
    mapView: MapView?,
    mode: ShareMode,
    bgHue: Float,
    trailHue: Float,
    bgUri: Uri?,
    includeStats: Boolean,
    weight: Double,
    height: Double,
    age: Int,
    gender: String
): Bitmap = withContext(Dispatchers.Default) {
    // Try loading snapshot if passed via Intent
    val snapshotPath = (context as? SharePreviewActivity)?.intent?.getStringExtra("snapshotPath")
        ?: session.snapshotPath
    val snapshotBitmap = snapshotPath?.let { path ->
        val f = File(path)
        if (f.exists()) BitmapFactory.decodeFile(path) else null
    }
    var staticBgUriCache: Uri? = null
    var staticBgBitmapCache: Bitmap? = null
    val width = 1080
    val height = 1920
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val paint = Paint()
    val statsHeight = if (includeStats) 300f else 0f
    val overlayTop = height - statsHeight
    val mapRect = RectF(0f, 0f, width.toFloat(), overlayTop)

    fun ensureCorrectOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val exif = androidx.exifinterface.media.ExifInterface(inputStream!!)
            val rotation = when (exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            inputStream.close()

            if (rotation != 0f) {
                val matrix = Matrix().apply { postRotate(rotation) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }
    // ---- Background ----
    val background: Bitmap? = when (mode) {
        ShareMode.SATELLITE -> {
            // Try existing snapshot first
            snapshotBitmap ?: run {
                if (mapView != null) {
                    try {
                        generateSatelliteSnapshot(context, mapView, session)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                } else null
            }
        }
        ShareMode.STATIC -> {
            if (bgUri != null) {

                // Only process when a NEW image is selected
                if (staticBgUriCache != bgUri || staticBgBitmapCache == null) {
                    staticBgUriCache = bgUri

                    val decoded = withContext(Dispatchers.IO) {
                        MediaStore.Images.Media.getBitmap(context.contentResolver, bgUri)
                    }

                    val rotated = ensureCorrectOrientation(context, bgUri, decoded)

                    staticBgBitmapCache = withContext(Dispatchers.Default) {
                        Bitmap.createScaledBitmap(rotated, width, overlayTop.toInt(), true)
                    }
                }

                staticBgBitmapCache
            } else null
        }


    }











// ---- 1. Draw BACKGROUND ----
    if (background != null) {
        canvas.drawBitmap(background, null, mapRect, null)
    } else {
        paint.color = android.graphics.Color.argb(
            (hueToColor(bgHue).alpha * 255).toInt(),
            (hueToColor(bgHue).red * 255).toInt(),
            (hueToColor(bgHue).green * 255).toInt(),
            (hueToColor(bgHue).blue * 255).toInt()
        )
        canvas.drawRect(mapRect, paint)
    }

// ---- 2. Always draw TRAIL when mode is STATIC ----
    if (mode == ShareMode.STATIC) {
        val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(
                (hueToColor(trailHue).alpha * 255).toInt(),
                (hueToColor(trailHue).red * 255).toInt(),
                (hueToColor(trailHue).green * 255).toInt(),
                (hueToColor(trailHue).blue * 255).toInt()
            )
            strokeWidth = 10f
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

        drawTrailCentered(canvas, session.polyline, mapRect, trailPaint)
    }


    if (includeStats) {
        // ---- Bottom Overlay ----
        val overlayPaint = Paint().apply {
            shader = LinearGradient(
                0f, overlayTop, 0f, height.toFloat(),
                Color.argb(180, 0, 0, 0),
                Color.argb(255, 0, 0, 0),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, overlayTop, width.toFloat(), height.toFloat(), overlayPaint)

        // ---- Text ----
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

        // --- Bottom Info ---
        val baseY = height - 130f // slightly lower than before
        canvas.drawText(session.activity.uppercase(), 60f, baseY - 90f, titlePaint)

        val spacing = width / 4f
        val km = session.distanceMeters / 1000.0
        val pace = if (km > 0) session.durationSec / 60.0 / km else 0.0 // min/km

        val smallText = Paint(textPaint).apply { textSize = 44f } // a bit smaller for pace

        // 🏃 Distance
        canvas.drawText("%.2f km".format(km), 40f, baseY, textPaint)
        canvas.drawText("Distance", 40f, baseY + 45f, subPaint)

        // ⏱ Duration
        canvas.drawText(formatHMS(session.durationSec), spacing + 20f, baseY, textPaint)
        canvas.drawText("Duration", spacing + 20f, baseY + 45f, subPaint)

        // 🕒 Pace (center-align the “min/km” a bit)
        canvas.drawText("%.1f".format(pace), spacing * 2 + 60f, baseY, smallText)
        canvas.drawText("min/km", spacing * 2 + 60f, baseY + 45f, subPaint)
        canvas.drawText("Pace", spacing * 2 + 60f, baseY + 85f, subPaint)

        // 🔥 Calories
        // ------- REAL USER DATA CALORIES -------
        val met = when (session.activity.lowercase()) {
            "walking" -> 3.8
            "running" -> 9.8
            "cycling" -> 8.0
            else -> 9.8
        }

        val caloriesBurned = calcCalories(
            met = met,
            weightKg = weight.toDouble(),
            heightCm = height.toDouble(),
            age = age,
            gender = gender,
            durationSec = session.durationSec
        ).roundToInt()

        canvas.drawText(
            "$caloriesBurned cal",
            spacing * 3 + 60f,
            baseY,
            textPaint
        )
        canvas.drawText(
            "Calories",
            spacing * 3 + 60f,
            baseY + 45f,
            subPaint
        )
    }


    bmp
}

private suspend fun generateSatelliteSnapshot(
    context: Context,
    mapView: MapView,
    session: SessionStats
): Bitmap? = suspendCancellableCoroutine { cont ->
    mapView.getMapAsync { mapLibreMap ->
        try {
            mapLibreMap.setStyle(
                Style.Builder()
                    .withSource(
                        RasterSource(
                            "satellite-source",
                            "https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
                        )
                    )
                    .withLayer(RasterLayer("satellite-layer", "satellite-source"))
            ) {
                // Move camera to fit trail
                zoomToTrail(mapView, mapLibreMap, session.polyline)

                // Wait for the camera to stop moving
                val idleListener = object : org.maplibre.android.maps.MapLibreMap.OnCameraIdleListener {
                    override fun onCameraIdle() {
                        mapLibreMap.removeOnCameraIdleListener(this)

                        // Give MapLibre a moment to render all raster tiles
                        mapView.postDelayed({
                            try {
                                mapLibreMap.snapshot { bmp ->
                                    cont.resume(bmp)
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                                cont.resume(null)
                            }
                        }, 1200) // ~1.2s delay for smoother tile rendering
                    }
                }
                mapLibreMap.addOnCameraIdleListener(idleListener)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            cont.resume(null)
        }
    }
}



private fun zoomToTrail(
    mapView: MapView,
    map: org.maplibre.android.maps.MapLibreMap,
    poly: List<Point>
) {
    if (poly.size < 2) return

    val boundsBuilder = org.maplibre.android.geometry.LatLngBounds.Builder()
    poly.forEach { point ->
        boundsBuilder.include(org.maplibre.android.geometry.LatLng(point.latitude(), point.longitude()))
    }

    val bounds = try {
        boundsBuilder.build()
    } catch (e: Exception) {
        e.printStackTrace()
        return
    }

    map.getStyle {
        mapView.post {
            try {
                val padding = 100
                val update = org.maplibre.android.camera.CameraUpdateFactory.newLatLngBounds(bounds, padding)
                map.animateCamera(update, 1000)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}





private fun captureSnapshotAfterDelay(
    mapView: MapView,
    map: org.maplibre.android.maps.MapLibreMap,
    cont: kotlin.coroutines.Continuation<Bitmap?>,
    trail: List<Point>? = null
) {
    // Zoom to trail first if available
    trail?.let {
        zoomToTrail(mapView, map, it)
    }

    // Take snapshot after a short delay to let camera finish moving
    mapView.postDelayed({
        runCatching {
            map.snapshot { bmp ->
                bmp?.let {
                    cont.resume(it)
                } ?: mapView.postDelayed({
                    runCatching {
                        map.snapshot { bmp2 ->
                            cont.resume(bmp2)
                        }
                    }.onFailure {
                        it.printStackTrace()
                        cont.resume(null)
                    }
                }, 1500)
            } // <-- closes map.snapshot
        }.onFailure {
            it.printStackTrace()
            cont.resume(null)
        }
    }, 1500) // <-- closes postDelayed
} // <-- closes function




private fun drawTrailCentered(
    canvas: Canvas,
    poly: List<Point>,
    mapRect: RectF,
    trailPaint: Paint
) {
    if (poly.size < 2) return

    val pad = 32f
    val availW = mapRect.width() - pad * 2
    val availH = mapRect.height() - pad * 2
    val xs = poly.map { it.longitude() }
    val ys = poly.map { it.latitude() }
    val minX = xs.minOrNull() ?: return
    val maxX = xs.maxOrNull() ?: return
    val minY = ys.minOrNull() ?: return
    val maxY = ys.maxOrNull() ?: return

    val spanX = (maxX - minX).coerceAtLeast(1e-9)
    val spanY = (maxY - minY).coerceAtLeast(1e-9)
    val scale = minOf((availW / spanX).toFloat(), (availH / spanY).toFloat())
    val extraX = (availW - spanX * scale) / 2f
    val extraY = (availH - spanY * scale) / 2f
    val left = mapRect.left + pad + extraX
    val top = mapRect.top + pad + extraY + 40f

    val path = Path()
    poly.forEachIndexed { i, p ->
        val x = left + ((p.longitude() - minX) * scale).toFloat()
        val y = top + ((maxY - p.latitude()) * scale).toFloat()
        if (i == 0) path.moveTo(x.toFloat(), y.toFloat()) else path.lineTo(x.toFloat(), y.toFloat())

    }

    canvas.drawPath(path, trailPaint)
}
