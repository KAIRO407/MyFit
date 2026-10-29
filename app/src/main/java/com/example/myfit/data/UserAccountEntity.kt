package com.example.myfit.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_account")
data class UserAccountEntity(
    @PrimaryKey val id: Int = 1,
    val username: String,
    val avatar: String,
    val level: Int = 1,
    val xp: Int = 0,
    val coins: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
