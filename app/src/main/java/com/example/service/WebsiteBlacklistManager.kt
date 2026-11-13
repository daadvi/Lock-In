package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.net.URI

object WebsiteBlacklistManager {
    private const val TAG = "WebsiteBlacklist"
    private const val PREFS_NAME = "lock_in_website_blacklist"
    private const val KEY_BLACKLIST = "blocked_websites_json"
    private const val KEY_ENABLED = "website_blacklist_enabled"

    private val _blacklistedWebsites = MutableStateFlow<List<String>>(emptyList())
    val blacklistedWebsites: StateFlow<List<String>> = _blacklistedWebsites.asStateFlow()

    private val _isBlacklistEnabled = MutableStateFlow(true)
    val isBlacklistEnabled: StateFlow<Boolean> = _isBlacklistEnabled.asStateFlow()

    val POPULAR_SUGGESTIONS = listOf(
        "bet365.com",
        "betano.com",
        "blaze.com",
        "tiktok.com",
        "kwai.com",
        "twitter.com",
        "x.com",
        "instagram.com",
        "facebook.com",
        "reddit.com",
        "twitch.tv",
        "netflix.com"
    )

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isBlacklistEnabled.value = prefs.getBoolean(KEY_ENABLED, true)

        val jsonString = prefs.getString(KEY_BLACKLIST, null)
        if (!jsonString.isNullOrBlank()) {
            try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    val item = array.getString(i).trim().lowercase()
                    if (item.isNotEmpty() && !list.contains(item)) {
                        list.add(item)
                    }
                }
                _blacklistedWebsites.value = list
            } catch (e: Exception) {
                Log.e(TAG, "Error loading blacklisted websites", e)
                _blacklistedWebsites.value = emptyList()
            }
        } else {
            _blacklistedWebsites.value = emptyList()
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun saveList(context: Context, list: List<String>) {
        val array = JSONArray()
        for (site in list) {
            array.put(site)
        }
        getPrefs(context).edit().putString(KEY_BLACKLIST, array.toString()).apply()
        _blacklistedWebsites.value = list
    }

    fun cleanDomain(raw: String): String {
        var clean = raw.trim().lowercase()
        // Strip control characters, zero-width spaces, and whitespace
        clean = clean.replace(Regex("[\\p{Cntrl}\\s\\u200B]"), "")
        clean = clean.removePrefix("https://").removePrefix("http://")
        clean = clean.removePrefix("www.")
        val slashIndex = clean.indexOf('/')
        if (slashIndex != -1) {
            clean = clean.substring(0, slashIndex)
        }
        val colonIndex = clean.indexOf(':')
        if (colonIndex != -1) {
            clean = clean.substring(0, colonIndex)
        }
        return clean.replace(Regex("[^a-z0-9.-]"), "").trim('.')
    }

    fun addWebsite(context: Context, rawSite: String): Boolean {
        val cleaned = cleanDomain(rawSite)
        if (cleaned.isBlank()) return false
        val current = _blacklistedWebsites.value.toMutableList()
        if (!current.contains(cleaned)) {
            current.add(cleaned)
            saveList(context, current)
            return true
        }
        return false
    }

    fun removeWebsite(context: Context, rawSite: String): Boolean {
        val cleaned = cleanDomain(rawSite)
        val current = _blacklistedWebsites.value.toMutableList()
        val removed = current.removeAll { it.equals(cleaned, ignoreCase = true) || it.equals(rawSite.trim(), ignoreCase = true) }
        if (removed) {
            saveList(context, current)
            return true
        }
        return false
    }

    fun setBlacklistEnabled(context: Context, enabled: Boolean) {
        _isBlacklistEnabled.value = enabled
        getPrefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /**
     * Checks if the detected browser URL or text matches any blacklisted domain.
     * Returns the matched domain if blocked, or null if allowed.
     */
    fun isWebsiteBlacklisted(rawUrlOrDomain: String?): String? {
        if (rawUrlOrDomain.isNullOrBlank() || !_isBlacklistEnabled.value) return null
        val cleaned = cleanDomain(rawUrlOrDomain)
        if (cleaned.isBlank()) return null

        val currentList = _blacklistedWebsites.value
        for (blocked in currentList) {
            if (cleaned == blocked) return blocked
            if (cleaned.endsWith(".$blocked")) return blocked
            if (blocked.contains(".") && cleaned.contains(blocked)) return blocked
            // Check if domain without TLD matches e.g. "bet365"
            val blockedNoTld = blocked.substringBeforeLast(".")
            if (blockedNoTld.length >= 4 && cleaned.contains(blockedNoTld)) {
                return blocked
            }
        }
        return null
    }
}
