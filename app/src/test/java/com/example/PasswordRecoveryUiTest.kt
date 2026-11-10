package com.example

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import com.example.ui.screens.ChangePasswordDialog
import com.example.ui.theme.LockInTheme
import com.example.util.CryptoPasswordHelper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class PasswordRecoveryUiTest {
    @get:Rule val compose = createComposeRule()
    private val app get() = LockInApplication.instance
    private val old = "SenhaAntiga!123"
    private val replacement = "NovaSenha!456"
    private var saves = 0
    private var launchedIntent: Intent? = null
    private var launchedRequestCode: Int? = null
    private val resultRegistry = object : ActivityResultRegistry() {
        override fun <I : Any?, O : Any?> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?
        ) {
            launchedRequestCode = requestCode
            launchedIntent = input as? Intent
        }
    }
    private val registryOwner = object : ActivityResultRegistryOwner {
        override val activityResultRegistry: ActivityResultRegistry = resultRegistry
    }
    @Before fun setup() {
        app.userPreferences.updatePassword(old)
        shadowOf(app.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).setIsDeviceSecure(true)
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
                LockInTheme { ChangePasswordDialog(app.userPreferences.getTrustPassword(), Color(0xFFFF5500), {
                    saves++; app.userPreferences.updatePassword(it)
                }, {}) }
            }
        }
    }
    private fun type(tag: String, value: String) {
        val field = compose.onNodeWithTag(tag).performScrollTo()
        value.forEach { field.performTextInput(it.toString()) }
    }
    private fun newPassword() { type("password_new", replacement); type("password_confirm", replacement) }
    private fun openRecovery() {
        compose.onNodeWithTag("password_forgot").performScrollTo().performClick()
    }
    private fun verifyRecovery() {
        compose.onNodeWithTag("password_recovery_verify").performScrollTo().performClick()
        assertEquals("android.app.action.CONFIRM_DEVICE_CREDENTIAL", launchedIntent?.action)
    }
    private fun recover() { openRecovery(); verifyRecovery() }
    private fun result(code: Int) {
        compose.runOnIdle {
            resultRegistry.dispatchResult(requireNotNull(launchedRequestCode), code, Intent())
        }
    }

    @Test fun changingAnExistingHashedPasswordWorksAndWrongCaseIsRejected() {
        type("password_current", old.lowercase())
        compose.onNodeWithTag("password_current_verify").assertIsNotEnabled()
        compose.onNodeWithTag("password_current").performTextClearance()
        type("password_current", old)
        compose.onNodeWithTag("password_current_verify").performScrollTo().assertIsEnabled().performClick()
        newPassword()
        compose.onNodeWithTag("password_save").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(1, saves)
            assertTrue(CryptoPasswordHelper.verifyPassword(replacement, app.userPreferences.getTrustPassword()))
            assertFalse(CryptoPasswordHelper.verifyPassword(old, app.userPreferences.getTrustPassword()))
        }
    }
    @Test fun recoveryRequiresAndroidSuccessAndPersistsOnlyTheNewHash() {
        recover()
        assertEquals("android.app.action.CONFIRM_DEVICE_CREDENTIAL", launchedIntent?.action)
        compose.onNodeWithTag("password_save").assertDoesNotExist()
        result(Activity.RESULT_OK)
        newPassword()
        compose.onNodeWithTag("password_save").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(1, saves)
            val reloaded = com.example.data.local.UserPreferencesRepository(app)
            assertTrue(CryptoPasswordHelper.verifyPassword(replacement, reloaded.getTrustPassword()))
            assertNotEquals(replacement, reloaded.getTrustPassword())
        }
    }
    @Test fun cancelledCredentialConfirmationCannotResetThePassword() {
        recover(); result(Activity.RESULT_CANCELED)
        compose.onNodeWithTag("password_save").assertDoesNotExist()
        assertEquals(0, saves)
        assertTrue(CryptoPasswordHelper.verifyPassword(old, app.userPreferences.getTrustPassword()))
    }
    @Test fun recoveryFailsClosedWithoutDeviceScreenLock() {
        shadowOf(app.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).setIsDeviceSecure(false)
        openRecovery()
        compose.onNodeWithTag("password_recovery_verify").performScrollTo().performClick()
        assertNull(launchedIntent)
        compose.onNodeWithTag("password_save").assertDoesNotExist()
        compose.onNodeWithTag("password_error").assertTextContains(
            "Este aparelho não tem PIN", substring = true, ignoreCase = true
        )
        assertEquals(0, saves)
    }
    @Test fun aPreviousAuthenticationCannotBeUsedAfterExpiry() {
        recover(); result(Activity.RESULT_OK)
        newPassword()
        compose.onNodeWithTag("password_save").assertIsEnabled()
        compose.runOnIdle { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(121)) }
        if (compose.onAllNodesWithTag("password_save").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("password_save").performClick()
        }
        compose.runOnIdle { assertEquals(0, saves) }
    }
    @Test fun mismatchedConfirmationCannotBeSavedAfterAuthentication() {
        recover(); result(Activity.RESULT_OK)
        type("password_new", replacement); type("password_confirm", replacement.lowercase())
        compose.onNodeWithTag("password_save").assertIsNotEnabled()
        assertEquals(0, saves)
    }
}
