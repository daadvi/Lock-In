package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.LockLevel
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import com.example.ui.theme.LocalCustomTextColor
import com.example.ui.theme.LocalIsDarkTheme
import com.example.util.EmptyTextToolbar
import com.example.util.isManualTyping

@Composable
fun ChangeLockLevelDialog(
    currentLevel: LockLevel,
    onDismiss: () -> Unit,
    onLevelChanged: (LockLevel) -> Unit
) {
    val context = LocalContext.current
    val accentColor = LocalCustomTextColor.current
    val isDark = LocalIsDarkTheme.current
    val app = com.example.LockInApplication.instance
    val userPrefs = app.userPreferences

    val cardBg = if (isDark) Color(0xFF18181B) else CmfWhite
    val textColor = if (isDark) Color.White else CmfDark
    val subtextColor = if (isDark) Color(0xFFA1A1AA) else CmfMutedText
    val borderColor = if (isDark) Color(0xFF27272A) else CmfBorder

    var pendingTargetLevel by remember { mutableStateOf<LockLevel?>(null) }
    var isInDowngradeChallenge by remember { mutableStateOf(false) }

    // Downgrade challenge states
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var successfulPassCount by remember { mutableIntStateOf(0) }
    var phraseInput by remember { mutableStateOf("") }
    var currentChallengeStage by remember { mutableIntStateOf(1) } // 1: Password repeats, 2: Phrase
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val requiredPassRepeats = currentLevel.passwordRepeatCount
    val requiredPhrase = currentLevel.requiredPhrase

    fun executeLevelChange(newLevel: LockLevel) {
        userPrefs.setLockLevel(newLevel)
        onLevelChanged(newLevel)
        Toast.makeText(context, "Nível de Lock atualizado para '${newLevel.title}'!", Toast.LENGTH_LONG).show()
        onDismiss()
    }

    Dialog(
        onDismissRequest = {
            if (!isInDowngradeChallenge) onDismiss()
        },
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
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isInDowngradeChallenge) "REDUÇÃO DE NÍVEL" else "NÍVEIS DE LOCK",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = textColor
                            )
                        }

                        IconButton(
                            onClick = {
                                if (isInDowngradeChallenge) {
                                    isInDowngradeChallenge = false
                                    pendingTargetLevel = null
                                    passwordInput = ""
                                    phraseInput = ""
                                    successfulPassCount = 0
                                    errorMessage = null
                                } else {
                                    onDismiss()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isInDowngradeChallenge) Icons.Default.ArrowBack else Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = subtextColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isInDowngradeChallenge) {
                        // Level Selection View
                        Text(
                            text = "Nível atual: ${currentLevel.title.uppercase()} (${currentLevel.badgeLabel})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Aumentar o nível de Lock é livre e imediato. Para diminuir, você deverá cumprir o desafio de disciplina do seu nível atual.",
                            fontSize = 11.5.sp,
                            color = subtextColor,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LockLevel.entries.forEach { level ->
                                val isCurrent = level == currentLevel
                                val isUpgrade = level.levelNumber > currentLevel.levelNumber
                                val isDowngrade = level.levelNumber < currentLevel.levelNumber

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isCurrent) accentColor.copy(alpha = 0.08f)
                                            else if (isDark) Color(0xFF202024) else Color(0xFFF9F9FB)
                                        )
                                        .border(
                                            width = if (isCurrent) 1.5.dp else 1.dp,
                                            color = if (isCurrent) accentColor else borderColor,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            if (isCurrent) return@clickable
                                            if (isUpgrade) {
                                                executeLevelChange(level)
                                            } else if (isDowngrade) {
                                                pendingTargetLevel = level
                                                isInDowngradeChallenge = true
                                                successfulPassCount = 0
                                                currentChallengeStage = 1
                                                passwordInput = ""
                                                phraseInput = ""
                                                errorMessage = null
                                            }
                                        }
                                        .padding(14.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isCurrent) accentColor else borderColor
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${level.levelNumber}",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.White,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = level.title,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textColor
                                                )
                                            }

                                            if (isCurrent) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(accentColor)
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "ATIVO",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Black,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color.White
                                                    )
                                                }
                                            } else if (isUpgrade) {
                                                Text(
                                                    text = "AUMENTAR (LIVRE) →",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color(0xFF10B981)
                                                )
                                            } else {
                                                Text(
                                                    text = "DIMINUIR (DESAFIO) →",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color(0xFFEF4444)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = level.description,
                                            fontSize = 11.sp,
                                            color = subtextColor,
                                            lineHeight = 15.sp
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Desbloqueios: ${level.maxDailyUnlocksPerApp}x/dia",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = textColor
                                            )
                                            Text(
                                                text = "Repetições de senha: ${level.passwordRepeatCount}x",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = textColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Downgrade Challenge View
                        val target = pendingTargetLevel ?: currentLevel
                        CompositionLocalProvider(LocalTextToolbar provides EmptyTextToolbar) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                        .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(10.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "DESAFIO DE REDUÇÃO: ${currentLevel.title.uppercase()} → ${target.title.uppercase()}",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFFEF4444)
                                            )
                                            Text(
                                                text = "Para diminuir o nível de Lock, você deve digitar a senha $requiredPassRepeats vezes sem errar e digitar a frase sem errar.",
                                                fontSize = 11.sp,
                                                color = textColor,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                if (currentChallengeStage == 1) {
                                    // Password repetition stage
                                    Text(
                                        text = "ETAPA 1: DIGITE A SENHA $requiredPassRepeats VEZES (${successfulPassCount + 1}/$requiredPassRepeats)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = accentColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Se errar a senha a qualquer momento, o progresso voltará para a 1ª repetição. Cópia e colagem bloqueada.",
                                        fontSize = 11.sp,
                                        color = subtextColor
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Dots indicator
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        for (i in 0 until requiredPassRepeats) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(horizontal = 4.dp)
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (i < successfulPassCount) Color(0xFF10B981)
                                                        else if (i == successfulPassCount) accentColor
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

                                    Spacer(modifier = Modifier.height(16.dp))

                                    OutlinedTextField(
                                        value = passwordInput,
                                        onValueChange = { newVal ->
                                            if (!isManualTyping(passwordInput, newVal)) {
                                                errorMessage = "Cópia e colagem não são permitidas! Digite manualmente."
                                            } else {
                                                passwordInput = newVal
                                                errorMessage = null
                                            }
                                        },
                                        label = { Text("Senha (${successfulPassCount + 1} de $requiredPassRepeats)") },
                                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = {
                                            if (userPrefs.verifyTrustPassword(passwordInput)) {
                                                val next = successfulPassCount + 1
                                                if (next >= requiredPassRepeats) {
                                                    successfulPassCount = next
                                                    currentChallengeStage = 2
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
                                                errorMessage = "Senha incorreta! A contagem foi zerada e voltou para a 1ª repetição."
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
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = errorMessage!!,
                                            fontSize = 11.sp,
                                            color = Color(0xFFEF4444),
                                            lineHeight = 15.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            if (userPrefs.verifyTrustPassword(passwordInput)) {
                                                val next = successfulPassCount + 1
                                                if (next >= requiredPassRepeats) {
                                                    successfulPassCount = next
                                                    currentChallengeStage = 2
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
                                                errorMessage = "Senha incorreta! A contagem foi zerada e voltou para a 1ª repetição."
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = if (successfulPassCount + 1 >= requiredPassRepeats) "CONCLUIR SENHAS E IR PARA A FRASE"
                                            else "CONFIRMAR SENHA (${successfulPassCount + 1}/$requiredPassRepeats)",
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        )
                                    }
                                } else {
                                    // Phrase confirmation stage
                                    Text(
                                        text = "ETAPA 2: DIGITE A FRASE SEM ERRAR",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = accentColor
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Digite exatamente a frase abaixo, caractere por caractere (cópia e colagem desabilitada):",
                                        fontSize = 11.sp,
                                        color = subtextColor
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isDark) Color(0xFF222226) else Color(0xFFF1F1F4))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = "\"$requiredPhrase\"",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textColor,
                                            lineHeight = 17.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    OutlinedTextField(
                                        value = phraseInput,
                                        onValueChange = { newVal ->
                                            if (!isManualTyping(phraseInput, newVal, maxBurst = 3)) {
                                                errorMessage = "Cópia e colagem não são permitidas! Digite a frase manualmente."
                                            } else {
                                                phraseInput = newVal
                                                errorMessage = null
                                            }
                                        },
                                        label = { Text("Digite a frase acima") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2
                                    )

                                    if (errorMessage != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = errorMessage!!,
                                            fontSize = 11.sp,
                                            color = Color(0xFFEF4444),
                                            lineHeight = 15.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            if (com.example.util.KeyboardFuzzyMatcher.matchesWithKeyboardTolerance(phraseInput, requiredPhrase)) {
                                                executeLevelChange(target)
                                            } else {
                                                errorMessage = "Frase incorreta! Digite a frase apresentada."
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "CONFIRMAR REDUÇÃO PARA ${target.title.uppercase()}",
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
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
