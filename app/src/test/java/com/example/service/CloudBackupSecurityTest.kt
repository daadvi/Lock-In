package com.example.service

import com.example.LockInApplication
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CloudBackupSecurityTest {
    private val app get() = LockInApplication.instance
    private val prefs get() = app.getSharedPreferences("lock_in_cloud_account_prefs", 0)
    private val email = "owner@example.test"
    private val password = "Dummy-account-password-123"

    @Before fun reset() {
        CloudSyncManager.signOut(app)
        prefs.edit().clear().commit()
        File(app.filesDir, "cloud_backups").deleteRecursively()
        app.getExternalFilesDir(null)?.let { File(it, "cloud_backups").deleteRecursively() }
    }

    private suspend fun mutateAndAwaitAutomaticBackup(mutate: () -> Unit) {
        val job = app.applicationScope.coroutineContext[kotlinx.coroutines.Job]!!
        val existing = job.children.toSet()
        mutate()
        // Wait only for children started by this mutation, not permanent application observers.
        kotlinx.coroutines.withTimeout(5000) {
            job.children.filter { it !in existing }.toList().forEach { it.join() }
        }
    }

    private fun legacyFile(owner: String, external: Boolean = false): File {
        val directory = if (external) app.getExternalFilesDir(null)!! else app.filesDir
        return File(File(directory, "cloud_backups"), "backup_${owner.replace(Regex("[^a-zA-Z0-9_]"), "_")}.json")
            .also { it.parentFile!!.mkdirs() }
    }

    private fun fixture(owner: String) = JSONObject().put("email", owner).put("userName", "Restored")

    @Test fun punctuationDistinctAccountsHaveIndependentBackups() = runBlocking {
        val first = "a.b@example.test"
        val second = "a+b@example.test"
        assertTrue(CloudSyncManager.registerAccount(app, first, password).isSuccess)
        assertTrue(CloudSyncManager.registerAccount(app, second, password).isSuccess)
        assertEquals(first, CloudSyncManager.getCloudBackupSummary(app, first)!!.email)
        assertEquals(second, CloudSyncManager.getCloudBackupSummary(app, second)!!.email)
        val owners = File(app.filesDir, "cloud_backups").listFiles()!!.filter { it.extension == "json" }
            .map { JSONObject(it.readText()).getString("email") }.toSet()
        assertEquals(setOf(first, second), owners)
    }

    @Test fun legacyMigrationRequiresMatchingOwnerAndStripsSecrets() {
        val first = "a.b@example.test"
        val alias = "a+b@example.test"
        val legacy = legacyFile(first)
        legacy.writeText(fixture(first).put("trustPassword", "secret").put("accountPasswordHash", "credential").toString())
        assertFalse(CloudSyncManager.hasCloudBackup(app, alias))
        assertNull(CloudSyncManager.getStoredPasswordHash(app, alias))
        assertTrue(CloudSyncManager.hasCloudBackup(app, first))
        assertFalse(legacy.exists())
        val migrated = File(app.filesDir, "cloud_backups").listFiles()!!.single()
        val root = JSONObject(migrated.readText())
        assertFalse(root.has("trustPassword"))
        assertFalse(root.has("accountPasswordHash"))
    }

    @Test fun externalBackupCannotSupplyCredentialsOrSettings() = runBlocking {
        legacyFile(email, external = true).writeText(fixture(email)
            .put("accountPasswordHash", CloudSyncManager.hashPassword(password)).toString())
        assertFalse(CloudSyncManager.isAccountRegistered(app, email))
        assertFalse(CloudSyncManager.hasCloudBackup(app, email))
        assertTrue(CloudSyncManager.loginAccount(app, email, password).isFailure)
    }

    @Test fun restoreCannotReplaceOrRehashExistingTrustPassword() = runBlocking {
        app.userPreferences.setRegistration("Owner", "Original-trust-123")
        val originalHash = app.userPreferences.getTrustPassword()
        assertTrue(CloudSyncManager.registerAccount(app, email, password).isSuccess)
        val file = File(app.filesDir, "cloud_backups").listFiles()!!.single { it.extension == "json" }
        val root = JSONObject(file.readText())
            .put("trustPassword", originalHash)
            .put("accountPasswordHash", CloudSyncManager.hashPassword("forged-password"))
        file.writeText(root.toString())
        assertTrue(CloudSyncManager.restoreDataFromCloud(app, email, afterTrustChallenge = true).success)
        assertEquals(originalHash, app.userPreferences.getTrustPassword())
        assertTrue(app.userPreferences.verifyTrustPassword("Original-trust-123"))
        assertTrue(app.userPreferences.isRegistered.value)
        assertTrue(com.example.util.AccountPasswordHash.verify(password, CloudSyncManager.getStoredPasswordHash(app, email)!!))
    }

    @Test fun wrongAccountAndMalformedBackupsDoNotChangeCredentials() = runBlocking {
        assertTrue(CloudSyncManager.registerAccount(app, email, password).isSuccess)
        val original = CloudSyncManager.getStoredPasswordHash(app, email)
        legacyFile("other@example.test").writeText(fixture("other@example.test").toString())
        assertFalse(CloudSyncManager.restoreDataFromCloud(app, "other@example.test").success)
        val file = File(app.filesDir, "cloud_backups").listFiles()!!.single { it.name.startsWith("backup_v3_") }
        file.writeText(fixture("other@example.test").toString())
        assertFalse(CloudSyncManager.restoreDataFromCloud(app, email, afterTrustChallenge = true).success)
        file.writeText("{broken")
        assertFalse(CloudSyncManager.restoreDataFromCloud(app, email, afterTrustChallenge = true).success)
        assertEquals(original, CloudSyncManager.getStoredPasswordHash(app, email))
    }

    @Test fun oversizedBackupIsRejected() = runBlocking {
        assertTrue(CloudSyncManager.registerAccount(app, email, password).isSuccess)
        val file = File(app.filesDir, "cloud_backups").listFiles()!!.single()
        file.writeText(fixture(email).put("padding", "x".repeat(1024 * 1024)).toString())
        assertFalse(CloudSyncManager.hasCloudBackup(app, email))
        assertFalse(CloudSyncManager.restoreDataFromCloud(app, email, afterTrustChallenge = true).success)
    }

    @Test fun deeplyNestedBackupIsRejectedBeforeRecursiveParsing() = runBlocking {
        assertTrue(CloudSyncManager.registerAccount(app, email, password).isSuccess)
        val file = File(app.filesDir, "cloud_backups").listFiles()!!.single()
        file.writeText("{\"email\":\"$email\",\"nested\":" + "[".repeat(2000) + "0" + "]".repeat(2000) + "}")
        assertFalse(CloudSyncManager.hasCloudBackup(app, email))
        assertFalse(CloudSyncManager.restoreDataFromCloud(app, email, afterTrustChallenge = true).success)
    }

    @Test fun signOutKeepsCredentialsAndNewBackupsContainNoSecrets() = runBlocking {
        assertTrue(CloudSyncManager.registerAccount(app, email, password).isSuccess)
        val file = File(app.filesDir, "cloud_backups").listFiles()!!.single()
        val root = JSONObject(file.readText())
        assertFalse(root.has("trustPassword"))
        assertFalse(root.has("accountPasswordHash"))
        CloudSyncManager.signOut(app)
        assertNull(CloudSyncManager.currentAccount.value)
        assertTrue(CloudSyncManager.isAccountRegistered(app, email))
        assertTrue(CloudSyncManager.loginAccount(app, email, "wrong-password").isFailure)
        assertTrue(CloudSyncManager.loginAccount(app, email, password).isSuccess)
    }
    @Test fun invalidLateRoutineDoesNotPartiallyRestoreSettings() = runBlocking {
        assertTrue(CloudSyncManager.registerAccount(app, email, password).isSuccess)
        mutateAndAwaitAutomaticBackup { ScreenTimeManager.setLimitMinutes(app, 45) }
        val file = File(app.filesDir, "cloud_backups").listFiles()!!.single()
        val root = JSONObject(file.readText()).put("screenTimeLimitMinutes", 999)
            .put("focusRoutines", org.json.JSONArray().put(JSONObject().put("id", "bad").put("startHour", 99)))
        file.writeText(root.toString())
        assertFalse(CloudSyncManager.restoreDataFromCloud(app, email, afterTrustChallenge = true).success)
        assertEquals(45, ScreenTimeManager.limitMinutesFlow.value)
    }
    @Test fun appSpecificRoutineRetainsItsScopeAfterBackup() = runBlocking {
        assertTrue(CloudSyncManager.registerAccount(app, email, password).isSuccess)
        val routine = com.example.data.local.FocusRoutine(name = "App only", startHour = 10, startMinute = 0,
            endHour = 11, endMinute = 0, targetPackage = "test.app", targetAppName = "Test app")
        mutateAndAwaitAutomaticBackup { FocusTimeManager.setAllRoutines(app, listOf(routine)) }
        assertTrue(CloudSyncManager.syncDataToCloud(app).success)
        val file = File(app.filesDir, "cloud_backups").listFiles()!!.single()
        val snapshot = file.readText()
        mutateAndAwaitAutomaticBackup { FocusTimeManager.setAllRoutines(app, emptyList()) }
        file.writeText(snapshot)
        assertTrue(CloudSyncManager.restoreDataFromCloud(app, email, afterTrustChallenge = true).success)
        assertEquals("test.app", FocusTimeManager.getRoutines(app).single().targetPackage)
    }
    @Test fun legacyAccountCredentialUpgradesOnlyAfterSuccessfulLogin() = runBlocking {
        val legacy = CloudSyncManager.hashPassword(password)
        prefs.edit().putString("pass_hash_$email", legacy).commit()
        assertTrue(CloudSyncManager.loginAccount(app, email, "wrong").isFailure)
        assertEquals(legacy, CloudSyncManager.getStoredPasswordHash(app, email))
        assertTrue(CloudSyncManager.loginAccount(app, email, password).isSuccess)
        val upgraded = CloudSyncManager.getStoredPasswordHash(app, email)!!
        assertNotEquals(legacy, upgraded)
        assertTrue(com.example.util.AccountPasswordHash.verify(password, upgraded))
    }

    @Test fun loginAndRestoreCannotSilentlyRollBackProtectedLimits() = runBlocking {
        app.userPreferences.setRegistration("Owner", "Trust-password-123")
        assertTrue(CloudSyncManager.registerAccount(app, email, password).isSuccess)
        mutateAndAwaitAutomaticBackup { ScreenTimeManager.setLimitMinutes(app, 45) }
        assertFalse(CloudSyncManager.restoreDataFromCloud(app, email).success)
        CloudSyncManager.signOut(app)
        assertTrue(CloudSyncManager.loginAccount(app, email, password).isSuccess)
        assertEquals(45, ScreenTimeManager.limitMinutesFlow.value)
    }

}
