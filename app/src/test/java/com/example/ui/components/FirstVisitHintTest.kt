package com.example.ui.components

import androidx.test.core.app.ApplicationProvider
import com.example.Screen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Dica de primeira visita: cada tela tem um guia (objetivo + como usar) e so aparece
 * enquanto o usuario nao tiver visitado a tela. Teste puro (sem Compose rule) porque a
 * regra que importa e a de exibicao unica por tela; a UI e um card alimentado por esses dados.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirstVisitHintTest {

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun resetVisits() {
        FirstVisitHints.resetAll(context)
    }

    @After
    fun resetVisitsAfter() {
        FirstVisitHints.resetAll(context)
    }

    @Test
    fun everyScreenHasAGuideWithContent() {
        for (screen in Screen.entries) {
            val guide = guideFor(screen)
            assertTrue("titulo vazio para $screen", guide.title.isNotBlank())
            assertTrue("objetivo vazio para $screen", guide.objective.isNotBlank())
            assertTrue("como usar vazio para $screen", guide.howTo.isNotEmpty())
            assertTrue(
                "passo vazio no como usar de $screen",
                guide.howTo.all { it.isNotBlank() }
            )
        }
    }

    @Test
    fun firstVisitIsUnseenAndDismissPersists() {
        assertFalse(FirstVisitHints.wasVisited(context, Screen.SETTINGS))
        FirstVisitHints.markVisited(context, Screen.SETTINGS)
        assertTrue(FirstVisitHints.wasVisited(context, Screen.SETTINGS))
    }

    @Test
    fun eachScreenIsTrackedIndependently() {
        FirstVisitHints.markVisited(context, Screen.FOCUS_TIME)
        assertTrue(FirstVisitHints.wasVisited(context, Screen.FOCUS_TIME))
        assertFalse(FirstVisitHints.wasVisited(context, Screen.STATISTICS))
        assertFalse(FirstVisitHints.wasVisited(context, Screen.DASHBOARD))
    }

    @Test
    fun resetAllMakesEveryScreenUnseenAgain() {
        for (screen in Screen.entries) {
            FirstVisitHints.markVisited(context, screen)
        }
        for (screen in Screen.entries) {
            assertTrue(FirstVisitHints.wasVisited(context, screen))
        }
        FirstVisitHints.resetAll(context)
        for (screen in Screen.entries) {
            assertFalse(FirstVisitHints.wasVisited(context, screen))
        }
    }

    @Test
    fun seenScreensCountMatchesVisitedOnes() {
        val unseen = Screen.entries.filterNot { FirstVisitHints.wasVisited(context, it) }
        assertEquals(Screen.entries.size, unseen.size)
        FirstVisitHints.markVisited(context, Screen.WEB_LINKS)
        val unseenAfter = Screen.entries.filterNot { FirstVisitHints.wasVisited(context, it) }
        assertEquals(Screen.entries.size - 1, unseenAfter.size)
    }
}