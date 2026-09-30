package com.example.glarmto.ui.camera

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
import com.example.glarmto.data.util.NutritionOcrParser
import com.example.glarmto.data.util.OpenFoodFactsApi
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

enum class ScannerMode {
    BARCODE, OCR
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScannerScreen(
    mode: ScannerMode,
    onResult: (BarcodeNutrition) -> Unit,
    onCancel: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    
    var isProcessing by remember { mutableStateOf(false) }
    val ignoredBarcodes = remember { mutableStateListOf<String>() }
    var notFoundBarcode by remember { mutableStateOf<String?>(null) }

    if (notFoundBarcode != null) {
        AlertDialog(
            onDismissRequest = { 
                ignoredBarcodes.add(notFoundBarcode!!)
                notFoundBarcode = null
            },
            title = { Text("Product Not Found", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
            text = { Text("ไม่พบข้อมูลสินค้าจากบาร์โค้ดนี้ในระบบ คุณต้องการสแกนบาร์โค้ดอื่นต่อ หรือ กลับไปพิมพ์ข้อมูลเอง?") },
            confirmButton = {
                Button(onClick = { onCancel() }) { Text("พิมพ์ข้อมูลเอง") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    ignoredBarcodes.add(notFoundBarcode!!)
                    notFoundBarcode = null 
                }) { Text("สแกนชิ้นอื่นต่อ") }
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
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val barcodeScanner = BarcodeScanning.getClient()
                        val textScanner = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        val executor = Executors.newSingleThreadExecutor()

                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            if (isProcessing) {
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
                                                if (!rawValue.isNullOrBlank() && !ignoredBarcodes.contains(rawValue)) {
                                                    isProcessing = true
                                                    coroutineScope.launch {
                                                        val result = OpenFoodFactsApi.getNutritionByBarcode(rawValue)
                                                        if (result != null) {
                                                            onResult(result)
                                                        } else {
                                                            notFoundBarcode = rawValue
                                                            isProcessing = false
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
                                                    onResult(parsed)
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
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = if (mode == ScannerMode.BARCODE) "Point at Food Barcode" else "Point at Nutrition Label",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                if (isProcessing) {
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
            Text("Camera permission is required to scan.")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text("Grant Permission")
            }
        }
    }
}
