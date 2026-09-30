package com.example.glarmto.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nutrition_log")
data class NutritionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val foodName: String,
    val calories: Int,
    val dateInMillis: Long,
    val username: String = "admin",
    /** XP this entry actually earned (0 if none), so deleting it takes back only that much. */
    val xpAwarded: Int = 0
)
