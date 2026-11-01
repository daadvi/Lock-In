package com.example.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.RestrictedAppEntity
import com.example.ui.components.DigitalClockDisplay
import com.example.ui.components.DotMatrixPattern
import com.example.ui.components.NothingCard
import com.example.ui.components.cmfTextFieldColors
import com.example.ui.theme.BlockyFontFamily
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import com.example.ui.theme.LocalCustomTextColor

@Composable
fun ClockLimitScreen(
    screenTimeLimitMinutes: Int,
    screenTimeResetHours: Int,
    onUpdateScreenTimeLimit: (Int) -> Unit,
    onUpdateResetIntervalHours: (Int) -> Unit,
    onResetScreenTimeNow: () -> Unit,
    restrictedApps: List<RestrictedAppEntity>,
    onUpdateAppLimit: (RestrictedAppEntity) -> Unit,
    onNavigateToTrustUnlock: ((packageName: String, appName: String) -> Unit)? = null,
    onBack: () -> Unit
) {
    val accentColor = LocalCustomTextColor.current
    val context = androidx.compose.ui.platform.LocalContext.current

    val initialMinutes = if (screenTimeLimitMinutes > 0) screenTimeLimitMinutes else 23
    var isEnabled by remember(screenTimeLimitMinutes) { mutableStateOf(screenTimeLimitMinutes > 0) }
    var tempLimitMinutes by remember(screenTimeLimitMinutes) { mutableIntStateOf(initialMinutes) }
    var tempResetHours by remember(screenTimeResetHours) { mutableIntStateOf(screenTimeResetHours) }
    var minutesInput by remember(screenTimeLimitMinutes) { mutableStateOf(if (screenTimeLimitMinutes > 0) screenTimeLimitMinutes.toString() else "23") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            DotMatrixPattern(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
            ) {
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
                            tint = CmfDark
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TEMPO DE TELA & LIMITES",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        color = CmfDark,
                        letterSpacing = 1.sp
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // CARD 1: TEMPO ABSOLUTO DE TELA DO TELEFONE
                    item {
                        NothingCard(
                            title = "TEMPO ABSOLUTO DE TELA DO CELULAR",
                            badge = if (isEnabled && screenTimeLimitMinutes > 0) "${tempLimitMinutes}m DEFINIDO" else "DESATIVADO"
                        ) {
                            Column {
                                // Enable / Disable Switch Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isEnabled) accentColor.copy(alpha = 0.08f) else Color(0xFFF7F7F9))
                                        .border(1.dp, if (isEnabled) accentColor.copy(alpha = 0.3f) else CmfBorder, RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "ATIVAR TEMPO ABSOLUTO",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = BlockyFontFamily,
                                            color = CmfDark
                                        )
                                        Text(
                                            text = if (isEnabled) "Limite diário no aparelho ativo." else "Sem limite de tempo absoluto diário.",
                                            fontSize = 10.sp,
                                            color = CmfMutedText
                                        )
                                    }
                                    Switch(
                                        checked = isEnabled,
                                        onCheckedChange = { checked ->
                                            isEnabled = checked
                                            if (checked) {
                                                onUpdateScreenTimeLimit(tempLimitMinutes)
                                            } else {
                                                onUpdateScreenTimeLimit(0)
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = accentColor
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (!isEnabled) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF4F4F6))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = "Você optou por NÃO ativar tempo absoluto no celular. O aparelho não será bloqueado ao longo do dia. Você ainda pode colocar limites individuais em apps específicos abaixo.",
                                            fontSize = 11.sp,
                                            color = CmfDark,
                                            lineHeight = 15.sp
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Tempo total de tela permitido para usar o telefone no dia inteiro:",
                                        fontSize = 12.sp,
                                        color = CmfDark,
                                        lineHeight = 16.sp
                                    )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Digital Clock display
                                DigitalClockDisplay(
                                    totalMinutes = tempLimitMinutes,
                                    onMinutesChanged = { newMin ->
                                        tempLimitMinutes = newMin
                                        minutesInput = newMin.toString()
                                        onUpdateScreenTimeLimit(newMin)
                                    },
                                    label = "TEMPO LIMITE DO APARELHO",
                                    subLabel = "${tempLimitMinutes} min"
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Direct Number Input + Stepper
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CmfLightGray)
                                            .clickable {
                                                if (tempLimitMinutes > 5) {
                                                    val newM = tempLimitMinutes - 5
                                                    tempLimitMinutes = newM
                                                    minutesInput = newM.toString()
                                                    onUpdateScreenTimeLimit(newM)
                                                }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                    ) {
                                        Text("-5m", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = BlockyFontFamily)
                                    }

                                    OutlinedTextField(
                                        value = minutesInput,
                                        onValueChange = { input ->
                                            val filtered = input.filter { it.isDigit() }
                                            minutesInput = filtered
                                            val num = filtered.toIntOrNull()
                                            if (num != null && num > 0) {
                                                tempLimitMinutes = num
                                                onUpdateScreenTimeLimit(num)
                                            }
                                        },
                                        placeholder = { Text("Ex: 23") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        trailingIcon = {
                                            Text(
                                                text = "minutos",
                                                fontFamily = BlockyFontFamily,
                                                fontSize = 11.sp,
                                                color = CmfMutedText,
                                                modifier = Modifier.padding(end = 12.dp)
                                            )
                                        },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("screen_time_minutes_input"),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = cmfTextFieldColors(accentColor = accentColor)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CmfLightGray)
                                            .clickable {
                                                val newM = tempLimitMinutes + 5
                                                tempLimitMinutes = newM
                                                minutesInput = newM.toString()
                                                onUpdateScreenTimeLimit(newM)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                    ) {
                                        Text("+5m", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = BlockyFontFamily)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Quick presets
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(15, 23, 30, 45, 60, 120).forEach { preset ->
                                        val isSelected = tempLimitMinutes == preset
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) accentColor else CmfLightGray)
                                                .border(1.dp, if (isSelected) accentColor else CmfBorder, RoundedCornerShape(6.dp))
                                                .clickable {
                                                    tempLimitMinutes = preset
                                                    minutesInput = preset.toString()
                                                    onUpdateScreenTimeLimit(preset)
                                                }
                                                .padding(vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = "${preset}m",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = BlockyFontFamily,
                                                color = if (isSelected) Color.White else CmfDark
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // RESET INTERVAL CONFIGURATION
                                Text(
                                    text = "TEMPO DE RESET (INTERVALO APÓS ESGOTAR):",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = BlockyFontFamily,
                                    color = accentColor,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Se o usuário usou o celular até o tempo acabar, ele só poderá usar o telefone depois desse intervalo, quando o timer reiniciará:",
                                    fontSize = 11.sp,
                                    color = CmfMutedText,
                                    lineHeight = 15.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                val resetOptions = listOf(
                                    1 to "1h",
                                    2 to "2h",
                                    4 to "4h",
                                    6 to "6h",
                                    12 to "12h",
                                    24 to "1 dia (padrão)"
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    resetOptions.forEach { (hrs, label) ->
                                        val isSel = tempResetHours == hrs
                                        Box(
                                            modifier = Modifier
                                                .weight(if (hrs == 24) 1.5f else 1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSel) accentColor else Color(0xFFEBEBF0))
                                                .clickable {
                                                    tempResetHours = hrs
                                                    onUpdateResetIntervalHours(hrs)
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = BlockyFontFamily,
                                                color = if (isSel) Color.White else CmfDark
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Immediate reset button
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF0F0F4))
                                        .clickable { onResetScreenTimeNow() }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "REINICIAR SALDO DE TELA AGORA",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = BlockyFontFamily,
                                            color = CmfDark
                                        )
                                    }
                                }
                                }
                            }
                        }
                    }

                    // CARD 2: INDIVIDUAL APP LIMITS
                    item {
                        Text(
                            text = "LIMITES ESPECÍFICOS POR APLICATIVO:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = BlockyFontFamily,
                            color = CmfMutedText
                        )
                    }

                    val activeApps = restrictedApps.filter {
                        it.isLimited && !com.example.util.AppFilterHelper.isLockInSelfApp(context, it.packageName, it.appName)
                    }
                    if (activeApps.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CmfLightGray.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nenhum aplicativo selecionado para limitar individualmente.\nO tempo de tela geral do aparelho se aplica a todos.",
                                    fontSize = 12.sp,
                                    color = CmfMutedText,
                                    fontFamily = BlockyFontFamily
                                )
                            }
                        }
                    } else {
                        items(activeApps, key = { it.packageName }) { app ->
                            IndividualAppClockCard(
                                app = app,
                                accentColor = accentColor,
                                onUpdateApp = onUpdateAppLimit,
                                onNavigateToTrustUnlock = onNavigateToTrustUnlock
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
fun IndividualAppClockCard(
    app: RestrictedAppEntity,
    accentColor: Color,
    onUpdateApp: (RestrictedAppEntity) -> Unit,
    onNavigateToTrustUnlock: ((packageName: String, appName: String) -> Unit)? = null
) {
    val context = LocalContext.current
    NothingCard(
        title = app.appName,
        badge = if (app.isShortVideoPlatform) "SUPORTA SHORTS" else "APP"
    ) {
        Column {
            val isDepleted = app.isLimitDepleted || app.isBlocked || (app.individualRemainingSeconds <= 0L && app.individualLimitMinutes > 0)
            if (isDepleted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE53935).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFFE53935), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Bloqueado",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Aquele app está bloqueado e o limite só pode ser redefinido quando o app estiver liberado de novo.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE53935),
                                lineHeight = 15.sp
                            )
                            if (onNavigateToTrustUnlock != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = { onNavigateToTrustUnlock(app.packageName, app.appName) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = "DESBLOQUEAR ESTE APP (QUEBRAR STREAK)",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // General / Total limit for this app
            DigitalClockDisplay(
                totalMinutes = app.individualLimitMinutes,
                onMinutesChanged = { newMin ->
                    if (isDepleted) {
                        android.widget.Toast.makeText(
                            context,
                            "Aquele app está bloqueado e o limite só pode ser redefinido quando o app estiver liberado de novo.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    } else {
                        onUpdateApp(app.copy(individualLimitMinutes = newMin, individualRemainingSeconds = newMin * 60L))
                    }
                },
                label = "LIMITE DO APLICATIVO",
                subLabel = "${app.individualLimitMinutes} min"
            )

            if (app.isShortVideoPlatform) {
                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF9F5), RoundedCornerShape(8.dp))
                        .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIMITES SEPARADOS PARA VÍDEOS CURTOS vs LONGOS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = BlockyFontFamily,
                                color = accentColor
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Shorts limit
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Shorts / Reels:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CmfDark
                                )
                                Text(
                                    text = "Bloqueia após ${app.shortVideoLimitMinutes} min",
                                    fontSize = 11.sp,
                                    color = CmfMutedText
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CmfLightGray)
                                        .clickable {
                                            if (isDepleted) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Aquele app está bloqueado e o limite só pode ser redefinido quando o app estiver liberado de novo.",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                val newM = (app.shortVideoLimitMinutes - 5).coerceAtLeast(1)
                                                onUpdateApp(app.copy(shortVideoLimitMinutes = newM, shortVideoRemainingSeconds = newM * 60L))
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                    Text("-5m", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CmfLightGray)
                                        .clickable {
                                            if (isDepleted) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Aquele app está bloqueado e o limite só pode ser redefinido quando o app estiver liberado de novo.",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                val newM = (app.shortVideoLimitMinutes + 5).coerceAtMost(180)
                                                onUpdateApp(app.copy(shortVideoLimitMinutes = newM, shortVideoRemainingSeconds = newM * 60L))
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("+5m", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Long videos limit
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Vídeos Longos:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CmfDark
                                )
                                Text(
                                    text = "Bloqueia após ${app.longVideoLimitMinutes} min",
                                    fontSize = 11.sp,
                                    color = CmfMutedText
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CmfLightGray)
                                        .clickable {
                                            if (isDepleted) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Aquele app está bloqueado e o limite só pode ser redefinido quando o app estiver liberado de novo.",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                val newM = (app.longVideoLimitMinutes - 5).coerceAtLeast(1)
                                                onUpdateApp(app.copy(longVideoLimitMinutes = newM, longVideoRemainingSeconds = newM * 60L))
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("-5m", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CmfLightGray)
                                        .clickable {
                                            if (isDepleted) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Aquele app está bloqueado e o limite só pode ser redefinido quando o app estiver liberado de novo.",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                val newM = (app.longVideoLimitMinutes + 5).coerceAtMost(180)
                                                onUpdateApp(app.copy(longVideoLimitMinutes = newM, longVideoRemainingSeconds = newM * 60L))
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("+5m", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
