package com.example.service

import android.app.Application
import android.util.Base64
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class PartyMessageSecurityTest {
    private val code = "LCK2-" + "42".repeat(32)

    @Test fun invitationsAreRandomAndLegacyCodesFailClosed() {
        val first = PartyMessageSecurity.createCode()
        val second = PartyMessageSecurity.createCode()
        assertTrue(PartyMessageSecurity.isValidCode(first))
        assertEquals(69, first.length)
        assertNotEquals(first, second)
        assertEquals(code, PartyMessageSecurity.normalizeCode("  ${code.lowercase()}  "))
        for (bad in listOf("", "LCK-4829", "ABC123", code.replace('-', '_'), code + "0")) {
            assertFalse(PartyMessageSecurity.isValidCode(bad))
            assertNull(PartyMessageSecurity.topicForCode(bad))
            assertNull(PartyMessageSecurity.encrypt(bad, "secret"))
            assertNull(PartyMessageSecurity.decrypt(bad, "LCK2.invalid"))
        }
    }

    @Test fun encryptedRoundTripUsesDistinctNoncesAndOpaqueTopics() {
        val payload = "{\"action\":\"sync_member\",\"name\":\"João\"}"
        val first = requireNotNull(PartyMessageSecurity.encrypt(code, payload))
        val second = requireNotNull(PartyMessageSecurity.encrypt(code, payload))
        assertEquals(payload, PartyMessageSecurity.decrypt(code, first))
        assertNotEquals(first, second)
        assertFalse(first.contains("João"))
        val topic = requireNotNull(PartyMessageSecurity.topicForCode(code))
        assertFalse(topic.contains(code.removePrefix("LCK2-"), ignoreCase = true))
        assertEquals(topic, PartyMessageSecurity.topicForCode(code.lowercase()))
        assertNotEquals(topic, PartyMessageSecurity.topicForCode("LCK2-" + "43".repeat(32)))
    }

    @Test fun wrongKeyTamperedNonceCiphertextAndVersionAreRejected() {
        val envelope = requireNotNull(PartyMessageSecurity.encrypt(code, "private approval"))
        assertNull(PartyMessageSecurity.decrypt("LCK2-" + "43".repeat(32), envelope))
        val bytes = Base64.decode(envelope.removePrefix("LCK2."), Base64.NO_WRAP)
        for (index in listOf(0, 12, bytes.lastIndex)) {
            val tampered = bytes.clone().also { it[index] = (it[index].toInt() xor 1).toByte() }
            assertNull(PartyMessageSecurity.decrypt(code, "LCK2." + Base64.encodeToString(tampered, Base64.NO_WRAP)))
        }
        assertNull(PartyMessageSecurity.decrypt(code, envelope.replaceFirst("LCK2.", "LCK1.")))
    }

    @Test fun plaintextMalformedTruncatedAndOversizedMessagesAreRejected() {
        for (envelope in listOf("{\"action\":\"resolve_install\"}", "LCK2.", "LCK2.!invalid", "LCK2.AAAA", "LCK2." + "A".repeat(4096))) {
            assertNull(PartyMessageSecurity.decrypt(code, envelope))
        }
        assertNull(PartyMessageSecurity.encrypt(code, "x".repeat(3001)))
        assertNull(PartyMessageSecurity.encrypt(code, "🤖".repeat(1000)))
        val maximum = requireNotNull(PartyMessageSecurity.encrypt(code, "x".repeat(3000)))
        assertTrue(maximum.toByteArray().size <= PartyMessageSecurity.MAX_ENVELOPE_BYTES)
        assertEquals("x".repeat(3000), PartyMessageSecurity.decrypt(code, maximum))
    }

    @Test fun lineReaderSupportsNormalAndUnterminatedFinalLines() {
        assertEquals(listOf("one", "two"), read("one\ntwo"))
        assertEquals(listOf("one", "two"), read("one\ntwo\n"))
        assertEquals(emptyList<String>(), read(""))
        assertEquals(8192, read("x".repeat(8192))?.single()?.length)
    }

    @Test fun oversizedLinesAndMessageFloodsAreBoundedBeforeParsing() {
        assertNull(read("x".repeat(8193)))
        assertEquals(100, read("\n".repeat(101))?.size)
        assertEquals(100, read("x\n".repeat(100))?.size)
        assertEquals(32, read(("x".repeat(8191) + "\n").repeat(33))?.size)
        // Limits count bytes, not UTF-16 characters.
        assertNull(read("é".repeat(4097)))
    }

    private fun read(value: String) = PartyMessageSecurity.readBoundedLines(ByteArrayInputStream(value.toByteArray(Charsets.UTF_8)))
    @Test fun relayTopicUsesOnlySafeCharactersAndFitsLengthBudget() {
        val topic = PartyMessageSecurity.topicForCode(PartyMessageSecurity.createCode())!!
        assertTrue(topic.length <= 64)
        assertTrue(Regex("[A-Za-z0-9_-]+").matches(topic))
    }

    @Test fun excessiveJsonNestingIsRejectedBeforeParsing() {
        assertFalse(PartyMessageSecurity.isShallowJson("[".repeat(2000) + "0" + "]".repeat(2000)))
        assertTrue(PartyMessageSecurity.isShallowJson("{\"a\":[{\"b\":\"[\"}]}"))
    }

}
