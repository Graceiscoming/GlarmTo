package com.example.glarmto.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_log")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val exerciseName: String,
    val weight: Double,
    val reps: Int,
    val dateInMillis: Long,
    val username: String = "admin",
    val sessionId: Int? = null,
    /** Rate of perceived exertion (1–10), optional. */
    val rpe: Int? = null
)
