package com.example.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Account login runs on Dispatchers.IO. SHA-1 PBKDF2 is available back to Android 24. */
internal object AccountPasswordHash {
    private const val PREFIX = "pbkdf2-sha1-v1:"
    private const val ITERATIONS = 600_000
    fun isCurrent(value: String) = value.startsWith(PREFIX)
    fun hash(password: String): String {
        require(password.length in 1..1024)
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return PREFIX + Base64.encodeToString(salt, Base64.NO_WRAP) + ":" + Base64.encodeToString(derive(password, salt), Base64.NO_WRAP)
    }
    fun verify(password: String, stored: String): Boolean {
        if (password.length !in 1..1024 || stored.length > 256 || !isCurrent(stored)) return false
        return try {
            val parts = stored.removePrefix(PREFIX).split(':')
            if (parts.size != 2) return false
            val salt = Base64.decode(parts[0], Base64.NO_WRAP)
            val expected = Base64.decode(parts[1], Base64.NO_WRAP)
            salt.size == 16 && expected.size == 32 && MessageDigest.isEqual(expected, derive(password, salt))
        } catch (_: Exception) { false }
    }
    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
}
