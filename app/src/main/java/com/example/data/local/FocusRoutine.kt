package com.example.data.local

import java.util.UUID

data class FocusRoutine(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // 1=Mon, 7=Sun
    val allowedPackages: List<String> = emptyList(), // Up to 3 emergency apps (for device-wide focus)
    val isEnabled: Boolean = true,
    val targetPackage: String? = null, // Legacy single app (null = device-wide focus)
    val targetAppName: String? = null, // Legacy single app display name
    val targetPackages: List<String> = emptyList(), // Apps blocked by THIS routine (1..N)
    val targetAppNames: List<String> = emptyList() // Display names parallel to targetPackages
) {
    // Resolved lists absorb the legacy single-app routines persisted before multi-app support.
    val resolvedTargetPackages: List<String>
        get() = if (targetPackages.isNotEmpty()) targetPackages else listOfNotNull(targetPackage).filter { it.isNotBlank() }

    val resolvedTargetNames: List<String>
        get() = if (targetAppNames.isNotEmpty()) targetAppNames else listOfNotNull(targetAppName).filter { it.isNotBlank() }

    val isAppSpecific: Boolean
        get() = resolvedTargetPackages.isNotEmpty()

    fun formatTimeRange(): String {
        return String.format("%02d:%02d → %02d:%02d", startHour, startMinute, endHour, endMinute)
    }

    fun formatDays(): String {
        if (daysOfWeek.size == 7) return "Todos os dias"
        val sorted = daysOfWeek.sorted()
        if (sorted == listOf(1, 2, 3, 4, 5)) return "Dias úteis (Seg-Sex)"
        if (sorted == listOf(6, 7)) return "Fim de semana (Sáb-Dom)"
        val dayLabels = mapOf(1 to "Seg", 2 to "Ter", 3 to "Qua", 4 to "Qui", 5 to "Sex", 6 to "Sáb", 7 to "Dom")
        return sorted.mapNotNull { dayLabels[it] }.joinToString(", ")
    }
}
