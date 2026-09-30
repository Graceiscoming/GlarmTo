package com.example.glarmto

import com.example.glarmto.testsupport.TestTexts
import com.example.glarmto.data.util.Equipment
import com.example.glarmto.data.util.ExerciseDef
import com.example.glarmto.data.util.ExerciseHistoryStats
import com.example.glarmto.data.util.MuscleGroup
import com.example.glarmto.data.util.WorkoutGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class ProgressiveOverloadTest {

    private val bench = ExerciseDef("Bench Press", MuscleGroup.Chest, Equipment.Barbell)
    private val curl = ExerciseDef("Dumbbell Curl", MuscleGroup.Arms, Equipment.Dumbbell)

    private fun history(weight: Double, reps: Int) =
        ExerciseHistoryStats(lastWeight = weight, lastReps = reps, maxWeight = weight, lastPerformedMillis = 0L)

    @Test
    fun `light barbell weight still increases when rep range top is hit`() {
        val (weight, note) = WorkoutGenerator.progressiveOverload(bench, "8-12", history(40.0, 12), TestTexts.english)

        assertEquals(42.5, weight!!, 0.001)
        assertFalse(note!!.contains("+0.0kg"))
    }

    @Test
    fun `light dumbbell weight still increases when rep range top is hit`() {
        val (weight, _) = WorkoutGenerator.progressiveOverload(curl, "8-12", history(10.0, 12), TestTexts.english)

        assertEquals(11.25, weight!!, 0.001)
    }

    @Test
    fun `heavy weight keeps the percentage based bump`() {
        val (weight, _) = WorkoutGenerator.progressiveOverload(bench, "8-12", history(100.0, 12), TestTexts.english)

        assertEquals(102.5, weight!!, 0.001)
    }

    @Test
    fun `bodyweight exercise gets no weight suggestion or zero kg deload note`() {
        val pushups = ExerciseDef("Pushups", MuscleGroup.Chest, Equipment.Bodyweight)
        val stalled = ExerciseHistoryStats(
            lastWeight = 0.0,
            lastReps = 12,
            maxWeight = 0.0,
            lastPerformedMillis = 0L,
            recentSets = listOf(0.0 to 12, 0.0 to 12, 0.0 to 12)
        )

        val (weight, note) = WorkoutGenerator.progressiveOverload(pushups, "15-20", stalled, TestTexts.english)

        assertEquals(null, weight)
        assertFalse(note!!.contains("0.0kg"))
    }

    @Test
    fun `weighted bodyweight exercise still uses the weight rules`() {
        val dips = ExerciseDef("Dips", MuscleGroup.Chest, Equipment.Bodyweight)

        val (weight, _) = WorkoutGenerator.progressiveOverload(dips, "15-20", history(10.0, 20), TestTexts.english)

        assertEquals(11.25, weight!!, 0.001)
    }

    @Test
    fun `no bump when rep range top was not hit`() {
        val (weight, note) = WorkoutGenerator.progressiveOverload(bench, "8-12", history(40.0, 9), TestTexts.english)

        assertEquals(40.0, weight!!, 0.001)
        assertNotNull(note)
    }
}
