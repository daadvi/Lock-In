package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import com.example.service.CloudSyncManager
import com.example.service.LockInVpnManager
import com.example.ui.components.AccountSyncDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.ui.components.cmfTextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.AppGroupEntity
import com.example.data.local.RestrictedAppEntity
import com.example.data.local.RestrictedWebsiteEntity
import com.example.service.StreakManager
import com.example.ui.components.AppConfigDialog
import com.example.ui.components.AppIconImage
import com.example.ui.components.AppWidgetCard
import com.example.ui.components.DotMatrixPattern
import com.example.ui.components.LockInBranding
import com.example.ui.components.NothingCard
import com.example.service.PlayStoreBlacklistManager
import com.example.ui.components.PlayStoreBlacklistDialog
import com.example.ui.theme.BlockyFontFamily
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import com.example.ui.theme.LocalCustomTextColor
import com.example.ui.theme.LocalIsDarkTheme

@Composable
fun DashboardScreen(
    userName: String,
    screenTimeLimitMinutes: Int = 23,
    screenTimeRemainingSeconds: Long = 23 * 60L,
    screenTimeResetHours: Int = 24,
    isScreenTimeLockout: Boolean = false,
    limitedApps: List<RestrictedAppEntity>,
    websites: List<RestrictedWebsiteEntity>,
    resetIntervalHours: Int = 24,
    floatingIslandSize: String = "MEDIUM",
    floatingIslandOpacity: Float = 0.95f,
    onNavigateToApps: () -> Unit,
    onNavigateToLimits: () -> Unit,
    onNavigateToWebsites: () -> Unit,
    onNavigateToStyling: () -> Unit,
    onNavigateToResetSettings: () -> Unit,
    onNavigateToTrustUnlock: ((packageName: String?, appName: String?) -> Unit)? = null,
    onNavigateToPermissions: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToFocusTime: () -> Unit,
    onNavigateToAppRoutine: ((packageName: String, appName: String) -> Unit)? = null,
    onUpdateAppEntity: (RestrictedAppEntity) -> Unit,
    onUpdateScreenTimeLimit: (Int) -> Unit = {},
    onUpdateResetIntervalHours: (Int) -> Unit = {},
    onResetScreenTimeNow: () -> Unit = {},
    // Compatibility parameters for legacy calls
    isSharedMode: Boolean = false,
    sharedRemainingSeconds: Long = 0L,
    sharedLimitMinutes: Int = 20,
    appGroups: List<AppGroupEntity> = emptyList(),
    onSaveAppGroup: (AppGroupEntity) -> Unit = {},
    onDeleteAppGroup: (String) -> Unit = {},
    onToggleSharedMode: ((Boolean) -> Unit)? = null,
    onUpdateSharedMinutes: ((Int) -> Unit)? = null
) {
    val accentColor = LocalCustomTextColor.current
    val context = LocalContext.current

    val remainingMinutes = screenTimeRemainingSeconds / 60
    val remainingSeconds = screenTimeRemainingSeconds % 60
    val formattedClock = String.format("%02d:%02d", remainingMinutes, remainingSeconds)

    val activeSelectedApps = remember(limitedApps) {
        limitedApps.filter {
            it.isLimited &&
            !com.example.util.AppFilterHelper.isLockInSelfApp(context, it.packageName, it.appName)
        }
    }
    val activeCount = activeSelectedApps.size

    val isDark = LocalIsDarkTheme.current
    var selectedAppForConfig by remember { mutableStateOf<RestrictedAppEntity?>(null) }
    var blockedAppForUnlock by remember { mutableStateOf<RestrictedAppEntity?>(null) }
    var inProgressAppNotice by remember { mutableStateOf<RestrictedAppEntity?>(null) }
    var showAdjustScreenTimeDialog by remember { mutableStateOf(false) }
    var showAccountSyncDialog by remember { mutableStateOf(false) }
    var showPlayStoreBlacklistDialog by remember { mutableStateOf(false) }

    val blacklistedAppsList by PlayStoreBlacklistManager.blacklistedApps.collectAsStateWithLifecycle()
    val isBlacklistActive by PlayStoreBlacklistManager.isBlacklistEnabled.collectAsStateWithLifecycle()

    val currentAccount by CloudSyncManager.currentAccount.collectAsStateWithLifecycle()

    val streakJustIncreased by StreakManager.streakJustIncreased.collectAsStateWithLifecycle()
    val currentStreak = remember(context, streakJustIncreased) { StreakManager.getStreak(context) }

    val resetIntervalLabel = if (screenTimeResetHours == 24) "1 dia (24h)" else "${screenTimeResetHours} horas"

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            DotMatrixPattern(modifier = Modifier.fillMaxSize(), spacingDp = 22f)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.displayCutout)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(10.dp)) }

                // Streak Celebration Card
                if (streakJustIncreased) {
                    item {
                        NothingCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = accentColor.copy(alpha = 0.15f),
                            borderColor = accentColor
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "🔥", fontSize = 22.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "SEQUÊNCIA SUBIU! +1 DIA",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = accentColor
                                        )
                                    }

                                    IconButton(
                                        onClick = { StreakManager.dismissStreakCelebration() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Fechar",
                                            tint = accentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Parabéns! Você respeitou o limite do seu aparelho ontem! Sua sequência atual é de $currentStreak dias consecutivos.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 15.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { StreakManager.dismissStreakCelebration() },
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                ) {
                                    Text(
                                        text = "CONTINUAR FOCADO",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Top Header: Branding Pill + Stats & Permissions buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LockInBranding(
                            iconSize = 18.dp,
                            textSize = 15.sp,
                            textColor = MaterialTheme.colorScheme.onBackground,
                            iconTint = accentColor
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        val pillBg = if (isDark) Color(0xFF222228) else CmfLightGray
                        val pillBorder = if (isDark) Color(0xFF32323E) else CmfBorder
                        val pillText = if (isDark) Color(0xFFE4E4E7) else CmfDark

                        Row(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Statistics Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(pillBg)
                                    .border(1.dp, pillBorder, RoundedCornerShape(8.dp))
                                    .clickable { onNavigateToStatistics() }
                                    .padding(horizontal = 9.dp, vertical = 6.dp)
                                    .testTag("dashboard_statistics_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.BarChart,
                                        contentDescription = "Estatísticas",
                                        tint = accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "STATS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = pillText,
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Permissions Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(pillBg)
                                    .border(1.dp, pillBorder, RoundedCornerShape(8.dp))
                                    .clickable { onNavigateToPermissions() }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Permissões",
                                        tint = accentColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            // Cloud Account & Sync Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (currentAccount != null) (if (isDark) Color(0xFF1B3B22) else Color(0xFFE8F5E9)) else pillBg)
                                    .border(1.dp, if (currentAccount != null) Color(0xFF81C784) else pillBorder, RoundedCornerShape(8.dp))
                                    .clickable { showAccountSyncDialog = true }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("dashboard_cloud_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (currentAccount != null) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                        contentDescription = "Conta e Nuvem",
                                        tint = if (currentAccount != null) Color(0xFF81C784) else accentColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (currentAccount != null) "NUVEM" else "SYNC",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (currentAccount != null) (if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)) else pillText,
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Bloqueio de Instalação Button (Prominent & easy to access)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(pillBg)
                                    .border(1.dp, pillBorder, RoundedCornerShape(8.dp))
                                    .clickable { showPlayStoreBlacklistDialog = true }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("dashboard_installation_blocker_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = "Bloqueio de Instalação",
                                        tint = accentColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "BLOQUEIO",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = pillText,
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }


                // HERO CARD: TEMPO ABSOLUTO DE TELA DO TELEFONE
                item {
                    val hasLimit = screenTimeLimitMinutes > 0
                    val progress = if (hasLimit) {
                        (screenTimeRemainingSeconds.toFloat() / (screenTimeLimitMinutes * 60f).coerceAtLeast(1f)).coerceIn(0f, 1f)
                    } else 0f

                    val badgeText = when {
                        !hasLimit -> "SEM LIMITE DEFINIDO"
                        isScreenTimeLockout -> "BLOQUEADO"
                        else -> "ATIVO"
                    }

                    val clockText = when {
                        !hasLimit -> "--:--"
                        isScreenTimeLockout -> "00:00"
                        else -> formattedClock
                    }

                    NothingCard(
                        title = "TEMPO DE TELA DO APARELHO",
                        badge = badgeText,
                        onClick = { showAdjustScreenTimeDialog = true }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Countdown Clock Display
                            Text(
                                text = clockText,
                                fontSize = 54.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (isScreenTimeLockout) Color(0xFFD32F2F) else accentColor,
                                letterSpacing = 3.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Clean progress bar
                            LinearProgressIndicator(
                                progress = { if (!hasLimit || isScreenTimeLockout) 0f else progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (isScreenTimeLockout) Color(0xFFD32F2F) else accentColor,
                                trackColor = Color(0xFFEBEBF0)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action pill button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accentColor.copy(alpha = 0.12f))
                                    .clickable { showAdjustScreenTimeDialog = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (hasLimit) {
                                        "LIMITE: ${screenTimeLimitMinutes}m • RESET: ${resetIntervalLabel.uppercase()}"
                                    } else {
                                        "DEFINIR MEU TEMPO DE TELA & RESET"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = accentColor,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = when {
                                    !hasLimit -> "Nenhum limite configurado de fábrica. Toque para definir seu limite de tempo de uso e intervalo de reset."
                                    isScreenTimeLockout -> "Tempo de tela esgotado! O telefone só poderá ser usado após o tempo de reset."
                                    else -> "Se você usar o celular até o tempo acabar, ele só poderá ser usado após o tempo de reset (${resetIntervalLabel}), quando o timer reiniciará."
                                },
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CmfMutedText,
                                textAlign = TextAlign.Center,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // Respected Time Limit Adherence Card (Adapted to app style with pontinho)
                item {
                    val isTodayViolated = StreakManager.isTodayViolated(context)
                    val adherenceScore = if (isTodayViolated || isScreenTimeLockout) "0%" else "100%"
                    val adherenceStatus = if (isTodayViolated || isScreenTimeLockout) "LIMITE EXCEDIDO" else "100% RESPEITADO"
                    val statusColor = if (isTodayViolated || isScreenTimeLockout) Color(0xFFD32F2F) else Color(0xFF00C853)

                    NothingCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = "LIMITE DE TEMPO RESPEITADO",
                        badge = adherenceStatus,
                        onClick = onNavigateToStatistics
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                // Pontinho with halo
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(statusColor.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = adherenceScore,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isDark) Color(0xFFF4F4F5) else CmfDark
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "DE ADERÊNCIA",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = CmfMutedText,
                                            modifier = Modifier.padding(bottom = 3.dp),
                                            softWrap = false
                                        )
                                    }
                                    Text(
                                        text = if (isTodayViolated || isScreenTimeLockout) "Limite quebrado hoje. Toque para ver estatísticas." else "Nenhum limite violado hoje. Disciplina mantida!",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = CmfMutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accentColor.copy(alpha = 0.12f))
                                    .clickable { onNavigateToStatistics() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "VER STATS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = accentColor,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // CARD DE DESTAQUE: BLOQUEIO DE INSTALAÇÃO (PLAY STORE BLACKLIST)
                item {
                    val count = blacklistedAppsList.size
                    NothingCard(
                        title = "BLOQUEIO DE INSTALAÇÃO (PLAY STORE)",
                        badge = if (isBlacklistActive) "PROTEÇÃO ATIVA" else "DESATIVADO",
                        onClick = { showPlayStoreBlacklistDialog = true }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(accentColor.copy(alpha = 0.12f))
                                            .border(1.dp, accentColor.copy(alpha = 0.3f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Block,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Text(
                                            text = if (count == 0) "Nenhum app proibido" else "$count ${if (count == 1) "app bloqueado" else "apps bloqueados"}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Impede instalação e uso via Play Store",
                                            fontSize = 10.sp,
                                            color = CmfMutedText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { showPlayStoreBlacklistDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp).testTag("dashboard_manage_installation_block_button")
                                ) {
                                    Text(
                                        text = "GERENCIAR",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = BlockyFontFamily,
                                        color = Color.White,
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // QUICK ACTIONS (BENTO GRID 2x2)
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Focus Time
                            BentoSmallTile(
                                modifier = Modifier.weight(1f),
                                label = "MODO FOCO TOTAL",
                                value = "LOCK IN",
                                subValue = "Bloqueio rígido imediato",
                                icon = Icons.Default.HourglassBottom,
                                onClick = onNavigateToFocusTime
                            )

                            // Apps
                            BentoSmallTile(
                                modifier = Modifier.weight(1f),
                                label = "APLICATIVOS",
                                value = "${activeCount} APPS",
                                subValue = "Gerenciar lista de apps",
                                icon = Icons.Default.Apps,
                                onClick = onNavigateToApps
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Websites
                            val isVpnRunning by LockInVpnManager.isVpnRunning.collectAsStateWithLifecycle()
                            BentoSmallTile(
                                modifier = Modifier.weight(1f),
                                label = "SITES WEB",
                                value = "${websites.size} SITES",
                                subValue = if (isVpnRunning) "Escudo DNS Ativo" else "Restrições de navegador",
                                icon = Icons.Default.Link,
                                onClick = onNavigateToWebsites
                            )

                            // Settings & Reset
                            BentoSmallTile(
                                modifier = Modifier.weight(1f),
                                label = "CONFIGURAR RESET",
                                value = "${screenTimeResetHours}h",
                                subValue = "Intervalo de reinício",
                                icon = Icons.Default.Schedule,
                                onClick = onNavigateToResetSettings
                            )
                        }
                    }
                }

                // Apps Widgets Carousel
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MEUS APPS MONITORADOS (${activeCount})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isDark) Color(0xFFF4F4F5) else CmfDark,
                                letterSpacing = 0.5.sp
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDark) Color(0xFF24242C) else CmfLightGray)
                                    .clickable { onNavigateToApps() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Gerenciar",
                                        tint = accentColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "GERENCIAR",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isDark) Color(0xFFE4E4E7) else CmfDark
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Toque em um app para ajustar o limite individual ou regras de Shorts.",
                            fontSize = 10.sp,
                            color = CmfMutedText,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (activeSelectedApps.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CmfLightGray.copy(alpha = 0.5f))
                                    .border(1.dp, CmfBorder, RoundedCornerShape(12.dp))
                                    .clickable { onNavigateToApps() }
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Nenhum aplicativo monitorado ainda.",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CmfDark
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Clique aqui para adicionar aplicativos à lista.",
                                        fontSize = 11.sp,
                                        color = accentColor,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        } else {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(activeSelectedApps, key = { it.packageName }) { app ->
                                    AppWidgetCard(
                                        app = app,
                                        accentColor = accentColor,
                                        onClick = {
                                            if (app.isBlocked || app.individualRemainingSeconds <= 0 || app.isLimitDepleted) {
                                                blockedAppForUnlock = app
                                            } else if (app.isUsageInProgress) {
                                                inProgressAppNotice = app
                                            } else {
                                                selectedAppForConfig = app
                                            }
                                        },
                                        modifier = Modifier.width(145.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "LOCK IN • DIGITAL WELLBEING",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CmfMutedText,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Davi Gabriel Vieira Mercês + Google AI Studio",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CmfDark.copy(alpha = 0.7f)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(90.dp)) }
            }
        }

        // App Options Dialog
        selectedAppForConfig?.let { app ->
            AppConfigDialog(
                app = app,
                accentColor = accentColor,
                onDismiss = { selectedAppForConfig = null },
                onSaveApp = { updatedApp ->
                    onUpdateAppEntity(updatedApp)
                },
                onNavigateToAppRoutine = onNavigateToAppRoutine,
                onNavigateToTrustUnlock = { pkg, name ->
                    onNavigateToTrustUnlock?.invoke(pkg, name)
                }
            )
        }

        // Security Unlock Dialog
        blockedAppForUnlock?.let { app ->
            AppSecurityUnlockDialog(
                app = app,
                accentColor = accentColor,
                onDismiss = { blockedAppForUnlock = null },
                onProceedToUnlock = {
                    val toUnlock = app
                    blockedAppForUnlock = null
                    onNavigateToTrustUnlock?.invoke(toUnlock.packageName, toUnlock.appName)
                }
            )
        }

        // In-Progress App Time Modification Barrier Dialog
        inProgressAppNotice?.let { app ->
            AppInProgressBlockedDialog(
                app = app,
                accentColor = accentColor,
                onDismiss = { inProgressAppNotice = null }
            )
        }

        // Dialog: Absolute Screen Time & Reset Time Config
        if (showAdjustScreenTimeDialog) {
            AdjustScreenTimeDialog(
                currentLimitMinutes = screenTimeLimitMinutes,
                currentResetHours = screenTimeResetHours,
                accentColor = accentColor,
                onDismiss = { showAdjustScreenTimeDialog = false },
                onSave = { newMinutes, newResetHours ->
                    onUpdateScreenTimeLimit(newMinutes)
                    onUpdateResetIntervalHours(newResetHours)
                    showAdjustScreenTimeDialog = false
                },
                onResetNow = {
                    onResetScreenTimeNow()
                    showAdjustScreenTimeDialog = false
                }
            )
        }

        if (showAccountSyncDialog) {
            AccountSyncDialog(
                accentColor = accentColor,
                onDismiss = { showAccountSyncDialog = false }
            )
        }

        if (showPlayStoreBlacklistDialog) {
            val userPrefs = remember { com.example.LockInApplication.instance.userPreferences }
            PlayStoreBlacklistDialog(
                accentColor = accentColor,
                trustPassword = userPrefs.getTrustPassword(),
                onDismiss = { showPlayStoreBlacklistDialog = false }
            )
        }
    }
}

@Composable
fun AdjustScreenTimeDialog(
    currentLimitMinutes: Int,
    currentResetHours: Int,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit,
    onResetNow: () -> Unit
) {
    var isEnabled by remember(currentLimitMinutes) { mutableStateOf(currentLimitMinutes > 0) }
    val initialMinutes = if (currentLimitMinutes > 0) currentLimitMinutes else 23
    var tempMinutes by remember(currentLimitMinutes) { mutableIntStateOf(initialMinutes) }
    var tempResetHours by remember(currentResetHours) { mutableIntStateOf(currentResetHours) }
    var minutesInput by remember(currentLimitMinutes) { mutableStateOf(initialMinutes.toString()) }

    val isDark = LocalIsDarkTheme.current
    val dialogBg = if (isDark) Color(0xFF18181B) else CmfWhite
    val cardBg = if (isDark) Color(0xFF27272A) else Color(0xFFF7F7F9)
    val textColor = if (isDark) Color.White else CmfDark
    val pillBg = if (isDark) Color(0xFF3F3F46) else Color(0xFFEBEBF0)
    val pillTextColor = if (isDark) Color.White else CmfDark

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        title = {
            Text(
                text = "TEMPO DE TELA & RESET",
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = textColor
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Section 0: Enable or Disable Absolute Screen Time
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isEnabled) accentColor.copy(alpha = 0.12f) else cardBg)
                        .border(1.dp, if (isEnabled) accentColor.copy(alpha = 0.4f) else (if (isDark) Color(0xFF3F3F46) else CmfBorder), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ATIVAR TEMPO ABSOLUTO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = textColor
                        )
                        Text(
                            text = if (isEnabled) "Limite diário ativo no aparelho." else "Sem limite de tempo absoluto diário.",
                            fontSize = 10.sp,
                            color = CmfMutedText
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
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
                            .background(cardBg)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Você optou por NÃO colocar tempo absoluto no aparelho. O celular não será bloqueado por tempo geral diário. Você ainda pode colocar limites individuais em apps específicos.",
                            fontSize = 11.sp,
                            color = textColor,
                            lineHeight = 15.sp
                        )
                    }
                } else {
                    // Section 1: Screen Time Limit
                Text(
                    text = "1. TEMPO DE TELA DO CELULAR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = accentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tempo total de tela permitido para usar o telefone no dia inteiro (ex: 23 minutos):",
                    fontSize = 11.sp,
                    color = CmfMutedText,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEBEBF0))
                            .clickable {
                                if (tempMinutes > 5) {
                                    val newM = tempMinutes - 5
                                    tempMinutes = newM
                                    minutesInput = newM.toString()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("-5m", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    OutlinedTextField(
                        value = minutesInput,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() }
                            minutesInput = filtered
                            val num = filtered.toIntOrNull()
                            if (num != null && num > 0) {
                                tempMinutes = num
                            }
                        },
                        placeholder = { Text("Ex: 23") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = {
                            Text(
                                text = "min",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = CmfMutedText,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_screen_time_input"),
                        shape = RoundedCornerShape(8.dp),
                        colors = cmfTextFieldColors(accentColor = accentColor)
                    )

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(pillBg)
                            .clickable {
                                val newM = tempMinutes + 5
                                tempMinutes = newM
                                minutesInput = newM.toString()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+5m", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = pillTextColor)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Presets for minutes (including 23m example!)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(15, 23, 30, 45, 60, 120).forEach { mins ->
                        val isSel = tempMinutes == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) accentColor else pillBg)
                                .clickable {
                                    tempMinutes = mins
                                    minutesInput = mins.toString()
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${mins}m",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSel) Color.White else pillTextColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Reset Interval
                Text(
                    text = "2. TEMPO DE RESET (INTERVALO)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = accentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Se você usar até o tempo acabar, o telefone bloqueia e só libera após o reset (ex: 2h, padrão 1 dia):",
                    fontSize = 11.sp,
                    color = CmfMutedText,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Reset options chips: 2h, 4h, 6h, 12h, 24h (1 dia)
                val resetChips = listOf(
                    1 to "1h",
                    2 to "2h",
                    4 to "4h",
                    6 to "6h",
                    12 to "12h",
                    24 to "1 dia"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    resetChips.forEach { (hrs, label) ->
                        val isSel = tempResetHours == hrs
                        Box(
                            modifier = Modifier
                                .weight(if (hrs == 24) 1.4f else 1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) accentColor else pillBg)
                                .clickable { tempResetHours = hrs }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSel) Color.White else pillTextColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Button to reset timer immediately
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(cardBg)
                        .clickable { onResetNow() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REINICIAR SALDO DE TELA AGORA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = textColor
                        )
                    }
                }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(if (isEnabled) tempMinutes else 0, tempResetHours) },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("SALVAR", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = CmfMutedText, fontFamily = FontFamily.Monospace)
            }
        }
    )
}

@Composable
fun AppSecurityUnlockDialog(
    app: RestrictedAppEntity,
    accentColor: Color,
    onDismiss: () -> Unit,
    onProceedToUnlock: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val currentLockLevel by com.example.LockInApplication.instance.userPreferences.lockLevel.collectAsStateWithLifecycle()
    val passwordRepeats = currentLockLevel.passwordRepeatCount
    val maxDailyUnlocks = currentLockLevel.maxDailyUnlocksPerApp
    val isAlreadyUnlockedToday = com.example.service.AppUnlockDailyTracker.isAppUnlockedToday(context, app.packageName)
    val dialogBg = if (isDark) Color(0xFF18181B) else CmfWhite
    val dialogBorder = Color(0xFFEF4444)
    val textColor = if (isDark) Color.White else CmfDark
    val subtextColor = if (isDark) Color(0xFFD1D5DB) else Color(0xFF52525B)

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(dialogBg)
                .border(1.8.dp, dialogBorder, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isAlreadyUnlockedToday) "LIMITE DIÁRIO ATINGIDO ($maxDailyUnlocks/$maxDailyUnlocks)" else "TEMPO ESGOTADO • BLOQUEIO RÍGIDO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFEF4444)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                AppIconImage(
                    packageName = app.packageName,
                    fallbackLetter = app.appName.firstOrNull()?.uppercase() ?: "A",
                    sizeDp = 54.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = app.appName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor
                )

                Text(
                    text = "00:00 restante hoje",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFEF4444)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) Color(0xFF241517) else Color(0xFFFEF2F2), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (isAlreadyUnlockedToday) {
                            "Você já atingiu o limite de $maxDailyUnlocks desbloqueio(s) deste aplicativo hoje, conforme o nível de Lock escolhido.\n\nPara preservar seu foco e meta diária, novos desbloqueios deste app só serão permitidos após o reset à meia-noite."
                        } else {
                            "Este aplicativo atingiu o limite de uso diário programado e foi totalmente bloqueado. Para poder reajustar o limite ou usá-lo novamente hoje, conclua o Desafio Rigoroso digitando sua senha $passwordRepeats vez${if (passwordRepeats > 1) "es consecutivas" else ""} e confirmando a frase de compromisso."
                        },
                        fontSize = 11.sp,
                        color = subtextColor,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (isAlreadyUnlockedToday) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF71717A),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "FECHAR (AGUARDAR MEIA-NOITE)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    Button(
                        onClick = onProceedToUnlock,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("security_unlock_action_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Desbloqueio",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DESBLOQUEAR (${passwordRepeats}X SENHA + FRASE)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Voltar ao Foco",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = subtextColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppInProgressBlockedDialog(
    app: RestrictedAppEntity,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val dialogBg = if (isDark) Color(0xFF18181B) else CmfWhite
    val dialogBorder = Color(0xFFF59E0B)
    val textColor = if (isDark) Color.White else CmfDark

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(dialogBg)
                .border(1.8.dp, dialogBorder, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "LIMITE EM ANDAMENTO • MODIFICAÇÃO BLOQUEADA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFD97706)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                AppIconImage(
                    packageName = app.packageName,
                    fallbackLetter = app.appName.firstOrNull()?.uppercase() ?: "A",
                    sizeDp = 54.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = app.appName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor
                )

                val remMin = app.totalAvailableSeconds / 60
                val remSec = app.totalAvailableSeconds % 60
                Text(
                    text = String.format("%02d:%02d restantes hoje", remMin, remSec),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFD97706)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) Color(0xFF261D0C) else Color(0xFFFEF3C7), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Você já utilizou este aplicativo hoje e ainda possui tempo restante disponível.\n\nPara manter a disciplina e evitar burlar suas metas de tempo, o Lock In não permite alterar o limite de tempo enquanto o timer estiver em andamento.\n\nAguarde o tempo esgotar (chegar a zero) para poder redefini-lo.",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E),
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "ENTENDIDO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun BentoSmallTile(
    label: String,
    value: String,
    subValue: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalCustomTextColor.current

    val isDark = LocalIsDarkTheme.current
    val tileBg = if (isDark) Color(0xFF18181C) else CmfWhite
    val tileBorder = if (isDark) Color(0xFF2C2C36) else CmfBorder
    val primaryText = if (isDark) Color(0xFFF4F4F5) else CmfDark
    val mutedText = if (isDark) Color(0xFFA1A1AA) else CmfMutedText

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(tileBg)
            .border(1.2.dp, tileBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = mutedText,
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = primaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subValue,
                fontSize = 10.sp,
                color = mutedText,
                maxLines = 1
            )
        }
    }
}
