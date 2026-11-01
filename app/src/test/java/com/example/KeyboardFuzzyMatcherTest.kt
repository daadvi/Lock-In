package com.example

import com.example.util.KeyboardFuzzyMatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardFuzzyMatcherTest {

    @Test
    fun testExactMatch() {
        val target = "Eu quero desbloquear"
        assertTrue(KeyboardFuzzyMatcher.matchesWithKeyboardTolerance(target, target))
    }

    @Test
    fun testCaseInsensitive() {
        val target = "Não vou procrastinar agora"
        val inputUpper = "NÃO VOU PROCRASTINAR AGORA"
        val inputMixed = "não Vou Procrastinar Agora"
        assertTrue(KeyboardFuzzyMatcher.matchesWithKeyboardTolerance(inputUpper, target))
        assertTrue(KeyboardFuzzyMatcher.matchesWithKeyboardTolerance(inputMixed, target))
    }

    @Test
    fun testAccentTolerance() {
        val target = "Não vou procrastinar agora"
        val inputNoAccents = "nao vou procrastinar agora"
        assertTrue(KeyboardFuzzyMatcher.matchesWithKeyboardTolerance(inputNoAccents, target))
    }

    @Test
    fun testMinorAdjacentKeyTypo() {
        val target = "Eu escolho meu futuro hoje"
        // 'w' is adjacent to 'e' ('Eu' -> 'Wu'), or 'j' adjacent to 'h' ('hoje' -> 'joje')
        val inputMinorTypo = "Eu escolho meu futuro joje"
        assertTrue(KeyboardFuzzyMatcher.matchesWithKeyboardTolerance(inputMinorTypo, target))
    }

    @Test
    fun testTranspositionTypo() {
        val target = "Eu escolho meu futuro hoje"
        // 'eu' -> 'ue'
        val inputTransposition = "Ue escolho meu futuro hoje"
        assertTrue(KeyboardFuzzyMatcher.matchesWithKeyboardTolerance(inputTransposition, target))
    }

    @Test
    fun testStrictRejectionOfExcessiveTypos() {
        val target = "Eu escolho meu futuro hoje"
        val gibberish = "Eu xzpqe meu asdfg hoje"
        assertFalse(KeyboardFuzzyMatcher.matchesWithKeyboardTolerance(gibberish, target))
    }

    @Test
    fun testStrictRejectionOfSkippedWords() {
        val target = "Eu escolho meu futuro hoje"
        val partial = "Eu meu hoje"
        assertFalse(KeyboardFuzzyMatcher.matchesWithKeyboardTolerance(partial, target))
    }

    @Test
    fun testLongMotivationalPhraseWithMinorTypo() {
        val longPhrase = "Eu realmente quero instalar um dos apps da lista de bloqueio e tenho consciência das minhas ações e compreendo que posso estar fazendo algo errado!"
        // Typo: 'reslmente' ('s' adjacent to 'a') and 'conscirncia' ('r' adjacent to 'e')
        val inputMinor = "Eu reslmente quero instalar um dos apps da lista de bloqueio e tenho conscirncia das minhas ações e compreendo que posso estar fazendo algo errado!"
        assertTrue(KeyboardFuzzyMatcher.matchesWithKeyboardTolerance(inputMinor, longPhrase))

        // Total gibberish must fail
        val badInput = "Eu quero instalar coisas erradas agora mesmo sem ligar para nada"
        assertFalse(KeyboardFuzzyMatcher.matchesWithKeyboardTolerance(badInput, longPhrase))
    }
}
