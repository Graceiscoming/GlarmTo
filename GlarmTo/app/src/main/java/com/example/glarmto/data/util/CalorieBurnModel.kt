package com.example.glarmto.data.util

import kotlin.math.roundToInt

/**
 * Linear regression trained offline on the public "Calories Burnt Prediction" dataset
 * (15,000 rows: Gender, Age, Height, Weight, Duration, Heart_Rate, Body_Temp -> Calories;
 * originally a Kaggle dataset, mirrored at github.com/mdatheeb/Calories-Burned-Predictor).
 *
 * Heart_Rate and Body_Temp were dropped from training since this app has no sensor to measure
 * them and inventing values would make the estimate dishonest. The reduced 5-feature model
 * (Gender, Age, Height, Weight, Duration — all things the app already has from the user's
 * profile plus a logged session's duration) still holds R^2 = 0.93, RMSE ~16 kcal on an 80/20
 * holdout split, fit by ordinary least squares. Coefficients are baked in as constants — no
 * TFLite runtime or model file needed for a model this small.
 */
object CalorieBurnModel {
    private const val INTERCEPT = -31.15041753
    private const val COEF_MALE = -0.90268514
    private const val COEF_AGE = 0.49863375
    private const val COEF_HEIGHT_CM = -0.19703545
    private const val COEF_WEIGHT_KG = 0.30670631
    private const val COEF_DURATION_MIN = 7.15907114

    fun estimateCaloriesBurned(
        isMale: Boolean,
        age: Int,
        heightCm: Double,
        weightKg: Double,
        durationMinutes: Double
    ): Int {
        if (age <= 0 || heightCm <= 0.0 || weightKg <= 0.0 || durationMinutes <= 0.0) return 0

        val estimate = INTERCEPT +
            (if (isMale) COEF_MALE else 0.0) +
            COEF_AGE * age +
            COEF_HEIGHT_CM * heightCm +
            COEF_WEIGHT_KG * weightKg +
            COEF_DURATION_MIN * durationMinutes

        return estimate.coerceAtLeast(0.0).roundToInt()
    }
}
