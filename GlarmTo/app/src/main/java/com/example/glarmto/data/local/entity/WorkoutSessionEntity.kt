package com.example.glarmto.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val sessionId: Int = 0,
    val startTimeInMillis: Long,
    val endTimeInMillis: Long? = null,
    val durationSeconds: Long = 0,
    val dateInMillis: Long,
    val username: String = "admin",
    val sessionName: String = "",
    val notes: String = "",
    val exhaustionLevel: Int = 0,
    val satisfactionLevel: Int = 0
)
