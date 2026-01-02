package com.example.routinereminder.ui

import kotlinx.coroutines.withContext
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect

@Composable
fun BarcodeScannerScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )
    val barcodeState = remember { mutableStateOf<String?>(null) }
    var isProcessingScan by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }


    LaunchedEffect(Unit) {
        snapshotFlow { barcodeState.value }
            .filterNotNull()
            .collect { barcode: String ->

                try {
                    if (barcode.isBlank() || barcode.length < 5) {
                        Toast.makeText(
                            context,
                            "Invalid barcode detected.",
                            Toast.LENGTH_SHORT
                        ).show()

                        barcodeState.value = null
                        isProcessingScan = false
                        return@collect
                    }

                    val apiUrl =
                        "https://world.openfoodfacts.org/api/v0/product/$barcode.json"

                    val result = withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val json = java.net.URL(apiUrl).readText()
                            val obj = org.json.JSONObject(json)

                            val status = obj.optInt("status", 0)
                            if (status != 1) return@withContext Pair(false, "")

                            val product =
                                obj.optJSONObject("product")
                                    ?: return@withContext Pair(false, "")

                            val name =
                                product.optString("product_name", "")

                            val nutriments =
                                product.optJSONObject("nutriments")

                            val hasNutrients =
                                nutriments != null && nutriments.length() > 0

                            val categories =
                                product.optString("categories", "").lowercase()

                            val isFoodItem =
                                hasNutrients ||
                                        categories.contains("food") ||
                                        categories.contains("drink") ||
                                        categories.contains("beverage")

                            Pair(
                                isFoodItem,
                                name.ifBlank { "Unknown product" }
                            )
                        } catch (e: Exception) {
                            Log.e("BarcodeScanner", "Lookup failed", e)
                            Pair(false, "")
                        }
                    }

                    val isFood = result.first
                    val productName = result.second

                    if (isFood) {
                        Toast.makeText(
                            context,
                            "Scanned: $productName",
                            Toast.LENGTH_SHORT
                        ).show()

                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("barcode", barcode)

                        navController.popBackStack(
                            route = Screen.CalorieTracker.route,
                            inclusive = false
                        )

                    } else {
                        Toast.makeText(
                            context,
                            "This barcode is not a food product.",
                            Toast.LENGTH_SHORT
                        ).show()

                        barcodeState.value = null
                        isProcessingScan = false
                    }

                } catch (e: Exception) {
                    Log.e("BarcodeScanner", "Processing error", e)

                    Toast.makeText(
                        context,
                        "Failed to process barcode.",
                        Toast.LENGTH_SHORT
                    ).show()

                    barcodeState.value = null
                    isProcessingScan = false
                }
            }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { context ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    val previewView = PreviewView(context)

                    val executor = Executors.newSingleThreadExecutor()
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build()
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(executor, BarcodeAnalyzer { barcode ->
                                    if (!isProcessingScan) {
                                        isProcessingScan = true
                                        barcodeState.value = barcode
                                    }

                                })
                            }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    }, ContextCompat.getMainExecutor(context))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text("Camera permission required", modifier = Modifier.align(Alignment.Center))
        }
    }
}

class BarcodeAnalyzer(private val onBarcodeDetected: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()
    private val scanner = BarcodeScanning.getClient(options)


    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: androidx.camera.core.ImageProxy) {


        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {

                        onBarcodeDetected(barcodes[0].rawValue ?: "")
                    }
                }
                .addOnFailureListener {
                    Log.e("BarcodeAnalyzer", "Barcode scanning failed", it)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }
}
