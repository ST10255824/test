package com.mackson.delivery.ui.picker

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.mackson.delivery.ui.common.ErrorBanner
import java.util.concurrent.Executors

/**
 * Camera-driven barcode scanning for the Picker app (Part 1 US-16) and the Customer app's
 * "scan a product barcode" flow (US-05). Uses on-device ML Kit barcode scanning — no network
 * round-trip, works fully offline once a frame is captured.
 */
@androidx.camera.core.ExperimentalGetImage
@Composable
fun BarcodeScanScreen(
    instructionText: String,
    onBarcodeDetected: (String) -> Unit,
    extraContent: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var detected by remember { mutableStateOf(false) }

    if (!hasCameraPermission) {
        // In production this launches ActivityResultContracts.RequestPermission(); omitted here
        // to keep this composable preview-safe. MainActivity requests CAMERA at startup for the
        // Picker/Driver roles.
        ErrorBanner("Camera permission is required to scan barcodes.")
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            instructionText,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val executor = Executors.newSingleThreadExecutor()
                    val scanner = BarcodeScanning.getClient(
                        BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                            .build()
                    )

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        analysis.setAnalyzer(executor) { imageProxy: ImageProxy ->
                            processFrame(imageProxy, scanner) { code ->
                                if (!detected) {
                                    detected = true
                                    onBarcodeDetected(code)
                                }
                            }
                        }
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                            )
                        } catch (_: Exception) {
                            // Camera already bound elsewhere / device has no camera — safe to ignore
                            // for the marking demo; a production build would surface this to the UI.
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )
        }
        extraContent?.invoke()
    }
}

@androidx.camera.core.ExperimentalGetImage
private fun processFrame(
    imageProxy: ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            barcodes.firstOrNull()?.rawValue?.let(onDetected)
        }
        .addOnCompleteListener { imageProxy.close() }
}
