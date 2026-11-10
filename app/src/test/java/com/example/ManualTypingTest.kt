package com.example

import com.example.util.isManualTyping
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualTypingTest {
    @Test fun rejectsBulkInputEvenWhenSelectionReplacementMakesTextShorter() {
        assertFalse(isManualTyping("", "frase ditada"))
        assertFalse(isManualTyping("xxxxxxxxxxx", "texto colado"))
        assertFalse(isManualTyping("muitos caracteres selecionados", "colar"))
        assertFalse(isManualTyping("aa", "bc"))
        assertFalse(isManualTyping("antes xxxx depois", "antes cola depois"))
    }

    @Test fun allowsTypingAccentsCursorEditsDeletionAndUnicode() {
        assertTrue(isManualTyping("a", "ab"))
        assertTrue(isManualTyping("ac", "abc"))
        assertTrue(isManualTyping("a", "á"))
        assertTrue(isManualTyping("a", "a\u0301"))
        assertTrue(isManualTyping("", "😀"))
        assertTrue(isManualTyping("12345", "1"))
        assertTrue(isManualTyping("12345", ""))
        assertTrue(isManualTyping("seleção", "x"))
        assertTrue(isManualTyping("mesmo texto", "mesmo texto"))
    }
}
