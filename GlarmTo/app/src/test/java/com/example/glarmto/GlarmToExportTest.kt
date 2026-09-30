package com.example.glarmto

import com.example.glarmto.data.local.entity.NutritionEntity
import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.local.entity.WaterEntity
import com.example.glarmto.data.local.entity.WorkoutEntity
import com.example.glarmto.data.util.GlarmToExport
import org.junit.Assert.assertTrue
import org.junit.Test

class GlarmToExportTest {

    @Test
    fun `toJson contains version username and arrays`() {
        val user = UserEntity(username = "u1", dailyGoal = 2000)
        val w = WorkoutEntity(
            id = 1,
            exerciseName = "Bench",
            weight = 60.0,
            reps = 8,
            dateInMillis = 100L,
            username = "u1",
            sessionId = null,
            rpe = 8
        )
        val json = GlarmToExport.toJson("u1", user, listOf(w), emptyList(), emptyList())
        assertTrue(json.contains("\"exportVersion\": 1"))
        assertTrue(json.contains("\"username\": \"u1\""))
        assertTrue(json.contains("\"exerciseName\": \"Bench\""))
        assertTrue(json.contains("\"rpe\": 8"))
        assertTrue(json.contains("\"workouts\": ["))
    }

    @Test
    fun `toCsv contains header and rows`() {
        val csv = GlarmToExport.toCsv(
            workouts = listOf(
                WorkoutEntity(1, "S", 50.0, 10, 1L, "u", null, 7)
            ),
            nutrition = listOf(NutritionEntity(1, "Rice", 300, 2L, "u")),
            water = listOf(WaterEntity(1, "u", 3L, 250))
        )
        assertTrue(csv.contains("# GlarmTo export"))
        assertTrue(csv.contains("workout,"))
        assertTrue(csv.contains("nutrition,"))
        assertTrue(csv.contains("water,"))
        assertTrue(csv.contains("S;50.0;10"))
    }
}
