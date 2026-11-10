package com.example

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import com.example.ui.components.PermissionSetup
import com.example.ui.theme.LockInTheme
import com.example.util.AppPermission
import com.example.util.PermissionAccess
import com.example.util.PermissionSnapshot
import com.example.util.PermissionStatus
import com.github.takahirom.roborazzi.captureRoboImage
import java.util.concurrent.atomic.AtomicInteger
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
class PermissionSetupUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private class FakeAccess : PermissionAccess {
        @Volatile var state = PermissionSnapshot(AppPermission.entries.associateWith { PermissionStatus.PENDING }, true)
        val reads = AtomicInteger()
        val opened = mutableListOf<AppPermission>()
        var detailsOpened = 0
        var canOpen = true
        override fun read(): PermissionSnapshot { reads.incrementAndGet(); return state }
        override fun open(permission: AppPermission): Boolean { opened.add(permission); return canOpen }
        override fun openAppDetails(): Boolean { detailsOpened++; return canOpen }
    }

    private fun show(
        access: FakeAccess,
        themeMode: String = "LIGHT",
        fontScale: Float = 1f,
        requireEssentialPermissions: Boolean = false,
        onContinue: () -> Unit = {}
    ) {
        compose.setContent {
            LockInTheme(themeMode = themeMode) {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        PermissionSetup(
                            modifier = Modifier.padding(16.dp),
                            onContinue = onContinue,
                            requireEssentialPermissions = requireEssentialPermissions,
                            permissionAccess = access
                        )
                    }
                }
            }
        }
        compose.waitUntil(5000) { compose.onAllNodesWithTag("permission_progress").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }

    private fun resumeActivity() {
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
    }

    @Test fun returnFromAndroidRefreshesActualStatusAndOffersNextPermissionWithoutPolling() {
        val access = FakeAccess()
        show(access)
        compose.onNodeWithTag("permission_progress").assertTextEquals("0 de 7 autorizações ativadas")
        compose.onNodeWithTag("permission_next").performClick()
        assertEquals(listOf(AppPermission.ACCESSIBILITY), access.opened)
        val readCount = access.reads.get()
        access.state = access.state.copy(statuses = access.state.statuses + (AppPermission.ACCESSIBILITY to PermissionStatus.GRANTED))
        compose.mainClock.advanceTimeBy(10_000)
        compose.waitForIdle()
        assertEquals(readCount, access.reads.get())
        compose.onNodeWithTag("permission_progress").assertTextEquals("0 de 7 autorizações ativadas")
        resumeActivity()
        compose.waitUntil(5000) { compose.onAllNodesWithText("1 de 7 autorizações ativadas").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("permission_next").performClick()
        assertEquals(AppPermission.USAGE, access.opened.last())
        compose.onRoot().captureRoboImage("build/outputs/permission-setup.png")
    }

    @Test fun restrictedSettingsGuideIsReachableDuringLoginAndDoesNotPretendToGrantAccess() {
        val access = FakeAccess()
        show(access)
        compose.onNodeWithTag("permission_list").performScrollToNode(hasTestTag("restricted_settings_help"))
        compose.onNodeWithTag("restricted_settings_help").performClick()
        compose.onNodeWithText("Permitir configurações restritas").assertIsDisplayed()
        compose.onNodeWithTag("restricted_open_details").performClick()
        assertEquals(1, access.detailsOpened)
        compose.onNodeWithTag("restricted_open_accessibility").performClick()
        assertEquals(listOf(AppPermission.ACCESSIBILITY), access.opened)
        assertFalse(access.state.isGranted(AppPermission.ACCESSIBILITY))
    }

    @Test fun missingEssentialPermissionsRequireAnExplicitChoiceToContinue() {
        var continued = false
        show(FakeAccess()) { continued = true }
        compose.onNodeWithTag("onboarding_permissions_next_button").performClick()
        compose.onNodeWithText("Continuar com funções limitadas?").assertIsDisplayed()
        assertFalse(continued)
        compose.onNodeWithTag("permission_continue_anyway").performClick()
        assertTrue(continued)
    }

    @Test fun guidedOnboardingWaitsForEveryEssentialPermission() {
        val access = FakeAccess()
        var continued = false
        show(access, requireEssentialPermissions = true) { continued = true }
        compose.onNodeWithTag("permission_tutorial_requirement").assertIsDisplayed()
        compose.onNodeWithTag("onboarding_permissions_next_button").assertIsNotEnabled()
        access.state = access.state.copy(statuses = AppPermission.entries.associateWith {
            if (it.essential) PermissionStatus.GRANTED else PermissionStatus.PENDING
        })
        resumeActivity()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("3 de 3 acessos essenciais prontos").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("onboarding_permissions_next_button").assertIsEnabled().performClick()
        assertTrue(continued)
    }

    @Test fun optionalPermissionsDoNotPreventContinuingOnceEssentialAccessIsGranted() {
        val access = FakeAccess()
        access.state = access.state.copy(statuses = AppPermission.entries.associateWith {
            if (it.essential) PermissionStatus.GRANTED else PermissionStatus.PENDING
        })
        var continued = false
        show(access) { continued = true }
        compose.onNodeWithTag("onboarding_permissions_next_button").performClick()
        assertTrue(continued)
        compose.onNodeWithText("Continuar com funções limitadas?").assertDoesNotExist()
    }

    @Test fun missingAndroidSettingsScreenShowsRecoveryInstructions() {
        val access = FakeAccess().apply { canOpen = false }
        show(access)
        compose.onNodeWithTag("permission_next").performClick()
        compose.onNodeWithTag("permission_list").performScrollToNode(hasTestTag("permission_feedback"))
        compose.onNodeWithTag("permission_feedback").assertTextContains("Não foi possível abrir essa tela", substring = true)
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp")
    fun smallScreenWithLargerTextKeepsNavigationReachableInDarkMode() {
        show(FakeAccess(), themeMode = "DARK", fontScale = 1.3f)
        compose.onNodeWithTag("onboarding_permissions_next_button").assertIsDisplayed()
        compose.onNodeWithTag("permission_list").performScrollToNode(hasTestTag("restricted_settings_help"))
        compose.onNodeWithTag("restricted_settings_help").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/outputs/permission-setup-compact.png")
        compose.onNodeWithTag("onboarding_permissions_next_button").performClick()
        compose.onNodeWithText("Continuar com funções limitadas?").assertIsDisplayed()
    }
}
