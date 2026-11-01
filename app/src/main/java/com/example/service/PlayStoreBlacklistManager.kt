package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray

object PlayStoreBlacklistManager {

    private const val PREFS_NAME = "lock_in_playstore_blacklist"
    private const val KEY_BLACKLIST = "blocked_apps_json"
    private const val KEY_ENABLED = "blacklist_enabled"

    private val _blacklistedApps = MutableStateFlow<List<String>>(emptyList())
    val blacklistedApps: StateFlow<List<String>> = _blacklistedApps.asStateFlow()

    private val _isBlacklistEnabled = MutableStateFlow(true)
    val isBlacklistEnabled: StateFlow<Boolean> = _isBlacklistEnabled.asStateFlow()

    // Canonical app names, common misspellings/aliases, and their package patterns
    private val APP_ALIASES_MAP = mapOf(
        "tiktok" to listOf("tiktok", "tik tok", "tictoc", "tecktok", "musically", "zhiliaoapp", "trill", "tik_tok"),
        "instagram" to listOf("instagram", "insta", "instagran", "instagra", "ig"),
        "facebook" to listOf("facebook", "face", "facebok", "feice", "fb", "meta"),
        "kwai" to listOf("kwai", "quai", "kuaishou", "nebula"),
        "free fire" to listOf("free fire", "freefire", "freefair", "freefari", "ff", "dts.freefire"),
        "roblox" to listOf("roblox", "roblx", "robloks", "rbx"),
        "twitter" to listOf("twitter", "twiter", "twittr", "x", "tweet"),
        "snapchat" to listOf("snapchat", "snap", "snapcht", "sneap"),
        "whatsapp" to listOf("whatsapp", "whats", "zap", "wpp", "watsapp", "watshap"),
        "youtube" to listOf("youtube", "yotube", "you tube", "yt"),
        "tinder" to listOf("tinder", "tínder"),
        "brawl stars" to listOf("brawl stars", "brawl", "brawstars", "supercell.brawlstars"),
        "subway surfers" to listOf("subway surfers", "subway", "subwaysurfers", "kiloo.subwaysurf"),
        "pubg" to listOf("pubg", "pupg", "pubg mobile", "tencent.ig"),
        "call of duty" to listOf("call of duty", "cod", "cod mobile", "activision.callofduty"),
        "clash royale" to listOf("clash royale", "clash", "clashroyale", "supercell.clashroyale"),
        "candy crush" to listOf("candy crush", "candy", "candycrush", "king.candycrush")
    )

    val KNOWN_PACKAGES_MAP = mapOf(
        "tiktok" to listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
        "instagram" to listOf("com.instagram.android"),
        "facebook" to listOf("com.facebook.katana", "com.facebook.lite", "com.facebook.orca"),
        "kwai" to listOf("com.kwai.video", "com.kuaishou.nebula"),
        "free fire" to listOf("com.dts.freefireth", "com.dts.freefiremax"),
        "roblox" to listOf("com.roblox.client"),
        "twitter" to listOf("com.twitter.android"),
        "snapchat" to listOf("com.snapchat.android"),
        "whatsapp" to listOf("com.whatsapp", "com.whatsapp.w4b"),
        "youtube" to listOf("com.google.android.youtube"),
        "tinder" to listOf("com.tinder"),
        "brawl stars" to listOf("com.supercell.brawlstars"),
        "subway surfers" to listOf("com.kiloo.subwaysurf"),
        "pubg" to listOf("com.tencent.ig"),
        "call of duty" to listOf("com.activision.callofduty.shooter"),
        "clash royale" to listOf("com.supercell.clashroyale", "com.supercell.clashofclans"),
        "candy crush" to listOf("com.king.candycrushsaga")
    )

    // Popular apps that users often want to prevent from being installed
    val POPULAR_SUGGESTIONS = listOf(
        "TikTok",
        "Instagram",
        "Kwai",
        "Free Fire",
        "Roblox",
        "Twitter",
        "X",
        "Tinder",
        "Snapchat",
        "Brawl Stars",
        "Subway Surfers",
        "PUBG",
        "Call of Duty",
        "Clash Royale",
        "Candy Crush"
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
                    list.add(array.getString(i))
                }
                _blacklistedApps.value = list
            } catch (_: Exception) {
                _blacklistedApps.value = emptyList()
            }
        } else {
            _blacklistedApps.value = emptyList()
        }
    }

    private fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray(_blacklistedApps.value)
        prefs.edit()
            .putString(KEY_BLACKLIST, array.toString())
            .putBoolean(KEY_ENABLED, _isBlacklistEnabled.value)
            .apply()

        // Auto-save changes to cloud if user is signed in
        try {
            com.example.LockInApplication.instance.applicationScope.launch {
                CloudSyncManager.syncDataToCloud(context)
            }
        } catch (_: Exception) {}
    }

    fun setBlacklist(context: Context, apps: List<String>, enabled: Boolean) {
        _blacklistedApps.value = apps.distinct()
        _isBlacklistEnabled.value = enabled
        save(context)
    }

    fun setBlacklistEnabled(context: Context, enabled: Boolean) {
        _isBlacklistEnabled.value = enabled
        save(context)
    }

    fun addApp(context: Context, appNameOrKeyword: String): Boolean {
        val clean = appNameOrKeyword.trim()
        if (clean.isBlank()) return false
        val current = _blacklistedApps.value.toMutableList()
        if (!current.any { it.equals(clean, ignoreCase = true) }) {
            current.add(clean)
            _blacklistedApps.value = current
            save(context)
            return true
        }
        return false
    }

    fun removeApp(context: Context, appNameOrKeyword: String) {
        val current = _blacklistedApps.value.toMutableList()
        current.removeAll { it.equals(appNameOrKeyword.trim(), ignoreCase = true) }
        _blacklistedApps.value = current
        save(context)
    }

    private fun normalize(str: String): String {
        return str.lowercase().trim()
            .replace(Regex("[áàãâä]"), "a")
            .replace(Regex("[éèêë]"), "e")
            .replace(Regex("[íìîï]"), "i")
            .replace(Regex("[óòõôö]"), "o")
            .replace(Regex("[úùûü]"), "u")
            .replace(Regex("[ç]"), "c")
            .replace(Regex("[^a-z0-9]"), "")
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[m][n]
    }

    fun getCanonicalKeysFor(term: String): List<String> {
        val norm = normalize(term)
        if (norm.isBlank()) return emptyList()
        val matchedKeys = mutableListOf<String>()
        val tokens = norm.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }

        for ((canonical, aliases) in APP_ALIASES_MAP) {
            val normCanonical = normalize(canonical)
            if (normCanonical == norm || tokens.contains(normCanonical)) {
                matchedKeys.add(canonical)
                continue
            }
            for (alias in aliases) {
                val normAlias = normalize(alias)
                if (normAlias.isBlank()) continue
                // Short aliases (length < 4 like 'x', 'ig', 'fb', 'ff') must ONLY match exactly
                if (normAlias.length < 4) {
                    if (norm == normAlias || tokens.contains(normAlias)) {
                        matchedKeys.add(canonical)
                        break
                    }
                } else {
                    // For longer aliases (length >= 4), match full string or word tokens
                    if (norm == normAlias || tokens.contains(normAlias)) {
                        matchedKeys.add(canonical)
                        break
                    }
                }
            }
        }
        return matchedKeys
    }

    private fun isFuzzyMatch(text: String, blockedKeyword: String): Boolean {
        val normText = normalize(text)
        val normBlocked = normalize(blockedKeyword)
        if (normText.isBlank() || normBlocked.isBlank()) return false

        // Exact normalized match
        if (normText == normBlocked) return true

        // Strict word token check
        val textTokens = normText.split(Regex("[^a-z0-9]+")).filter { it.length >= 3 }
        if (textTokens.contains(normBlocked)) return true

        // Canonical alias match (only if blockedKeyword is genuinely a canonical target)
        val textCanonicals = getCanonicalKeysFor(text)
        val blockedCanonicals = getCanonicalKeysFor(blockedKeyword)
        if (textCanonicals.isNotEmpty() && blockedCanonicals.isNotEmpty() &&
            textCanonicals.intersect(blockedCanonicals.toSet()).isNotEmpty()) {
            return true
        }

        // Levenshtein distance check for obvious typos (strictly length >= 5, distance <= 1, same length +/- 1)
        if (normText.length >= 5 && normBlocked.length >= 5 && kotlin.math.abs(normText.length - normBlocked.length) <= 1) {
            val dist = levenshteinDistance(normText, normBlocked)
            if (dist <= 1) return true
        }

        return false
    }

    private val SYSTEM_SAFE_PACKAGES = setOf(
        "com.android.systemui",
        "android",
        "com.android.settings",
        "com.android.vending",
        "com.google.android.permissioncontroller",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller"
    )

    /**
     * Checks if a title or string of text matches any blacklisted item,
     * avoiding false positives on common words.
     */
    fun isAppBlacklisted(textOrTitle: String?): String? {
        if (!_isBlacklistEnabled.value || textOrTitle.isNullOrBlank()) return null
        val lower = textOrTitle.trim().lowercase()
        if (lower.length < 3) return null

        // Safe words that should never trigger an app blacklist
        if (lower == "android" || lower == "google" || lower == "configurações" || lower == "settings" ||
            lower == "lock in" || lower == "sistema" || lower == "início" || lower == "launcher" ||
            lower == "play store" || lower == "google play store" || lower == "instalar" || lower == "install" ||
            lower == "cancelar" || lower == "cancel" || lower == "concluir" || lower == "done" || lower == "abrir" ||
            lower == "open" || lower == "deseja instalar" || lower == "do you want to install" || lower == "pacote" ||
            lower == "package" || lower == "instalador" || lower == "installer" || lower == "atualizar" || lower == "update") {
            return null
        }

        for (blocked in _blacklistedApps.value) {
            val b = blocked.trim().lowercase()
            if (b.isBlank()) continue

            // Exact match
            if (lower == b) return blocked

            // Starts with or ends with
            if (lower.startsWith("$b ") || lower.startsWith("$b:") || lower.startsWith("$b -") ||
                lower.startsWith("$b–") || lower.startsWith("$b •") || lower.endsWith(" $b") ||
                lower.contains(" $b ") || lower.contains(" $b:") || lower.contains(" $b-")) {
                return blocked
            }

            // Word token match (only tokens with length >= 4)
            val tokens = lower.split(Regex("[\\s,.:;\\-_/()\\[\\]]+")).filter { it.length >= 4 }
            if (tokens.any { isFuzzyMatch(it, b) }) return blocked

            // Full string fuzzy match if text is not too long
            if (lower.length <= 40 && isFuzzyMatch(lower, b)) return blocked
        }

        return null
    }

    /**
     * Checks a collection of on-screen texts (e.g. from Package Installer or Play Store)
     * and returns the matched blacklisted app name if any text matches, or null otherwise.
     */
    fun findBlacklistMatchInTexts(texts: List<String>): String? {
        if (!_isBlacklistEnabled.value || texts.isEmpty()) return null
        for (t in texts) {
            val match = isAppBlacklisted(t)
            if (match != null) return match
        }
        return null
    }

    /**
     * Checks if a package name matches any blacklisted app (by mapped package or package text).
     */
    fun isPackageBlacklisted(packageName: String): String? {
        if (!_isBlacklistEnabled.value || packageName.isBlank()) return null
        val p = packageName.lowercase()

        // Never blacklist safe system packages or own app
        if (SYSTEM_SAFE_PACKAGES.contains(p) || p.contains("launcher") || p.contains("systemui") ||
            p.contains("keyboard") || p.contains("inputmethod")) {
            return null
        }

        for (blocked in _blacklistedApps.value) {
            val b = blocked.trim().lowercase()
            if (b.isBlank()) continue

            // 1. Check known package map first (most accurate)
            val canonicals = getCanonicalKeysFor(b)
            for (c in canonicals) {
                val knownPkgs = KNOWN_PACKAGES_MAP[c] ?: emptyList()
                if (knownPkgs.any { it.equals(p, ignoreCase = true) || p.startsWith(it) }) {
                    return blocked
                }
            }

            // 2. Check direct identifier containment in package name (e.g. "instagram", "tiktok", "freefire")
            val normB = normalize(b)
            if (normB.length >= 4 && p.contains(normB)) {
                return blocked
            }
        }

        return null
    }

    private val labelCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    /**
     * Full check: checks package name, and queries the application label via PackageManager
     * to check if either matches any blacklisted entry (exact or fuzzy).
     * Uses memory cache to avoid blocking IPC calls on accessibility event loop (prevents ANR).
     */
    fun isAppOrPackageBlacklisted(context: Context, packageName: String): String? {
        if (!_isBlacklistEnabled.value || packageName.isBlank()) return null
        if (packageName == context.packageName) return null

        val p = packageName.lowercase()
        if (SYSTEM_SAFE_PACKAGES.contains(p) || p.contains("launcher") || p.contains("systemui") ||
            p.contains("keyboard") || p.contains("inputmethod")) {
            return null
        }

        // 1. Check package name first (instant, 0 IPC)
        val pkgMatch = isPackageBlacklisted(packageName)
        if (pkgMatch != null) return pkgMatch

        // 2. Query application label with memory cache
        try {
            val cachedLabel = labelCache[packageName]
            if (cachedLabel != null) {
                return isAppBlacklisted(cachedLabel)
            }

            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val label = pm.getApplicationLabel(appInfo).toString()
            labelCache[packageName] = label
            val labelMatch = isAppBlacklisted(label)
            if (labelMatch != null) return labelMatch
        } catch (_: Exception) {}

        return null
    }

    /**
     * Finds if a blacklisted app is currently installed on the device, returning its package name.
     */
    fun findInstalledPackageForBlacklist(context: Context, appNameOrKeyword: String?): String? {
        if (appNameOrKeyword.isNullOrBlank()) return null
        val pm = context.packageManager

        // 1. Check known packages mapping first
        val canonicals = getCanonicalKeysFor(appNameOrKeyword)
        for (c in canonicals) {
            val pkgs = KNOWN_PACKAGES_MAP[c] ?: emptyList()
            for (p in pkgs) {
                if (isPackageInstalled(context, p)) {
                    return p
                }
            }
        }

        // 2. Scan installed applications
        try {
            val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (info in installed) {
                if (info.packageName == context.packageName) continue
                val lowerPkg = info.packageName.lowercase()
                if (SYSTEM_SAFE_PACKAGES.contains(lowerPkg) || lowerPkg.contains("launcher") || lowerPkg.contains("systemui")) {
                    continue
                }

                // Check package name
                if (isPackageBlacklisted(info.packageName) != null) {
                    return info.packageName
                }

                // Check label
                val label = pm.getApplicationLabel(info).toString()
                if (isAppBlacklisted(label) != null) {
                    return info.packageName
                }
            }
        } catch (_: Exception) {}

        return null
    }

    /**
     * Checks if a package is installed on the device.
     */
    fun isPackageInstalled(context: Context, packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return try {
            context.packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if a URL, web address, or browser domain belongs to any app in the installation blacklist.
     * Prevents access to the official websites of blacklisted apps across all browsers!
     */
    fun isWebsiteBlacklistedByInstallationBlacklist(urlOrDomain: String?): String? {
        if (!_isBlacklistEnabled.value || urlOrDomain.isNullOrBlank()) return null

        val clean = urlOrDomain.lowercase()
            .replace("https://", "")
            .replace("http://", "")
            .replace("www.", "")
            .split("/")
            .firstOrNull()
            ?.split("?")
            ?.firstOrNull() ?: return null

        if (clean.length < 3) return null

        for (blocked in _blacklistedApps.value) {
            val b = blocked.trim().lowercase()
            if (b.isBlank()) continue

            // 1. Resolve canonical domains via AppWebsiteMapper
            val canonicals = getCanonicalKeysFor(b)
            for (c in canonicals) {
                val knownPkgs = KNOWN_PACKAGES_MAP[c] ?: emptyList()
                for (pkg in knownPkgs) {
                    val domains = com.example.util.AppWebsiteMapper.getWebsitesForApp(pkg, blocked)
                    if (domains.any { clean.contains(it) || it.contains(clean) }) {
                        return blocked
                    }
                }
            }

            // 2. Direct domains from AppWebsiteMapper using dummy package
            val directDomains = com.example.util.AppWebsiteMapper.getWebsitesForApp("com.$b", blocked)
            if (directDomains.any { clean.contains(it) || it.contains(clean) }) {
                return blocked
            }

            // 3. Normalized heuristic: e.g. "tiktok" in "tiktok.com", "instagram" in "instagram.com"
            val normB = normalize(b)
            if (normB.length >= 4 && clean.contains(normB)) {
                return blocked
            }
        }

        return null
    }
}
