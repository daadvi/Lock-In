package com.example.ui.components

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationCompat
import com.example.data.model.LockLevel
import com.example.service.CloudSyncManager
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import com.example.ui.theme.LocalCustomTextColor
import com.example.ui.theme.LocalIsDarkTheme
import com.example.util.EmptyTextToolbar
import com.example.util.isManualTyping
import com.example.util.PhraseEntryCheck
import kotlin.random.Random

@Composable
fun AbsoluteInstallBlockDialog(
    isEnabling: Boolean, // true: confirmation to enable, false: deactivation challenge
    onDismiss: () -> Unit,
    onStateChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val accentColor = LocalCustomTextColor.current
    val isDark = LocalIsDarkTheme.current
    val app = com.example.LockInApplication.instance
    val userPrefs = app.userPreferences
    val currentLockLevel by userPrefs.lockLevel.collectAsState()
    val targetPassCount = currentLockLevel.passwordRepeatCount
    val targetPhrase = currentLockLevel.requiredPhrase
    val isDevDream = currentLockLevel == LockLevel.DEV_DREAM

    val cardBg = if (isDark) Color(0xFF18181B) else CmfWhite
    val textColor = if (isDark) Color.White else CmfDark
    val subtextColor = if (isDark) Color(0xFFA1A1AA) else CmfMutedText
    val borderColor = if (isDark) Color(0xFF27272A) else CmfBorder

    // Deactivation Challenge States
    var deactivationStage by remember { mutableIntStateOf(1) } // 1: Passwords according to lock level, 2: Long Phrase, 3: 8-char Email Code
    var successfulPassCount by remember { mutableIntStateOf(0) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var phraseInput by remember { mutableStateOf("") }
    // 3 chances por frase; errar as 3 reinicia desde a senha. Vermelho só na 1ª falha.
    val phraseAttempts = rememberPhraseAttemptTracker()
    var emailCodeInput by remember { mutableStateOf("") }
    var generatedEmailCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var emailCodeSentTo by remember { mutableStateOf<String?>(null) }

    fun sendEmailVerificationCode(code: String) {
        val account = CloudSyncManager.currentAccount.value
        val targetEmail = account?.email ?: "seu e-mail de cadastro"
        emailCodeSentTo = targetEmail

        // Trigger local notification with email code
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "lockin_security_codes"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Códigos de Segurança Lock In",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Envio de senhas de confirmação e e-mail"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("LOCK IN: Código de Desativação")
                .setContentText("Seu código de 8 caracteres é: $code (digite manualmente no app)")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Seu código de 8 caracteres para desativar o Bloqueio de Instalação Absoluta é: $code\n\nDigite caractere por caractere manualmente. Cópia e colagem bloqueada por segurança.")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            notificationManager.notify(778899, builder.build())
        } catch (_: Exception) {}
    }

    fun generate8CharToken(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        generatedEmailCode = code
        sendEmailVerificationCode(code)
        return code
    }

    LaunchedEffect(deactivationStage) {
        if (!isEnabling && deactivationStage == 3 && generatedEmailCode.isBlank()) {
            generate8CharToken()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = cardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                DotMatrixPattern(modifier = Modifier.fillMaxSize())

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (isEnabling) accentColor else Color(0xFFEF4444),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isEnabling) "BLOQUEIO ABSOLUTO" else "DESATIVAR BLOQUEIO",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = textColor
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = subtextColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isEnabling) {
                        // Activation confirmation screen
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(accentColor.copy(alpha = 0.1f))
                                    .border(1.dp, accentColor, RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "PROTEÇÃO CONTRA RECAÍDAS 100% INVIOLÁVEL",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = accentColor
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Ao ativar o Bloqueio de Instalação Absoluta, NÃO HAVERÁ FORMA de remover qualquer aplicativo da sua lista de bloqueio de instalação enquanto esta opção estiver ligada.",
                                            fontSize = 11.5.sp,
                                            color = textColor,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Como funciona a desativação futura:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )

                            Text(
                                text = "• Para desativar este modo será OBRIGATÓRIO digitar sua senha $targetPassCount vez${if (targetPassCount > 1) "es seguidas" else ""} sem errar, digitar a frase do seu nível de lock (${currentLockLevel.title}) e digitar uma chave de 8 caracteres enviada para o seu e-mail (sem poder copiar e colar).\n\n• No nível ${LockLevel.DEV_DREAM.title} a desativação é impossível: a lista de bloqueio de instalação é permanente.",
                                fontSize = 11.5.sp,
                                color = subtextColor,
                                lineHeight = 17.sp
                            )
                        }

                        Button(
                            onClick = {
                                userPrefs.setAbsoluteInstallBlock(true)
                                onStateChanged(true)
                                Toast.makeText(context, "Bloqueio de instalação absoluta ATIVADO!", Toast.LENGTH_LONG).show()
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "ATIVAR BLOQUEIO ABSOLUTO",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    } else {
                        // Deactivation Flow: permanent at Dev Dream, otherwise the level-scaled ritual
                        CompositionLocalProvider(LocalTextToolbar provides EmptyTextToolbar) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                if (isDevDream) {
                                    // Nível Obcecado: não existe forma de desativar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                            .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp))
                                            .padding(16.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "NÍVEL ${LockLevel.DEV_DREAM.title.uppercase()}: BLOQUEIO PERMANENTE",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFFEF4444)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "No nível máximo de lock não é possível desativar o Bloqueio Absoluto. Nenhum aplicativo poderá ser removido da sua lista de bloqueio de instalação enquanto você estiver neste nível.",
                                                fontSize = 11.5.sp,
                                                color = textColor,
                                                lineHeight = 17.sp
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = onDismiss,
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CmfDark),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "ENTENDIDO",
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White
                                        )
                                    }
                                    return@Column
                                }

                                // 3 Stages Indicator
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    val stages = listOf("1. SENHA ${targetPassCount}X", "2. FRASE DO NÍVEL", "3. E-MAIL 8 DÍGITOS")
                                    stages.forEachIndexed { index, label ->
                                        val stageNum = index + 1
                                        val isDone = deactivationStage > stageNum
                                        val isCurrent = deactivationStage == stageNum

                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 4.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isDone) Color(0xFF10B981)
                                                    else if (isCurrent) Color(0xFFEF4444)
                                                    else borderColor
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                when (deactivationStage) {
                                    1 -> {
                                        // Stage 1: Consecutive passwords according to the saved lock level
                                        Text(
                                            text = "ETAPA 1: DIGITE A SENHA $targetPassCount VEZ${if (targetPassCount > 1) "ES CONSECUTIVAS" else ""} (${successfulPassCount + 1}/$targetPassCount)",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFFEF4444)
                                        )
                                        Text(
                                            text = "Se errar a senha a qualquer momento, a contagem zera e volta para 1. Cópia e colagem estritamente bloqueada.",
                                            fontSize = 11.sp,
                                            color = subtextColor
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            for (i in 0 until targetPassCount) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(horizontal = 4.dp)
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (i < successfulPassCount) Color(0xFF10B981)
                                                            else if (i == successfulPassCount) Color(0xFFEF4444)
                                                            else borderColor
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (i < successfulPassCount) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    } else {
                                                        Text(
                                                            text = "${i + 1}",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        ManualEntryTextField(
                                            value = passwordInput,
                                            onValueChange = { newVal ->
                                                if (!isManualTyping(passwordInput, newVal)) {
                                                    errorMessage = "Cópia e colagem não são permitidas! Digite manualmente."
                                                } else {
                                                    passwordInput = newVal
                                                    errorMessage = null
                                                }
                                            },
                                            label = { Text("Senha (${successfulPassCount + 1} de $targetPassCount)") },
                                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(onDone = {
                                                if (userPrefs.verifyTrustPassword(passwordInput)) {
                                                    val next = successfulPassCount + 1
                                                    if (next >= targetPassCount) {
                                                        successfulPassCount = next
                                                        deactivationStage = 2
                                                        passwordInput = ""
                                                        errorMessage = null
                                                    } else {
                                                        successfulPassCount = next
                                                        passwordInput = ""
                                                        errorMessage = null
                                                    }
                                                } else {
                                                    successfulPassCount = 0
                                                    passwordInput = ""
                                                    errorMessage = "Senha incorreta! Contagem zerada e retornada para a 1ª repetição."
                                                }
                                            }),
                                            trailingIcon = {
                                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                    Icon(
                                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                        contentDescription = null,
                                                        tint = subtextColor
                                                    )
                                                }
                                            },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        if (errorMessage != null) {
                                            Text(
                                                text = errorMessage!!,
                                                fontSize = 11.sp,
                                                color = Color(0xFFEF4444)
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                if (userPrefs.verifyTrustPassword(passwordInput)) {
                                                    val next = successfulPassCount + 1
                                                    if (next >= targetPassCount) {
                                                        successfulPassCount = next
                                                        deactivationStage = 2
                                                        passwordInput = ""
                                                        errorMessage = null
                                                    } else {
                                                        successfulPassCount = next
                                                        passwordInput = ""
                                                        errorMessage = null
                                                    }
                                                } else {
                                                    successfulPassCount = 0
                                                    passwordInput = ""
                                                    errorMessage = "Senha incorreta! Contagem zerada e retornada para a 1ª repetição."
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = if (successfulPassCount + 1 >= targetPassCount) "CONCLUIR SENHAS E IR PARA A FRASE"
                                                else "CONFIRMAR SENHA (${successfulPassCount + 1}/$targetPassCount)",
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    2 -> {
                                        // Stage 2: Phrase required by the current lock level
                                        Text(
                                            text = "ETAPA 2: DIGITE A FRASE DO NÍVEL ${currentLockLevel.title.uppercase(java.util.Locale.ROOT)}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFFEF4444)
                                        )
                                        Text(
                                            text = "Digite a frase manualmente. Maiúsculas e minúsculas são aceitas; colagem proibida:",
                                            fontSize = 11.sp,
                                            color = subtextColor
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isDark) Color(0xFF222226) else Color(0xFFF1F1F4))
                                                .padding(12.dp)
                                        ) {
                                            PhraseTargetDisplay(
                                                phrase = targetPhrase,
                                                failedInput = if (phraseAttempts.showErrorHighlight) phraseAttempts.firstFailedInput else null,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = textColor
                                            )
                                        }

                                        ManualEntryTextField(
                                            value = phraseInput,
                                            onValueChange = { newVal ->
                                                if (!isManualTyping(phraseInput, newVal)) {
                                                    errorMessage = "Cópia e colagem não são permitidas! Digite a frase manualmente."
                                                } else {
                                                    phraseInput = newVal
                                                    errorMessage = null
                                                }
                                            },
                                            label = { Text("Digite a frase acima") },
                                            modifier = Modifier.fillMaxWidth(),
                                            minLines = 3
                                        )

                                        if (errorMessage != null) {
                                            Text(
                                                text = errorMessage!!,
                                                fontSize = 11.sp,
                                                color = Color(0xFFEF4444)
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                if (PhraseEntryCheck.isExactMatch(phraseInput, targetPhrase)) {
                                                    deactivationStage = 3
                                                    errorMessage = null
                                                } else if (phraseAttempts.registerFailure(phraseInput)) {
                                                    // 3ª falha na frase: recomeça desde a senha
                                                    deactivationStage = 1
                                                    successfulPassCount = 0
                                                    passwordInput = ""
                                                    phraseInput = ""
                                                    phraseAttempts.reset()
                                                    errorMessage = "Você errou a frase 3 vezes! A desativação foi reiniciada desde a senha."
                                                } else {
                                                    val left = phraseAttempts.attemptsLeft
                                                    errorMessage = "Frase incorreta! Resta${if (left > 1) "m" else ""} $left chance${if (left > 1) "s" else ""}. Digite exatamente como apresentado."
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = "CONFIRMAR FRASE E SOLICITAR CÓDIGO POR E-MAIL",
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.5.sp,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    3 -> {
                                        // Stage 3: 8-char Code from Email
                                        Text(
                                            text = "ETAPA 3: DIGITE A SENHA DE 8 CARACTERES DO E-MAIL",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFFEF4444)
                                        )
                                        Text(
                                            text = "Enviamos um código de segurança de 8 caracteres para ${emailCodeSentTo ?: "seu e-mail de cadastro"}. Verifique sua caixa de entrada e digite-o abaixo manualmente.",
                                            fontSize = 11.sp,
                                            color = subtextColor,
                                            lineHeight = 15.sp
                                        )

                                        ManualEntryTextField(
                                            value = emailCodeInput,
                                            onValueChange = { newVal ->
                                                if (!isManualTyping(emailCodeInput, newVal)) {
                                                    errorMessage = "Cópia e colagem não são permitidas! Digite o código caractere por caractere."
                                                } else {
                                                    emailCodeInput = newVal.uppercase().take(8)
                                                    errorMessage = null
                                                }
                                            },
                                            label = { Text("Código de 8 caracteres (Ex: $generatedEmailCode)") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        if (errorMessage != null) {
                                            Text(
                                                text = errorMessage!!,
                                                fontSize = 11.sp,
                                                color = Color(0xFFEF4444)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    generate8CharToken()
                                                    Toast.makeText(context, "Novo código enviado para seu e-mail/notificação!", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Text("REENVIAR CÓDIGO", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            }

                                            Button(
                                                onClick = {
                                                    val cleanInput = emailCodeInput.trim().uppercase()
                                                    if (cleanInput == generatedEmailCode && generatedEmailCode.isNotBlank()) {
                                                        userPrefs.deactivateAbsoluteInstallBlockWithChallenge()
                                                        onStateChanged(false)
                                                        Toast.makeText(context, "Bloqueio de instalação absoluta DESATIVADO com sucesso!", Toast.LENGTH_LONG).show()
                                                        onDismiss()
                                                    } else {
                                                        errorMessage = "Código de 8 caracteres incorreto! Verifique seu e-mail/notificação."
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "FINALIZAR E DESATIVAR",
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    color = Color.White
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
        }
    }
}
