package com.example.service

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** The invitation is a bearer secret: only share it with trusted members.
 * The relay learns neither the invitation nor message contents. Group members still
 * share one key; this is not a substitute for independent authenticated identities.
 */
internal object PartyMessageSecurity {
    const val MAX_ENVELOPE_BYTES = 4096
    const val MAX_POLL_BYTES = 256 * 1024
    const val MAX_LINE_BYTES = 8192
    const val MAX_POLL_LINES = 100
    private const val MAX_PLAINTEXT_BYTES = 3000
    private const val ENVELOPE_PREFIX = "LCK2."
    private val codePattern = Regex("LCK2-[0-9A-F]{64}")
    private val base64Pattern = Regex("[A-Za-z0-9+/]+={0,2}")
    private val random = SecureRandom()
    private val aad = "LockInPartyMessage:v2".toByteArray(Charsets.US_ASCII)

    fun normalizeCode(code: String): String = code.trim().uppercase(Locale.ROOT)
    fun isValidCode(code: String): Boolean = code.length <= 80 && codePattern.matches(normalizeCode(code))

    fun createCode(): String = "LCK2-" + ByteArray(32).also(random::nextBytes).toHex()

    private fun secret(code: String): ByteArray? {
        if (!isValidCode(code)) return null
        val hex = normalizeCode(code).removePrefix("LCK2-")
        return ByteArray(32) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }

    private fun derive(secret: ByteArray, domain: String): ByteArray =
        MessageDigest.getInstance("SHA-256").run {
            update(domain.toByteArray(Charsets.US_ASCII))
            digest(secret)
        }

    fun topicForCode(code: String): String? = secret(code)?.let {
        "lockin_party_v2_" + Base64.encodeToString(derive(it, "LockIn:party:topic:v2:"),
            Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE)
    }

    fun encrypt(code: String, plaintext: String): String? {
        val secret = secret(code) ?: return null
        if (plaintext.length > MAX_PLAINTEXT_BYTES) return null
        val bytes = plaintext.toByteArray(Charsets.UTF_8)
        if (bytes.size > MAX_PLAINTEXT_BYTES) return null
        return try {
            val nonce = ByteArray(12).also(random::nextBytes)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(derive(secret, "LockIn:party:key:v2:"), "AES"), GCMParameterSpec(128, nonce))
            cipher.updateAAD(aad)
            ENVELOPE_PREFIX + Base64.encodeToString(nonce + cipher.doFinal(bytes), Base64.NO_WRAP)
        } catch (_: Exception) { null }
    }

    fun decrypt(code: String, envelope: String): String? {
        val secret = secret(code) ?: return null
        if (envelope.length > MAX_ENVELOPE_BYTES || !envelope.startsWith(ENVELOPE_PREFIX)) return null
        val encoded = envelope.removePrefix(ENVELOPE_PREFIX)
        if (!base64Pattern.matches(encoded)) return null
        return try {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            if (bytes.size < 28 || bytes.size > MAX_PLAINTEXT_BYTES + 28) return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(derive(secret, "LockIn:party:key:v2:"), "AES"), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            cipher.updateAAD(aad)
            val plain = cipher.doFinal(bytes, 12, bytes.size - 12)
            Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(plain)).toString()
        } catch (_: Exception) { null }
    }

    /** Bounds bytes before allocating a line or invoking a JSON parser. Caller owns input.
     * Returns a complete-line prefix at batch limits so a legitimate backlog can advance.
     */
    fun readBoundedLines(input: InputStream): List<String>? {
        val lines = ArrayList<String>()
        val line = ByteArrayOutputStream()
        val buffered = input.buffered(1024)
        var total = 0
        while (true) {
            val byte = buffered.read()
            if (byte == -1) break
            if (++total > MAX_POLL_BYTES) return lines.takeIf { it.isNotEmpty() }
            if (byte == '\n'.code) {
                lines.add(line.toString("UTF-8"))
                line.reset()
                if (lines.size == MAX_POLL_LINES) return lines
            } else {
                if (line.size() >= MAX_LINE_BYTES) return null
                line.write(byte)
            }
        }
        if (line.size() > 0) {
            lines.add(line.toString("UTF-8"))
        }
        return lines
    }

    fun isShallowJson(text: String): Boolean = com.example.util.JsonInputGuard.isValid(text)

    private fun ByteArray.toHex(): String = joinToString("") { "%02X".format(Locale.ROOT, it.toInt() and 255) }
}
