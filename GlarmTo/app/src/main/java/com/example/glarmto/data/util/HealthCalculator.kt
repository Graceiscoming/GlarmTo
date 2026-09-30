package com.example.glarmto.data.util

import kotlin.math.roundToInt

object HealthCalculator {
    /**
     * Calculates Total Daily Energy Expenditure (TDEE) based on Mifflin-St Jeor formula
     * and assumes a moderate activity level (multiplier of 1.55).
     * 
     * @param age User's age in years
     * @param weight User's weight in kilograms
     * @param height User's height in centimeters
     * @param isMale True if male, false if female
     * @return The daily calorie goal (maintenance TDEE) rounded to the nearest integer.
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
