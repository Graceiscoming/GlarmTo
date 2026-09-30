package com.example.glarmto

import com.example.glarmto.data.util.HealthCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Consolidated Unit Test for Login, Profile calculation and Health logic.
 */
class LoginSetupTest {

    @Test
    fun `test male TDEE calculation is correct`() {
        // Given: 25 years old male, 70kg, 175cm
        val age = 25
        val weight = 70.0
        val height = 175.0
        val isMale = true

        // BMR = (10 * 70) + (6.25 * 175) - (5 * 25) + 5 = 1673.75
        // Default workoutDays = 3 → activity multiplier 1.375 (see HealthCalculator)
        // TDEE = 1673.75 * 1.375 = 2301.40625 → 2301
        val expectedTdee = 2301

        val actualTdee = HealthCalculator.calculateTdee(age, weight, height, isMale)
        
        assertEquals("Male TDEE calculation failed", expectedTdee, actualTdee)
    }

    @Test
    fun `test female TDEE calculation is correct`() {
        // Given: 30 years old female, 60kg, 160cm
        val age = 30
        val weight = 60.0
        val height = 160.0
        val isMale = false

        // BMR = 1289; default workoutDays = 3 → multiplier 1.375
        // TDEE = 1289 * 1.375 = 1772.375 → 1772
        val expectedTdee = 1772

        val actualTdee = HealthCalculator.calculateTdee(age, weight, height, isMale)
        
        assertEquals("Female TDEE calculation failed", expectedTdee, actualTdee)
    }

    @Test
    fun `test calculation with invalid inputs returns default`() {
        // Given invalid inputs, it should return a safe default (e.g., 2000)
        assertEquals(2000, HealthCalculator.calculateTdee(0, 70.0, 175.0, true))
        assertEquals(2000, HealthCalculator.calculateTdee(25, -10.0, 175.0, true))
        assertEquals(2000, HealthCalculator.calculateTdee(25, 70.0, 0.0, true))
    }
}
