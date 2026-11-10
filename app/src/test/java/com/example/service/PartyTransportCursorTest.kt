package com.example.service

import android.app.Application
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class PartyTransportCursorTest {
    private val code = "LCK2-" + "42".repeat(32)

    private fun encryptedEvent(id: String, timestamp: Long = 123456L): String = requireNotNull(
        PartyMessageSecurity.encrypt(code, JSONObject()
            .put("eventId", id)
            .put("timestamp", timestamp)
            .put("action", "ACTION_SYNC_MEMBER")
            .put("senderId", "peer")
            .put("payload", JSONObject().put("userId", "peer"))
            .toString())
    )

    private fun relayLine(id: String, message: String): String = JSONObject()
        .put("id", id)
        .put("time", 123L)
        .put("event", "message")
        .put("message", message)
        .toString()

    private fun boundedLines(lines: List<String>): List<String> = requireNotNull(
        PartyMessageSecurity.readBoundedLines(ByteArrayInputStream(lines.joinToString("\n").toByteArray(Charsets.UTF_8)))
    )

    @Test fun senderClockAheadDoesNotMoveTransportCursorIntoTheFuture() {
        val future = System.currentTimeMillis() + 86_400_000L
        val result = FriendsPartyCloudClient.parsePollBatch(code,
            listOf(relayLine("relay0000001", encryptedEvent("authenticated-id", future))))
        assertEquals("relay0000001", result.nextCursor)
        assertEquals("authenticated-id", result.events.single().eventId)
        assertEquals(future, result.events.single().timestamp)
    }

    @Test fun rejectedPlaintextAndInvalidAuthenticatedMessagesStillAdvanceCursor() {
        val plaintext = relayLine("relay0000001", "{\"action\":\"ACTION_RESOLVE_INSTALL\"}")
        val invalid = relayLine("relay0000002", encryptedEvent("invalid-time", 0L))
        val result = FriendsPartyCloudClient.parsePollBatch(code, listOf(plaintext, invalid))
        assertTrue(result.events.isEmpty())
        assertEquals("relay0000002", result.nextCursor)
    }

    @Test fun wrongEnvelopeVersionIsRejectedButDoesNotPinTheBatch() {
        val wrongVersion = encryptedEvent("old-protocol").replaceFirst("LCK2.", "LCK1.")
        val result = FriendsPartyCloudClient.parsePollBatch(code,
            listOf(relayLine("relay0000001", wrongVersion)))
        assertTrue(result.events.isEmpty())
        assertEquals("relay0000001", result.nextCursor)
    }

    @Test fun moreThanOneHundredMessagesInTheSameSecondResumeAfterTheLastId() {
        val lines = (1..150).map { index ->
            relayLine("relay" + index.toString().padStart(7, '0'), encryptedEvent("event-$index"))
        }
        val first = FriendsPartyCloudClient.parsePollBatch(code, boundedLines(lines))
        assertEquals(100, first.events.size)
        assertEquals("relay0000100", first.nextCursor)

        // Model the relay's since=<id> continuation using arrival order, not timestamps.
        val cursorIndex = lines.indexOfFirst { JSONObject(it).getString("id") == first.nextCursor }
        val second = FriendsPartyCloudClient.parsePollBatch(code, boundedLines(lines.drop(cursorIndex + 1)), first.nextCursor)
        assertEquals(50, second.events.size)
        assertEquals("relay0000150", second.nextCursor)
        assertEquals((1..150).map { "event-$it" }, (first.events + second.events).map { it.eventId })
    }

    @Test fun malformedOuterIdsCannotReplaceTheCursor() {
        val prior = "relay0000001"
        val invalidIds = listOf("", "short", "x".repeat(129), "../../secret", "abc&since=all", "é".repeat(12))
        val lines = invalidIds.map { relayLine(it, encryptedEvent("ignored")) } +
            JSONObject().put("event", "open").put("id", "relay0000002").toString()
        val result = FriendsPartyCloudClient.parsePollBatch(code, lines, prior)
        assertTrue(result.events.isEmpty())
        assertEquals(prior, result.nextCursor)
        assertNull(FriendsPartyCloudClient.parsePollBatch(code, emptyList(), "all&poll=0").nextCursor)
    }
}
