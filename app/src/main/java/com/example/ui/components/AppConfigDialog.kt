package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.RestrictedAppEntity
import com.example.service.AppVideoCategoryHelper
import com.example.ui.theme.BlockyFontFamily
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite

enum class AppControlMode {
    ABSOLUTE_TIME,
    VIDEO_LIMITS
}

@Composable
fun AppConfigDialog(
    app: RestrictedAppEntity,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSaveApp: (RestrictedAppEntity) -> Unit,
    onNavigateToAppRoutine: ((packageName: String, appName: String) -> Unit)? = null,
    onNavigateToTrustUnlock: ((packageName: String, appName: String) -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val pkg = app.packageName
    val hasShortsOnly = AppVideoCategoryHelper.hasShortVideosOnly(pkg)
    val hasBothVideos = AppVideoCategoryHelper.hasBothShortAndLongVideos(pkg)
    val hasLongOnly = AppVideoCategoryHelper.hasLongVideosOnly(pkg)
    val hasAnyVideos = AppVideoCategoryHelper.hasVideos(pkg) || app.hasVideoContent || app.isShortVideoPlatform
    val isDepleted = app.isLimitDepleted

    // Initial mode selection:
    // If app has videos and useAbsoluteTime is false -> VIDEO_LIMITS
    // Otherwise -> ABSOLUTE_TIME
    var controlMode by remember {
        mutableStateOf(
            if (hasAnyVideos && !app.useAbsoluteTime) AppControlMode.VIDEO_LIMITS
            else AppControlMode.ABSOLUTE_TIME
        )
    }

    // Absolute time input
    var individualMinutes by remember {
        mutableIntStateOf(if (app.individualLimitMinutes > 0) app.individualLimitMinutes else 20)
    }
    var individualInput by remember { mutableStateOf(individualMinutes.toString()) }

    // Short video time input
    var shortLimitMinutes by remember {
        mutableIntStateOf(if (app.shortVideoLimitMinutes > 0) app.shortVideoLimitMinutes else 10)
    }
    var shortInput by remember { mutableStateOf(shortLimitMinutes.toString()) }

    // Long video time input
    var longLimitMinutes by remember {
        mutableIntStateOf(if (app.longVideoLimitMinutes > 0) app.longVideoLimitMinutes else 25)
    }
    var longInput by remember { mutableStateOf(longLimitMinutes.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.appName.uppercase(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = BlockyFontFamily,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = when {
                                hasBothVideos -> "VÍDEOS CURTOS & LONGOS DETECTADOS"
                                hasShortsOnly -> "VÍDEOS CURTOS (REELS/SHORTS)"
                                hasLongOnly -> "VÍDEOS LONGOS DETECTADOS"
                                hasAnyVideos -> "CONTÉM CONTEÚDO EM VÍDEO"
                                else -> "APP SEM VÍDEOS DETECTADOS"
                            },
                            fontSize = 9.sp,
                            fontFamily = BlockyFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when {
                                !hasAnyVideos || controlMode == AppControlMode.ABSOLUTE_TIME -> "${individualMinutes}m TOTAL"
                                hasBothVideos -> "${shortLimitMinutes}m / ${longLimitMinutes}m"
                                hasShortsOnly -> "${shortLimitMinutes}m SHORTS"
                                else -> "${longLimitMinutes}m VÍDEOS"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = BlockyFontFamily,
                            color = accentColor
                        )
                    }
                }

                val isUnlockedToday = com.example.service.AppUnlockDailyTracker.isAppUnlockedToday(context, app.packageName)

                if (app.isUsageInProgress) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFEF3C7))
                            .border(1.5.dp, Color(0xFFF59E0B), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Em andamento",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "LIMITE EM ANDAMENTO • BLOQUEADO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = BlockyFontFamily,
                                    color = Color(0xFFB45309)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Você já utilizou este app hoje e ainda possui tempo restante (${app.totalAvailableSeconds / 60}m restantes). Para manter o foco e evitar burlar suas metas, você deve aguardar o timer esgotar (chegar a zero) para poder redefini-lo.",
                                fontSize = 10.5.sp,
                                color = Color(0xFF92400E),
                                lineHeight = 14.sp
                            )
                        }
                    }
                } else if (isDepleted) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE53935).copy(alpha = 0.12f))
                            .border(1.5.dp, Color(0xFFE53935), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Bloqueado",
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isUnlockedToday) "LIMITE DIÁRIO ATINGIDO (1/1)" else "APLICATIVO BLOQUEADO",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = BlockyFontFamily,
                                        color = Color(0xFFE53935)
                                    )
                                    Text(
                                        text = if (isUnlockedToday) {
                                            "Este app já foi desbloqueado 1 vez hoje. O Lock In só permite 1 desbloqueio por dia por app. Aguarde a meia-noite."
                                        } else {
                                            "O tempo limite deste app chegou a zero. Disponível 1 desbloqueio hoje mediante desafio."
                                        },
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 14.sp
                                    )
                                }
                            }

                            if (!isUnlockedToday && onNavigateToTrustUnlock != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onNavigateToTrustUnlock(app.packageName, app.appName)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE53935),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "DESBLOQUEAR (5X SENHA + FRASE)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = BlockyFontFamily
                                    )
                                }
                            }
                        }
                    }
                }

                if (onNavigateToAppRoutine != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .clickable {
                                onDismiss()
                                onNavigateToAppRoutine(app.packageName, app.appName)
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "BLOQUEAR EM HORÁRIOS ESPECÍFICOS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = BlockyFontFamily,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Criar rotina de horários para este app na aba de rotinas",
                                        fontSize = 9.5.sp,
                                        color = CmfMutedText
                                    )
                                }
                            }
                            Text(
                                text = "ABRIR →",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = BlockyFontFamily,
                                color = accentColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // CASE 1: APPS WITHOUT VIDEOS
                if (!hasAnyVideos) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF7F7F9))
                            .border(1.dp, CmfBorder, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Este app não possui catálogo de vídeos curtos ou longos. Defina o tempo total permitido para ele:",
                                fontSize = 11.sp,
                                color = CmfDark,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "TEMPO LIMITE ABSOLUTO NO APP:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BlockyFontFamily,
                        color = CmfDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    TimeInputRow(
                        minutes = individualMinutes,
                        input = individualInput,
                        unitSuffix = "min",
                        accentColor = accentColor,
                        onMinutesChanged = { m ->
                            individualMinutes = m
                            individualInput = m.toString()
                        },
                        onInputChanged = { s -> individualInput = s }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PresetChipsRow(
                        presets = listOf(15, 30, 45, 60),
                        currentMinutes = individualMinutes,
                        accentColor = accentColor,
                        onSelect = { p ->
                            individualMinutes = p
                            individualInput = p.toString()
                        }
                    )
                } else {
                    // CASE 2: APPS WITH VIDEOS
                    Text(
                        text = "ESCOLHA O MODO DE CONTROLE:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        color = CmfDark
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // OPTION A: TEMPO ABSOLUTO DO APP TODO
                    val isModeAbsolute = controlMode == AppControlMode.ABSOLUTE_TIME
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isModeAbsolute) accentColor.copy(alpha = 0.08f) else Color(0xFFF9F9FB))
                            .border(1.5.dp, if (isModeAbsolute) accentColor else CmfBorder, RoundedCornerShape(10.dp))
                            .clickable { controlMode = AppControlMode.ABSOLUTE_TIME }
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isModeAbsolute) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isModeAbsolute) accentColor else CmfMutedText,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "1. TEMPO ABSOLUTO DO APP",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = BlockyFontFamily,
                                        color = if (isModeAbsolute) CmfDark else CmfMutedText
                                    )
                                    Text(
                                        text = "Tempo total único para todo o app. Ao esgotar, o app inteiro bloqueia.",
                                        fontSize = 10.sp,
                                        color = CmfMutedText,
                                        lineHeight = 14.sp
                                    )
                                }
                            }

                            if (isModeAbsolute) {
                                Spacer(modifier = Modifier.height(10.dp))
                                TimeInputRow(
                                    minutes = individualMinutes,
                                    input = individualInput,
                                    unitSuffix = "min",
                                    accentColor = accentColor,
                                    onMinutesChanged = { m ->
                                        individualMinutes = m
                                        individualInput = m.toString()
                                    },
                                    onInputChanged = { s -> individualInput = s }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                PresetChipsRow(
                                    presets = listOf(15, 20, 30, 45, 60),
                                    currentMinutes = individualMinutes,
                                    accentColor = accentColor,
                                    onSelect = { p ->
                                        individualMinutes = p
                                        individualInput = p.toString()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // OPTION B: BLOQUEAR / LIMITAR VÍDEOS
                    val isModeVideos = controlMode == AppControlMode.VIDEO_LIMITS
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isModeVideos) accentColor.copy(alpha = 0.08f) else Color(0xFFF9F9FB))
                            .border(1.5.dp, if (isModeVideos) accentColor else CmfBorder, RoundedCornerShape(10.dp))
                            .clickable { controlMode = AppControlMode.VIDEO_LIMITS }
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isModeVideos) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isModeVideos) accentColor else CmfMutedText,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "2. BLOQUEAR / LIMITAR VÍDEOS",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = BlockyFontFamily,
                                        color = if (isModeVideos) CmfDark else CmfMutedText
                                    )
                                    Text(
                                        text = when {
                                            hasBothVideos -> "Define limites independentes: uma box para vídeos curtos e outra para vídeos longos."
                                            hasShortsOnly -> "Este app só possui vídeos curtos. Define limite específico para Shorts/Reels."
                                            else -> "Este app só possui vídeos longos. Define limite específico para vídeos longos."
                                        },
                                        fontSize = 10.sp,
                                        color = CmfMutedText,
                                        lineHeight = 14.sp
                                    )
                                }
                            }

                            if (isModeVideos) {
                                Spacer(modifier = Modifier.height(12.dp))

                                // BOX PARA VÍDEOS CURTOS (Se tiver vídeos curtos)
                                if (hasBothVideos || hasShortsOnly) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CmfWhite)
                                            .border(1.dp, if (hasBothVideos) accentColor.copy(alpha = 0.5f) else CmfBorder, RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Videocam,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (pkg.contains("instagram")) "LIMITE PARA VÍDEOS CURTOS (REELS):" else "LIMITE PARA VÍDEOS CURTOS (SHORTS):",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = BlockyFontFamily,
                                                color = CmfDark
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        TimeInputRow(
                                            minutes = shortLimitMinutes,
                                            input = shortInput,
                                            unitSuffix = "min shorts",
                                            accentColor = accentColor,
                                            onMinutesChanged = { m ->
                                                shortLimitMinutes = m
                                                shortInput = m.toString()
                                            },
                                            onInputChanged = { s -> shortInput = s }
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        PresetChipsRow(
                                            presets = listOf(5, 10, 15, 20, 30),
                                            currentMinutes = shortLimitMinutes,
                                            accentColor = accentColor,
                                            onSelect = { p ->
                                                shortLimitMinutes = p
                                                shortInput = p.toString()
                                            }
                                        )
                                    }
                                }

                                // BOX PARA VÍDEOS LONGOS (Se tiver vídeos longos)
                                if (hasBothVideos || hasLongOnly) {
                                    if (hasBothVideos) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CmfWhite)
                                            .border(1.dp, if (hasBothVideos) Color(0xFF0288D1).copy(alpha = 0.5f) else CmfBorder, RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.VideoLibrary,
                                                contentDescription = null,
                                                tint = Color(0xFF0288D1),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "LIMITE PARA VÍDEOS LONGOS:",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = BlockyFontFamily,
                                                color = CmfDark
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        TimeInputRow(
                                            minutes = longLimitMinutes,
                                            input = longInput,
                                            unitSuffix = "min longos",
                                            accentColor = Color(0xFF0288D1),
                                            onMinutesChanged = { m ->
                                                longLimitMinutes = m
                                                longInput = m.toString()
                                            },
                                            onInputChanged = { s -> longInput = s }
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        PresetChipsRow(
                                            presets = listOf(15, 20, 30, 45, 60),
                                            currentMinutes = longLimitMinutes,
                                            accentColor = Color(0xFF0288D1),
                                            onSelect = { p ->
                                                longLimitMinutes = p
                                                longInput = p.toString()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (onNavigateToAppRoutine != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.12f))
                            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable {
                                onDismiss()
                                onNavigateToAppRoutine(app.packageName, app.appName)
                            }
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "BLOQUEAR EM HORÁRIOS ESPECÍFICOS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = BlockyFontFamily,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Programar rotina de horários para este app na aba de rotinas",
                                        fontSize = 9.sp,
                                        color = CmfMutedText
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Ir para Rotinas",
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions: Cancel & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CmfLightGray,
                            contentColor = CmfDark
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancelar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (isDepleted) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Aquele app está bloqueado e o limite só pode ser redefinido quando o app estiver liberado de novo.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                return@Button
                            }

                            val finalUseAbsolute = !hasAnyVideos || controlMode == AppControlMode.ABSOLUTE_TIME
                            val isVideoMode = hasAnyVideos && controlMode == AppControlMode.VIDEO_LIMITS

                            val finalShortMinutes = if (hasBothVideos || hasShortsOnly) shortLimitMinutes else 0
                            val finalLongMinutes = if (hasBothVideos || hasLongOnly) longLimitMinutes else 0
                            val totalVideoMinutes = (finalShortMinutes + finalLongMinutes).coerceAtLeast(1)

                            val updated = app.copy(
                                isLimited = true,
                                useAbsoluteTime = finalUseAbsolute,
                                isShortVideoPlatform = hasShortsOnly || hasBothVideos,
                                hasShortVideoContent = hasShortsOnly || hasBothVideos,
                                hasVideoContent = hasAnyVideos,
                                individualLimitMinutes = if (isVideoMode) totalVideoMinutes else individualMinutes,
                                individualRemainingSeconds = if (isVideoMode) totalVideoMinutes * 60L else individualMinutes * 60L,
                                limitShortVideosOnly = isVideoMode && hasShortsOnly,
                                limitLongVideosAlso = isVideoMode && (hasBothVideos || hasLongOnly),
                                shortVideoLimitMinutes = finalShortMinutes,
                                shortVideoRemainingSeconds = finalShortMinutes * 60L,
                                longVideoLimitMinutes = finalLongMinutes,
                                longVideoRemainingSeconds = finalLongMinutes * 60L
                            )
                            onSaveApp(updated)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("save_app_config_button"),
                        enabled = !isDepleted && !app.isUsageInProgress,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDepleted || app.isUsageInProgress) Color.Gray else accentColor,
                            disabledContainerColor = Color(0xFF6B7280),
                            contentColor = Color.White,
                            disabledContentColor = Color(0xFFD1D5DB)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = when {
                                app.isUsageInProgress -> "Limite em Andamento"
                                isDepleted -> "Bloqueado"
                                else -> "Salvar Limite"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeInputRow(
    minutes: Int,
    input: String,
    unitSuffix: String,
    accentColor: Color,
    onMinutesChanged: (Int) -> Unit,
    onInputChanged: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CmfLightGray)
                .clickable {
                    if (minutes > 5) onMinutesChanged(minutes - 5)
                },
            contentAlignment = Alignment.Center
        ) {
            Text("-5m", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        OutlinedTextField(
            value = input,
            onValueChange = { newVal ->
                val filtered = newVal.filter { it.isDigit() }
                onInputChanged(filtered)
                val num = filtered.toIntOrNull()
                if (num != null && num > 0) {
                    onMinutesChanged(num)
                }
            },
            placeholder = { Text("Ex: 15") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            trailingIcon = {
                Text(
                    text = unitSuffix,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = CmfMutedText,
                    modifier = Modifier.padding(end = 8.dp)
                )
            },
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = cmfTextFieldColors(accentColor = accentColor)
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CmfLightGray)
                .clickable {
                    onMinutesChanged(minutes + 5)
                },
            contentAlignment = Alignment.Center
        ) {
            Text("+5m", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun PresetChipsRow(
    presets: List<Int>,
    currentMinutes: Int,
    accentColor: Color,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        presets.forEach { preset ->
            val isSel = currentMinutes == preset
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSel) accentColor else CmfLightGray)
                    .clickable { onSelect(preset) }
                    .padding(vertical = 7.dp)
            ) {
                Text(
                    text = "${preset}m",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BlockyFontFamily,
                    color = if (isSel) Color.White else CmfDark
                )
            }
        }
    }
}
