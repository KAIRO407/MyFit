package com.example.myfit.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_steps")
data class StepEntity(
    @PrimaryKey val date: String,
    val steps: Int
)
