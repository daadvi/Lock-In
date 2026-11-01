package com.example.util

import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

/**
 * Intelligent fuzzy matcher for confirmation phrases.
 * 
 * Rules:
 * - Case-insensitive (e.g., 'A' == 'a').
 * - Accent/diacritic-tolerant (e.g., 'á', 'ã', 'à', 'â' == 'a', 'ç' == 'c').
 * - Tolerates minor typing errors if adjacent on standard mobile QWERTY / ABNT2 keyboards.
 * - Rejects heavy typing errors (strict threshold: max 1-2 typos for short phrases, 3-4 for long phrases).
 * - Guard rails on word count and length to prevent skipping words or partial phrases.
 */
object KeyboardFuzzyMatcher {

    private val ADJACENT_KEYS: Map<Char, Set<Char>> = mapOf(
        'q' to setOf('w', 'a', 's', '1', '2'),
        'w' to setOf('q', 'e', 'a', 's', 'd', '2', '3'),
        'e' to setOf('w', 'r', 's', 'd', 'f', '3', '4'),
        'r' to setOf('e', 't', 'd', 'f', 'g', '4', '5'),
        't' to setOf('r', 'y', 'f', 'g', 'h', '5', '6'),
        'y' to setOf('t', 'u', 'g', 'h', 'j', '6', '7'),
        'u' to setOf('y', 'i', 'h', 'j', 'k', '7', '8'),
        'i' to setOf('u', 'o', 'j', 'k', 'l', '8', '9'),
        'o' to setOf('i', 'p', 'k', 'l', 'ç', '9', '0'),
        'p' to setOf('o', 'l', 'ç', '0'),
        'a' to setOf('q', 'w', 's', 'z'),
        's' to setOf('a', 'w', 'e', 'd', 'z', 'x'),
        'd' to setOf('s', 'e', 'r', 'f', 'x', 'c'),
        'f' to setOf('d', 'r', 't', 'g', 'c', 'v'),
        'g' to setOf('f', 't', 'y', 'h', 'v', 'b'),
        'h' to setOf('g', 'y', 'u', 'j', 'b', 'n'),
        'j' to setOf('h', 'u', 'i', 'k', 'n', 'm'),
        'k' to setOf('j', 'i', 'o', 'l', 'm'),
        'l' to setOf('k', 'o', 'p', 'ç'),
        'ç' to setOf('l', 'p'),
        'z' to setOf('a', 's', 'x'),
        'x' to setOf('z', 's', 'd', 'c'),
        'c' to setOf('x', 'd', 'f', 'v'),
        'v' to setOf('c', 'f', 'g', 'b', ' '),
        'b' to setOf('v', 'g', 'h', 'n', ' '),
        'n' to setOf('b', 'h', 'j', 'm', ' '),
        'm' to setOf('n', 'j', 'k', ' ')
    )

    /**
     * Normalizes a phrase by stripping accents, folding case, removing smart quotes,
     * and collapsing whitespace.
     */
    fun normalize(text: String): String {
        val cleanQuotes = text
            .replace('“', '"')
            .replace('”', '"')
            .replace('‘', '\'')
            .replace('’', '\'')
            .replace('«', '"')
            .replace('»', '"')

        val nfd = Normalizer.normalize(cleanQuotes, Normalizer.Form.NFD)
        val stripped = nfd.replace(Regex("\\p{M}"), "")
            .replace("ç", "c")
            .replace("Ç", "c")

        return stripped
            .lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Checks if two characters are physically adjacent on a QWERTY / ABNT2 keyboard.
     */
    fun areKeysAdjacent(c1: Char, c2: Char): Boolean {
        if (c1 == c2) return true
        val low1 = c1.lowercaseChar()
        val low2 = c2.lowercaseChar()
        return ADJACENT_KEYS[low1]?.contains(low2) == true || ADJACENT_KEYS[low2]?.contains(low1) == true
    }

    /**
     * Calculates weighted Damerau-Levenshtein distance between two normalized strings.
     * - Exact match: 0.0
     * - Adjacent key substitution: 0.8
     * - Non-adjacent key substitution: 2.0
     * - Insertion / Deletion: 1.2
     * - Transposition of adjacent characters: 0.8
     */
    fun weightedDistance(s1: String, s2: String): Float {
        val n = s1.length
        val m = s2.length
        if (n == 0) return m * 1.2f
        if (m == 0) return n * 1.2f

        val dp = Array(n + 1) { FloatArray(m + 1) }

        for (i in 0..n) {
            dp[i][0] = i * 1.2f
        }
        for (j in 0..m) {
            dp[0][j] = j * 1.2f
        }

        for (i in 1..n) {
            val c1 = s1[i - 1]
            for (j in 1..m) {
                val c2 = s2[j - 1]
                val subCost = if (c1 == c2) {
                    0f
                } else if (areKeysAdjacent(c1, c2)) {
                    0.8f
                } else {
                    2.0f
                }

                var minCost = min(
                    dp[i - 1][j] + 1.2f,        // deletion
                    min(
                        dp[i][j - 1] + 1.2f,    // insertion
                        dp[i - 1][j - 1] + subCost // substitution
                    )
                )

                // Damerau transposition
                if (i > 1 && j > 1 && s1[i - 1] == s2[j - 2] && s1[i - 2] == s2[j - 1]) {
                    minCost = min(minCost, dp[i - 2][j - 2] + 0.8f)
                }

                dp[i][j] = minCost
            }
        }

        return dp[n][m]
    }

    /**
     * Verifies if [userInput] is a valid match for [targetPhrase], allowing slight typos
     * and keyboard adjacency errors, but strictly rejecting large or sloppy errors.
     */
    fun matchesWithKeyboardTolerance(userInput: String, targetPhrase: String): Boolean {
        val normInput = normalize(userInput)
        val normTarget = normalize(targetPhrase)

        if (normInput.isEmpty() && normTarget.isNotEmpty()) return false
        if (normInput == normTarget) return true

        val maxLenDiff = when {
            normTarget.length <= 35 -> 3
            normTarget.length <= 70 -> 4
            else -> 6
        }

        if (abs(normInput.length - normTarget.length) > maxLenDiff) {
            return false
        }

        val inputWords = normInput.split(" ").filter { it.isNotBlank() }
        val targetWords = normTarget.split(" ").filter { it.isNotBlank() }

        if (abs(inputWords.size - targetWords.size) > 1) {
            return false
        }

        val maxAllowedCost = when {
            normTarget.length <= 35 -> 1.8f // allows ~2 adjacent typos or 1 small mistake
            normTarget.length <= 70 -> 2.8f // allows ~3 adjacent typos
            normTarget.length <= 110 -> 3.6f // allows ~4 adjacent typos
            else -> 4.4f // allows ~4-5 adjacent typos in a long sentence of 140+ chars
        }

        val distance = weightedDistance(normInput, normTarget)
        return distance <= maxAllowedCost
    }
}
