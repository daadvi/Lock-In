package com.example.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.LockInApplication
import com.example.Screen
import com.example.ui.theme.LockInTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirstVisitHintTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var app: LockInApplication

    @Before
    fun resetVisits() {
        app = ApplicationProvider.getApplicationContext()
        FirstVisitHints.resetAll(app)
    }

    @Test
    fun firstVisitShowsHintAndDismissHidesItAndPersists() {
        compose.setContent {
            LockInTheme {
                FirstVisitHintOverlay(screen = Screen.SETTINGS)
            }
        }
        // Primeira visita na área: a dica aparece.
        compose.onNodeWithTag("first_visit_hint", useUnmergedTree = true).assertExists()

        // ENTENDI esconde a dica e grava a visita.
        compose.onNodeWithTag("first_visit_hint_dismiss", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("first_visit_hint", useUnmergedTree = true).assertDoesNotExist()
        assertTrue(FirstVisitHints.wasVisited(app, Screen.SETTINGS))
    }

    @Test
    fun anotherScreenHasItsOwnHintToo() {
        // Cada tela tem sua própria dica de primeira visita.
        compose.setContent {
            LockInTheme {
                FirstVisitHintOverlay(screen = Screen.FOCUS_TIME)
            }
        }
        compose.onNodeWithTag("first_visit_hint", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("first_visit_hint_dismiss", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("first_visit_hint", useUnmergedTree = true).assertDoesNotExist()

        assertFalse(FirstVisitHints.wasVisited(app, Screen.STATISTICS))
        assertTrue(FirstVisitHints.wasVisited(app, Screen.FOCUS_TIME))
    }

    @Test
    fun visitedScreenDoesNotShowHintAgain() {
        FirstVisitHints.markVisited(app, Screen.FRIENDS_PANEL)
        compose.setContent {
            LockInTheme {
                FirstVisitHintOverlay(screen = Screen.FRIENDS_PANEL)
            }
        }
        compose.onNodeWithTag("first_visit_hint", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun resetAllShowsHintsAgain() {
        FirstVisitHints.markVisited(app, Screen.SETTINGS)
        assertTrue(FirstVisitHints.wasVisited(app, Screen.SETTINGS))

        FirstVisitHints.resetAll(app)
        assertFalse(FirstVisitHints.wasVisited(app, Screen.SETTINGS))

        compose.setContent {
            LockInTheme {
                FirstVisitHintOverlay(screen = Screen.SETTINGS)
            }
        }
        compose.onNodeWithTag("first_visit_hint", useUnmergedTree = true).assertExists()
    }
}