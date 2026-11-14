package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.example.util.EmptyTextToolbar
import com.example.util.isManualTyping
import com.example.util.PhraseEntryCheck
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlockyFontFamily
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import com.example.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.delay

enum class HighFrictionChallengeType {
    UNBLOCK_INSTALLATION,
    UNINSTALL_LOCK_IN,
    DEACTIVATE_DEVICE_ADMIN
}

@Composable
fun HighFrictionSecurityChallengeFlow(
    challengeType: HighFrictionChallengeType,
    targetItemName: String? = null,
    correctPassword: String,
    accentColor: Color,
    onCancel: () -> Unit,
    onSuccess: () -> Unit
) {
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var successfulPassCount by remember { mutableIntStateOf(0) }

    // Stage tracking:
    // Stage 1: Consecutive correct passwords according to the saved lock level
    // Stage 2: Write stage 2 phrase without error
    // Stage 3: Reflection cards appear
    // Stage 4: Write stage 4 alternating commitment phrase without error
    var currentStage by remember { mutableIntStateOf(1) }

    var stage2Input by remember { mutableStateOf("") }
    var stage4Input by remember { mutableStateOf("") }

    // 3 chances por frase; errar as 3 reinicia tudo desde a senha.
    // O vermelho na frase aparece só na 1ª falha (ver PhraseAttemptTracker).
    val stage2Attempts = rememberPhraseAttemptTracker()
    val stage4Attempts = rememberPhraseAttemptTracker()

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pasteWarning by remember { mutableStateOf<String?>(null) }

    val currentLockLevel = remember {
        try {
            com.example.LockInApplication.instance.userPreferences.lockLevel.value
        } catch (_: Exception) {
            com.example.data.model.LockLevel.BASIC
        }
    }

    val targetPassCount = currentLockLevel.passwordRepeatCount

    // Alternating final phrases for Stage 4
    val finalPhrases = remember(challengeType) {
        when (challengeType) {
            HighFrictionChallengeType.UNBLOCK_INSTALLATION -> listOf(
                "Eu quero instalar esse app e sei o que vou fazer com ele, mesmo sendo errado",
                "Quero instalar esse aplicativo, mesmo me desrespeitando",
                "Eu estou ciente do que vou fazer agora e aceito as consequências de instalar esse app"
            )
            HighFrictionChallengeType.UNINSTALL_LOCK_IN -> listOf(
                "Eu quero desinstalar o Lock In e sei o que vou perder com isso, mesmo sendo errado",
                "Quero desinstalar o aplicativo, mesmo me desrespeitando",
                "Eu estou ciente do que vou fazer agora e aceito as consequências de desinstalar o Lock In"
            )
            HighFrictionChallengeType.DEACTIVATE_DEVICE_ADMIN -> listOf(
                "Eu quero desativar o administrador e sei o perigo de perder minha proteção, mesmo sendo errado",
                "Quero retirar o Lock In dos administradores, mesmo me desrespeitando",
                "Eu estou ciente do que vou fazer agora e aceito as consequências de desativar o administrador"
            )
        }
    }

    var finalPhraseIndex by remember { mutableIntStateOf((0..2).random()) }
    val requiredStage4Phrase = finalPhrases[finalPhraseIndex % finalPhrases.size]

    val requiredStage2Phrase = remember(challengeType, targetItemName, currentLockLevel) {
        when (challengeType) {
            HighFrictionChallengeType.UNBLOCK_INSTALLATION ->
                currentLockLevel.requiredPhrase
            HighFrictionChallengeType.UNINSTALL_LOCK_IN ->
                "Tenho plena certeza de que desejo desinstalar o Lock In"
            HighFrictionChallengeType.DEACTIVATE_DEVICE_ADMIN ->
                "Tenho plena certeza de que desejo desativar o administrador do Lock In"
        }
    }

    val reflectionPhrases = remember(challengeType) {
        when (challengeType) {
            HighFrictionChallengeType.UNBLOCK_INSTALLATION -> listOf(
                "Você sabe o que está fazendo!",
                "Lembre-se do seu propósito",
                "Você pode está fazendo algo errado"
            )
            HighFrictionChallengeType.UNINSTALL_LOCK_IN -> listOf(
                "Você sabe o que está fazendo!",
                "Lembre-se do seu propósito",
                "Você pode estar cometendo um grande erro"
            )
            HighFrictionChallengeType.DEACTIVATE_DEVICE_ADMIN -> listOf(
                "Você sabe o que está fazendo!",
                "Lembre-se do seu propósito",
                "Você pode estar desativando sua única proteção"
            )
        }
    }

    fun resetAllBackToStart(reason: String) {
        successfulPassCount = 0
        passwordInput = ""
        stage2Input = ""
        stage4Input = ""
        currentStage = 1
        errorMessage = reason
        stage2Attempts.reset()
        stage4Attempts.reset()
        // Alternate to another phrase for the next cycle
        finalPhraseIndex = (finalPhraseIndex + 1) % finalPhrases.size
    }

    var isVerifyingPassword by remember { mutableStateOf(false) }

    fun verifyPasswordStep() {
        if (isVerifyingPassword) return
        isVerifyingPassword = true
        try {
            if (com.example.util.CryptoPasswordHelper.verifyPassword(passwordInput, correctPassword)) {
                val next = successfulPassCount + 1
                if (next >= targetPassCount) {
                    successfulPassCount = targetPassCount
                    currentStage = 2
                    errorMessage = null
                    passwordInput = ""
                } else {
                    successfulPassCount = next
                    errorMessage = null
                    passwordInput = ""
                }
            } else {
                resetAllBackToStart("Senha incorreta! Todas as $targetPassCount tentativas foram zeradas por segurança.")
            }
        } finally {
            isVerifyingPassword = false
        }
    }

    fun verifyStage2Phrase() {
        if (PhraseEntryCheck.isExactMatch(stage2Input, requiredStage2Phrase)) {
            if (challengeType == HighFrictionChallengeType.UNBLOCK_INSTALLATION) {
                // Completed password (1/3/5x) + phrase: unblock blacklist item immediately
                onSuccess()
            } else {
                currentStage = 3
                errorMessage = null
            }
        } else if (stage2Attempts.registerFailure(stage2Input)) {
            resetAllBackToStart("Você errou a frase 3 vezes! Por segurança, todo o protocolo foi reiniciado desde a senha.")
        } else {
            stage2Attempts.attemptsLeft.let { left ->
                errorMessage = "Frase incorreta! Resta${if (left > 1) "m" else ""} $left chance${if (left > 1) "s" else ""}. Digite exatamente como pedido."
            }
        }
    }

    fun verifyStage4Phrase() {
        if (PhraseEntryCheck.isExactMatch(stage4Input, requiredStage4Phrase)) {
            onSuccess()
        } else if (stage4Attempts.registerFailure(stage4Input)) {
            resetAllBackToStart("Você errou a frase final 3 vezes! Por segurança, todo o protocolo foi reiniciado desde a senha.")
        } else {
            stage4Attempts.attemptsLeft.let { left ->
                errorMessage = "Frase incorreta! Resta${if (left > 1) "m" else ""} $left chance${if (left > 1) "s" else ""}. Digite exatamente como pedido."
            }
        }
    }

    // Auto-advance from Stage 3 reflection to Stage 4 after user reflects
    var reflectionShownCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(currentStage) {
        if (currentStage == 3) {
            delay(2800L)
            currentStage = 4
        }
    }

    val isDark = LocalIsDarkTheme.current
    val screenBg = if (isDark) Color(0xFF0C0C0E) else CmfWhite
    val textColor = if (isDark) Color.White else CmfDark
    val subtextColor = if (isDark) Color(0xFFA1A1AA) else CmfMutedText
    val phraseBoxBg = if (isDark) Color(0xFF1E1E26) else Color(0xFFF3F4F6)
    val phraseBoxBorder = if (isDark) Color(0xFF2E2E38) else CmfBorder

    val headerTitle = when (challengeType) {
        HighFrictionChallengeType.UNBLOCK_INSTALLATION -> "DESBLOQUEIO DE INSTALAÇÃO"
        HighFrictionChallengeType.UNINSTALL_LOCK_IN -> "PROTOCOLO DE DESINSTALAÇÃO"
        HighFrictionChallengeType.DEACTIVATE_DEVICE_ADMIN -> "PROTOCOLO DE ADMINISTRADOR"
    }

    val headerSub = when (challengeType) {
        HighFrictionChallengeType.UNBLOCK_INSTALLATION -> "REMOÇÃO DA LISTA DE BLOQUEIO"
        HighFrictionChallengeType.UNINSTALL_LOCK_IN -> "BLINDAGEM CONTRA DESINSTALAÇÃO"
        HighFrictionChallengeType.DEACTIVATE_DEVICE_ADMIN -> "BLINDAGEM DO ADMINISTRADOR DO SISTEMA"
    }

    CompositionLocalProvider(LocalTextToolbar provides EmptyTextToolbar) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = screenBg
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                DotMatrixPattern(modifier = Modifier.fillMaxSize())

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onCancel) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Cancelar",
                                tint = textColor
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = headerTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = BlockyFontFamily,
                                color = textColor,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = headerSub,
                                fontSize = 9.sp,
                                fontFamily = BlockyFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Context Warning Card
                    NothingCard(
                        title = "PROTOCOLO DE SEGURANÇA MÁXIMA",
                        badge = "ALTA FRICÇÃO"
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = when (challengeType) {
                                            HighFrictionChallengeType.UNBLOCK_INSTALLATION ->
                                                "Para tirar este app do bloqueio, complete todas as etapas sem errar:"
                                            HighFrictionChallengeType.UNINSTALL_LOCK_IN ->
                                                "Para desinstalar o Lock In, complete todas as etapas sem errar:"
                                            HighFrictionChallengeType.DEACTIVATE_DEVICE_ADMIN ->
                                                "Para desativar o Lock In dos administradores, complete todas as etapas:"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Text(
                                        text = "1. Digitar a senha $targetPassCount vez${if (targetPassCount > 1) "es" else ""} sem errar.\n2. Escrever a frase sem errar (qualquer erro zera tudo)." +
                                            if (challengeType == HighFrictionChallengeType.UNBLOCK_INSTALLATION) "" else
                                                "\n3. Refletir sobre as frases de propósito.\n4. Digitar a frase final de compromisso.",
                                        fontSize = 10.sp,
                                        color = subtextColor,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Error Message Banner (Red)
                    if (errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFEBEE))
                                .border(1.dp, Color(0xFFEF5350), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = Color(0xFFC62828),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Paste Warning Banner (Orange)
                    if (pasteWarning != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFF3E0))
                                .border(1.dp, Color(0xFFFFB74D), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = pasteWarning ?: "",
                                color = Color(0xFFE65100),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // ==========================================
                    // ETAPA 1: SENHA DE SEGURANÇA CONFORME O NÍVEL
                    // ==========================================
                    Text(
                        text = "ETAPA 1: DIGITAR A SENHA ($targetPassCount VEZ${if (targetPassCount > 1) "ES" else ""} SEM ERRAR)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Step Indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in 1..targetPassCount) {
                            val isCompleted = i <= successfulPassCount
                            val isCurrent = i == successfulPassCount + 1 && successfulPassCount < targetPassCount
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            isCompleted -> accentColor
                                            isCurrent -> accentColor.copy(alpha = 0.15f)
                                            else -> CmfLightGray
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (isCurrent) accentColor else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isCompleted) "✓" else "$i/$targetPassCount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isCompleted) Color.White else if (isCurrent) accentColor else CmfMutedText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (successfulPassCount < targetPassCount) {
                        Text(
                            text = "DIGITE A SENHA (${successfulPassCount + 1}ª VEZ DE $targetPassCount):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        ManualEntryTextField(
                            value = passwordInput,
                            onValueChange = { newVal ->
                                if (!isManualTyping(passwordInput, newVal)) {
                                    pasteWarning = "Tentativa de colar detectada! Digite caractere por caractere."
                                } else {
                                    pasteWarning = null
                                    passwordInput = newVal
                                }
                            },
                            placeholder = { Text("Digite sua Senha de Confiança") },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { verifyPasswordStep() }),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = CmfMutedText
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("challenge_password_input"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { verifyPasswordStep() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("verify_password_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(8.dp),
                            enabled = passwordInput.isNotBlank()
                        ) {
                            Text(
                                text = "CONFIRMAR SENHA (${successfulPassCount + 1}ª/$targetPassCount)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = BlockyFontFamily
                            )
                        }
                    } else {
                        // Passwords completed banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Senha digitada $targetPassCount vez${if (targetPassCount > 1) "es" else ""} com sucesso! Prossiga para a frase.",
                                fontSize = 10.sp,
                                color = Color(0xFF1B5E20),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ==========================================
                    // ETAPA 2: ESCREVER UMA FRASE SEM ERRAR
                    // ==========================================
                    Text(
                        text = "ETAPA 2: ESCREVER A FRASE SEM ERRAR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        color = if (currentStage >= 2) textColor else subtextColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Você tem 3 chances. Se errar as 3, o protocolo reinicia desde a senha. Na 1ª falha, as palavras erradas ficam vermelhas acima.",
                        fontSize = 10.sp,
                        color = subtextColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(phraseBoxBg)
                            .border(1.dp, phraseBoxBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        PhraseTargetDisplay(
                            phrase = requiredStage2Phrase,
                            failedInput = if (stage2Attempts.showErrorHighlight) stage2Attempts.firstFailedInput else null,
                            modifier = Modifier.testTag("stage2_phrase_target"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ManualEntryTextField(
                        value = stage2Input,
                        onValueChange = { newVal ->
                            if (!isManualTyping(stage2Input, newVal)) {
                                pasteWarning = "Colar não é permitido! Digite a frase caractere por caractere."
                            } else {
                                pasteWarning = null
                                stage2Input = newVal
                            }
                        },
                        placeholder = { Text("Digite a frase exata acima") },
                        enabled = currentStage == 2,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stage2_phrase_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentStage == 2) {
                        Button(
                            onClick = { verifyStage2Phrase() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("verify_stage2_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(8.dp),
                            enabled = stage2Input.isNotBlank()
                        ) {
                            Text(
                                text = "CONFIRMAR FRASE DA ETAPA 2",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = BlockyFontFamily
                            )
                        }
                    } else if (currentStage > 2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Frase confirmada sem erros!",
                                fontSize = 10.sp,
                                color = Color(0xFF1B5E20),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ==========================================
                    // ETAPA 3 & 4: FRASES DE REFLEXÃO E COMPROMISSO FINAL
                    // ==========================================
                    if (currentStage >= 3) {
                        Text(
                            text = "ETAPA 3: REFLEXÃO DE PROPÓSITO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = BlockyFontFamily,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Reflection Cards
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            reflectionPhrases.forEach { phrase ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(accentColor.copy(alpha = 0.08f))
                                        .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "• \"$phrase\"",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = BlockyFontFamily,
                                        color = accentColor,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // ==========================================
                        // ETAPA 4: FRASE FINAL ALTERNADA
                        // ==========================================
                        Text(
                            text = "ETAPA 4: DECLARAÇÃO FINAL OBRIGATÓRIA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = BlockyFontFamily,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Digite esta declaração manualmente. Maiúsculas e minúsculas são aceitas:",
                            fontSize = 10.sp,
                            color = subtextColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFF3E0))
                                .border(1.5.dp, Color(0xFFFF9800), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            PhraseTargetDisplay(
                                phrase = requiredStage4Phrase,
                                failedInput = if (stage4Attempts.showErrorHighlight) stage4Attempts.firstFailedInput else null,
                                modifier = Modifier.fillMaxWidth().testTag("stage4_phrase_target"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFE65100),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        ManualEntryTextField(
                            value = stage4Input,
                            onValueChange = { newVal ->
                                if (!isManualTyping(stage4Input, newVal)) {
                                    pasteWarning = "Colar não é permitido! Digite a declaração caractere por caractere."
                                } else {
                                    pasteWarning = null
                                    stage4Input = newVal
                                }
                            },
                            placeholder = { Text("Digite a declaração final acima") },
                            enabled = currentStage == 4,
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("stage4_phrase_input"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { verifyStage4Phrase() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("final_confirm_action_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            enabled = currentStage == 4 && stage4Input.isNotBlank()
                        ) {
                            Text(
                                text = when (challengeType) {
                                    HighFrictionChallengeType.UNBLOCK_INSTALLATION -> "CONFIRMAR E LIBERAR INSTALAÇÃO"
                                    HighFrictionChallengeType.UNINSTALL_LOCK_IN -> "CONFIRMAR E DESINSTALAR LOCK IN"
                                    HighFrictionChallengeType.DEACTIVATE_DEVICE_ADMIN -> "CONFIRMAR E DESATIVAR ADMINISTRADOR"
                                },
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                fontFamily = BlockyFontFamily,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

private fun normalizeChallengePhrase(text: String): String {
    return text.trim()
        .replace(Regex("\\s+"), " ")
        .replace('“', '"')
        .replace('”', '"')
        .replace('‘', '\'')
        .replace('’', '\'')
}
