package com.example.glarmto.data.util

import kotlin.math.roundToInt

object HealthCalculator {
    /**
     * Calculates Total Daily Energy Expenditure (TDEE) from the Mifflin-St Jeor BMR, scaled by an
     * activity multiplier chosen from [workoutDays], then shifted for the [goal].
     *
     * Activity multiplier: 0-1 days = 1.2, 2-3 = 1.375, 4-5 = 1.55, 6+ = 1.725.
     * Goal: "Cut" subtracts 400 kcal (never below 1200), "Bulk" adds 400 kcal, anything else is maintenance.
     *
     * @param age User's age in years
     * @param weight User's weight in kilograms
     * @param height User's height in centimeters
     * @param isMale True if male, false if female
     * @param workoutDays Training days per week
     * @param goal "Cut", "Bulk", or "Maintain"
     * @return The daily calorie goal rounded to the nearest integer, or 2000 if the inputs are invalid.
     */
    fun calculateTdee(age: Int, weight: Double, height: Double, isMale: Boolean, workoutDays: Int = 3, goal: String = "Maintain"): Int {
        if (age <= 0 || weight <= 0.0 || height <= 0.0) return 2000 // Default fallback

        // Mifflin-St Jeor Equation for BMR
        val bmr = if (isMale) {
            (10 * weight) + (6.25 * height) - (5 * age) + 5
        } else {
            (10 * weight) + (6.25 * height) - (5 * age) - 161
        }
        
        val activityMultiplier = when {
            workoutDays <= 1 -> 1.2
            workoutDays <= 3 -> 1.375
            workoutDays <= 5 -> 1.55
            else -> 1.725
        }
        
        val tdee = (bmr * activityMultiplier).roundToInt()
        
        return when (goal) {
            "Cut" -> (tdee - 400).coerceAtLeast(1200)
            "Bulk" -> tdee + 400
            else -> tdee
        }
    }

    /**
     * Grams of each macro from daily calories and percentage split (4 kcal/g protein & carb, 9 kcal/g fat).
     * Percentages are normalized if they do not sum to 100.
     */
    fun macroGramsFromCalories(
        dailyCalories: Int,
        proteinPct: Int,
        carbPct: Int,
        fatPct: Int
    ): Triple<Int, Int, Int> {
        if (dailyCalories <= 0) return Triple(0, 0, 0)
        val sum = (proteinPct + carbPct + fatPct).coerceAtLeast(1)
        val p = proteinPct.toDouble() / sum
        val c = carbPct.toDouble() / sum
        val f = fatPct.toDouble() / sum
        val pCal = dailyCalories * p
        val cCal = dailyCalories * c
        val fCal = dailyCalories * f
        return Triple(
            (pCal / 4.0).roundToInt(),
            (cCal / 4.0).roundToInt(),
            (fCal / 9.0).roundToInt()
        )
    }
}
