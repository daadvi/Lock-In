package com.example.data.model

import java.io.Serializable

enum class RequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}

data class FriendAppUsage(
    val packageName: String,
    val appName: String,
    val usageSeconds: Long
) : Serializable

data class PartyMember(
    val userId: String,
    val displayName: String,
    val isHost: Boolean = false,
    val screenTimeSecondsToday: Long = 0L,
    val screenTimeLimitMinutes: Int = 180,
    val topApps: List<FriendAppUsage> = emptyList(),
    val respectPercentage: Int = 100, // 0 - 100% adherence to screen time and app limits
    val respectMinutesToday: Int = 180, // Daily respect time
    val streakDays: Int = 0,
    val violationsToday: Int = 0,
    val lastActiveEpoch: Long = System.currentTimeMillis(),
    val isOnline: Boolean = true,
    val shareDetailedScreenTime: Boolean = true // Whether allies in Friends Panel can see absolute screen time and per-app usage
) : Serializable

data class PartyAlert(
    val id: String = java.util.UUID.randomUUID().toString(),
    val memberName: String,
    val appName: String = "",
    val alertType: String = "OVER_LIMIT", // "OVER_LIMIT", "CHEER", "NUDGE"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

data class InstallApprovalRequest(
    val requestId: String,
    val requesterId: String,
    val requesterName: String,
    val targetPackage: String,
    val targetAppName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: RequestStatus = RequestStatus.PENDING,
    val resolvedById: String? = null,
    val resolvedByName: String? = null,
    val resolvedEpoch: Long? = null
) : Serializable

data class LeaveApprovalRequest(
    val requestId: String,
    val requesterId: String,
    val requesterName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: RequestStatus = RequestStatus.PENDING,
    val resolvedById: String? = null,
    val resolvedByName: String? = null,
    val resolvedEpoch: Long? = null
) : Serializable

data class PartyState(
    val partyCode: String,
    val partyName: String = "Lock In Party",
    val hostUserId: String,
    val members: List<PartyMember> = emptyList(),
    val pendingInstallRequests: List<InstallApprovalRequest> = emptyList(),
    val pendingLeaveRequests: List<LeaveApprovalRequest> = emptyList(),
    val alerts: List<PartyAlert> = emptyList(),
    val createdEpoch: Long = System.currentTimeMillis()
) : Serializable
