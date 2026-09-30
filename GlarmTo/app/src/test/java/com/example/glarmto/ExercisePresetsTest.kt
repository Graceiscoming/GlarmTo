package com.example.glarmto

import com.example.glarmto.data.util.ExercisePresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExercisePresetsTest {

    @Test
    fun `allExercises is sorted and non-empty`() {
        val all = ExercisePresets.allExercises
        assertTrue(all.isNotEmpty())
        assertEquals(all.sorted(), all)
    }

    @Test
    fun `allExercises has no duplicates`() {
        val all = ExercisePresets.allExercises
        assertEquals(all.size, all.distinct().size)
    }

    @Test
    fun `categories cover known muscle groups`() {
        assertTrue(ExercisePresets.categories.containsKey("Chest"))
        assertTrue(ExercisePresets.categories.containsKey("Back"))
        assertTrue(ExercisePresets.categories.containsKey("Legs"))
    }
}
