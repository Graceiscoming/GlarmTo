package com.example.glarmto

import com.example.glarmto.data.util.Equipment
import com.example.glarmto.data.util.MuscleGroup
import com.example.glarmto.data.util.WorkoutGenerator
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class WorkoutGeneratorDeterminismTest {

    private fun generate(seed: Int) = WorkoutGenerator.generateWorkout(
        availableTimeMins = 60,
        equipmentConstraints = listOf(Equipment.Barbell, Equipment.Dumbbell),
        focusMuscles = listOf(MuscleGroup.Chest, MuscleGroup.Back),
        now = 1_000_000L,
        random = Random(seed)
    )

    @Test
    fun `same seed gives the same workout`() {
        assertEquals(generate(42), generate(42))
    }
}
