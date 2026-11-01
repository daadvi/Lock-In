package com.example.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class CloudPartyEvent(
    val eventId: String,
    val timestamp: Long,
    val action: String,
    val senderId: String,
    val payload: JSONObject
)

object FriendsPartyCloudClient {

    private const val TAG = "PartyCloudClient"
    private const val BASE_URL = "https://ntfy.sh"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private fun getTopicForParty(code: String): String? {
        val clean = code.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")
        if (clean.isBlank()) return null
        return "lockin_party_$clean"
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
            put("action", action)
            put("senderId", senderId)
            put("timestamp", System.currentTimeMillis())
            put("payload", payload)
        }

        return@withContext try {
            val url = "$BASE_URL/$topic"
            val requestBuilder = Request.Builder()
                .url(url)
                .post(eventObj.toString().toRequestBody(JSON_MEDIA_TYPE))

            if (!notificationTitle.isNullOrBlank()) {
                requestBuilder.header("Title", notificationTitle)
            }
            if (!notificationBody.isNullOrBlank()) {
                requestBuilder.header("Priority", "high")
                requestBuilder.header("Tags", "warning,lock")
            }

            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error publishing event to cloud room $topic: ${e.message}")
            false
        }
    }

    /**
     * Polls recent messages for the given party code since a given epoch timestamp.
     */
    suspend fun pollEvents(
        partyCode: String,
        sinceTimestamp: Long
    ): List<CloudPartyEvent> = withContext(Dispatchers.IO) {
        val topic = getTopicForParty(partyCode) ?: return@withContext emptyList()
        val eventsList = mutableListOf<CloudPartyEvent>()

        return@withContext try {
            // ntfy.sh supports ?poll=1 and ?since=...
            val sinceSeconds = if (sinceTimestamp > 0L) (sinceTimestamp / 1000L).coerceAtLeast(1L) else (System.currentTimeMillis() / 1000L - 120L)
            val url = "$BASE_URL/$topic/json?poll=1&since=$sinceSeconds"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext emptyList()
                }

                val body = response.body?.byteStream() ?: return@withContext emptyList()
                val reader = BufferedReader(InputStreamReader(body))
                var line: String? = reader.readLine()
                while (line != null) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                        try {
                            val root = JSONObject(trimmed)
                            val eventType = root.optString("event")
                            if (eventType == "message") {
                                val msgId = root.optString("id", "")
                                val msgTime = root.optLong("time", 0L) * 1000L
                                val rawMsg = root.optString("message", "")
                                if (rawMsg.startsWith("{")) {
                                    val inner = JSONObject(rawMsg)
                                    val action = inner.optString("action")
                                    val senderId = inner.optString("senderId")
                                    val payload = inner.optJSONObject("payload") ?: JSONObject()
                                    eventsList.add(
                                        CloudPartyEvent(
                                            eventId = msgId,
                                            timestamp = if (msgTime > 0L) msgTime else System.currentTimeMillis(),
                                            action = action,
                                            senderId = senderId,
                                            payload = payload
                                        )
                                    )
                                }
                            }
                        } catch (_: Exception) {}
                    }
                    line = reader.readLine()
                }
                eventsList
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error polling cloud events for $topic: ${e.message}")
            emptyList()
        }
    }
}
