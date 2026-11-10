package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.example.ui.screens.OnboardingState
import com.example.ui.screens.OnboardingStep
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OnboardingPermissionsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun backupRegistrationDoesNotSkipPermissionSetup() {
        compose.waitForIdle()
        compose.activityRule.scenario.onActivity { activity ->
            ViewModelProvider(activity)[OnboardingState::class.java].apply {
                name.value = "Conta restaurada"
                password.value = "SenhaDeTeste@2026"
                step.value = OnboardingStep.PERMISSIONS
            }
            // This is the same registration update emitted by backup restoration.
            LockInApplication.instance.userPreferences.setRegistration("Conta restaurada", "SenhaDeTeste@2026")
        }
        compose.waitForIdle()
        compose.onNodeWithTag("onboarding_permissions_next_button").assertExists()
        compose.onNodeWithText("Prepare o Lock In").assertExists()
        compose.waitUntil(5000) { compose.onAllNodesWithTag("permission_progress").fetchSemanticsNodes().isNotEmpty() }
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
        compose.onNodeWithText("Continuar para o nível de bloqueio").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/outputs/onboarding-permissions.png")
    }

    @Test fun recreationKeepsPermissionStepAndAuthenticationDraftInMemory() {
        compose.waitForIdle()
        lateinit var original: OnboardingState
        compose.activityRule.scenario.onActivity { activity ->
            original = ViewModelProvider(activity)[OnboardingState::class.java]
            original.step.value = OnboardingStep.PERMISSIONS
            original.name.value = "Pessoa"
            original.password.value = "SenhaDeTeste@2026"
        }
        compose.waitForIdle()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.activityRule.scenario.onActivity { activity ->
            val restored = ViewModelProvider(activity)[OnboardingState::class.java]
            assertSame(original, restored)
            assertEquals(OnboardingStep.PERMISSIONS, restored.step.value)
            assertEquals("SenhaDeTeste@2026", restored.password.value)
        }
        compose.onNodeWithTag("onboarding_permissions_next_button").assertExists()
    }
}
