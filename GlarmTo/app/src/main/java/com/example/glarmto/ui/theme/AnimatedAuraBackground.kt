package com.example.glarmto.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedAuraBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "AuraAnimation")
    
    // Fast pulsing and sweeping loops!
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "phase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(6500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "phase2"
    )

    val phase3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(9000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "phase3"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Deep rich galaxy base
        drawRect(color = Color(0xFF070014))

        val cx1 = (width / 2) + cos(phase1) * (width / 1.5f)
        val cy1 = (height / 2) + sin(phase1 * 1.5f) * (height / 2)

        val cx2 = (width * 0.8f) + cos(phase2 + PI.toFloat()) * (width / 2)
        val cy2 = (height * 0.3f) + sin(phase2 * 0.8f) * (height / 2)

        val cx3 = (width * 0.2f) + sin(phase3) * (width / 1.5f)
        val cy3 = (height * 0.8f) + cos(phase3) * (height / 2)

        // Orb 1 - Deep Neon Magenta
        drawOrb(
            center = Offset(cx1, cy1),
            radius = width * 1.2f * pulse,
            color = Color(0xFFE900FF).copy(alpha = 0.5f)
        )

        // Orb 2 - Electric Cyan
        drawOrb(
            center = Offset(cx2, cy2),
            radius = width * 1.4f * (2f - pulse),
            color = Color(0xFF00D2FF).copy(alpha = 0.45f)
        )

        // Orb 3 - Pure Neon Purple
        drawOrb(
            center = Offset(cx3, cy3),
            radius = width * 1.5f * pulse,
            color = Color(0xFF6F00FF).copy(alpha = 0.6f)
        )
        
        // Ambient center glow
        drawOrb(
            center = Offset(width / 2, height / 2),
            radius = width * 1.5f,
            color = Color(0xFF430099).copy(alpha = 0.3f)
        )
    }
}

private fun DrawScope.drawOrb(center: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, color.copy(alpha = 0.2f), Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

