package com.example.glarmto.data.util

/** Muscle balance numbers for the dashboard radar chart. */
object MuscleBalance {
    /** Radar axes, in display order. Core is tracked by [ExerciseLibrary] but is not a radar axis. */
    val RADAR_AXES = listOf(MuscleGroup.Chest, MuscleGroup.Back, MuscleGroup.Legs, MuscleGroup.Arms, MuscleGroup.Shoulders)

    /**
     * Sets per radar axis for the given logged exercise names, scaled so the busiest axis is 1f.
     * Every axis is always present (0f when untouched). Exercises that can't be placed are ignored.
     */
    fun radarScores(exerciseNames: List<String>): Map<String, Float> {
        val counts = RADAR_AXES.associateWith { 0 }.toMutableMap()
        exerciseNames.forEach { name ->
            val muscle = ExerciseLibrary.classify(name) ?: return@forEach
            if (muscle in counts) counts[muscle] = counts.getValue(muscle) + 1
        }
        val max = counts.values.maxOrNull()?.coerceAtLeast(1) ?: 1
        return RADAR_AXES.associate { it.name to counts.getValue(it).toFloat() / max }
    }
}
