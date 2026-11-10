package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.theme.*
import com.example.ui.tutorial.*
import com.example.ui.screens.LockLevelStepContent
import com.example.data.model.LockLevel
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
class TutorialUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun guideCanHideFinishAndResumeWithoutBlockingLogin() {
        LockInApplication.instance.getSharedPreferences("lock_in_tutorial", 0).edit().clear().commit()
        compose.setContent { LockInTheme(themeMode = "DARK") { TutorialEntry() } }
        compose.onNodeWithTag("tutorial_open").performClick()
        compose.onNodeWithTag("tutorial_next").performClick()
        compose.onNodeWithTag("tutorial_title").assertTextEquals("Sua conta")
        compose.onNodeWithTag("tutorial_hide").performClick()
        compose.onNodeWithTag("tutorial_guide").assertDoesNotExist()
        compose.onNodeWithTag("tutorial_open").performClick()
        compose.onNodeWithTag("tutorial_title").assertTextEquals("Sua conta")
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        compose.onNodeWithTag("tutorial_guide").captureRoboImage("build/outputs/tutorial/guide-dark.png",
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record))
        repeat(tutorialLessons.lastIndex - 1) { compose.onNodeWithTag("tutorial_next").performClick() }
        compose.onNodeWithTag("tutorial_next").performClick()
        compose.onNodeWithTag("tutorial_guide").assertDoesNotExist()
        assertTrue(LockInApplication.instance.getSharedPreferences("lock_in_tutorial",0).getBoolean("completed",false))
    }
    @Test fun levelsHaveDifferentPalettesAndSelectionStillWorksInBothThemes() {
        var theme by mutableStateOf("DARK")
        var selected by mutableStateOf(LockLevel.BASIC)
        compose.setContent { LockInTheme(themeMode = theme) {
            val palettes = LockLevel.entries.map { lockLevelColors(it) }
            assertEquals(3, palettes.map { it.accent }.toSet().size)
            assertNotEquals(palettes[2].accent, palettes[2].end)
            androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                LockLevelStepContent(CmfDefaultOrange, selected, { selected = it }, {})
            }
        } }
        for (mode in listOf("DARK", "LIGHT")) {
            compose.runOnIdle { theme = mode }
            for (level in LockLevel.entries) {
                compose.onNodeWithTag("lock_level_${level.id}").performScrollTo().performClick().assertIsSelected()
                assertEquals(level, selected)
                compose.mainClock.advanceTimeBy(500)
                compose.waitForIdle()
                compose.onRoot().captureRoboImage("build/outputs/tutorial/level-${mode.lowercase()}-${level.id}.png",
                    roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record))
            }
        }
        assertEquals(listOf(1,3,5), LockLevel.entries.map { it.passwordRepeatCount })
    }
    @Test fun recordingNamesAreUniqueAndSafeForFiles() {
        assertEquals(28, tutorialLessons.size)
        assertEquals(28, tutorialLessons.map { it.audioName }.toSet().size)
        assertTrue(tutorialLessons.all { it.audioName.matches(Regex("[0-9]{2}_[a-z_]+")) && it.speech.isNotBlank() })
    }
}
