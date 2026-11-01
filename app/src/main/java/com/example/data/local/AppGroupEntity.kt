package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "app_groups")
data class AppGroupEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val packageNamesCsv: String, // Comma-separated package names
    val limitMinutes: Int = 15,
    val remainingSeconds: Long = 15 * 60L,
    val isBlocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getPackageList(): List<String> {
        return packageNamesCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun containsPackage(packageName: String): Boolean {
        return getPackageList().any { it.equals(packageName, ignoreCase = true) }
    }
}
