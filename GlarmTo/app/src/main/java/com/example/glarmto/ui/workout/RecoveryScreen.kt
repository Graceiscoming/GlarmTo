package com.example.glarmto.ui.workout

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.glarmto.GlarmToApplication
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as GlarmToApplication
    val viewModel: RecoveryViewModel = viewModel(
        factory = RecoveryViewModelFactory(application.repository)
    )

    val recoveryStatus by viewModel.recoveryStatus.collectAsState()
    val smartRecommendation by viewModel.smartRecommendation.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchAndCalculateRecovery()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Muscle Recovery 🩸",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Based on the last 72 hours of training",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("AI Recommendation", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Text(smartRecommendation, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        if (recoveryStatus.isEmpty()) {
            item {
                CircularProgressIndicator()
            }
        } else {
            items(recoveryStatus) { status ->
                RecoveryBarItem(status = status)
            }
        }
    }
}

@Composable
fun RecoveryBarItem(status: MuscleRecovery) {
    val pct = status.recoveryPercentage
    val barColor = when {
        pct >= 0.8f -> Color(0xFF4CAF50) // Green
        pct >= 0.4f -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = pct,
        animationSpec = tween(durationMillis = 1000)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    status.muscleGroup.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    "${(animatedProgress * 100).roundToInt()}% Recovered",
                    fontWeight = FontWeight.ExtraBold,
                    color = barColor
                )
            }

            Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                val cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                // Background Track
                drawRoundRect(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    size = size,
                    cornerRadius = cornerRadius
                )
                // Foreground Progress
                drawRoundRect(
                    color = barColor,
                    size = Size(size.width * animatedProgress, size.height),
                    cornerRadius = cornerRadius
                )
            }
            
            if (pct < 0.3f) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFF44336), modifier = Modifier.size(14.dp))
                    Text("Needs more rest before heavy training.", fontSize = 12.sp, color = Color(0xFFF44336))
                }
            }
        }
    }
}
