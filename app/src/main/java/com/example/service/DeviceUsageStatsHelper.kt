package com.example.service

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import com.example.data.local.RestrictedAppEntity
import com.example.ui.screens.ChartBarData
import com.example.ui.screens.StatsPeriod
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class InstallFilterMode(val label: String) {
    BEFORE("Antes"),
    AFTER("Depois")
}

data class DeviceAppUsageItem(
    val packageName: String,
    val appName: String,
    val foregroundMinutes: Int,
    val isRestricted: Boolean,
    val limitMinutes: Int = 0
)

data class DeviceStatsResult(
    val chartBars: List<ChartBarData>,
    val totalDeviceMinutes: Int,
    val totalRestrictedMinutes: Int,
    val savedMinutes: Int,
    val adherenceRatio: Float,
    val topApps: List<DeviceAppUsageItem>,
    val periodLabel: String
)

object DeviceUsageStatsHelper {

    fun getAppInstallTime(context: Context): Long {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0)).firstInstallTime
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
            }
        } catch (_: Exception) {
            System.currentTimeMillis() - 7 * 24 * 3600 * 1000L
        }
    }

    fun hasUsageAccessPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun getIgnoredPackages(context: Context): Set<String> {
        val set = mutableSetOf(
            context.packageName,
            "android",
            "com.android.systemui",
            "com.google.android.googlequicksearchbox",
            "com.google.android.inputmethod.latin",
            "com.android.settings"
        )
        try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolveInfo?.activityInfo?.packageName?.let { set.add(it) }
        } catch (_: Exception) {}
        return set
    }

    fun queryDeviceStats(
        context: Context,
        period: StatsPeriod,
        offset: Int,
        restrictedApps: List<RestrictedAppEntity>,
        filterMode: InstallFilterMode = InstallFilterMode.AFTER
    ): DeviceStatsResult {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        val hasPermission = hasUsageAccessPermission(context)
        val ignored = getIgnoredPackages(context)
        val restrictedMap = restrictedApps.associateBy { it.packageName }

        if (usm == null || !hasPermission) {
            return generateFallbackStats(period, offset, restrictedApps, filterMode)
        }

        return try {
            when (period) {
                StatsPeriod.DAY -> queryDayStats(context, usm, offset, ignored, restrictedMap, filterMode)
                StatsPeriod.WEEK -> queryWeekStats(context, usm, offset, ignored, restrictedMap, filterMode)
                StatsPeriod.MONTH -> queryMonthStats(context, usm, offset, ignored, restrictedMap, filterMode)
                StatsPeriod.YEAR -> queryYearStats(context, usm, offset, ignored, restrictedMap, filterMode)
            }
        } catch (e: Exception) {
            generateFallbackStats(period, offset, restrictedApps, filterMode)
        }
    }

    private fun queryDayStats(
        context: Context,
        usm: UsageStatsManager,
        offset: Int,
        ignored: Set<String>,
        restrictedMap: Map<String, RestrictedAppEntity>,
        filterMode: InstallFilterMode
    ): DeviceStatsResult {
        val installTime = getAppInstallTime(context)
        val cal = Calendar.getInstance()
        if (filterMode == InstallFilterMode.BEFORE) {
            cal.timeInMillis = installTime
            cal.add(Calendar.DAY_OF_YEAR, offset - 1)
        } else {
            cal.add(Calendar.DAY_OF_YEAR, offset)
        }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = if (filterMode == InstallFilterMode.AFTER && offset == 0) {
            maxOf(cal.timeInMillis, installTime)
        } else {
            cal.timeInMillis
        }

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endLimit = if (filterMode == InstallFilterMode.BEFORE) minOf(cal.timeInMillis, installTime) else minOf(cal.timeInMillis, System.currentTimeMillis())
        val endOfDay = maxOf(endLimit, startOfDay + 1000L)

        val hourlyMinutes = IntArray(24) { 0 }
        val packageTimeMap = mutableMapOf<String, Long>()

        // 1. Process usage events for hourly precision
        val events = usm.queryEvents(startOfDay, maxOf(endOfDay, startOfDay + 1000L))
        val event = UsageEvents.Event()
        var currentPkg: String? = null
        var lastResumedTime = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            if (ignored.contains(pkg)) continue

            val ts = event.timeStamp
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    currentPkg = pkg
                    lastResumedTime = ts
                }
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                    if (currentPkg == pkg && lastResumedTime > 0L) {
                        val duration = (ts - lastResumedTime).coerceAtLeast(0L)
                        packageTimeMap[pkg] = (packageTimeMap[pkg] ?: 0L) + duration

                        // Attribute minutes to the hour slot
                        val eventCal = Calendar.getInstance().apply { timeInMillis = lastResumedTime }
                        val hour = eventCal.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
                        val durationMin = (duration / 60_000L).toInt()
                        hourlyMinutes[hour] = (hourlyMinutes[hour] + durationMin).coerceAtMost(60)

                        lastResumedTime = 0L
                        currentPkg = null
                    }
                }
            }
        }

        // If queryEvents returned limited data (some OEMs prune event logs), fallback to interval daily stats
        if (packageTimeMap.isEmpty()) {
            val statsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, endOfDay)
            statsList?.forEach { stat ->
                if (!ignored.contains(stat.packageName) && stat.totalTimeInForeground > 0) {
                    packageTimeMap[stat.packageName] = stat.totalTimeInForeground
                }
            }
            // Distribute across typical daylight hours if events were pruned
            val totalMins = (packageTimeMap.values.sum() / 60_000L).toInt()
            if (totalMins > 0) {
                val activeHours = listOf(8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22)
                val perHour = totalMins / activeHours.size
                activeHours.forEach { h -> hourlyMinutes[h] = perHour.coerceAtMost(60) }
            }
        }

        val bars = (0..23).map { hour ->
            val mins = hourlyMinutes[hour]
            ChartBarData(
                label = "${hour}h",
                valueMinutes = mins,
                fullDateDesc = "HORÁRIO ${hour}:00 • ${mins} MIN"
            )
        }

        val dayDateFormat = SimpleDateFormat("dd 'DE' MMMM", Locale("pt", "BR"))
        val dateLabel = when (offset) {
            0 -> "HOJE • ${dayDateFormat.format(Date(startOfDay)).uppercase()}"
            -1 -> "ONTEM • ${dayDateFormat.format(Date(startOfDay)).uppercase()}"
            else -> dayDateFormat.format(Date(startOfDay)).uppercase()
        }

        return buildResult(context, bars, packageTimeMap, restrictedMap, dateLabel)
    }

    private fun queryWeekStats(
        context: Context,
        usm: UsageStatsManager,
        offset: Int,
        ignored: Set<String>,
        restrictedMap: Map<String, RestrictedAppEntity>,
        filterMode: InstallFilterMode
    ): DeviceStatsResult {
        val installTime = getAppInstallTime(context)
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        if (filterMode == InstallFilterMode.BEFORE) {
            cal.timeInMillis = installTime
            cal.add(Calendar.WEEK_OF_YEAR, offset - 1)
        } else {
            cal.add(Calendar.WEEK_OF_YEAR, offset)
        }
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val dayNames = listOf("SEG", "TER", "QUA", "QUI", "SEX", "SÁB", "DOM")
        val fullDayNames = listOf("SEGUNDA-FEIRA", "TERÇA-FEIRA", "QUARTA-FEIRA", "QUINTA-FEIRA", "SEXTA-FEIRA", "SÁBADO", "DOMINGO")
        val bars = mutableListOf<ChartBarData>()
        val packageTimeMap = mutableMapOf<String, Long>()

        val now = System.currentTimeMillis()

        for (i in 0 until 7) {
            val dayStart = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val dayEnd = if (filterMode == InstallFilterMode.BEFORE) minOf(cal.timeInMillis - 1, installTime) else minOf(cal.timeInMillis - 1, now)

            var dayMinutes = 0
            if (dayStart <= dayEnd) {
                val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, dayStart, maxOf(dayEnd, dayStart + 1000L))
                stats?.forEach { s ->
                    if (!ignored.contains(s.packageName) && s.totalTimeInForeground > 0) {
                        dayMinutes += (s.totalTimeInForeground / 60_000L).toInt()
                        packageTimeMap[s.packageName] = (packageTimeMap[s.packageName] ?: 0L) + s.totalTimeInForeground
                    }
                }
            }

            bars.add(
                ChartBarData(
                    label = dayNames[i],
                    valueMinutes = dayMinutes,
                    fullDateDesc = "${fullDayNames[i]} • ${dayMinutes / 60}H ${dayMinutes % 60}MIN"
                )
            )
        }

        val baseLabel = if (offset == 0) "ESTA SEMANA" else "SEMANA ANTERIOR"
        val weekLabel = if (filterMode == InstallFilterMode.BEFORE) "$baseLabel (ANTES)" else "$baseLabel (DEPOIS)"
        return buildResult(context, bars, packageTimeMap, restrictedMap, weekLabel)
    }

    private fun queryMonthStats(
        context: Context,
        usm: UsageStatsManager,
        offset: Int,
        ignored: Set<String>,
        restrictedMap: Map<String, RestrictedAppEntity>,
        filterMode: InstallFilterMode
    ): DeviceStatsResult {
        val installTime = getAppInstallTime(context)
        val cal = Calendar.getInstance()
        if (filterMode == InstallFilterMode.BEFORE) {
            cal.timeInMillis = installTime
            cal.add(Calendar.MONTH, offset - 1)
        } else {
            cal.add(Calendar.MONTH, offset)
        }
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("pt", "BR"))
        val monthLabel = monthFormat.format(cal.time).uppercase()
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val bars = mutableListOf<ChartBarData>()
        val packageTimeMap = mutableMapOf<String, Long>()
        val now = System.currentTimeMillis()

        val dayNamesShort = listOf("DOM", "SEG", "TER", "QUA", "QUI", "SEX", "SÁB")

        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val dayStart = cal.timeInMillis
            val dayEnd = if (filterMode == InstallFilterMode.BEFORE) minOf(dayStart + 86_400_000L - 1, installTime) else minOf(dayStart + 86_400_000L - 1, now)

            var dayMinutes = 0
            if (dayStart <= dayEnd) {
                val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, dayStart, maxOf(dayEnd, dayStart + 1000L))
                stats?.forEach { s ->
                    if (!ignored.contains(s.packageName) && s.totalTimeInForeground > 0) {
                        dayMinutes += (s.totalTimeInForeground / 60_000L).toInt()
                        packageTimeMap[s.packageName] = (packageTimeMap[s.packageName] ?: 0L) + s.totalTimeInForeground
                    }
                }
            }

            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
            val dayName = dayNamesShort.getOrElse(dayOfWeek) { "DIA" }

            bars.add(
                ChartBarData(
                    label = "$day",
                    valueMinutes = dayMinutes,
                    fullDateDesc = "$dayName $day • ${dayMinutes / 60}H ${dayMinutes % 60}MIN"
                )
            )
        }

        val fullMonthLabel = if (filterMode == InstallFilterMode.BEFORE) "$monthLabel (ANTES)" else "$monthLabel (DEPOIS)"
        return buildResult(context, bars, packageTimeMap, restrictedMap, fullMonthLabel)
    }

    private fun queryYearStats(
        context: Context,
        usm: UsageStatsManager,
        offset: Int,
        ignored: Set<String>,
        restrictedMap: Map<String, RestrictedAppEntity>,
        filterMode: InstallFilterMode
    ): DeviceStatsResult {
        val installTime = getAppInstallTime(context)
        val cal = Calendar.getInstance()
        cal.add(Calendar.YEAR, offset)
        cal.set(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val targetYear = cal.get(Calendar.YEAR)
        val monthNames = listOf("JAN", "FEV", "MAR", "ABR", "MAI", "JUN", "JUL", "AGO", "SET", "OUT", "NOV", "DEZ")
        val fullMonthNames = listOf("JANEIRO", "FEVEREIRO", "MARÇO", "ABRIL", "MAIO", "JUNHO", "JULHO", "AGOSTO", "SETEMBRO", "OUTUBRO", "NOVEMBRO", "DEZEMBRO")

        val bars = mutableListOf<ChartBarData>()
        val packageTimeMap = mutableMapOf<String, Long>()
        val now = System.currentTimeMillis()

        for (m in 0..11) {
            cal.set(Calendar.MONTH, m)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val monthStart = cal.timeInMillis
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, maxDay)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            val monthEnd = if (filterMode == InstallFilterMode.BEFORE) minOf(cal.timeInMillis, installTime) else minOf(cal.timeInMillis, now)

            var monthMinutes = 0
            if (monthStart <= monthEnd) {
                val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_MONTHLY, monthStart, maxOf(monthEnd, monthStart + 1000L))
                stats?.forEach { s ->
                    if (!ignored.contains(s.packageName) && s.totalTimeInForeground > 0) {
                        monthMinutes += (s.totalTimeInForeground / 60_000L).toInt()
                        packageTimeMap[s.packageName] = (packageTimeMap[s.packageName] ?: 0L) + s.totalTimeInForeground
                    }
                }
            }

            bars.add(
                ChartBarData(
                    label = monthNames[m],
                    valueMinutes = monthMinutes,
                    fullDateDesc = "${fullMonthNames[m]} $targetYear • ${monthMinutes / 60}H ${monthMinutes % 60}MIN"
                )
            )
        }

        val yearLabel = if (filterMode == InstallFilterMode.BEFORE) "$targetYear (ANTES)" else "$targetYear (DEPOIS)"
        return buildResult(context, bars, packageTimeMap, restrictedMap, yearLabel)
    }

    private fun buildResult(
        context: Context,
        bars: List<ChartBarData>,
        packageTimeMap: Map<String, Long>,
        restrictedMap: Map<String, RestrictedAppEntity>,
        periodLabel: String
    ): DeviceStatsResult {
        val pm = context.packageManager
        val appUsageList = mutableListOf<DeviceAppUsageItem>()

        var totalDeviceMillis = 0L
        var totalRestrictedMillis = 0L

        packageTimeMap.forEach { (pkg, millis) ->
            totalDeviceMillis += millis
            val isRestricted = restrictedMap.containsKey(pkg) && restrictedMap[pkg]?.isLimited == true
            if (isRestricted) {
                totalRestrictedMillis += millis
            }

            val appName = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                appInfo.loadLabel(pm).toString()
            } catch (_: Exception) {
                pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            }

            val mins = (millis / 60_000L).toInt()
            if (mins > 0 || isRestricted) {
                val limitMinutes = restrictedMap[pkg]?.individualLimitMinutes ?: 0
                appUsageList.add(
                    DeviceAppUsageItem(
                        packageName = pkg,
                        appName = appName,
                        foregroundMinutes = mins,
                        isRestricted = isRestricted,
                        limitMinutes = limitMinutes
                    )
                )
            }
        }

        // Also ensure any restricted app is present even if 0 usage recorded today
        restrictedMap.values.forEach { restricted ->
            if (restricted.isLimited && appUsageList.none { it.packageName == restricted.packageName }) {
                appUsageList.add(
                    DeviceAppUsageItem(
                        packageName = restricted.packageName,
                        appName = restricted.appName,
                        foregroundMinutes = 0,
                        isRestricted = true,
                        limitMinutes = restricted.individualLimitMinutes
                    )
                )
            }
        }

        // Prioritize: restricted apps first, then highest usage down
        val sortedApps = appUsageList.sortedWith(
            compareByDescending<DeviceAppUsageItem> { it.isRestricted }
                .thenByDescending { it.foregroundMinutes }
        )

        val totalDeviceMinutes = (totalDeviceMillis / 60_000L).toInt()
        val totalRestrictedMinutes = (totalRestrictedMillis / 60_000L).toInt()

        // Adherence: how well the user stayed within restricted limits
        val totalAllowedLimit = restrictedMap.values.filter { it.isLimited }.sumOf { it.individualLimitMinutes }
        val adherence = if (totalAllowedLimit <= 0) {
            0.95f
        } else if (totalRestrictedMinutes <= totalAllowedLimit) {
            1.0f
        } else {
            (totalAllowedLimit.toFloat() / totalRestrictedMinutes).coerceIn(0.15f, 1.0f)
        }

        // Estimated minutes saved based on typical phone addiction baselines (3 hours = 180 min)
        val savedMinutes = maxOf(0, 180 - totalRestrictedMinutes)

        return DeviceStatsResult(
            chartBars = bars,
            totalDeviceMinutes = totalDeviceMinutes,
            totalRestrictedMinutes = totalRestrictedMinutes,
            savedMinutes = savedMinutes,
            adherenceRatio = adherence,
            topApps = sortedApps,
            periodLabel = periodLabel
        )
    }

    private fun generateFallbackStats(
        period: StatsPeriod,
        offset: Int,
        restrictedApps: List<RestrictedAppEntity>,
        filterMode: InstallFilterMode = InstallFilterMode.AFTER
    ): DeviceStatsResult {
        val bars = when (period) {
            StatsPeriod.DAY -> (0..23).map { hour ->
                ChartBarData(label = "${hour}h", valueMinutes = 0, fullDateDesc = "HORÁRIO ${hour}:00 • 0 MIN")
            }
            StatsPeriod.WEEK -> listOf("SEG", "TER", "QUA", "QUI", "SEX", "SÁB", "DOM").map {
                ChartBarData(label = it, valueMinutes = 0, fullDateDesc = "$it • 0 MIN")
            }
            StatsPeriod.MONTH -> (1..30).map {
                ChartBarData(label = "$it", valueMinutes = 0, fullDateDesc = "DIA $it • 0 MIN")
            }
            StatsPeriod.YEAR -> listOf("JAN", "FEV", "MAR", "ABR", "MAI", "JUN", "JUL", "AGO", "SET", "OUT", "NOV", "DEZ").map {
                ChartBarData(label = it, valueMinutes = 0, fullDateDesc = "$it • 0 MIN")
            }
        }

        val appItems = restrictedApps.map {
            DeviceAppUsageItem(
                packageName = it.packageName,
                appName = it.appName,
                foregroundMinutes = 0,
                isRestricted = it.isLimited,
                limitMinutes = it.individualLimitMinutes
            )
        }

        return DeviceStatsResult(
            chartBars = bars,
            totalDeviceMinutes = 0,
            totalRestrictedMinutes = 0,
            savedMinutes = 0,
            adherenceRatio = 1.0f,
            topApps = appItems,
            periodLabel = "PERMISSÃO PENDENTE"
        )
    }

    const val EXTRA_PACKAGE_NAME = InstallationBlockerHelper.EXTRA_PACKAGE_NAME
    const val EXTRA_APP_NAME = InstallationBlockerHelper.EXTRA_APP_NAME
    const val EXTRA_BLOCKED_REASON = InstallationBlockerHelper.EXTRA_BLOCKED_REASON

    const val DEFAULT_APP_NAME = InstallationBlockerHelper.DEFAULT_APP_NAME
    const val DEFAULT_BLOCKED_REASON = InstallationBlockerHelper.DEFAULT_BLOCKED_REASON

    val BLOCKED_INSTALLER_PACKAGES = InstallationBlockerHelper.BLOCKED_INSTALLER_PACKAGES

    /**
     * Checks if the foreground application in the last 5 seconds belongs to a known installer
     * or Google Play Store, and blocks it if detected.
     */
    fun checkAndBlockInstallation(context: Context, detectedPackage: String? = null): Boolean {
        return InstallationBlockerHelper.checkAndBlockInstallation(context, detectedPackage)
    }

    fun isInstallerPackage(packageName: String?): Boolean {
        return InstallationBlockerHelper.isInstallerPackage(packageName)
    }

    fun getForegroundPackageLast5Seconds(context: Context): String? {
        return InstallationBlockerHelper.getForegroundPackageLast5Seconds(context)
    }

    fun triggerBlockedActivity(
        context: Context,
        detectedPackage: String,
        appName: String = DEFAULT_APP_NAME,
        blockedReason: String = DEFAULT_BLOCKED_REASON
    ) {
        InstallationBlockerHelper.triggerBlockedActivity(context, detectedPackage, appName, blockedReason)
    }
}
