package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.example.ui.screens.AppSelectionScreen
import com.example.ui.theme.LockInTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppSelectionUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun emptyAppListShowsExplanationInsteadOfBlankScreen() {
        compose.setContent {
            LockInTheme {
                AppSelectionScreen(emptyList(), onSaveApp = {}, onRemoveApp = {}, onBack = {})
            }
        }
        compose.waitUntil(5000) { compose.onAllNodesWithText("Nenhum aplicativo encontrado.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Nenhum aplicativo encontrado.").assertIsDisplayed()
    }
}
