package com.example.glarmto.ui.workout

import android.Manifest
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.glarmto.data.util.PoseAngleMath
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AiPoseTrackerScreen(onClose: () -> Unit) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var detectedPose by remember { mutableStateOf<Pose?>(null) }
    var scaleFactorX by remember { mutableStateOf(1f) }
    var scaleFactorY by remember { mutableStateOf(1f) }
    var feedbackText by remember { mutableStateOf("Stand in front of camera") }

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

                        // Use Front Camera for Form Checking usually
                        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val options = PoseDetectorOptions.Builder()
                            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                            .build()
                        val poseDetector = PoseDetection.getClient(options)
                        val executor = Executors.newSingleThreadExecutor()

                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                
                                // Calculate scaling factors.
                                // We assume portrait orientation for the camera.
                                val isPortrait = imageProxy.imageInfo.rotationDegrees == 90 || imageProxy.imageInfo.rotationDegrees == 270
                                val imageWidth = if (isPortrait) imageProxy.height else imageProxy.width
                                val imageHeight = if (isPortrait) imageProxy.width else imageProxy.height
                                
                                scaleFactorX = previewView.width.toFloat() / imageWidth.toFloat()
                                scaleFactorY = previewView.height.toFloat() / imageHeight.toFloat()

                                poseDetector.process(image)
                                    .addOnSuccessListener { pose ->
                                        detectedPose = pose
                                        
                                        // Simple feedback logic for Squats
                                        val hip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
                                        val knee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
                                        val ankle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
                                        
                                        if (hip != null && knee != null && ankle != null && 
                                            hip.inFrameLikelihood > 0.8f && knee.inFrameLikelihood > 0.8f) {
                                            
                                            val angle = PoseAngleMath.getAngle(hip, knee, ankle)
                                            if (angle < 90) {
                                                feedbackText = "Good Depth!"
                                            } else if (angle < 140) {
                                                feedbackText = "Go Lower!"
                                            } else {
                                                feedbackText = "Standing"
                                            }
                                        } else {
                                            feedbackText = "Full body not visible"
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
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

            // Draw Skeleton
            detectedPose?.let { pose ->
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val getPoint = { type: Int ->
                        pose.getPoseLandmark(type)?.let {
                            // Because front camera is mirrored, we might need to flip X coordinates
                            val x = size.width - (it.position.x * scaleFactorX)
                            val y = it.position.y * scaleFactorY
                            Offset(x, y)
                        }
                    }

                    // Draw Left Arm
                    val lShoulder = getPoint(PoseLandmark.LEFT_SHOULDER)
                    val lElbow = getPoint(PoseLandmark.LEFT_ELBOW)
                    val lWrist = getPoint(PoseLandmark.LEFT_WRIST)
                    
                    if (lShoulder != null && lElbow != null) drawLine(Color.Green, lShoulder, lElbow, strokeWidth = 8f, cap = StrokeCap.Round)
                    if (lElbow != null && lWrist != null) drawLine(Color.Green, lElbow, lWrist, strokeWidth = 8f, cap = StrokeCap.Round)

                    // Draw Right Arm
                    val rShoulder = getPoint(PoseLandmark.RIGHT_SHOULDER)
                    val rElbow = getPoint(PoseLandmark.RIGHT_ELBOW)
                    val rWrist = getPoint(PoseLandmark.RIGHT_WRIST)
                    
                    if (rShoulder != null && rElbow != null) drawLine(Color.Cyan, rShoulder, rElbow, strokeWidth = 8f, cap = StrokeCap.Round)
                    if (rElbow != null && rWrist != null) drawLine(Color.Cyan, rElbow, rWrist, strokeWidth = 8f, cap = StrokeCap.Round)

                    // Draw Torso connection
                    if (lShoulder != null && rShoulder != null) drawLine(Color.White, lShoulder, rShoulder, strokeWidth = 8f, cap = StrokeCap.Round)

                    // Draw Left Leg
                    val lHip = getPoint(PoseLandmark.LEFT_HIP)
                    val lKnee = getPoint(PoseLandmark.LEFT_KNEE)
                    val lAnkle = getPoint(PoseLandmark.LEFT_ANKLE)
                    
                    if (lShoulder != null && lHip != null) drawLine(Color.White, lShoulder, lHip, strokeWidth = 8f, cap = StrokeCap.Round)
                    if (lHip != null && lKnee != null) drawLine(Color.Red, lHip, lKnee, strokeWidth = 8f, cap = StrokeCap.Round)
                    if (lKnee != null && lAnkle != null) drawLine(Color.Red, lKnee, lAnkle, strokeWidth = 8f, cap = StrokeCap.Round)

                    // Draw Right Leg
                    val rHip = getPoint(PoseLandmark.RIGHT_HIP)
                    val rKnee = getPoint(PoseLandmark.RIGHT_KNEE)
                    val rAnkle = getPoint(PoseLandmark.RIGHT_ANKLE)
                    
                    if (rShoulder != null && rHip != null) drawLine(Color.White, rShoulder, rHip, strokeWidth = 8f, cap = StrokeCap.Round)
                    if (rHip != null && rKnee != null) drawLine(Color.Magenta, rHip, rKnee, strokeWidth = 8f, cap = StrokeCap.Round)
                    if (rKnee != null && rAnkle != null) drawLine(Color.Magenta, rKnee, rAnkle, strokeWidth = 8f, cap = StrokeCap.Round)
                    
                    if (lHip != null && rHip != null) drawLine(Color.White, lHip, rHip, strokeWidth = 8f, cap = StrokeCap.Round)
                    
                    // Draw circles on joints
                    val allPoints = listOfNotNull(lShoulder, lElbow, lWrist, rShoulder, rElbow, rWrist, lHip, lKnee, lAnkle, rHip, rKnee, rAnkle)
                    for (point in allPoints) {
                        drawCircle(Color.Yellow, radius = 12f, center = point)
                    }
                }
            }

            // UI Overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(onClick = onClose, colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha=0.5f))) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Surface(
                    color = if (feedbackText == "Good Depth!") Color(0xFF4CAF50).copy(alpha = 0.8f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = feedbackText,
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Camera permission is required.")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text("Grant Permission")
            }
        }
    }
}
