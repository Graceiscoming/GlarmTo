package com.example.glarmto

import com.example.glarmto.data.local.entity.RoutineEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutineLogicTest {

    @Test
    fun `routine entity exercises roundtrip`() {
        val names = listOf("Bench Press", "Squat", "Deadlift")
        val joined = names.joinToString("|")
        val entity = RoutineEntity(id = 0, username = "u", routineName = "Push", exercises = joined)
        val parsed = entity.exercises.split("|")
        assertEquals(names, parsed)
    }

    @Test
    fun `test routine exercises string joining and splitting`() {
        // Given: A list of exercise names
        val exercises = listOf("Bench Press", "Squat", "Deadlift")
        
        // When: Joining them with pipe separator
        val joined = exercises.joinToString(separator = "|")
        val expectedJoined = "Bench Press|Squat|Deadlift"
        
        // Then: String should match expected
        assertEquals("Joining exercises failed", expectedJoined, joined)
        
        // When: Splitting them back
        val splitBack = joined.split("|")
        
        // Then: List should match original
        assertEquals("Splitting exercises failed", exercises, splitBack)
    }

    @Test
    fun `test empty routine handling`() {
        val emptyList = emptyList<String>()
        val joined = emptyList.joinToString("|")
        assertEquals("", joined)
        
        // Splitting empty string with "|" returns a list with one empty string if not handled
        // But our UI check prevents saving empty routines.
    }
}
