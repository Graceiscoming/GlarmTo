package com.example.glarmto

import com.example.glarmto.data.util.PlateCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlateCalculatorTest {

    @Test
    fun `exact load with standard plates`() {
        val plates = listOf(20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
        val r = PlateCalculator.computeLoad(targetTotalKg = 100.0, barKg = 20.0, plateSizesKg = plates)
        assertNotNull(r)
        assertTrue(PlateCalculator.isGoodEnough(r!!))
        assertEquals(40.0, r.weightPerSideKg, 0.01)
    }

    @Test
    fun `bar only target`() {
        val r = PlateCalculator.computeLoad(20.0, 20.0, listOf(10.0, 5.0))
        assertNotNull(r)
        assertEquals(0.0, r!!.weightPerSideKg, 0.001)
        assertTrue(r.platesPerSide.isEmpty())
    }

    @Test
    fun `invalid target below bar returns null`() {
        assertEquals(null, PlateCalculator.computeLoad(10.0, 20.0, listOf(5.0)))
    }
}
