package com.example

import android.app.Application
import com.example.util.JsonInputGuard
import com.example.service.PartyMessageSecurity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JsonInputGuardTest {
    @Test fun commentsCannotHideDeepArraysFromPreflight() {
        val payload = "{\"a\": /* \" */ " + "[".repeat(2000) + "0" + "]".repeat(2000) + " /* \" */ }"
        assertTrue(payload.length < 8192)
        // Stop at rejection; never pass this input to the recursive parser.
        assertFalse(JsonInputGuard.isValid(payload))
        assertFalse(PartyMessageSecurity.isShallowJson(payload))
    }
    @Test fun singleQuotesAndBareStringsAreRejected() {
        assertFalse(JsonInputGuard.isValid("{'a':'text'}"))
        assertFalse(JsonInputGuard.isValid("{\"a\":bare\"text}"))
        assertFalse(JsonInputGuard.isValid("{unquoted:1}"))
    }
    @Test fun normalJsonAndDelimitersInsideStringsWork() {
        assertTrue(JsonInputGuard.isValid("{\"a\":[true,false,null,12,-1.2e3],\"b\":\"/* [ ] ' \\\" */\"}"))
        assertTrue(JsonInputGuard.isValid("[".repeat(32) + "0" + "]".repeat(32)))
        assertFalse(JsonInputGuard.isValid("[".repeat(33) + "0" + "]".repeat(33)))
        assertFalse(JsonInputGuard.isValid("{\"n\":" + "9".repeat(129) + "}"))
        assertFalse(JsonInputGuard.isValid("{}{}"))
    }
}
