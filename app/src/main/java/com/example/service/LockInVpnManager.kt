package com.example.service

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.VpnService
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * LockInVpnManager
 *
 * Manages the lifecycle, state, and statistics of the local "Escudo de Rede" (Firewall/DNS Sinkhole).
 * Ensures zero external server usage, strictly local on-device filtering, and zero interference
 * with banking applications.
 */
object LockInVpnManager {

    private const val TAG = "LockInVpnManager"
    private const val PREFS_NAME = "lock_in_vpn_preferences"
    private const val KEY_VPN_ENABLED = "vpn_shield_enabled"
    private const val KEY_BLOCKED_COUNT = "vpn_blocked_count"
    private const val KEY_LAST_DATE = "vpn_stats_date"

    private val _isVpnEnabled = MutableStateFlow(false)
    val isVpnEnabled: StateFlow<Boolean> = _isVpnEnabled.asStateFlow()

    private val _isVpnRunning = MutableStateFlow(false)
    val isVpnRunning: StateFlow<Boolean> = _isVpnRunning.asStateFlow()

    private val _blockedDnsCount = MutableStateFlow(0)
    val blockedDnsCount: StateFlow<Int> = _blockedDnsCount.asStateFlow()

    private val _depletedPackages = MutableStateFlow<Set<String>>(emptySet())
    val depletedPackages: StateFlow<Set<String>> = _depletedPackages.asStateFlow()

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isVpnEnabled.value = prefs.getBoolean(KEY_VPN_ENABLED, false)

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val savedDate = prefs.getString(KEY_LAST_DATE, "")
        if (savedDate == today) {
            _blockedDnsCount.value = prefs.getInt(KEY_BLOCKED_COUNT, 0)
        } else {
            prefs.edit().putString(KEY_LAST_DATE, today).putInt(KEY_BLOCKED_COUNT, 0).apply()
            _blockedDnsCount.value = 0
        }
    }

    fun isPermissionRequired(context: Context): Boolean {
        return VpnService.prepare(context) != null
    }

    fun setVpnRunning(running: Boolean) {
        _isVpnRunning.value = running
    }

    fun setVpnEnabled(context: Context, enabled: Boolean) {
        _isVpnEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_VPN_ENABLED, enabled).apply()

        if (enabled) {
            startVpn(context)
        } else {
            stopVpn(context)
        }
    }

    fun startVpn(context: Context) {
        try {
            val intent = Intent(context, LockInVpnService::class.java).apply {
                action = LockInVpnService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.i(TAG, "Requested LockInVpnService start")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start LockInVpnService", e)
        }
    }

    fun stopVpn(context: Context) {
        try {
            val intent = Intent(context, LockInVpnService::class.java).apply {
                action = LockInVpnService.ACTION_STOP
            }
            context.startService(intent)
            _isVpnRunning.value = false
            Log.i(TAG, "Requested LockInVpnService stop")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop LockInVpnService", e)
        }
    }

    fun recordBlockedQuery(context: Context, domain: String) {
        val newCount = _blockedDnsCount.value + 1
        _blockedDnsCount.value = newCount
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_BLOCKED_COUNT, newCount).apply()
        } catch (_: Exception) {}
        Log.d(TAG, "Blocked DNS query for domain: $domain (Total today: $newCount)")
    }

    fun registerDepletedApp(context: Context, packageName: String) {
        val current = _depletedPackages.value.toMutableSet()
        if (current.add(packageName)) {
            _depletedPackages.value = current
            Log.i(TAG, "App registered as depleted for network cut-off: $packageName")
            // If VPN is running, refresh routing/tunnel
            if (_isVpnRunning.value) {
                startVpn(context)
            }
        }
    }

    fun clearDepletedApp(context: Context, packageName: String) {
        val current = _depletedPackages.value.toMutableSet()
        if (current.remove(packageName)) {
            _depletedPackages.value = current
            Log.i(TAG, "App cleared from depleted network cut-off: $packageName")
            if (_isVpnRunning.value) {
                startVpn(context)
            }
        }
    }

    fun clearAllDepletedApps(context: Context) {
        if (_depletedPackages.value.isNotEmpty()) {
            _depletedPackages.value = emptySet()
            if (_isVpnRunning.value) {
                startVpn(context)
            }
        }
    }
}
