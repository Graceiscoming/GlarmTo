package com.example.glarmto.data.util

import androidx.annotation.StringRes
import com.example.glarmto.R

/**
 * Display text for values that are stored or matched by their English name (muscle groups, equipment,
 * goals). The stored value never changes with the language; only what the user reads does.
 */
@StringRes
fun MuscleGroup.labelRes(): Int = when (this) {
    MuscleGroup.Chest -> R.string.muscle_chest
    MuscleGroup.Back -> R.string.muscle_back
    MuscleGroup.Legs -> R.string.muscle_legs
    MuscleGroup.Shoulders -> R.string.muscle_shoulders
    MuscleGroup.Arms -> R.string.muscle_arms
    MuscleGroup.Core -> R.string.muscle_core
}

@StringRes
fun Equipment.labelRes(): Int = when (this) {
    Equipment.Barbell -> R.string.equipment_barbell
    Equipment.Dumbbell -> R.string.equipment_dumbbell
    Equipment.Machine -> R.string.equipment_machine
    Equipment.Cable -> R.string.equipment_cable
    Equipment.Bodyweight -> R.string.equipment_bodyweight
    Equipment.None -> R.string.equipment_none
}

object Goals {
    const val CUT = "Cut"
    const val MAINTAIN = "Maintain"
    const val BULK = "Bulk"
    val all = listOf(CUT, MAINTAIN, BULK)

    /** The text for a stored goal, or null for a value this version doesn't know. */
    @StringRes
    fun labelRes(goal: String): Int? = when (goal) {
        CUT -> R.string.goal_cut
        MAINTAIN -> R.string.goal_maintain
        BULK -> R.string.goal_bulk
        else -> null
    }
}
