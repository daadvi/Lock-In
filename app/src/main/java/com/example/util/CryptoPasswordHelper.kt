package com.example.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * CryptoPasswordHelper
 *
 * Implements salted cryptographic hashing for master passwords.
 * Prevents plaintext password storage in SharedPreferences/databases.
 * Supports transparent upgrade for legacy plaintext passwords.
 */
object CryptoPasswordHelper {

    private const val HASH_PREFIX = "v1:salt:"
    private const val ITERATIONS = 1000

    fun hashPassword(password: String): String {
        if (password.isBlank()) return ""
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hash = computeHash(password, salt)
        val hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)
        return "$HASH_PREFIX$saltBase64:$hashBase64"
    }

    fun verifyPassword(inputPassword: String, storedHashOrPlain: String): Boolean {
        if (storedHashOrPlain.isBlank()) {
            return inputPassword.isBlank()
        }

        // 1. Check if stored in secure hashed format
        if (storedHashOrPlain.startsWith(HASH_PREFIX)) {
            val parts = storedHashOrPlain.removePrefix(HASH_PREFIX).split(":")
            if (parts.size != 2) return false
            return try {
                val salt = Base64.decode(parts[0], Base64.NO_WRAP)
                val expectedHash = Base64.decode(parts[1], Base64.NO_WRAP)
                val actualHash = computeHash(inputPassword, salt)
                MessageDigest.isEqual(expectedHash, actualHash)
            } catch (_: Exception) {
                false
            }
        }

        // 2. Legacy fallback: Direct constant-time comparison for passwords created before hashing
        return MessageDigest.isEqual(inputPassword.toByteArray(Charsets.UTF_8), storedHashOrPlain.toByteArray(Charsets.UTF_8))
    }

    private fun computeHash(password: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        var hash = digest.digest(password.toByteArray(Charsets.UTF_8))
        for (i in 1 until ITERATIONS) {
            digest.reset()
            digest.update(hash)
            hash = digest.digest(salt)
        }
        return hash
    }
}
