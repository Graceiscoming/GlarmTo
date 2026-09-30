package com.example.glarmto

import com.example.glarmto.data.util.LevelMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelMathTest {

    @Test
    fun `zero and negative xp is level 1`() {
        assertEquals(1, LevelMath.levelForXp(0))
        assertEquals(1, LevelMath.levelForXp(-50))
    }

    @Test
    fun `level 1 threshold is zero and thresholds strictly increase`() {
        assertEquals(0L, LevelMath.totalXpThreshold(1))
        assertEquals(0L, LevelMath.totalXpThreshold(0))
        for (level in 1..80) {
            assertTrue(
                "threshold($level) must be below threshold(${level + 1})",
                LevelMath.totalXpThreshold(level) < LevelMath.totalXpThreshold(level + 1)
            )
        }
    }

    @Test
    fun `reaching a threshold exactly gives that level - regression for the 210 xp bug`() {
        // 210 XP is the level 3 threshold; the old log-based formula returned level 2 here.
        assertEquals(210L, LevelMath.totalXpThreshold(3))
        assertEquals(3, LevelMath.levelForXp(210))
        for (level in 2..80) {
            val threshold = LevelMath.totalXpThreshold(level)
            assertEquals("exactly at threshold of level $level", level, LevelMath.levelForXp(threshold))
        }
    }

    @Test
    fun `one xp below a threshold is still the previous level`() {
        for (level in 2..80) {
            val threshold = LevelMath.totalXpThreshold(level)
            assertEquals("one below threshold of level $level", level - 1, LevelMath.levelForXp(threshold - 1))
        }
    }

    @Test
    fun `level never decreases as xp grows`() {
        var previous = 1
        for (xp in 0L..20_000L) {
            val level = LevelMath.levelForXp(xp)
            assertTrue("level went down at $xp xp", level >= previous)
            previous = level
        }
    }

    @Test
    fun `level agrees with the thresholds for every xp value`() {
        for (xp in 0L..20_000L) {
            val level = LevelMath.levelForXp(xp)
            assertTrue("xp $xp should have reached level $level", xp >= LevelMath.totalXpThreshold(level))
            assertTrue("xp $xp should be below level ${level + 1}", xp < LevelMath.totalXpThreshold(level + 1))
        }
    }

    @Test
    fun `first levels match the documented curve`() {
        assertEquals(1, LevelMath.levelForXp(99))
        assertEquals(2, LevelMath.levelForXp(100))
        assertEquals(2, LevelMath.levelForXp(209))
        assertEquals(3, LevelMath.levelForXp(210))
        assertEquals(3, LevelMath.levelForXp(330))
        assertEquals(4, LevelMath.levelForXp(331))
    }

    @Test
    fun `xp needed for next level matches the gap between thresholds within rounding`() {
        for (level in 1..40) {
            val gap = LevelMath.totalXpThreshold(level + 1) - LevelMath.totalXpThreshold(level)
            val needed = LevelMath.xpRequiredForNextLevel(level)
            assertTrue("level $level: gap=$gap needed=$needed", kotlin.math.abs(gap - needed) <= 1)
        }
    }

    @Test
    fun `very large xp does not loop forever or overflow`() {
        val level = LevelMath.levelForXp(Int.MAX_VALUE.toLong())
        assertTrue(level in 100..300)
    }
}
