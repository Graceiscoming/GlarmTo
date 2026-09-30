package com.example.glarmto

import com.example.glarmto.data.util.HealthCalculator
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthCalculatorGoalsTest {

    @Test
    fun `Cut goal is below maintenance TDEE`() {
        val maintain = HealthCalculator.calculateTdee(30, 75.0, 180.0, true, workoutDays = 3, goal = "Maintain")
        val cut = HealthCalculator.calculateTdee(30, 75.0, 180.0, true, workoutDays = 3, goal = "Cut")
        assertTrue(cut < maintain)
    }

    @Test
    fun `Bulk goal is above maintenance TDEE`() {
        val maintain = HealthCalculator.calculateTdee(30, 75.0, 180.0, true, workoutDays = 3, goal = "Maintain")
        val bulk = HealthCalculator.calculateTdee(30, 75.0, 180.0, true, workoutDays = 3, goal = "Bulk")
        assertTrue(bulk > maintain)
    }

    @Test
    fun `higher workoutDays does not decrease TDEE for same inputs`() {
        val low = HealthCalculator.calculateTdee(28, 70.0, 175.0, false, workoutDays = 2, goal = "Maintain")
        val high = HealthCalculator.calculateTdee(28, 70.0, 175.0, false, workoutDays = 6, goal = "Maintain")
        assertTrue(high >= low)
    }
}
