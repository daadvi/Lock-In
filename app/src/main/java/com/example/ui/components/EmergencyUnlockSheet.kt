package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.example.util.EmptyTextToolbar
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserPreferencesRepository
import com.example.service.EmergencyUnlockManager
import com.example.service.StreakManager
import com.example.ui.theme.BlockyFontFamily
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Slide-up button placed on the bottom-left of the lock screen dock.
 * Sliding it upwards through a tall track opens the Emergency Unlock Screen.
 */
@Composable
fun SlideUpEmergencyButton(
    modifier: Modifier = Modifier,
    onTrigger: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val dragOffsetY = remember { Animatable(0f) }

    // Generous travel distance of ~94dp (over 250px)
    val maxTravelPx = remember(density) { with(density) { -94.dp.toPx() } }

    val remainingUnlocks = remember { EmergencyUnlockManager.getRemainingUnlocksThisMonth(context) }
    val canUse = remainingUnlocks > 0

    val dragProgress = (dragOffsetY.value / maxTravelPx).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .width(56.dp)
            .height(148.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF131316))
            .border(
                1.5.dp,
                if (canUse) Color(0xFF2C2C34) else Color(0xFF4A1A1A),
                RoundedCornerShape(28.dp)
            )
            .clickable {
                if (!canUse) {
                    Toast.makeText(
                        context,
                        "Limite mensal atingido! Este botão só pode ser usado 2 vezes por mês (0 restantes). O limite será resetado no dia 1º do próximo mês.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Deslize o botão até o topo do trilho para abrir o Desbloqueio ($remainingUnlocks/2 restantes este mês)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .padding(5.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Track cues: Upper Target Slot + Guided Chevrons
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Target slot at the very top
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (dragProgress >= 0.75f) Color(0xFFFF5500).copy(alpha = 0.35f)
                        else Color(0xFF1D1D22)
                    )
                    .border(
                        1.dp,
                        if (dragProgress >= 0.75f) Color(0xFFFF5500) else Color(0xFF33333E),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (dragProgress >= 0.75f) Icons.Default.LockOpen else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (dragProgress >= 0.75f) Color(0xFFFF5500) else Color(0xFF71717A),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Upward arrow indicators in the center of the track
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = if (canUse) Color(0xFFFF5500).copy(alpha = 0.7f) else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (canUse) "DESLIZE" else "0/2",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = BlockyFontFamily,
                    color = if (canUse) Color(0xFFA1A1AA) else Color(0xFFE53935),
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "AO TOPO",
                    fontSize = 6.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BlockyFontFamily,
                    color = Color(0xFF71717A)
                )
            }

            // Small bottom spacer for the knob base position
            Spacer(modifier = Modifier.size(38.dp))
        }

        // Draggable Knob (Requires dragging up by ~94dp)
        Box(
            modifier = Modifier
                .offset { IntOffset(0, dragOffsetY.value.roundToInt()) }
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    if (!canUse) Color(0xFF1A1A1E)
                    else if (dragProgress >= 0.75f) Color(0xFFFF5500)
                    else Color(0xFF26262D)
                )
                .border(
                    1.5.dp,
                    if (!canUse) Color(0xFF333338)
                    else if (dragProgress >= 0.75f) Color.White
                    else Color(0xFFFF5500).copy(alpha = 0.75f),
                    CircleShape
                )
                .pointerInput(canUse) {
                    if (!canUse) return@pointerInput
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                val current = dragOffsetY.value
                                val target = (current + dragAmount).coerceIn(maxTravelPx, 0f)
                                dragOffsetY.snapTo(target)
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                if (dragOffsetY.value <= maxTravelPx * 0.78f) {
                                    // Successfully dragged at least 78% all the way to the top of the tall track
                                    dragOffsetY.animateTo(0f, spring())
                                    onTrigger()
                                } else {
                                    dragOffsetY.animateTo(0f, spring())
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                dragOffsetY.animateTo(0f, spring())
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (canUse) Icons.Default.LockOpen else Icons.Default.Lock,
                contentDescription = "Desbloqueio de Emergência",
                tint = if (dragProgress >= 0.75f) Color.White else if (canUse) Color(0xFFFF5500) else Color.Gray,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Full-screen Emergency Unlock screen displaying:
 * 1. Warning that streak / sequência will be lost and reset to 0
 * 2. Consecutive correct password entries according to the saved lock level
 * 3. Exact confirmation phrase: "Estou seguro que quero desbloquear meu dispositivo"
 * 4. Monthly usage restriction (2 times per month)
 */
@Composable
fun EmergencyUnlockScreen(
    onDismiss: () -> Unit,
    onUnlockConfirmed: () -> Unit
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferencesRepository(context) }
    val targetPassCount = remember(userPrefs) { userPrefs.getLockLevel().passwordRepeatCount }
    val correctPassword = remember {
        val stored = userPrefs.getTrustPassword()
        if (stored.isNotBlank()) stored else "LockIn@2026"
    }

    val remainingMonthlyUnlocks = remember { EmergencyUnlockManager.getRemainingUnlocksThisMonth(context) }
    val usedMonthlyUnlocks = remember { EmergencyUnlockManager.getUsedCountThisMonth(context) }
    val currentStreak = remember { StreakManager.getStreak(context) }

    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var successfulPassCount by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var phraseInput by remember { mutableStateOf("") }

    val requiredPhrase = EmergencyUnlockManager.CONFIRMATION_PHRASE
    val isPhraseValid = com.example.util.KeyboardFuzzyMatcher.matchesWithKeyboardTolerance(phraseInput, requiredPhrase)

    val isFullyReady = successfulPassCount >= targetPassCount && isPhraseValid

    fun verifyPasswordAttempt() {
        if (com.example.util.CryptoPasswordHelper.verifyPassword(passwordInput, correctPassword)) {
            val next = successfulPassCount + 1
            if (next >= targetPassCount) {
                successfulPassCount = targetPassCount
                errorMessage = null
            } else {
                successfulPassCount = next
                errorMessage = null
                passwordInput = ""
            }
        } else {
            // Strict security rule: Any mistake resets the counter to 0!
            successfulPassCount = 0
            errorMessage = "Senha incorreta! A contagem foi zerada. Digite a senha $targetPassCount vez${if (targetPassCount > 1) "es consecutivas" else ""} sem erro."
            passwordInput = ""
        }
    }

    CompositionLocalProvider(LocalTextToolbar provides EmptyTextToolbar) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF09090B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top bar: Back / Close button + Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E1E24))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar ao Bloqueio",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "DESBLOQUEIO DE EMERGÊNCIA",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.size(38.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Monthly Quota Pill & Guarantee
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1F1F24))
                            .border(1.dp, Color(0xFF33333D), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "USO MENSAL: ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = BlockyFontFamily,
                            color = Color(0xFFA1A1AA)
                        )
                        Text(
                            text = "$usedMonthlyUnlocks/2 USADOS • $remainingMonthlyUnlocks RESTANTES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = BlockyFontFamily,
                            color = if (remainingMonthlyUnlocks > 0) Color(0xFFFF5500) else Color(0xFFE53935)
                        )
                    }

                    Text(
                        text = "↻ A contagem de 2 utilizações é sempre resetada no 1º dia de cada mês.",
                        fontSize = 9.sp,
                        fontFamily = BlockyFontFamily,
                        color = Color(0xFF8E8E98)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Critical Streak Loss Warning Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF2A0F0F))
                        .border(1.5.dp, Color(0xFFE53935), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ATENÇÃO: SUA STREAK SERÁ ZERADA!",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = BlockyFontFamily,
                                color = Color(0xFFE53935)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Ao desbloquear o dispositivo antes do término, sua sequência atual de $currentStreak dias consecutivos será totalmente perdida e resetada para 0.\n\nEste botão só pode ser usado 2 vezes por mês.",
                            fontSize = 12.sp,
                            color = Color(0xFFFFDEDE),
                            lineHeight = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // SECTION 1: PASSWORD ACCORDING TO THE SAVED LOCK LEVEL
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF141417))
                        .border(1.dp, Color(0xFF26262E), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "1. DIGITE SUA SENHA ($targetPassCount VEZ${if (targetPassCount > 1) "ES" else ""} SEM ERRO)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = BlockyFontFamily,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Para garantir que o desbloqueio não foi por impulso, você deve digitar sua senha $targetPassCount vez${if (targetPassCount > 1) "es consecutivas" else ""}. Qualquer erro zera a contagem.",
                            fontSize = 11.sp,
                            color = Color(0xFFA1A1AA),
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // One progress indicator per required password entry
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (step in 1..targetPassCount) {
                                val isDone = successfulPassCount >= step
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDone) Color(0xFF00C853) else Color(0xFF222228))
                                        .border(
                                            1.dp,
                                            if (isDone) Color(0xFF00C853) else Color(0xFF33333D),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isDone) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "$step/$targetPassCount",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = BlockyFontFamily,
                                            color = Color(0xFF71717A)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (successfulPassCount < targetPassCount) {
                            ManualEntryTextField(
                                value = passwordInput,
                                onValueChange = {
                                    passwordInput = it
                                    errorMessage = null
                                },
                                label = { Text("Senha (Tentativa ${successfulPassCount + 1} de $targetPassCount)") },
                                placeholder = { Text("Digite sua senha...") },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { verifyPasswordAttempt() }),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Alternar Visibilidade",
                                            tint = Color.White
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("emergency_unlock_password_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFF5500),
                                    unfocusedBorderColor = Color(0xFF3A3A44),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = Color(0xFFFF5500),
                                    unfocusedLabelColor = Color.Gray
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { verifyPasswordAttempt() },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5500))
                            ) {
                                Text(
                                    text = "CONFIRMAR ETAPA (${successfulPassCount + 1}/$targetPassCount)",
                                    fontWeight = FontWeight.Black,
                                    fontFamily = BlockyFontFamily,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        } else {
                            // Password sequence completed banner
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF003816))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF00C853),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Senha confirmada $targetPassCount vez${if (targetPassCount > 1) "es" else ""} com sucesso!",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF81C784)
                                )
                            }
                        }

                        errorMessage?.let { error ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error,
                                color = Color(0xFFE53935),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 2: CONFIRMATION PHRASE
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF141417))
                        .border(1.dp, Color(0xFF26262E), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "2. DIGITE A FRASE DE CONFIRMAÇÃO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = BlockyFontFamily,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Digite a frase manualmente. Maiúsculas e minúsculas são aceitas:",
                            fontSize = 11.sp,
                            color = Color(0xFFA1A1AA)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Box showing the exact required phrase
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E24))
                                .border(1.dp, Color(0xFF33333F), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = requiredPhrase,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFFFB300),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        ManualEntryTextField(
                            value = phraseInput,
                            onValueChange = { phraseInput = it },
                            placeholder = { Text("Digite a frase exata aqui...", fontSize = 12.sp) },
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("emergency_unlock_phrase_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isPhraseValid) Color(0xFF00C853) else Color(0xFFFF5500),
                                unfocusedBorderColor = if (isPhraseValid) Color(0xFF00C853) else Color(0xFF3A3A44),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        if (isPhraseValid) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF00C853),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Frase confirmada!",
                                    fontSize = 11.sp,
                                    color = Color(0xFF81C784),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // FINAL UNLOCK BUTTON
                Button(
                    onClick = {
                        if (isFullyReady) {
                            EmergencyUnlockManager.recordUnlockUsed(context)
                            Toast.makeText(
                                context,
                                "Dispositivo desbloqueado. Sua sequência foi resetada para 0.",
                                Toast.LENGTH_LONG
                            ).show()
                            onUnlockConfirmed()
                        }
                    },
                    enabled = isFullyReady,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("emergency_unlock_confirm_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935),
                        disabledContainerColor = Color(0xFF282830),
                        contentColor = Color.White,
                        disabledContentColor = Color(0xFF6B6B78)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFullyReady) "DESBLOQUEAR (ZERAR SEQUÊNCIA)" else "PREENCHA A SENHA (${targetPassCount}X) E A FRASE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = BlockyFontFamily,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Cancel button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1B1B20),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "CANCELAR E MANTER MEU FOCO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BlockyFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun normalizeEmergencyPhrase(text: String): String {
    return text.trim()
        .replace(Regex("\\s+"), " ")
        .replace('“', '"')
        .replace('”', '"')
        .replace('‘', '\'')
        .replace('’', '\'')
}
