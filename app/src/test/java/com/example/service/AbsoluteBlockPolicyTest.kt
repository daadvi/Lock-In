package com.example.service

import android.content.Context
import com.example.LockInApplication
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.LockLevel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Bloqueio absoluto: sempre ativo por padrão, desafio por nível, impossível no Sonho do Dev. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AbsoluteBlockPolicyTest {
    private val app = LockInApplication.instance
    private val prefs get() = app.userPreferences

    @Before fun resetState() {
        app.getSharedPreferences("lock_in_user_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        prefs.setLockLevel(LockLevel.BASIC)
        prefs.setAbsoluteInstallBlock(true)
        PlayStoreBlacklistManager.setBlacklist(app, emptyList(), true)
    }

    @Test fun absoluteBlockIsActiveByDefault() {
        val fresh = UserPreferencesRepository(app)
        assertTrue(fresh.isAbsoluteInstallBlock.value)
    }

    @Test fun raisingToDevDreamForcesAbsoluteBlockOn() {
        prefs.setAbsoluteInstallBlock(false)
        prefs.setLockLevel(LockLevel.DEV_DREAM)
        assertTrue(prefs.isAbsoluteInstallBlockActive())
    }

    @Test fun devDreamCannotDeactivateEvenWithChallenge() {
        prefs.setLockLevel(LockLevel.DEV_DREAM)
        prefs.deactivateAbsoluteInstallBlockWithChallenge()
        assertTrue(prefs.isAbsoluteInstallBlockActive())
    }

    @Test fun lowerLevelsCanDeactivateWithChallenge() {
        prefs.setLockLevel(LockLevel.BASIC)
        prefs.deactivateAbsoluteInstallBlockWithChallenge()
        assertFalse(prefs.isAbsoluteInstallBlockActive())
    }

    @Test fun removeAppIsBlockedWhileAbsoluteBlockActive() {
        PlayStoreBlacklistManager.addApp(app, "TikTok")
        val removed = PlayStoreBlacklistManager.removeApp(app, "TikTok")
        assertFalse(removed)
        assertTrue(PlayStoreBlacklistManager.blacklistedApps.value.contains("TikTok"))
    }

    @Test fun removeAppWorksOnlyAfterChallengeDeactivation() {
        PlayStoreBlacklistManager.addApp(app, "TikTok")
        prefs.setLockLevel(LockLevel.DETERMINED)
        prefs.deactivateAbsoluteInstallBlockWithChallenge()
        val removed = PlayStoreBlacklistManager.removeApp(app, "TikTok")
        assertTrue(removed)
        assertFalse(PlayStoreBlacklistManager.blacklistedApps.value.contains("TikTok"))
    }

    @Test fun restoreStyleWriteCannotShrinkTheBlacklistWhileActive() {
        PlayStoreBlacklistManager.setBlacklist(app, listOf("TikTok", "Kwai"), true)
        // Simula um restore de backup voltando uma lista menor: só pode UNIR.
        PlayStoreBlacklistManager.setBlacklist(app, listOf("Kwai"), true)
        val apps = PlayStoreBlacklistManager.blacklistedApps.value
        assertTrue(apps.contains("TikTok"))
        assertTrue(apps.contains("Kwai"))
    }

    @Test fun devDreamNeverLosesBlacklistEntries() {
        PlayStoreBlacklistManager.setBlacklist(app, listOf("TikTok"), true)
        prefs.setLockLevel(LockLevel.DEV_DREAM)
        assertFalse(PlayStoreBlacklistManager.removeApp(app, "TikTok"))
        PlayStoreBlacklistManager.setBlacklist(app, emptyList(), true)
        assertTrue(PlayStoreBlacklistManager.blacklistedApps.value.contains("TikTok"))
    }
}
