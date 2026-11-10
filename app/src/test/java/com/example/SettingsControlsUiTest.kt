package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.local.UserPreferencesRepository
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LockInTheme
import com.github.takahirom.roborazzi.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalRoborazziApi::class)
class SettingsControlsUiTest {
    @get:Rule val compose = createComposeRule()
    private val prefs get() = LockInApplication.instance.userPreferences
    private fun show() {
        compose.setContent {
            val theme by prefs.themeMode.collectAsState()
            val haptic by prefs.hapticFeedback.collectAsState()
            val reminder by prefs.streakReminders.collectAsState()
            LockInTheme(themeMode = theme) {
                SettingsScreen(theme, haptic, reminder, true, false, false,
                    currentPassword = { "" }, onSaveThemeMode = prefs::setThemeMode,
                    onSaveHapticFeedback = prefs::setHapticFeedback,
                    onSaveStreakReminders = prefs::setStreakReminders,
                    onSavePreLimitAlerts = {}, onSaveMindfulDelay = {}, onSaveStrictMode = {},
                    onUpdatePassword = {}, onResetToDefaults = {}, onNavigateToStyling = {},
                    onNavigateToResetSettings = {}, onNavigateToPermissions = {}, onBack = {})
            }
        }
    }
    @Test fun themeButtonsAndHeroPersistSelectionAndRenderBothThemes() {
        show()
        for (mode in listOf("DARK", "LIGHT", "SYSTEM")) {
            compose.onNodeWithTag("settings_theme_$mode").performClick().assertIsSelected()
            assertEquals(mode, UserPreferencesRepository(LockInApplication.instance).themeMode.value)
            compose.onRoot().captureRoboImage("build/outputs/settings/${mode.lowercase()}.png",
                roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record))
        }
        compose.onNodeWithTag("settings_hero_theme").performClick()
        compose.onNodeWithTag("settings_theme_DARK").assertIsSelected()
    }
    @Test fun shortcutsAndRowsToggleTheSamePersistentPreferences() {
        prefs.setHapticFeedback(true)
        prefs.setStreakReminders(true)
        show()
        compose.onNodeWithTag("settings_hero_haptic").performClick()
        compose.onNodeWithTag("settings_content").performScrollToNode(hasTestTag("settings_toggle_haptic"))
        compose.onNodeWithTag("settings_toggle_haptic").assertIsOff().performClick().assertIsOn()
        compose.onNodeWithTag("settings_content").performScrollToNode(hasTestTag("settings_hero_reminder"))
        compose.onNodeWithTag("settings_hero_reminder").performClick()
        compose.onNodeWithTag("settings_content").performScrollToNode(hasTestTag("settings_toggle_streak"))
        compose.onNodeWithTag("settings_toggle_streak").assertIsOff().performClick().assertIsOn()
        val restored = UserPreferencesRepository(LockInApplication.instance)
        assertTrue(restored.hapticFeedback.value)
        assertTrue(restored.streakReminders.value)
    }
    @Test fun dockNavigatesFromSettingsSubpageAndShowsSelectedTab() {
        var destination: Screen? = null
        compose.setContent { LockInTheme(themeMode = "DARK") {
            com.example.ui.components.AppBottomDock(Screen.STYLING,
                com.example.ui.theme.CmfDefaultOrange, { destination = it })
        } }
        compose.onNodeWithTag("dock_nav_settings").assertIsSelected().performClick()
        assertEquals(Screen.SETTINGS, destination)
        compose.onNodeWithTag("dock_nav_dashboard").performClick()
        assertEquals(Screen.DASHBOARD, destination)
    }
    @Test fun disabledNotificationsShowPermissionRecovery() {
        org.robolectric.Shadows.shadowOf(LockInApplication.instance)
            .denyPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        prefs.setStreakReminders(true)
        show()
        compose.onNodeWithTag("settings_content").performScrollToNode(hasTestTag("settings_reminder_permissions"))
        compose.onNodeWithTag("settings_reminder_permissions").assertIsDisplayed()
    }
    @Test fun portraitUsesTwoSeparateColumns() {
        prefs.setStreakReminders(false)
        show()
        compose.onNodeWithTag("settings_content").performScrollToNode(hasTestTag("settings_toggle_haptic"))
        val left = compose.onNodeWithTag("settings_toggle_haptic").fetchSemanticsNode().boundsInRoot
        val right = compose.onNodeWithTag("settings_toggle_streak").fetchSemanticsNode().boundsInRoot
        assertEquals(left.top, right.top, 1f)
        assertTrue(left.right < right.left)
    }
    @Test @Config(qualifiers = "w891dp-h411dp-land")
    fun landscapeUsesFourColumnsAndEverySectionRemainsReachable() {
        prefs.setStreakReminders(false)
        prefs.setThemeMode("DARK")
        show()
        compose.onNodeWithTag("settings_content").performScrollToNode(hasTestTag("settings_toggle_haptic"))
        val boxes = listOf("haptic", "streak", "pre_limit", "mindful").map {
            compose.onNodeWithTag("settings_toggle_$it").fetchSemanticsNode().boundsInRoot
        }
        boxes.forEach { assertEquals(boxes.first().top, it.top, 1f) }
        boxes.zipWithNext().forEach { (a,b) -> assertTrue(a.right < b.left) }
        compose.onRoot().captureRoboImage("build/outputs/settings/landscape.png",
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record))
        for (key in listOf("password", "uninstall", "permissions", "info_card")) {
            compose.onNodeWithTag("settings_content").performScrollToNode(hasTestTag("settings_$key"))
            compose.onNodeWithTag("settings_$key").assertIsDisplayed()
        }
    }
    @Test fun infoCardOpensTheCreditsDialog() {
        show()
        compose.onNodeWithTag("settings_content").performScrollToNode(hasTestTag("settings_info_card"))
        compose.onNodeWithTag("settings_info_card").performClick()
        compose.onNodeWithText("LOCK IN • DIGITAL WELLBEING").assertIsDisplayed()
    }
}
