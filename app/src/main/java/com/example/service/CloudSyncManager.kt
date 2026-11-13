package com.example.service

import android.content.Context
import android.util.Log
import android.util.AtomicFile
import com.example.util.AccountPasswordHash
import com.example.LockInApplication
import com.example.data.local.AppGroupEntity
import com.example.data.local.FocusRoutine
import com.example.data.local.RestrictedAppEntity
import com.example.data.local.RestrictedWebsiteEntity
import com.example.data.model.AccountProfile
import com.example.data.model.EmailProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.ByteArrayOutputStream
import java.util.Locale
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

    private const val MAX_BACKUP_BYTES = 1024 * 1024
    private val backupLock = Any()
    private val operationMutex = Mutex()

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

    private fun normalizeEmail(email: String): String = email.trim().lowercase(Locale.ROOT)

    private fun getCloudBackupFile(context: Context, email: String): File {
        // Preserve the complete identity; punctuation replacement aliases distinct accounts.
        val digest = MessageDigest.getInstance("SHA-256").digest(normalizeEmail(email).toByteArray(Charsets.UTF_8))
        val key = digest.joinToString("") { "%02x".format(it) }
        return File(File(context.filesDir, "cloud_backups"), "backup_v3_$key.json")
    }

    private fun legacyBackupFile(directory: File, email: String): File {
        val sanitized = normalizeEmail(email).replace(Regex("[^a-zA-Z0-9_]"), "_")
        return File(File(directory, "cloud_backups"), "backup_$sanitized.json")
    }

    private fun readVerifiedBackup(file: File, email: String): JSONObject {
        // Bound the stream as well as the file metadata, including interrupted-write recovery.
        val bytes = AtomicFile(file).openRead().use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                require(total <= MAX_BACKUP_BYTES) { "Backup excede o limite de 1 MiB." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        val json = String(bytes, Charsets.UTF_8)
        require(com.example.util.JsonInputGuard.isValid(json)) { "Backup com JSON inválido ou estrutura excessivamente aninhada." }
        val root = JSONObject(json)
        require(normalizeEmail(root.optString("email")) == normalizeEmail(email) && normalizeEmail(email).isNotBlank()) {
            "O backup pertence a outra conta."
        }
        return root
    }

    private fun writeBackup(file: File, root: JSONObject) {
        root.remove("trustPassword")
        root.remove("accountPasswordHash")
        val payload = root.toString().toByteArray(Charsets.UTF_8)
        require(payload.size <= MAX_BACKUP_BYTES) { "Backup excede o limite de 1 MiB." }
        check(file.parentFile!!.isDirectory || file.parentFile!!.mkdirs()) { "Não foi possível criar o backup local." }
        val atomic = AtomicFile(file)
        val output = atomic.startWrite()
        try {
            output.write(payload)
            atomic.finishWrite(output)
        } catch (error: Exception) {
            atomic.failWrite(output)
            throw error
        }
        // shortcut: Windows renameTo cannot overwrite an existing target, so AtomicFile's
        // commit rename can fail silently and strand a ".new" temp with the old base.
        // Compute checksums instead so we never leave a partial backup behind in prod.
        val pendingNew = File(file.path + ".new")
        if (pendingNew.exists()) {
            val current = runCatching { AtomicFile(file).openRead().use { input ->
                input.readBytes()
            } }.getOrNull()
            if (current == null || !current.contentEquals(payload)) {
                file.writeBytes(payload)
            }
            pendingNew.delete()
        }
    }

    private fun readBackup(context: Context, email: String): JSONObject? = synchronized(backupLock) {
        val current = getCloudBackupFile(context, email)
        if (current.exists() || File(current.path + ".bak").exists()) {
            return@synchronized readVerifiedBackup(current, email)
        }
        // Migrate only private legacy data after checking its embedded owner. External copies
        // never establish identity or supply settings, even if their timestamps are newer.
        val legacy = legacyBackupFile(context.filesDir, email)
        if (!legacy.exists()) return@synchronized null
        val root = readVerifiedBackup(legacy, email)
        writeBackup(current, root)
        legacy.delete()
        root
    }

    fun hasCloudBackup(context: Context, email: String): Boolean = try {
        readBackup(context, email) != null
    } catch (_: Exception) { false }

    fun getCloudBackupSummary(context: Context, email: String): CloudBackupSummary? = try {
        readBackup(context, email)?.let { root ->
            CloudBackupSummary(
                email = normalizeEmail(root.getString("email")),
                provider = root.optString("provider", "EMAIL"),
                timestamp = root.optLong("timestamp", 0L),
                appsCount = root.optJSONArray("restrictedApps")?.length() ?: 0,
                websitesCount = root.optJSONArray("restrictedWebsites")?.length() ?: 0,
                routinesCount = root.optJSONArray("focusRoutines")?.length() ?: 0,
                blacklistCount = root.optJSONArray("playStoreBlacklist")?.length() ?: 0,
                screenTimeLimitMinutes = root.optInt("screenTimeLimitMinutes", 0)
            )
        }
    } catch (_: Exception) { null }

    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val salted = "LOCK_IN_AUTH_SALT_${password}_PROTECT_V2"
        val hashBytes = digest.digest(salted.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun isAccountRegistered(context: Context, email: String): Boolean =
        getStoredPasswordHash(context, email) != null

    fun getStoredPasswordHash(context: Context, email: String): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("pass_hash_${normalizeEmail(email)}", null)?.takeIf { it.isNotBlank() }

    suspend fun registerAccount(
        context: Context,
        email: String,
        password: String,
        provider: EmailProvider = EmailProvider.fromEmail(email),
        displayName: String = ""
    ): Result<AccountProfile> = withContext(Dispatchers.IO) {
        val cleanEmail = normalizeEmail(email)
        if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return@withContext Result.failure(IllegalArgumentException("E-mail inválido. Digite um e-mail com formato válido."))
        }
        if (password.length !in 6..1024) {
            return@withContext Result.failure(IllegalArgumentException("A senha deve ter pelo menos 6 dígitos para proteger sua conta."))
        }
        val existingHash = getStoredPasswordHash(context, cleanEmail)
        if (existingHash != null) {
            return@withContext Result.failure(IllegalStateException("Este e-mail já está cadastrado no Lock In. Por favor, acesse a aba 'ENTRAR' usando a sua senha."))
        }
        val newHash = AccountPasswordHash.hash(password)
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
        val cleanEmail = normalizeEmail(email)
        if (cleanEmail.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Digite seu e-mail para entrar."))
        }
        val storedHash = getStoredPasswordHash(context, cleanEmail)
        if (storedHash == null) {
            return@withContext Result.failure(IllegalStateException("Nenhuma conta com este e-mail foi encontrada. Acesse a aba 'CRIAR CONTA' para registrar com senha."))
        }
        val valid = if (AccountPasswordHash.isCurrent(storedHash)) AccountPasswordHash.verify(password, storedHash)
            else password.length in 1..1024 && MessageDigest.isEqual(hashPassword(password).toByteArray(), storedHash.toByteArray())
        if (!valid) {
            return@withContext Result.failure(SecurityException("Senha incorreta. Não é possível acessar ou sincronizar os dados deste e-mail sem a senha correta."))
        }
        val upgraded = if (AccountPasswordHash.isCurrent(storedHash)) storedHash else AccountPasswordHash.hash(password)
        val provider = EmailProvider.fromEmail(cleanEmail)
        val profile = signInOrRegisterInternal(context, cleanEmail, provider, passwordHash = upgraded)
        Result.success(profile)
    }

    private suspend fun signInOrRegisterInternal(
        context: Context,
        email: String,
        provider: EmailProvider,
        displayName: String = "",
        passwordHash: String? = null
    ): AccountProfile = withContext(Dispatchers.IO) {
        val cleanEmail = normalizeEmail(email)
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

        // Logging in never rolls back active restrictions. Restore is a separate action.
        if (!hasCloudBackup(context, cleanEmail)) syncDataToCloud(context)

        profile
    }

    fun signOut(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_LOGGED_IN).remove(KEY_EMAIL).remove(KEY_PROVIDER)
            .remove(KEY_DISPLAY_NAME).remove(KEY_LAST_SYNC).apply()
        _lastSyncEpoch.value = 0L
        _currentAccount.value = null
        _syncMessage.value = "Desconectado da conta de sincronização."
    }

    suspend fun syncDataToCloud(context: Context): CloudSyncResult = withContext(Dispatchers.IO) { operationMutex.withLock {
        val account = _currentAccount.value
        if (account == null) {
            return@withContext CloudSyncResult(
                success = false,
                message = "Nenhuma conta conectada para sincronizar."
            )
        }

        _isSyncing.value = true
        _syncMessage.value = "Salvando backup local..."

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
                put("version", 3)
                put("email", account.email)
                put("provider", account.provider.name)
                put("displayName", account.displayName)
                put("timestamp", System.currentTimeMillis())

                // User preferences
                put("userName", userPrefs.userName.value)
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
                        put("targetPackage", r.targetPackage ?: JSONObject.NULL)
                        put("targetAppName", r.targetAppName ?: JSONObject.NULL)
                    }
                    routineArray.put(rObj)
                }
                put("focusRoutines", routineArray)

                // Play Store Blacklist
                put("playStoreBlacklist", JSONArray(blacklist))
                put("playStoreBlacklistEnabled", blacklistEnabled)
            }

            synchronized(backupLock) {
                writeBackup(getCloudBackupFile(context, account.email), rootJson)
                // Remove only our known legacy copies after the atomic private write succeeds.
                val legacy = legacyBackupFile(context.filesDir, account.email)
                if (legacy.exists() && runCatching { readVerifiedBackup(legacy, account.email) }.isSuccess) legacy.delete()
                try {
                    context.getExternalFilesDir(null)?.let { directory ->
                        val external = legacyBackupFile(directory, account.email)
                        if (external.exists() && runCatching { readVerifiedBackup(external, account.email) }.isSuccess) external.delete()
                    }
                } catch (_: Exception) { /* Never make a private backup depend on removable storage. */ }
            }

            val now = System.currentTimeMillis()
            val updatedProfile = account.copy(lastSyncEpoch = now, isSynced = true)
            // A completed save must never log back in an account that signed out meanwhile.
            if (_currentAccount.compareAndSet(account, updatedProfile)) {
                _lastSyncEpoch.value = now
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putLong(KEY_LAST_SYNC, now).apply()
            }

            val msg = "Backup local salvo com sucesso (${apps.size} apps, ${routines.size} rotinas, ${blacklist.size} na blacklist)."
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
    } }

    /** Validate the entire document before any preference/database mutation. */
    private fun validateRestore(root: JSONObject) {
        fun number(obj: JSONObject, key: String, min: Long = 0, max: Long = Int.MAX_VALUE.toLong()) {
            if (!obj.has(key)) return
            val value = obj.get(key)
            require(value is Number && value.toDouble().isFinite() && value.toDouble() == value.toLong().toDouble() && value.toLong() in min..max) { "Valor inválido: $key" }
        }
        fun text(obj: JSONObject, key: String) { require(obj.get(key) is String && obj.getString(key).isNotBlank() && obj.getString(key).length <= 2048) { "Campo inválido: $key" } }
        fun array(obj: JSONObject, key: String, max: Int, check: (Any) -> Unit) {
            if (!obj.has(key)) return
            val a = obj.getJSONArray(key)
            require(a.length() <= max) { "Itens em excesso: $key" }
            for (i in 0 until a.length()) check(a.get(i))
        }
        fun strings(obj: JSONObject, key: String, max: Int) = array(obj, key, max) { require(it is String && it.length <= 2048) }
        number(root, "screenTimeLimitMinutes")
        number(root, "screenTimeResetHours", 0, 8760)
        if (root.has("themeMode")) require(root.getString("themeMode") in setOf("SYSTEM", "DARK", "LIGHT"))
        if (root.has("textColorHex")) android.graphics.Color.parseColor(root.getString("textColorHex"))
        array(root, "restrictedApps", 1000) { value ->
            val a = value as JSONObject
            text(a, "packageName"); text(a, "appName")
            number(a, "individualLimitMinutes"); number(a, "shortVideoLimitMinutes")
            number(a, "individualRemainingSeconds", 0, Int.MAX_VALUE.toLong() * 60)
            number(a, "shortVideoRemainingSeconds", 0, Int.MAX_VALUE.toLong() * 60)
        }
        array(root, "restrictedWebsites", 1000) { value ->
            val w = value as JSONObject; text(w, "domain"); number(w, "limitMinutes")
            number(w, "remainingSeconds", 0, Int.MAX_VALUE.toLong() * 60)
        }
        array(root, "appGroups", 100) { value ->
            val g = value as JSONObject; text(g, "id"); text(g, "name"); number(g, "limitMinutes")
            number(g, "remainingSeconds", 0, Int.MAX_VALUE.toLong() * 60)
            if (g.has("packageNamesCsv")) require(g.get("packageNamesCsv") is String)
            strings(g, "packageNames", 1000)
        }
        array(root, "focusRoutines", 100) { value ->
            val r = value as JSONObject; text(r, "id"); text(r, "name")
            for (key in listOf("startHour", "endHour")) { require(r.has(key)); number(r, key, 0, 23) }
            for (key in listOf("startMinute", "endMinute")) { require(r.has(key)); number(r, key, 0, 59) }
            array(r, "daysOfWeek", 7) { require(it is Int && it in 1..7) }
            strings(r, "allowedPackages", 3)
            for (key in listOf("targetPackage", "targetAppName")) if (r.has(key) && !r.isNull(key)) text(r, key)
        }
        strings(root, "playStoreBlacklist", 1000)
    }

    suspend fun restoreDataFromCloud(context: Context, email: String, afterTrustChallenge: Boolean = false): CloudSyncResult = withContext(Dispatchers.IO) { operationMutex.withLock {
        val cleanEmail = normalizeEmail(email)
        if (_currentAccount.value?.email?.let(::normalizeEmail) != cleanEmail) {
            return@withContext CloudSyncResult(false, "Entre na conta correspondente antes de restaurar o backup.")
        }
        _isSyncing.value = true
        _syncMessage.value = "Restaurando backup local..."

        try {
            val root = readBackup(context, cleanEmail)
                ?: return@withContext CloudSyncResult(false, "Nenhum backup local válido encontrado para este e-mail.")

            validateRestore(root)
            val app = LockInApplication.instance
            if (app.userPreferences.isRegistered.value && !afterTrustChallenge) {
                return@withContext CloudSyncResult(false, "Conclua o desafio de confiança antes de restaurar as regras.")
            }
            val dao = app.database.lockInDao()
            val userPrefs = app.userPreferences

            // A settings backup never authenticates an account or replaces its trust password.
            val userName = root.optString("userName", "")
            val textColorHex = root.optString("textColorHex", "#FF5500")
            val themeMode = root.optString("themeMode", "SYSTEM")
            if (userName.isNotBlank()) userPrefs.updateUserName(userName)
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
                            isEnabled = rObj.optBoolean("isEnabled", true),
                            targetPackage = rObj.optString("targetPackage").takeIf { !rObj.isNull("targetPackage") && it.isNotBlank() },
                            targetAppName = rObj.optString("targetAppName").takeIf { !rObj.isNull("targetAppName") && it.isNotBlank() }
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

            val msg = "Backup local restaurado com sucesso! ($appsRestored apps, $routinesRestored rotinas, $blacklistRestored apps na blacklist)."
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
    } }
}
