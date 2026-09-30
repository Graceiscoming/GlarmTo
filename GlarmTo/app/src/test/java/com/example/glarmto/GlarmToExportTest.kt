package com.example.glarmto

import com.example.glarmto.data.local.entity.NutritionEntity
import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.local.entity.WaterEntity
import com.example.glarmto.data.local.entity.WorkoutEntity
import com.example.glarmto.data.util.GlarmToExport
import com.example.glarmto.data.util.PasswordHasher
import org.junit.Assert.assertFalse
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

    @Test
    fun `toCsv quotes nutrition food names containing commas and quotes`() {
        val csv = GlarmToExport.toCsv(
            workouts = emptyList(),
            nutrition = listOf(NutritionEntity(1, "Rice, \"fried\"", 300, 2L, "u")),
            water = emptyList()
        )
        assertTrue(csv.contains("nutrition,1,u,2,\"Rice, \"\"fried\"\";300\""))
    }

    @Test
    fun `toCsv quotes usernames containing commas in water rows`() {
        val csv = GlarmToExport.toCsv(
            workouts = emptyList(),
            nutrition = emptyList(),
            water = listOf(WaterEntity(1, "a,b", 3L, 250))
        )
        assertTrue(csv.contains("water,1,\"a,b\",3,250"))
    }

    @Test
    fun `toJson never includes the password or its hash`() {
        val user = UserEntity(username = "u1", password = PasswordHasher.hash("topsecret", iterations = 2))

        val json = GlarmToExport.toJson("u1", user, emptyList(), emptyList(), emptyList())

        assertFalse(json.contains("topsecret"))
        assertFalse(json.contains("pbkdf2"))
        assertFalse(json.lowercase().contains("password"))
    }
}
