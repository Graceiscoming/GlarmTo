package com.example.glarmto

import com.example.glarmto.data.local.entity.WorkoutEntity
import com.example.glarmto.data.util.MuscleGroup
import com.example.glarmto.data.util.RecoveryCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class RecoveryCalculatorTest {

    private val hour = 60L * 60 * 1000
    private val now = 1_000L * hour

    private fun benchSet(at: Long) = WorkoutEntity(0, "Bench Press", 60.0, 8, at, "u", null, null)

    @Test
    fun `no workouts means full recovery`() {
        val result = RecoveryCalculator.calculate(emptyList(), now)

        MuscleGroup.values().forEach { assertEquals(1f, result[it]!!, 0.0001f) }
    }

    @Test
    fun `a recent set costs 15 percent minus the time already recovered`() {
        val result = RecoveryCalculator.calculate(listOf(benchSet(now - hour)), now)

        // 0.15 damage, 1h of 48h recovered
        assertEquals(1f - (0.15f - 1f / 48f), result[MuscleGroup.Chest]!!, 0.001f)
    }

    @Test
    fun `sets older than the recovery window do not add fatigue`() {
        val oldSets = List(5) { benchSet(now - 60 * hour) }
        val result = RecoveryCalculator.calculate(oldSets + benchSet(now - hour), now)

        // Only the one recent set counts, same as the single-set case.
        assertEquals(1f - (0.15f - 1f / 48f), result[MuscleGroup.Chest]!!, 0.001f)
    }

    @Test
    fun `only old history leaves every muscle fully recovered`() {
        val result = RecoveryCalculator.calculate(List(10) { benchSet(now - 400 * hour) }, now)

        assertEquals(1f, result[MuscleGroup.Chest]!!, 0.0001f)
    }
}
