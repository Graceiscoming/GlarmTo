package com.example.glarmto.ui.camera

import com.example.glarmto.R
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.glarmto.data.util.BarcodeNutrition
import com.example.glarmto.data.util.BarcodeScanGate
import com.example.glarmto.data.util.NutritionOcrParser
import com.example.glarmto.GlarmToApplication
import com.example.glarmto.data.util.LookupResult
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch

enum class ScannerMode {
    BARCODE, OCR
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScannerScreen(
    mode: ScannerMode,
    onResult: (BarcodeNutrition, scannedBarcode: String?) -> Unit,
    onCancel: () -> Unit,
    /** The user chose to type an unknown product in themselves; the barcode is passed so it can be remembered. */
    onEnterManually: (barcode: String) -> Unit = {}
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Releases the camera, the ML Kit detectors and the analysis thread when this screen leaves.
    val cameraSession = remember { CameraSession() }
    DisposableEffect(cameraSession) { onDispose { cameraSession.close() } }
    
    var isProcessing by remember { mutableStateOf(false) }
    val barcodeGate = remember { BarcodeScanGate() }
    var notFoundBarcode by remember { mutableStateOf<String?>(null) }
    // True when the product wasn't in the offline data and the online search couldn't be reached.
    var notFoundBecauseOffline by remember { mutableStateOf(false) }
    val productLookup = remember { (context.applicationContext as GlarmToApplication).productLookup }

    // A "not found" barcode is already ignored by the gate; dismissing just re-opens scanning.
    val dismissNotFound = {
        barcodeGate.dismissNotFound()
        notFoundBarcode = null
        notFoundBecauseOffline = false
        isProcessing = false
    }

    if (notFoundBarcode != null) {
        AlertDialog(
            onDismissRequest = dismissNotFound,
            title = { Text(stringResource(R.string.product_not_found), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
            text = {
                Text(
                    stringResource(
                        if (notFoundBecauseOffline) R.string.barcode_not_found_offline
                        else R.string.no_product_was_found_for_this_barcode_scan
                    )
                )
            },
            confirmButton = {
                Button(onClick = {
                    notFoundBarcode?.let(onEnterManually)
                    onCancel()
                }) { Text(stringResource(R.string.enter_manually)) }
            },
            dismissButton = {
                TextButton(onClick = dismissNotFound) { Text(stringResource(R.string.scan_another)) }
            }
        )
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (cameraPermissionState.status.isGranted) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        // The screen may have been closed before the camera provider was ready.
                        if (cameraSession.isClosed) return@addListener
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val barcodeScanner = cameraSession.own(BarcodeScanning.getClient())
                        val textScanner = cameraSession.own(TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS))

                        imageAnalysis.setAnalyzer(cameraSession.analysisExecutor) { imageProxy ->
                            if (cameraSession.isClosed || isProcessing) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                            
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                
                                if (mode == ScannerMode.BARCODE) {
                                    barcodeScanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            if (barcodes.isNotEmpty()) {
                                                val rawValue = barcodes.first().rawValue
                                                if (rawValue != null && barcodeGate.tryBegin(rawValue)) {
                                                    isProcessing = true
                                                    coroutineScope.launch {
                                                        when (val result = productLookup.lookup(rawValue)) {
                                                            is LookupResult.Found -> {
                                                                barcodeGate.onFound()
                                                                onResult(result.product, rawValue)
                                                            }
                                                            else -> {
                                                                // Stay "processing" until the dialog is dismissed so the camera
                                                                // can't start another lookup behind it.
                                                                barcodeGate.onNotFound(rawValue)
                                                                notFoundBecauseOffline = result is LookupResult.NotFoundOffline
                                                                notFoundBarcode = rawValue
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        .addOnFailureListener { Log.e("Scanner", "Barcode failed", it) }
                                        .addOnCompleteListener { imageProxy.close() }
                                } else {
                                    textScanner.process(image)
                                        .addOnSuccessListener { text ->
                                            if (text.text.isNotEmpty()) {
                                                val parsed = NutritionOcrParser.parseNutritionFromLabel(text.text)
                                                // If we found some valid data
                                                if (parsed.calories > 0 || parsed.protein > 0) {
                                                    isProcessing = true
                                                    onResult(parsed, null)
                                                }
                                            }
                                        }
                                        .addOnFailureListener { Log.e("Scanner", "OCR failed", it) }
                                        .addOnCompleteListener { imageProxy.close() }
                                }
                            } else {
                                imageProxy.close()
                            }
                        }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            val unbind = { cameraProvider.unbind(preview, imageAnalysis) }
                            if (!cameraSession.onUnbind(unbind)) unbind()
                        } catch (exc: Exception) {
                            Log.e("Scanner", "Use case binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // UI Overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(onClick = onCancel, colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha=0.5f))) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Color.White)
                    }
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(if (mode == ScannerMode.BARCODE) R.string.point_at_food_barcode else R.string.point_at_nutrition_label),
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = if (mode == ScannerMode.BARCODE) 4.dp else 16.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (mode == ScannerMode.BARCODE) {
                            // Open Food Facts asks to be credited (ODbL).
                            Text(
                                text = stringResource(R.string.product_data_credit),
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                if (isProcessing && notFoundBarcode == null) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.camera_permission_is_required_to_scan))
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text(stringResource(R.string.grant_permission))
            }
        }
    }
}
