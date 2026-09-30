package com.example.glarmto.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_log")
data class WaterEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val dateInMillis: Long,
    val amountMl: Int
)
