package com.example.glarmto.ui.nutrition

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun AnimatedWaterGlass(
    fillPercentage: Float,
    modifier: Modifier = Modifier
) {
    // Animate the fill ratio so it smoothly rises/falls
    val animatedFill by animateFloatAsState(
        targetValue = fillPercentage.coerceIn(0f, 1f),
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "WaterLevelAnimation"
    )

    // Infinite transition for the wave motion
    val infiniteTransition = rememberInfiniteTransition(label = "WaveAnimation")
    
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Box(modifier = modifier.height(200.dp).width(120.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val strokeWidth = 4.dp.toPx()
            val glassCornerRadius = 16.dp.toPx()

            // 1. Create the Path for the glass shape (Rounded at bottom)
            val glassPath = Path().apply {
                moveTo(0f, 0f) // Top left
                lineTo(0f, height - glassCornerRadius) // Down to bottom left curve start
                quadraticBezierTo(0f, height, glassCornerRadius, height) // Bottom left curve
                lineTo(width - glassCornerRadius, height) // Bottom straight line
                quadraticBezierTo(width, height, width, height - glassCornerRadius) // Bottom right curve
                lineTo(width, 0f) // Up to top right
                // Notice we do not close the path at the top, representing an open cup
            }

            val clipPathForWater = Path().apply {
                moveTo(0f, 0f) // Top left
                lineTo(0f, height - glassCornerRadius)
                quadraticBezierTo(0f, height, glassCornerRadius, height)
                lineTo(width - glassCornerRadius, height)
                quadraticBezierTo(width, height, width, height - glassCornerRadius)
                lineTo(width, 0f)
                close() // Close it for clipping content accurately inside
            }

            // Draw the glass outline
            drawPath(
                path = glassPath,
                color = Color.White.copy(alpha = 0.5f), // Reflected glass edge
                style = Stroke(width = strokeWidth)
            )

            // 2. Calculate Water Fill Level
            val waterHeight = height * animatedFill
            val waterStartY = height - waterHeight

            // 3. Draw Water with clipping so it fits nicely inside the rounded bottom
            if (animatedFill > 0f) {
                clipPath(path = clipPathForWater) {
                    val wavePath = Path().apply {
                        val amplitude = 8.dp.toPx() // Height of wave
                        val frequency = 1.5f // Number of waves fitting in the width

                        moveTo(0f, height) // start at bottom-left
                        lineTo(0f, waterStartY) // up to current water level

                        // Draw sine wave across the surface
                        var currentX = 0f
                        while (currentX < width) {
                            val scaledX = (currentX / width) * 2 * Math.PI * frequency
                            val currentY = waterStartY + sin(scaledX - phase).toFloat() * amplitude
                            lineTo(currentX, currentY)
                            currentX += 5f // Small steps for smooth curve
                        }
                        
                        // complete the bounding box for the water
                        lineTo(width, waterStartY)
                        lineTo(width, height)
                        close()
                    }

                    // Draw the primary wave (Front)
                    drawPath(
                        path = wavePath,
                        color = Color(0xAA00BFFF) // Refreshing transparent blue
                    )
                    
                    // Draw a secondary darker wave offset to simulate depth
                    val wavePathBack = Path().apply {
                        val amplitude = 6.dp.toPx()
                        val frequency = 1.2f

                        moveTo(0f, height)
                        lineTo(0f, waterStartY)

                        var currentX = 0f
                        while (currentX < width) {
                            val scaledX = (currentX / width) * 2 * Math.PI * frequency
                            // Reverse the phase for contrast
                            val currentY = waterStartY + sin(scaledX + phase).toFloat() * amplitude
                            lineTo(currentX, currentY)
                            currentX += 5f
                        }
                        lineTo(width, waterStartY)
                        lineTo(width, height)
                        close()
                    }
                    
                    drawPath(
                        path = wavePathBack,
                        color = Color(0x66008B8B) // Darker, background cyan
                    )
                }
            }
        }
    }
}
