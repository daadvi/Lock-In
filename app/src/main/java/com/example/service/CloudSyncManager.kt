package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.LockInApplication
import com.example.data.local.AppGroupEntity
import com.example.data.local.FocusRoutine
import com.example.data.local.RestrictedAppEntity
import com.example.data.local.RestrictedWebsiteEntity
import com.example.data.model.AccountProfile
import com.example.data.model.EmailProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class CloudSyncResult(
    val success: Boolean,
    val message: String,
    val appsCount: Int = 0,
    val websitesCount: Int = 0,
    val routinesCount: Int = 0,
    val blacklistCount: Int = 0
)

data class CloudBackupSummary(
    val email: String,
    val provider: String,
    val timestamp: Long,
    val appsCount: Int,
    val websitesCount: Int,
    val routinesCount: Int,
    val blacklistCount: Int,
    val screenTimeLimitMinutes: Int
)

object CloudSyncManager {

    private const val TAG = "CloudSyncManager"
    private const val PREFS_NAME = "lock_in_cloud_account_prefs"
    private const val KEY_LOGGED_IN = "key_logged_in"
    private const val KEY_EMAIL = "key_email"
    private const val KEY_PROVIDER = "key_provider"
    private const val KEY_DISPLAY_NAME = "key_display_name"
    private const val KEY_LAST_SYNC = "key_last_sync"

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _currentAccount = MutableStateFlow<AccountProfile?>(null)
    val currentAccount: StateFlow<AccountProfile?> = _currentAccount.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncEpoch = MutableStateFlow(0L)
    val lastSyncEpoch: StateFlow<Long> = _lastSyncEpoch.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean(KEY_LOGGED_IN, false)
        if (isLoggedIn) {
            val email = prefs.getString(KEY_EMAIL, null)
            if (!email.isNullOrBlank()) {
                val providerName = prefs.getString(KEY_PROVIDER, EmailProvider.CUSTOM.name) ?: EmailProvider.CUSTOM.name
                val provider = try {
                    EmailProvider.valueOf(providerName)
                } catch (_: Exception) {
                    EmailProvider.fromEmail(email)
                }
                val displayName = prefs.getString(KEY_DISPLAY_NAME, email.substringBefore("@")) ?: email.substringBefore("@")
                val lastSync = prefs.getLong(KEY_LAST_SYNC, 0L)
                _currentAccount.value = AccountProfile(
                    email = email,
                    provider = provider,
                    displayName = displayName,
                    lastSyncEpoch = lastSync,
                    isSynced = lastSync > 0
                )
                _lastSyncEpoch.value = lastSync
            }
        }
    }

    private fun getCloudBackupFile(context: Context, email: String): File {
        val sanitized = email.trim().lowercase().replace(Regex("[^a-zA-Z0-9_]"), "_")
        val dir = File(context.filesDir, "cloud_backups")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "backup_$sanitized.json")
    }

    private fun getExternalBackupFile(context: Context, email: String): File? {
        val sanitized = email.trim().lowercase().replace(Regex("[^a-zA-Z0-9_]"), "_")
        val extDir = try { context.getExternalFilesDir(null) } catch (_: Exception) { null } ?: return null
        val dir = File(extDir, "cloud_backups")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "backup_$sanitized.json")
    }

    private fun getExistingBackupFile(context: Context, email: String): File? {
        val internalFile = getCloudBackupFile(context, email)
        val externalFile = getExternalBackupFile(context, email)
        return when {
            internalFile.exists() && internalFile.length() > 0 && externalFile != null && externalFile.exists() && externalFile.length() > 0 -> {
                if (externalFile.lastModified() > internalFile.lastModified()) externalFile else internalFile
            }
            internalFile.exists() && internalFile.length() > 0 -> internalFile
            externalFile != null && externalFile.exists() && externalFile.length() > 0 -> externalFile
            else -> null
        }
    }

    fun hasCloudBackup(context: Context, email: String): Boolean {
        val file = getExistingBackupFile(context, email)
        return file != null && file.exists() && file.length() > 0
    }

    fun getCloudBackupSummary(context: Context, email: String): CloudBackupSummary? {
        val file = getExistingBackupFile(context, email) ?: return null
        return try {
            val jsonStr = file.readText()
            val root = JSONObject(jsonStr)
            CloudBackupSummary(
                email = root.optString("email", email),
                provider = root.optString("provider", "EMAIL"),
                timestamp = root.optLong("timestamp", file.lastModified()),
                appsCount = root.optJSONArray("restrictedApps")?.length() ?: 0,
                websitesCount = root.optJSONArray("restrictedWebsites")?.length() ?: 0,
                routinesCount = root.optJSONArray("focusRoutines")?.length() ?: 0,
                blacklistCount = root.optJSONArray("playStoreBlacklist")?.length() ?: 0,
                screenTimeLimitMinutes = root.optInt("screenTimeLimitMinutes", 0)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading backup summary", e)
            null
        }
    }

    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val salted = "LOCK_IN_AUTH_SALT_${password}_PROTECT_V2"
        val hashBytes = digest.digest(salted.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun isAccountRegistered(context: Context, email: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val prefHash = prefs.getString("pass_hash_$cleanEmail", null)
        if (!prefHash.isNullOrBlank()) return true
        val backupFile = getExistingBackupFile(context, cleanEmail)
        if (backupFile != null && backupFile.exists() && backupFile.length() > 0) {
            return try {
                val json = JSONObject(backupFile.readText())
                val h = json.optString("accountPasswordHash", "")
                h.isNotBlank()
            } catch (_: Exception) {
                false
            }
        }
        return false
    }

    fun getStoredPasswordHash(context: Context, email: String): String? {
        val cleanEmail = email.trim().lowercase()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val prefHash = prefs.getString("pass_hash_$cleanEmail", null)
        if (!prefHash.isNullOrBlank()) return prefHash
        val backupFile = getExistingBackupFile(context, cleanEmail)
        if (backupFile != null && backupFile.exists() && backupFile.length() > 0) {
            try {
                val json = JSONObject(backupFile.readText())
                val h = json.optString("accountPasswordHash", "")
                if (h.isNotBlank()) return h
            } catch (_: Exception) {}
        }
        return null
    }

    suspend fun registerAccount(
        context: Context,
        email: String,
        password: String,
        provider: EmailProvider = EmailProvider.fromEmail(email),
        displayName: String = ""
    ): Result<AccountProfile> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return@withContext Result.failure(IllegalArgumentException("E-mail inválido. Digite um e-mail com formato válido."))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("A senha deve ter pelo menos 6 dígitos para proteger sua conta."))
        }
        val existingHash = getStoredPasswordHash(context, cleanEmail)
        if (existingHash != null) {
            return@withContext Result.failure(IllegalStateException("Este e-mail já está cadastrado no Lock In. Por favor, acesse a aba 'ENTRAR' usando a sua senha."))
        }
        val newHash = hashPassword(password)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("pass_hash_$cleanEmail", newHash).apply()

        val profile = signInOrRegisterInternal(context, cleanEmail, provider, displayName, passwordHash = newHash)
        Result.success(profile)
    }

    suspend fun loginAccount(
        context: Context,
        email: String,
        password: String
    ): Result<AccountProfile> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Digite seu e-mail para entrar."))
        }
        val storedHash = getStoredPasswordHash(context, cleanEmail)
        if (storedHash == null) {
            return@withContext Result.failure(IllegalStateException("Nenhuma conta com este e-mail foi encontrada. Acesse a aba 'CRIAR CONTA' para registrar com senha."))
        }
        val inputHash = hashPassword(password)
        if (inputHash != storedHash) {
            return@withContext Result.failure(SecurityException("Senha incorreta. Não é possível acessar ou sincronizar os dados deste e-mail sem a senha correta."))
        }
        val provider = EmailProvider.fromEmail(cleanEmail)
        val profile = signInOrRegisterInternal(context, cleanEmail, provider, passwordHash = storedHash)
        Result.success(profile)
    }

    suspend fun signInOrRegister(
        context: Context,
        email: String,
        provider: EmailProvider,
        displayName: String = ""
    ): AccountProfile = signInOrRegisterInternal(context, email, provider, displayName, null)

    private suspend fun signInOrRegisterInternal(
        context: Context,
        email: String,
        provider: EmailProvider,
        displayName: String = "",
        passwordHash: String? = null
    ): AccountProfile = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val name = displayName.trim().ifBlank { cleanEmail.substringBefore("@") }

        val profile = AccountProfile(
            email = cleanEmail,
            provider = provider,
            displayName = name,
            lastSyncEpoch = 0L,
            isSynced = false
        )

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_EMAIL, cleanEmail)
            .putString(KEY_PROVIDER, provider.name)
            .putString(KEY_DISPLAY_NAME, name)
        if (!passwordHash.isNullOrBlank()) {
            editor.putString("pass_hash_$cleanEmail", passwordHash)
        }
        editor.apply()

        _currentAccount.value = profile

        // If there's an existing cloud backup for this email, automatically restore it
        if (hasCloudBackup(context, cleanEmail)) {
            restoreDataFromCloud(context, cleanEmail)
        } else {
            // First time login with this email: immediately sync current settings to cloud
            syncDataToCloud(context)
        }

        profile
    }

    fun signOut(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        _currentAccount.value = null
        _syncMessage.value = "Desconectado da conta de sincronização."
    }

    suspend fun syncDataToCloud(context: Context): CloudSyncResult = withContext(Dispatchers.IO) {
        val account = _currentAccount.value
        if (account == null) {
            return@withContext CloudSyncResult(
                success = false,
                message = "Nenhuma conta conectada para sincronizar."
            )
        }

        _isSyncing.value = true
        _syncMessage.value = "Sincronizando com a nuvem..."

        try {
            val app = LockInApplication.instance
            val dao = app.database.lockInDao()
            val userPrefs = app.userPreferences

            // 1. Gather all local data
            val apps = dao.getAllAppsList()
            val websites = dao.getAllWebsitesList()
            val appGroups = dao.getAllAppGroupsList()
            val routines = FocusTimeManager.getRoutines(context)
            val blacklist = PlayStoreBlacklistManager.blacklistedApps.value
            val blacklistEnabled = PlayStoreBlacklistManager.isBlacklistEnabled.value

            val screenTimeLimitMinutes = ScreenTimeManager.limitMinutesFlow.value
            val screenTimeResetHours = ScreenTimeManager.resetIntervalHoursFlow.value
            val screenTimeEnabled = ScreenTimeManager.isEnabledFlow.value

            val rootJson = JSONObject().apply {
                put("version", 2)
                put("email", account.email)
                put("provider", account.provider.name)
                put("displayName", account.displayName)
                put("timestamp", System.currentTimeMillis())

                // User preferences
                val storedPwdHash = getStoredPasswordHash(context, account.email)
                if (!storedPwdHash.isNullOrBlank()) {
                    put("accountPasswordHash", storedPwdHash)
                }
                put("userName", userPrefs.userName.value)
                put("trustPassword", userPrefs.getTrustPassword())
                put("textColorHex", userPrefs.textColorHex.value)
                put("themeMode", userPrefs.themeMode.value)
                put("screenTimeLimitMinutes", screenTimeLimitMinutes)
                put("screenTimeResetHours", screenTimeResetHours)
                put("screenTimeEnabled", screenTimeEnabled)
                put("streakCount", StreakManager.streakFlow.value)

                // Restricted Apps
                val appsArray = JSONArray()
                for (a in apps) {
                    val aObj = JSONObject().apply {
                        put("packageName", a.packageName)
                        put("appName", a.appName)
                        put("isLimited", a.isLimited)
                        put("individualLimitMinutes", a.individualLimitMinutes)
                        put("individualRemainingSeconds", a.individualRemainingSeconds)
                        put("useAbsoluteTime", a.useAbsoluteTime)
                        put("isShortVideoPlatform", a.isShortVideoPlatform)
                        put("limitShortVideosOnly", a.limitShortVideosOnly)
                        put("shortVideoLimitMinutes", a.shortVideoLimitMinutes)
                        put("shortVideoRemainingSeconds", a.shortVideoRemainingSeconds)
                        put("hasVideoContent", a.hasVideoContent)
                        put("hasShortVideoContent", a.hasShortVideoContent)
                        put("associatedWebsites", a.associatedWebsites)
                    }
                    appsArray.put(aObj)
                }
                put("restrictedApps", appsArray)

                // Restricted Websites
                val webArray = JSONArray()
                for (w in websites) {
                    val wObj = JSONObject().apply {
                        put("domain", w.domain)
                        put("linkedPackageName", w.linkedPackageName ?: "")
                        put("limitMinutes", w.limitMinutes)
                        put("remainingSeconds", w.remainingSeconds)
                        put("isBlocked", w.isBlocked)
                    }
                    webArray.put(wObj)
                }
                put("restrictedWebsites", webArray)

                // App Groups
                val groupArray = JSONArray()
                for (g in appGroups) {
                    val gObj = JSONObject().apply {
                        put("id", g.id)
                        put("name", g.name)
                        put("packageNamesCsv", g.packageNamesCsv)
                        put("limitMinutes", g.limitMinutes)
                        put("remainingSeconds", g.remainingSeconds)
                        put("isBlocked", g.isBlocked)
                    }
                    groupArray.put(gObj)
                }
                put("appGroups", groupArray)

                // Focus Routines
                val routineArray = JSONArray()
                for (r in routines) {
                    val rObj = JSONObject().apply {
                        put("id", r.id)
                        put("name", r.name)
                        put("startHour", r.startHour)
                        put("startMinute", r.startMinute)
                        put("endHour", r.endHour)
                        put("endMinute", r.endMinute)
                        put("daysOfWeek", JSONArray(r.daysOfWeek))
                        put("allowedPackages", JSONArray(r.allowedPackages))
                        put("isEnabled", r.isEnabled)
                    }
                    routineArray.put(rObj)
                }
                put("focusRoutines", routineArray)

                // Play Store Blacklist
                put("playStoreBlacklist", JSONArray(blacklist))
                put("playStoreBlacklistEnabled", blacklistEnabled)
            }

            // Save to persistent cloud backup file (internal + external storage)
            val jsonPayload = rootJson.toString(2)
            val file = getCloudBackupFile(context, account.email)
            file.writeText(jsonPayload)
            try {
                getExternalBackupFile(context, account.email)?.writeText(jsonPayload)
            } catch (_: Exception) {}

            val now = System.currentTimeMillis()
            _lastSyncEpoch.value = now

            val updatedProfile = account.copy(lastSyncEpoch = now, isSynced = true)
            _currentAccount.value = updatedProfile

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_LAST_SYNC, now).apply()

            val msg = "Sincronizado na nuvem com sucesso (${apps.size} apps, ${routines.size} rotinas, ${blacklist.size} na blacklist)."
            _syncMessage.value = msg

            CloudSyncResult(
                success = true,
                message = msg,
                appsCount = apps.size,
                websitesCount = websites.size,
                routinesCount = routines.size,
                blacklistCount = blacklist.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing to cloud", e)
            _syncMessage.value = "Falha ao sincronizar: ${e.message}"
            CloudSyncResult(success = false, message = "Erro ao sincronizar dados: ${e.message}")
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun restoreDataFromCloud(context: Context, email: String): CloudSyncResult = withContext(Dispatchers.IO) {
        val file = getExistingBackupFile(context, email)
        if (file == null || !file.exists()) {
            return@withContext CloudSyncResult(
                success = false,
                message = "Nenhum backup encontrado na nuvem para este e-mail."
            )
        }

        _isSyncing.value = true
        _syncMessage.value = "Restaurando dados da nuvem..."

        try {
            val jsonStr = file.readText()
            val root = JSONObject(jsonStr)

            val app = LockInApplication.instance
            val dao = app.database.lockInDao()
            val userPrefs = app.userPreferences

            // 1. Restore User Preferences
            val backupPwdHash = root.optString("accountPasswordHash", "")
            if (backupPwdHash.isNotBlank()) {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putString("pass_hash_${email.trim().lowercase()}", backupPwdHash).apply()
            }

            val userName = root.optString("userName", "")
            val trustPassword = root.optString("trustPassword", "")
            val textColorHex = root.optString("textColorHex", "#FF5500")
            val themeMode = root.optString("themeMode", "SYSTEM")

            if (userName.isNotBlank() && trustPassword.isNotBlank()) {
                userPrefs.setRegistration(userName, trustPassword)
            }
            if (textColorHex.isNotBlank()) {
                userPrefs.setTextColorHex(textColorHex)
            }
            if (themeMode.isNotBlank()) {
                userPrefs.setThemeMode(themeMode)
            }

            // 2. Restore Screen Time Limit
            val screenTimeLimit = root.optInt("screenTimeLimitMinutes", 0)
            val screenTimeResetHour = root.optInt("screenTimeResetHours", 0)
            val screenTimeEnabled = root.optBoolean("screenTimeEnabled", screenTimeLimit > 0)
            if (screenTimeEnabled && screenTimeLimit > 0) {
                ScreenTimeManager.setLimitMinutes(context, screenTimeLimit)
            } else {
                ScreenTimeManager.setLimitMinutes(context, 0)
            }
            if (screenTimeResetHour > 0) {
                ScreenTimeManager.setResetIntervalHours(context, screenTimeResetHour)
            }

            // 3. Restore Restricted Apps
            val appsArray = root.optJSONArray("restrictedApps")
            var appsRestored = 0
            if (appsArray != null) {
                val restoredApps = mutableListOf<RestrictedAppEntity>()
                for (i in 0 until appsArray.length()) {
                    val aObj = appsArray.getJSONObject(i)
                    restoredApps.add(
                        RestrictedAppEntity(
                            packageName = aObj.getString("packageName"),
                            appName = aObj.getString("appName"),
                            isLimited = aObj.optBoolean("isLimited", false),
                            individualLimitMinutes = aObj.optInt("individualLimitMinutes", 20),
                            individualRemainingSeconds = aObj.optLong("individualRemainingSeconds", 20 * 60L),
                            useAbsoluteTime = aObj.optBoolean("useAbsoluteTime", true),
                            isShortVideoPlatform = aObj.optBoolean("isShortVideoPlatform", false),
                            limitShortVideosOnly = aObj.optBoolean("limitShortVideosOnly", false),
                            shortVideoLimitMinutes = aObj.optInt("shortVideoLimitMinutes", 15),
                            shortVideoRemainingSeconds = aObj.optLong("shortVideoRemainingSeconds", 15 * 60L),
                            hasVideoContent = aObj.optBoolean("hasVideoContent", false),
                            hasShortVideoContent = aObj.optBoolean("hasShortVideoContent", false),
                            associatedWebsites = aObj.optString("associatedWebsites", "")
                        )
                    )
                }
                dao.insertApps(restoredApps)
                appsRestored = restoredApps.size
            }

            // 4. Restore Websites
            val webArray = root.optJSONArray("restrictedWebsites")
            var websitesRestored = 0
            if (webArray != null) {
                for (i in 0 until webArray.length()) {
                    val wObj = webArray.getJSONObject(i)
                    dao.insertWebsite(
                        RestrictedWebsiteEntity(
                            domain = wObj.getString("domain"),
                            linkedPackageName = if (wObj.has("linkedPackageName") && !wObj.isNull("linkedPackageName")) wObj.getString("linkedPackageName").ifBlank { null } else null,
                            limitMinutes = wObj.optInt("limitMinutes", 15),
                            remainingSeconds = wObj.optLong("remainingSeconds", 15 * 60L),
                            isBlocked = wObj.optBoolean("isBlocked", false)
                        )
                    )
                    websitesRestored++
                }
            }

            // 5. Restore App Groups
            val groupArray = root.optJSONArray("appGroups")
            if (groupArray != null) {
                for (i in 0 until groupArray.length()) {
                    val gObj = groupArray.getJSONObject(i)
                    val pkgsCsv = if (gObj.has("packageNamesCsv")) {
                        gObj.getString("packageNamesCsv")
                    } else {
                        val pkgsArr = gObj.optJSONArray("packageNames")
                        val pkgs = mutableListOf<String>()
                        if (pkgsArr != null) {
                            for (p in 0 until pkgsArr.length()) pkgs.add(pkgsArr.getString(p))
                        }
                        pkgs.joinToString(",")
                    }
                    dao.insertOrUpdateAppGroup(
                        AppGroupEntity(
                            id = gObj.getString("id"),
                            name = gObj.getString("name"),
                            packageNamesCsv = pkgsCsv,
                            limitMinutes = gObj.optInt("limitMinutes", 30),
                            remainingSeconds = gObj.optLong("remainingSeconds", 30 * 60L),
                            isBlocked = gObj.optBoolean("isBlocked", false)
                        )
                    )
                }
            }

            // 6. Restore Focus Routines
            val routineArray = root.optJSONArray("focusRoutines")
            var routinesRestored = 0
            if (routineArray != null) {
                val restoredRoutines = mutableListOf<FocusRoutine>()
                for (i in 0 until routineArray.length()) {
                    val rObj = routineArray.getJSONObject(i)
                    val daysArr = rObj.optJSONArray("daysOfWeek")
                    val days = mutableListOf<Int>()
                    if (daysArr != null) {
                        for (d in 0 until daysArr.length()) days.add(daysArr.getInt(d))
                    }
                    val pkgsArr = rObj.optJSONArray("allowedPackages")
                    val pkgs = mutableListOf<String>()
                    if (pkgsArr != null) {
                        for (p in 0 until pkgsArr.length()) pkgs.add(pkgsArr.getString(p))
                    }
                    restoredRoutines.add(
                        FocusRoutine(
                            id = rObj.getString("id"),
                            name = rObj.getString("name"),
                            startHour = rObj.getInt("startHour"),
                            startMinute = rObj.getInt("startMinute"),
                            endHour = rObj.getInt("endHour"),
                            endMinute = rObj.getInt("endMinute"),
                            daysOfWeek = if (days.isEmpty()) listOf(1, 2, 3, 4, 5, 6, 7) else days,
                            allowedPackages = pkgs.take(3),
                            isEnabled = rObj.optBoolean("isEnabled", true)
                        )
                    )
                }
                FocusTimeManager.setAllRoutines(context, restoredRoutines)
                routinesRestored = restoredRoutines.size
            }

            // 7. Restore Play Store Blacklist
            val blackArray = root.optJSONArray("playStoreBlacklist")
            val blacklistEnabled = root.optBoolean("playStoreBlacklistEnabled", true)
            var blacklistRestored = 0
            if (blackArray != null) {
                val list = mutableListOf<String>()
                for (i in 0 until blackArray.length()) {
                    list.add(blackArray.getString(i))
                }
                PlayStoreBlacklistManager.setBlacklist(context, list, blacklistEnabled)
                blacklistRestored = list.size
            }

            val now = System.currentTimeMillis()
            _lastSyncEpoch.value = now

            val msg = "Dados restaurados com sucesso da nuvem! ($appsRestored apps, $routinesRestored rotinas, $blacklistRestored apps na blacklist)."
            _syncMessage.value = msg

            CloudSyncResult(
                success = true,
                message = msg,
                appsCount = appsRestored,
                websitesCount = websitesRestored,
                routinesCount = routinesRestored,
                blacklistCount = blacklistRestored
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring from cloud", e)
            _syncMessage.value = "Falha ao restaurar: ${e.message}"
            CloudSyncResult(success = false, message = "Erro ao restaurar dados: ${e.message}")
        } finally {
            _isSyncing.value = false
        }
    }
}
