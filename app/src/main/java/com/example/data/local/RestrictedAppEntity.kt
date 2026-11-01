package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "restricted_apps")
data class RestrictedAppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isLimited: Boolean = true,
    val isShortVideoPlatform: Boolean = false,
    val limitShortVideosOnly: Boolean = false,
    val limitLongVideosAlso: Boolean = true,
    val individualLimitMinutes: Int = 15,
    val individualRemainingSeconds: Long = 15 * 60L,
    val shortVideoLimitMinutes: Int = 10,
    val shortVideoRemainingSeconds: Long = 10 * 60L,
    val longVideoLimitMinutes: Int = 25,
    val longVideoRemainingSeconds: Long = 25 * 60L,
    val isBlocked: Boolean = false,
    val useAbsoluteTime: Boolean = true,
    val hasVideoContent: Boolean = false,
    val hasShortVideoContent: Boolean = false,
    val associatedWebsites: String = "",
    val lastUsedTimestamp: Long = 0L
) {
    val totalAvailableSeconds: Long
        get() {
            if (!isLimited) return 0L
            if (useAbsoluteTime) return individualRemainingSeconds.coerceAtLeast(0L)
            val hasShort = (hasShortVideoContent || isShortVideoPlatform) && shortVideoLimitMinutes > 0
            val hasLong = (hasVideoContent || limitLongVideosAlso) && longVideoLimitMinutes > 0
            return when {
                hasShort && hasLong -> (shortVideoRemainingSeconds + longVideoRemainingSeconds).coerceAtLeast(0L)
                hasShort -> shortVideoRemainingSeconds.coerceAtLeast(0L)
                hasLong -> longVideoRemainingSeconds.coerceAtLeast(0L)
                else -> individualRemainingSeconds.coerceAtLeast(0L)
            }
        }

    val totalLimitMinutes: Int
        get() {
            if (useAbsoluteTime) return individualLimitMinutes
            val hasShort = (hasShortVideoContent || isShortVideoPlatform) && shortVideoLimitMinutes > 0
            val hasLong = (hasVideoContent || limitLongVideosAlso) && longVideoLimitMinutes > 0
            return when {
                hasShort && hasLong -> shortVideoLimitMinutes + longVideoLimitMinutes
                hasShort -> shortVideoLimitMinutes
                hasLong -> longVideoLimitMinutes
                else -> individualLimitMinutes
            }
        }

    val isLimitDepleted: Boolean
        get() {
            if (!isLimited) return false
            if (totalLimitMinutes <= 0) return false
            if (isBlocked) return true
            return if (useAbsoluteTime) {
                individualLimitMinutes > 0 && individualRemainingSeconds <= 0L
            } else {
                val hasShortRule = (hasShortVideoContent || isShortVideoPlatform) && shortVideoLimitMinutes > 0
                val hasLongRule = (hasVideoContent || limitLongVideosAlso) && longVideoLimitMinutes > 0
                val shortDepleted = hasShortRule && shortVideoRemainingSeconds <= 0L
                val longDepleted = hasLongRule && longVideoRemainingSeconds <= 0L

                when {
                    hasShortRule && hasLongRule -> shortDepleted && longDepleted
                    hasShortRule -> shortDepleted
                    hasLongRule -> longDepleted
                    else -> individualLimitMinutes > 0 && individualRemainingSeconds <= 0L
                }
            }
        }

    val isUsageInProgress: Boolean
        get() {
            if (!isLimited) return false
            if (isLimitDepleted) return false
            val totalLimitSec = totalLimitMinutes * 60L
            if (totalLimitSec <= 0L) return false
            val remainingSec = totalAvailableSeconds
            return remainingSec in 1 until totalLimitSec
        }
}
