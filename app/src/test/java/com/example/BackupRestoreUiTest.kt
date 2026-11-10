package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.service.CloudSyncManager
import com.example.service.ScreenTimeManager
import com.example.ui.components.AccountSyncDialog
import com.example.ui.theme.LockInTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class BackupRestoreUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun restoreOpensTrustChallengeAndCancelKeepsCurrentRules() = runBlocking {
        val app = LockInApplication.instance
        app.userPreferences.setRegistration("Owner", "Trust-pass-123")
        CloudSyncManager.registerAccount(app, "backup-ui@example.test", "Dummy-account-pass-123").getOrThrow()
        ScreenTimeManager.setLimitMinutes(app, 45)
        compose.setContent { LockInTheme { AccountSyncDialog(Color(0xFFFF5500), {}) } }
        compose.onNodeWithTag("restore_cloud_button").performScrollTo().performClick()
        compose.onNodeWithTag("trust_unlock_password_input").assertExists()
        assertEquals(45, ScreenTimeManager.limitMinutesFlow.value)
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithTag("restore_cloud_button").assertExists()
        assertEquals(45, ScreenTimeManager.limitMinutesFlow.value)
    }
}
