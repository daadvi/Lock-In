package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "restricted_websites")
data class RestrictedWebsiteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val linkedPackageName: String? = null,
    val limitMinutes: Int = 15,
    val remainingSeconds: Long = 15 * 60L,
    val isBlocked: Boolean = false
)
