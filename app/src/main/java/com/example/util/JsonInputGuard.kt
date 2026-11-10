package com.example.util

import android.util.JsonReader
import android.util.JsonToken
import java.io.StringReader

/** Strict, iterative preflight before Android's permissive, recursive JSONObject parser.
 * Callers must also bound input bytes. Comments, single quotes and bare strings are refused.
 */
internal object JsonInputGuard {
    fun isValid(input: String, maxDepth: Int = 32): Boolean {
        return try {
        JsonReader(StringReader(input)).use { reader ->
            reader.isLenient = false
            var depth = 0
            var hasValue = false
            while (true) {
                when (reader.peek()) {
                    JsonToken.BEGIN_OBJECT -> { if (++depth > maxDepth) return false; hasValue = true; reader.beginObject() }
                    JsonToken.BEGIN_ARRAY -> { if (++depth > maxDepth) return false; hasValue = true; reader.beginArray() }
                    JsonToken.END_OBJECT -> { reader.endObject(); depth-- }
                    JsonToken.END_ARRAY -> { reader.endArray(); depth-- }
                    JsonToken.NAME -> reader.nextName()
                    JsonToken.STRING -> reader.nextString()
                    JsonToken.NUMBER -> if (reader.nextString().length > 128) return false
                    JsonToken.BOOLEAN -> reader.nextBoolean()
                    JsonToken.NULL -> reader.nextNull()
                    JsonToken.END_DOCUMENT -> return hasValue && depth == 0
                    else -> return false
                }
            }
            @Suppress("UNREACHABLE_CODE")
            false
        }
    } catch (_: Exception) { false }
    }
}
