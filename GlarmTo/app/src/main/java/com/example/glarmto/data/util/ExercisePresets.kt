package com.example.glarmto.data.util

object ExercisePresets {
    val categories = mapOf(
        "Chest" to listOf("Bench Press", "Incline DB Press", "Chest Fly", "Pushups", "Dips"),
        "Back" to listOf("Deadlift", "Lat Pulldown", "Bent Over Row", "Pullups", "Seated Cable Row"),
        "Legs" to listOf("Squat", "Leg Press", "Leg Extension", "Leg Curl", "Lunges"),
        "Shoulders" to listOf("Overhead Press", "Lateral Raise", "Front Raise", "Face Pulls"),
        "Arms" to listOf("Bicep Curl", "Tricep Pushdown", "Hammer Curl", "Skull Crusher")
    )

    val allExercises = categories.values.flatten().sorted()
}
