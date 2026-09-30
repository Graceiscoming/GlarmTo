package com.example.glarmto

import com.example.glarmto.data.util.HealthCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MacroGramsTest {

    @Test
    fun `macroGramsFromCalories splits 30-40-30 at 2000 kcal`() {
        val (p, c, f) = HealthCalculator.macroGramsFromCalories(2000, 30, 40, 30)
        assertEquals(150, p)
        assertEquals(200, c)
        assertEquals(67, f)
    }

    @Test
    fun `macroGramsFromCalories normalizes when pct sum is not 100`() {
        val (p, c, f) = HealthCalculator.macroGramsFromCalories(2000, 20, 20, 20)
        assertTrue(p > 0 && c > 0 && f > 0)
    }

    @Test
    fun `macroGramsFromCalories returns zeros for non positive calories`() {
        val (p, c, f) = HealthCalculator.macroGramsFromCalories(0, 30, 40, 30)
        assertEquals(0, p)
        assertEquals(0, c)
        assertEquals(0, f)
    }
}
