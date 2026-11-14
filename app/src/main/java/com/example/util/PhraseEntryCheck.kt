package com.example.util

/**
 * Verificação estrita de frases de confirmação.
 *
 * Regras do usuário: maiúsculas/minúsculas são permitidas (não diferenciamos),
 * espaços a mais são perdoados, mas NENHUM erro de digitação é aceito — a frase
 * tem que casar exatamente, palavra por palavra.
 */
object PhraseEntryCheck {

    data class Segment(val text: String, val isWrong: Boolean)

    /** Normaliza para comparação: aspas inteligentes → simples, colapsa espaços, minúsculas. */
    fun normalize(input: String): String {
        val straightened = input
            .replace('\u2018', '\'')
            .replace('\u2019', '\'')
            .replace('\u201C', '"')
            .replace('\u201D', '"')
            .replace('\u2013', '-')
            .replace('\u2014', '-')
        return straightened.trim().split(Regex("\\s+")).joinToString(" ").lowercase(java.util.Locale.ROOT)
    }

    /** true só quando casa exatamente (case-insensitive, espaços colapsados, zero tolerância a typo). */
    fun isExactMatch(input: String, target: String): Boolean =
        normalize(input) == normalize(target)

    /**
     * Segmentos da frase ALVO para exibição: uma entrada por palavra (espaços preservados),
     * marcando [isWrong] quando a palavra digitada naquela posição falta ou difere.
     * Palavras extras digitadas deslocam a cauda, que então aparece errada.
     */
    fun wrongSegments(input: String, target: String): List<Segment> {
        val inputWords = normalize(input).split(" ").filter { it.isNotEmpty() }
        val out = mutableListOf<Segment>()
        target.split(" ").forEachIndexed { index, word ->
            if (word.isEmpty()) return@forEachIndexed
            if (index > 0) out.add(Segment(" ", isWrong = false))
            val typed = inputWords.getOrNull(index)
            out.add(Segment(word, isWrong = typed == null || typed != normalize(word)))
        }
        return out
    }
}