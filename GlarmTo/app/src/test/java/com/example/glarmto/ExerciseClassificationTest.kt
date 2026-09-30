package com.example.glarmto

import com.example.glarmto.data.util.ExerciseLibrary
import com.example.glarmto.data.util.MuscleBalance
import com.example.glarmto.data.util.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseClassificationTest {

    @Test
    fun `every library exercise is classified to its own muscle`() {
        ExerciseLibrary.exercises.forEach { def ->
            assertEquals(def.name, def.primaryMuscle, ExerciseLibrary.classify(def.name))
        }
    }

    @Test
    fun `keyword fallback alone agrees with the library for every exercise`() {
        // The keyword path is what custom-typed names go through, so it must get the library right on its own.
        ExerciseLibrary.exercises.forEach { def ->
            assertEquals("keyword guess for '${def.name}'", def.primaryMuscle, ExerciseLibrary.keywordMuscle(def.name))
        }
    }

    @Test
    fun `the names the old dashboard got wrong are now right`() {
        assertEquals(MuscleGroup.Shoulders, ExerciseLibrary.classify("Overhead Press"))
        assertEquals(MuscleGroup.Shoulders, ExerciseLibrary.classify("Arnold Press"))
        assertEquals(MuscleGroup.Chest, ExerciseLibrary.classify("Incline DB Press"))
        assertEquals(MuscleGroup.Shoulders, ExerciseLibrary.classify("Lateral Raise"))
        assertEquals(MuscleGroup.Shoulders, ExerciseLibrary.classify("Face Pulls"))
        assertEquals(MuscleGroup.Arms, ExerciseLibrary.classify("Tricep Pushdown"))
        assertEquals(MuscleGroup.Core, ExerciseLibrary.classify("Leg Raises"))
        assertEquals(MuscleGroup.Back, ExerciseLibrary.classify("Deadlift"))
        assertEquals(MuscleGroup.Chest, ExerciseLibrary.classify("Dips"))
        assertEquals(MuscleGroup.Legs, ExerciseLibrary.classify("Lunges"))
    }

    @Test
    fun `matching ignores case spacing and punctuation`() {
        assertEquals(MuscleGroup.Chest, ExerciseLibrary.classify("  bench press "))
        assertEquals(MuscleGroup.Chest, ExerciseLibrary.classify("BENCH-PRESS"))
        assertEquals(MuscleGroup.Back, ExerciseLibrary.classify("Pull-ups"))
        assertEquals(MuscleGroup.Chest, ExerciseLibrary.classify("Push-up"))
    }

    @Test
    fun `custom names are guessed from keywords`() {
        assertEquals(MuscleGroup.Chest, ExerciseLibrary.classify("Machine Chest Press"))
        assertEquals(MuscleGroup.Shoulders, ExerciseLibrary.classify("Dumbbell Shoulder Press"))
        assertEquals(MuscleGroup.Shoulders, ExerciseLibrary.classify("Rear Delt Fly"))
        assertEquals(MuscleGroup.Legs, ExerciseLibrary.classify("Romanian Deadlift"))
        assertEquals(MuscleGroup.Legs, ExerciseLibrary.classify("Hack Squat"))
        assertEquals(MuscleGroup.Legs, ExerciseLibrary.classify("Seated Calf Raise"))
        assertEquals(MuscleGroup.Arms, ExerciseLibrary.classify("Preacher Curl"))
        assertEquals(MuscleGroup.Arms, ExerciseLibrary.classify("Cable Tricep Kickback"))
        assertEquals(MuscleGroup.Back, ExerciseLibrary.classify("T-Bar Row"))
        assertEquals(MuscleGroup.Back, ExerciseLibrary.classify("Chin Up"))
        assertEquals(MuscleGroup.Core, ExerciseLibrary.classify("Hanging Knee Raise"))
        assertEquals(MuscleGroup.Core, ExerciseLibrary.classify("Sit Ups"))
    }

    @Test
    fun `specific rules beat generic ones`() {
        assertEquals("leg curl is legs, not arms", MuscleGroup.Legs, ExerciseLibrary.keywordMuscle("Seated Leg Curl"))
        assertEquals("calf raise is legs, not shoulders", MuscleGroup.Legs, ExerciseLibrary.keywordMuscle("Standing Calf Raise"))
        assertEquals("leg raise is core, not legs", MuscleGroup.Core, ExerciseLibrary.keywordMuscle("Hanging Leg Raise"))
        assertEquals("overhead tricep is arms, not shoulders", MuscleGroup.Arms, ExerciseLibrary.keywordMuscle("Overhead Tricep Extension"))
        assertEquals("lateral raise is shoulders, not back", MuscleGroup.Shoulders, ExerciseLibrary.keywordMuscle("Lateral Raise"))
    }

    @Test
    fun `unknown and blank names are not guessed`() {
        assertNull(ExerciseLibrary.classify("Zumba"))
        assertNull(ExerciseLibrary.classify(""))
        assertNull(ExerciseLibrary.classify("   "))
        assertNull(ExerciseLibrary.classify("@@@ !!!"))
    }

    // ---- radar ----

    @Test
    fun `radar always has the five axes at zero when there is no data`() {
        val scores = MuscleBalance.radarScores(emptyList())

        assertEquals(listOf("Chest", "Back", "Legs", "Arms", "Shoulders"), scores.keys.toList())
        assertEquals(setOf(0f), scores.values.toSet())
    }

    @Test
    fun `radar scales the busiest axis to 1`() {
        val scores = MuscleBalance.radarScores(
            listOf("Bench Press", "Bench Press", "Bench Press", "Bench Press", "Squat", "Squat", "Bicep Curl")
        )

        assertEquals(1f, scores.getValue("Chest"), 0.0001f)
        assertEquals(0.5f, scores.getValue("Legs"), 0.0001f)
        assertEquals(0.25f, scores.getValue("Arms"), 0.0001f)
        assertEquals(0f, scores.getValue("Back"), 0.0001f)
        assertEquals(0f, scores.getValue("Shoulders"), 0.0001f)
    }

    @Test
    fun `radar counts the exercises the old version miscounted`() {
        val scores = MuscleBalance.radarScores(listOf("Overhead Press", "Lateral Raise", "Incline DB Press", "Tricep Pushdown"))

        assertEquals(1f, scores.getValue("Shoulders"), 0.0001f)
        assertEquals(0.5f, scores.getValue("Chest"), 0.0001f)
        assertEquals(0.5f, scores.getValue("Arms"), 0.0001f)
        assertEquals("nothing here is a leg or back exercise", 0f, scores.getValue("Legs"), 0.0001f)
        assertEquals(0f, scores.getValue("Back"), 0.0001f)
    }

    @Test
    fun `radar ignores core and unknown exercises`() {
        val scores = MuscleBalance.radarScores(listOf("Plank", "Crunch", "Zumba", "Deadlift"))

        assertEquals(1f, scores.getValue("Back"), 0.0001f)
        assertEquals(0f, scores.getValue("Chest"), 0.0001f)
        assertEquals("only Deadlift counts", 1f, scores.values.sum(), 0.0001f)
    }

    @Test
    fun `radar scores stay within 0 and 1`() {
        val names = ExerciseLibrary.exercises.map { it.name } + listOf("Bench Press", "Squat", "Squat")
        MuscleBalance.radarScores(names).values.forEach { assertEquals(true, it in 0f..1f) }
    }
}
