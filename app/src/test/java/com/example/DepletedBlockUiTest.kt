package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.theme.LockInTheme
import com.example.ui.theme.CmfDefaultOrange
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DepletedBlockUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun exhaustedLimitShowsExistingScreenAndUnlockAction() {
        var opened = false
        compose.setContent { LockInTheme(themeMode = "DARK") {
            BlockedScreen("App de teste", "test.depleted", isDepletedLimit = true,
                accentColor = CmfDefaultOrange, onGoHome = {}, onOpenLockIn = { opened = true })
        } }
        compose.onNodeWithText("Você já excedeu o limite de uso deste app").assertIsDisplayed()
        compose.onNodeWithText("App de teste • 00:00 ESGOTADO").assertIsDisplayed()
        compose.onNodeWithTag("blocked_open_lock_in_button").performClick()
        assertTrue(opened)
    }
}
