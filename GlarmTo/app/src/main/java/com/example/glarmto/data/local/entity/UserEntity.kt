package com.example.glarmto.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_log")
data class UserEntity(
    @PrimaryKey val username: String,
    val password: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val age: Int = 0,
    val isMale: Boolean = true,
    val weight: Double = 0.0,
    val height: Double = 0.0,
    val dailyGoal: Int = 2500,
    val profileSetup: Boolean = false,
    val xp: Long = 0,
    val level: Int = 1,
    val defaultRestSeconds: Int = 60,
    val dailyXPEarned: Long = 0,
    val lastXPDate: Long = 0,
    val goal: String = "Maintain",
    val workoutDays: Int = 3,
    /** Macro split (% of calories); should sum to 100 when possible. */
    val macroProteinPct: Int = 30,
    val macroCarbPct: Int = 40,
    val macroFatPct: Int = 30,
    /** Daily water intake goal in milliliters. */
    val dailyWaterGoalMl: Int = 2000
)
