package com.example.glarmto.data.util

import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt

object PoseAngleMath {

    /**
     * Calculates the angle in degrees between three ML Kit PoseLandmarks.
     * The second landmark is the vertex.
     * Useful for checking Squat depth (Hip -> Knee -> Ankle).
     */
    fun getAngle(firstPoint: PoseLandmark?, midPoint: PoseLandmark?, lastPoint: PoseLandmark?): Double {
        if (firstPoint == null || midPoint == null || lastPoint == null) return 0.0

        val result = Math.toDegrees(
            atan2(
                lastPoint.position.y.toDouble() - midPoint.position.y,
                lastPoint.position.x.toDouble() - midPoint.position.x
            ) - atan2(
                firstPoint.position.y.toDouble() - midPoint.position.y,
                firstPoint.position.x.toDouble() - midPoint.position.x
            )
        )
        
        var angle = Math.abs(result) // Angle should never be negative
        if (angle > 180) {
            angle = 360.0 - angle // Always get the acute angle
        }
        return angle
    }
}
