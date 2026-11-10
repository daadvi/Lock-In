package com.example.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.UUID

data class CloudPartyEvent(
    val eventId: String,
    val timestamp: Long,
    val action: String,
    val senderId: String,
    val payload: JSONObject
)

data class CloudPartyPollBatch(
    val events: List<CloudPartyEvent>,
    val nextCursor: String?
)

object FriendsPartyCloudClient {

    private const val TAG = "PartyCloudClient"
    private const val BASE_URL = "https://ntfy.sh"
    private val MESSAGE_MEDIA_TYPE = "text/plain; charset=utf-8".toMediaType()
    private val transportIdPattern = Regex("[A-Za-z0-9]{12}")

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private fun getTopicForParty(code: String): String? {
        return PartyMessageSecurity.topicForCode(code)
    }

    /**
     * Publishes an event to the cloud room topic.
     */
    suspend fun publishEvent(
        partyCode: String,
        action: String,
        senderId: String,
        payload: JSONObject,
        notificationTitle: String? = null,
        notificationBody: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val topic = getTopicForParty(partyCode) ?: return@withContext false
        val eventObj = JSONObject().apply {
            put("eventId", UUID.randomUUID().toString())
            put("action", action)
            put("senderId", senderId)
            put("timestamp", System.currentTimeMillis())
            put("payload", payload)
        }

        val encrypted = PartyMessageSecurity.encrypt(partyCode, eventObj.toString()) ?: return@withContext false
        return@withContext try {
            currentCoroutineContext().ensureActive()
            val url = "$BASE_URL/$topic"
            val requestBuilder = Request.Builder()
                .url(url)
                .post(encrypted.toRequestBody(MESSAGE_MEDIA_TYPE))
            // Do not leak member names, app names, or approval details in relay headers.

            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Log.w(TAG, "Party publish failed")
            false
        }
    }

    /** Relay IDs paginate arrival order independently of members' device clocks. */
    suspend fun pollEvents(
        partyCode: String,
        sinceCursor: String?
    ): CloudPartyPollBatch = withContext(Dispatchers.IO) {
        val cursor = validTransportId(sinceCursor)
        val unchanged = CloudPartyPollBatch(emptyList(), cursor)
        val topic = getTopicForParty(partyCode) ?: return@withContext unchanged

        return@withContext try {
            val url = BASE_URL.toHttpUrl().newBuilder()
                .addPathSegment(topic)
                .addPathSegment("json")
                .addQueryParameter("poll", "1")
                .addQueryParameter("since", cursor ?: "all")
                .build()

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                // An expired relay ID can no longer locate a cached message. Restart
                // from the available cache; event IDs and pending status prevent reapproval.
                if (cursor != null && response.code in setOf(400, 404)) {
                    return@withContext CloudPartyPollBatch(emptyList(), null)
                }
                if (!response.isSuccessful) {
                    return@withContext unchanged
                }

                val body = response.body?.byteStream() ?: return@withContext unchanged
                val lines = PartyMessageSecurity.readBoundedLines(body) ?: return@withContext unchanged
                currentCoroutineContext().ensureActive()
                parsePollBatch(partyCode, lines, cursor)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Log.w(TAG, "Party polling failed")
            unchanged
        }
    }

    private fun validTransportId(id: String?): String? =
        id?.takeIf { it.length == 12 && transportIdPattern.matches(it) }

    /** Input comes from the bounded line reader. Invalid contents still consume a relay ID. */
    internal fun parsePollBatch(partyCode: String, lines: List<String>, sinceCursor: String? = null): CloudPartyPollBatch {
        val events = mutableListOf<CloudPartyEvent>()
        var cursor = validTransportId(sinceCursor)
        for (line in lines.take(PartyMessageSecurity.MAX_POLL_LINES)) {
            if (line.length > PartyMessageSecurity.MAX_LINE_BYTES ||
                line.toByteArray(Charsets.UTF_8).size > PartyMessageSecurity.MAX_LINE_BYTES) continue
            val trimmed = line.trim()
            if (!trimmed.startsWith("{") || !trimmed.endsWith("}") || !PartyMessageSecurity.isShallowJson(trimmed)) continue
            try {
                val root = JSONObject(trimmed)
                if (root.optString("event") != "message") continue
                val transportId = validTransportId(root.opt("id") as? String) ?: continue
                cursor = transportId
                val plaintext = PartyMessageSecurity.decrypt(partyCode, root.optString("message", "")) ?: continue
                if (!PartyMessageSecurity.isShallowJson(plaintext)) continue
                val inner = JSONObject(plaintext)
                val eventId = inner.optString("eventId", "")
                val timestamp = inner.optLong("timestamp", 0L)
                // This authenticated timestamp is metadata, never a transport cursor.
                if (eventId.isBlank() || eventId.length > 128 || timestamp <= 0) continue
                events.add(CloudPartyEvent(
                    eventId = eventId,
                    timestamp = timestamp,
                    action = inner.optString("action"),
                    senderId = inner.optString("senderId"),
                    payload = inner.optJSONObject("payload") ?: JSONObject()
                ))
            } catch (_: Exception) {}
        }
        return CloudPartyPollBatch(events, cursor)
    }
}
