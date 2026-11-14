package com.example.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.util.PhraseEntryCheck

/**
 * 3 chances por frase. Errar as 3 → o usuário volta para a senha (quem chama decide o reset).
 * O vermelho (destaque das palavras erradas) aparece APENAS na primeira falha.
 */
class PhraseAttemptTracker(private val maxAttempts: Int = MAX_ATTEMPTS) {

    var usedAttempts: Int by mutableStateOf(0)
        private set

    /** Guardado só na primeira falha — nas tentativas 2 e 3 não existe vermelho. */
    var firstFailedInput: String? by mutableStateOf(null)
        private set

    val showErrorHighlight: Boolean
        get() = usedAttempts == 1 && firstFailedInput != null

    val attemptsLeft: Int
        get() = (maxAttempts - usedAttempts).coerceAtLeast(0)

    val isExhausted: Boolean
        get() = usedAttempts >= maxAttempts

    /**
     * Registra uma falha. Retorna true quando as chances acabaram (chamador deve
     * reiniciar o fluxo pela senha).
     */
    fun registerFailure(input: String): Boolean {
        if (isExhausted) return true
        usedAttempts += 1
        if (usedAttempts == 1) firstFailedInput = input
        else firstFailedInput = null
        return isExhausted
    }

    fun reset() {
        usedAttempts = 0
        firstFailedInput = null
    }

    companion object {
        const val MAX_ATTEMPTS = 3
    }
}

@Composable
fun rememberPhraseAttemptTracker(): PhraseAttemptTracker = remember { PhraseAttemptTracker() }

/**
 * Exibe a frase a digitar. Quando [failedInput] não é nulo (só acontece na 1ª falha),
 * pinta de vermelho apenas as palavras erradas da frase ALVO.
 */
@Composable
fun PhraseTargetDisplay(
    phrase: String,
    failedInput: String?,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    wrongColor: Color = Color(0xFFE53935),
    fontSize: TextUnit = 12.sp,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    testTag: String? = null
) {
    val annotated: AnnotatedString = if (failedInput != null) {
        buildAnnotatedString {
            PhraseEntryCheck.wrongSegments(failedInput, phrase).forEach { segment ->
                if (segment.isWrong) {
                    withStyle(SpanStyle(color = wrongColor, fontWeight = FontWeight.Bold)) {
                        append(segment.text)
                    }
                } else {
                    append(segment.text)
                }
            }
        }
    } else {
        AnnotatedString(phrase)
    }

    Text(
        text = annotated,
        modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = textAlign,
        style = LocalTextStyle.current
    )
}