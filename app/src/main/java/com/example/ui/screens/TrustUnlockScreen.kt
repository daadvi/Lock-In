package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.LockInApplication
import com.example.data.model.LockLevel
import com.example.service.AppUnlockDailyTracker
import com.example.ui.components.DotMatrixPattern
import com.example.ui.components.NothingCard
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import com.example.ui.theme.LocalCustomTextColor
import com.example.ui.theme.LocalIsDarkTheme
import com.example.util.CryptoPasswordHelper
import com.example.util.EmptyTextToolbar
import com.example.util.isManualTyping

@Composable
fun TrustUnlockScreen(
    correctTrustPassword: String,
    targetAppName: String? = null,
    targetPackageName: String? = null,
    onUnlockConfirmed: () -> Unit,
    onBack: () -> Unit
) {
    val accentColor = LocalCustomTextColor.current
    val context = LocalContext.current

    val app = remember { LockInApplication.instance }
    val userPrefs = app.userPreferences
    val currentLockLevel by userPrefs.lockLevel.collectAsState()

    val targetPasswordRepeats = currentLockLevel.passwordRepeatCount
    val requiredPhrase = currentLockLevel.requiredPhrase
    val maxDailyUnlocks = currentLockLevel.maxDailyUnlocksPerApp

    val remainingUnlocksToday = if (targetPackageName != null) {
        AppUnlockDailyTracker.getRemainingUnlocksToday(context, targetPackageName)
    } else {
        maxDailyUnlocks
    }
    val isDailyLimitReached = remainingUnlocksToday <= 0

    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var phraseInput by remember { mutableStateOf("") }
    var successfulCount by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pasteWarning by remember { mutableStateOf<String?>(null) }
    var isUnlocked by remember { mutableStateOf(false) }

    var isVerifying by remember { mutableStateOf(false) }

    fun verifyCurrentEntry() {
        if (isVerifying) return
        isVerifying = true
        try {
            if (successfulCount < targetPasswordRepeats) {
                // Password verification step (1 to targetPasswordRepeats) with secure constant-time / salted hash check
                if (CryptoPasswordHelper.verifyPassword(passwordInput, correctTrustPassword)) {
                    val nextCount = successfulCount + 1
                    successfulCount = nextCount
                    errorMessage = null
                    pasteWarning = null
                    passwordInput = ""
                } else {
                    // Strict reset rule: if user fails password, count resets to 0!
                    successfulCount = 0
                    errorMessage = "Senha incorreta! A contagem foi zerada por segurança e voltou para a 1ª etapa."
                    pasteWarning = null
                    passwordInput = ""
                }
            } else {
                // Final step: Phrase confirmation with intelligent keyboard proximity tolerance and case insensitivity
                if (com.example.util.KeyboardFuzzyMatcher.matchesWithKeyboardTolerance(phraseInput, requiredPhrase)) {
                    if (targetPackageName != null) {
                        AppUnlockDailyTracker.recordAppUnlocked(context, targetPackageName)
                    }
                    isUnlocked = true
                    errorMessage = null
                    pasteWarning = null
                    onUnlockConfirmed()
                } else {
                    errorMessage = "Frase incorreta! Digite a frase:\n\"$requiredPhrase\""
                }
            }
        } finally {
            isVerifying = false
        }
    }

    val isDarkTheme = LocalIsDarkTheme.current
    val screenBg = if (isDarkTheme) Color(0xFF101012) else CmfWhite
    val textColor = if (isDarkTheme) Color.White else CmfDark
    val subtextColor = if (isDarkTheme) Color(0xFFA1A1AA) else CmfMutedText
    val fieldBg = if (isDarkTheme) Color(0xFF1C1C20) else CmfWhite

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
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        // Top Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Voltar",
                                    tint = textColor
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "DESBLOQUEIO DE CONFIANÇA",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = textColor,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "NÍVEL ${currentLockLevel.levelNumber}: ${currentLockLevel.title.uppercase()} • $remainingUnlocksToday/$maxDailyUnlocks RESTANTES HOJE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = accentColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Explanation Card with Daily Limit or Streak Warning
                        if (isDailyLimitReached) {
                            NothingCard(
                                title = "BLOQUEIO DIÁRIO ATIVO",
                                badge = "0/$maxDailyUnlocks RESTANTES"
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "ESTE APLICATIVO ESGOTOU OS DESBLOQUEIOS DE HOJE",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFE53935),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Pelas regras do nível de bloqueio '${currentLockLevel.title}', cada aplicativo só pode ser desbloqueado no máximo $maxDailyUnlocks vez${if (maxDailyUnlocks > 1) "es" else ""} por dia.\n\nPara preservar sua disciplina, novos desbloqueios deste app só serão permitidos após o reset diário à meia-noite.",
                                        fontSize = 11.sp,
                                        color = subtextColor,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = onBack,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = accentColor,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("VOLTAR", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        } else {
                            NothingCard(
                                title = if (targetAppName != null) "DESBLOQUEIO: ${targetAppName.uppercase()}" else "PROTOCOLO DE DISCIPLINA",
                                badge = if (successfulCount < targetPasswordRepeats) "ETAPA ${successfulCount + 1}/$targetPasswordRepeats (SENHA)" else "ETAPA FINAL (FRASE)"
                            ) {
                                Column {
                                    // STREAK WARNING BANNER (MANDATORY REQUIREMENT)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFE53935).copy(alpha = 0.12f))
                                            .border(1.2.dp, Color(0xFFE53935), RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = Color(0xFFE53935),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "ATENÇÃO: SUA OFENSIVA (STREAK) SERÁ QUEBRADA!",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color(0xFFE53935)
                                                )
                                                Text(
                                                    text = "Ao desbloquear este app antes do próximo ciclo de reinício, seu streak diário de foco será zerado.",
                                                    fontSize = 10.sp,
                                                    color = textColor,
                                                    lineHeight = 14.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = if (targetAppName != null) {
                                            "Nível de Lock: ${currentLockLevel.title} (${currentLockLevel.badgeLabel}). Para desbloquear $targetAppName, digite a senha $targetPasswordRepeats vez${if (targetPasswordRepeats > 1) "es seguidas (sem errar)" else " (sem errar)"} e a frase sem errar. Restam $remainingUnlocksToday desbloqueio(s) hoje para este app."
                                        } else {
                                            "Você deve digitar sua Senha de Confiança $targetPasswordRepeats vez${if (targetPasswordRepeats > 1) "es consecutivas" else ""} sem errar. Se errar a qualquer momento, a contagem volta para 1."
                                        },
                                        fontSize = 11.5.sp,
                                        color = textColor,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Step Indicators: Passwords + Final Phrase
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (successfulCount < targetPasswordRepeats) "SENHA: ETAPA ${successfulCount + 1} DE $targetPasswordRepeats" else "CONFIRMAÇÃO FINAL (FRASE)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (successfulCount > 0) accentColor else CmfMutedText,
                                    letterSpacing = 1.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (step in 1..targetPasswordRepeats) {
                                        val isDone = step <= successfulCount
                                        val isCurrent = step == successfulCount + 1

                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isDone) accentColor else if (isCurrent) accentColor.copy(alpha = 0.15f) else CmfLightGray
                                                )
                                                .border(
                                                    width = if (isCurrent) 1.5.dp else 1.dp,
                                                    color = if (isDone || isCurrent) accentColor else CmfBorder,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                        ) {
                                            if (isDone) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = "$step",
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 12.sp,
                                                    color = if (isCurrent) accentColor else CmfMutedText
                                                )
                                            }
                                        }
                                    }

                                    // Indicator for final phrase step
                                    val isPhraseCurrent = successfulCount == targetPasswordRepeats && !isUnlocked
                                    val isPhraseDone = isUnlocked
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isPhraseDone) accentColor else if (isPhraseCurrent) accentColor.copy(alpha = 0.15f) else CmfLightGray
                                            )
                                            .border(
                                                width = if (isPhraseCurrent) 1.5.dp else 1.dp,
                                                color = if (isPhraseDone || isPhraseCurrent) accentColor else CmfBorder,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                    ) {
                                        if (isPhraseDone) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "F",
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                color = if (isPhraseCurrent) accentColor else CmfMutedText
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            if (!isUnlocked) {
                                if (successfulCount < targetPasswordRepeats) {
                                    // Password input (Steps 1 to targetPasswordRepeats)
                                    Text(
                                        text = "DIGITE A SENHA DE DESBLOQUEIO (${successfulCount + 1}ª VEZ DE $targetPasswordRepeats):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    OutlinedTextField(
                                        value = passwordInput,
                                        onValueChange = { newVal ->
                                            if (!isManualTyping(passwordInput, newVal, maxBurst = 2)) {
                                                pasteWarning = "Digitação manual obrigatória! Copiar e colar foi bloqueado."
                                            } else {
                                                pasteWarning = null
                                                passwordInput = newVal
                                                errorMessage = null
                                            }
                                        },
                                        placeholder = {
                                            Text(
                                                "Digite caractere por caractere...",
                                                color = subtextColor
                                            )
                                        },
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            color = textColor,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        singleLine = true,
                                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = { verifyCurrentEntry() }
                                        ),
                                        trailingIcon = {
                                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                Icon(
                                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = "Alternar visibilidade",
                                                    tint = subtextColor
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("trust_unlock_password_input"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = textColor,
                                            unfocusedTextColor = textColor,
                                            cursorColor = accentColor,
                                            focusedBorderColor = accentColor,
                                            unfocusedBorderColor = if (isDarkTheme) Color(0xFF3F3F46) else CmfBorder,
                                            focusedContainerColor = fieldBg,
                                            unfocusedContainerColor = fieldBg
                                        )
                                    )
                                } else {
                                    // Phrase input (Step Final)
                                    Column {
                                        Text(
                                            text = "DIGITE EXATAMENTE A FRASE ABAIXO (SEM ERRAR):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = textColor
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = requiredPhrase,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = accentColor,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))

                                        OutlinedTextField(
                                            value = phraseInput,
                                            onValueChange = { newVal ->
                                                if (!isManualTyping(phraseInput, newVal, maxBurst = 3)) {
                                                    pasteWarning = "Digitação manual obrigatória! Copiar e colar foi bloqueado."
                                                } else {
                                                    pasteWarning = null
                                                    phraseInput = newVal
                                                    errorMessage = null
                                                }
                                            },
                                            placeholder = {
                                                Text(
                                                    "Digite a frase manualmente...",
                                                    color = subtextColor,
                                                    fontSize = 12.sp
                                                )
                                            },
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                color = textColor,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            singleLine = false,
                                            maxLines = 3,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Text,
                                                imeAction = ImeAction.Done
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onDone = { verifyCurrentEntry() }
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("trust_unlock_phrase_input"),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = textColor,
                                                unfocusedTextColor = textColor,
                                                cursorColor = accentColor,
                                                focusedBorderColor = accentColor,
                                                unfocusedBorderColor = if (isDarkTheme) Color(0xFF3F3F46) else CmfBorder,
                                                focusedContainerColor = fieldBg,
                                                unfocusedContainerColor = fieldBg
                                            )
                                        )
                                    }
                                }

                                if (pasteWarning != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFE65100),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = pasteWarning ?: "",
                                            color = Color(0xFFE65100),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (errorMessage != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFD32F2F),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = errorMessage ?: "",
                                            color = Color(0xFFD32F2F),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            } else {
                                // Unlocked Success Banner
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFE8F5E9))
                                        .border(1.dp, Color(0xFF4CAF50), RoundedCornerShape(12.dp))
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.LockOpen,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (targetAppName != null) "$targetAppName DESBLOQUEADO!" else "DESBLOQUEIO CONCLUÍDO",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF1B5E20)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (targetAppName != null) {
                                                "Você cumpriu o desafio do nível ${currentLockLevel.title}. O aplicativo $targetAppName foi liberado e o streak diário foi quebrado como consequência."
                                            } else {
                                                "Você provou sua intenção digitando a senha $targetPasswordRepeats vez${if (targetPasswordRepeats > 1) "es" else ""} e a frase sem errar."
                                            },
                                            fontSize = 12.sp,
                                            color = Color(0xFF2E7D32),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Action Button
                    if (!isDailyLimitReached) {
                        Column(modifier = Modifier.padding(bottom = 24.dp)) {
                            if (!isUnlocked) {
                                Button(
                                    onClick = { verifyCurrentEntry() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("trust_unlock_submit_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accentColor,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = if (successfulCount < targetPasswordRepeats) "Confirmar Senha (${successfulCount + 1} de $targetPasswordRepeats)" else "Confirmar Frase e Desbloquear",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            } else {
                                Button(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CmfDark,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "Voltar ao Início",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun normalizePhrase(text: String): String {
    return text.trim()
        .replace(Regex("\\s+"), " ")
        .replace('“', '"')
        .replace('”', '"')
        .replace('‘', '\'')
        .replace('’', '\'')
}
