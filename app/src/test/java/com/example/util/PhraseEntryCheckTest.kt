package com.example.util

import com.example.ui.components.PhraseAttemptTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regras do usuário: maiúsculas/minúsculas livres, espaços extras perdoados,
 * ZERO tolerância a typo, 3 chances por frase, vermelho só na 1ª falha.
 */
class PhraseEntryCheckTest {

    private val phrase = "Eu reconheço que desbloquear este aplicativo quebra meu foco e compromisso com o meu futuro"

    @Test
    fun acceptsUpperAndLowerCase() {
        assertTrue(PhraseEntryCheck.isExactMatch(phrase.uppercase(), phrase))
        assertTrue(PhraseEntryCheck.isExactMatch(phrase.lowercase(), phrase))
        assertTrue(PhraseEntryCheck.isExactMatch("   Eu reconheço que desbloquear este aplicativo quebra meu foco e compromisso   com o meu futuro", phrase))
    }

    @Test
    fun rejectsAnyTypo() {
        assertFalse(PhraseEntryCheck.isExactMatch("Eu reconheço que desbloquear este aplicativo quebra meu foco e compromisso com o meu futuru", phrase))
        assertFalse(PhraseEntryCheck.isExactMatch("Eu reconheco que desbloquear este aplicativo quebra meu foco e compromisso com o meu futuro", phrase))
        assertFalse(PhraseEntryCheck.isExactMatch("Eu reconheço que desbloquear este aplicativo quebra meu foco e compromisso", phrase))
        assertFalse(PhraseEntryCheck.isExactMatch("Eu reconheço que desbloquear este aplicativo quebra meu foco e compromisso com o meu futuro agora", phrase))
    }

    @Test
    fun wrongSegmentsMarksOnlyWrongWords() {
        val input = "Eu reconheço que desbloquear este aplicativo quebra meu foco e comprometimento com o meu futuro"
        val segments = PhraseEntryCheck.wrongSegments(input, phrase)
        // Segmentos carregam as palavras ALVO: a errada aqui é "compromisso" (digitado "comprometimento").
        val wrong = segments.filter { it.isWrong }.map { it.text.trim() }
        assertEquals(listOf("compromisso"), wrong)

        val missing = PhraseEntryCheck.wrongSegments("Eu reconheço", phrase)
            .filter { it.isWrong }.map { it.text.trim() }
        assertTrue(missing.contains("aplicativo") && missing.contains("futuro"))
    }

    @Test
    fun trackerGivesThreeChancesAndRedOnlyOnFirst() {
        val tracker = PhraseAttemptTracker()
        assertFalse(tracker.showErrorHighlight)
        assertFalse(tracker.registerFailure("errrado"))
        assertTrue(tracker.showErrorHighlight)
        assertEquals("errrado", tracker.firstFailedInput)
        assertEquals(2, tracker.attemptsLeft)

        assertFalse(tracker.registerFailure("errrado de novo"))
        assertFalse(tracker.showErrorHighlight)
        assertNull(tracker.firstFailedInput)
        assertEquals(1, tracker.attemptsLeft)

        assertTrue(tracker.registerFailure("errrado pela terceira vez"))
        assertTrue(tracker.isExhausted)
        assertFalse(tracker.showErrorHighlight)
        assertEquals(0, tracker.attemptsLeft)
    }

    @Test
    fun trackerResetClearsHighlight() {
        val tracker = PhraseAttemptTracker()
        tracker.registerFailure("errrado")
        assertTrue(tracker.showErrorHighlight)
        tracker.reset()
        assertFalse(tracker.showErrorHighlight)
        assertNull(tracker.firstFailedInput)
        assertEquals(3, tracker.attemptsLeft)
    }
}