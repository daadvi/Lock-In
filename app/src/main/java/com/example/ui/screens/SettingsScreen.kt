package com.example.ui.screens

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Language
import com.example.service.DeviceAdminManager
import com.example.service.CloudSyncManager
import com.example.UninstallProtectionActivity
import com.example.ui.components.PlayStoreBlacklistDialog
import com.example.ui.components.WebsiteBlacklistDialog
import com.example.ui.components.VpnShieldDialog
import com.example.ui.components.ChangeLockLevelDialog
import com.example.ui.components.AbsoluteInstallBlockDialog
import com.example.data.model.LockLevel
import com.example.service.LockInVpnManager
import com.example.ui.components.DeviceInstructionsCard
import com.example.util.DeviceInstructionHelper
import com.example.ui.components.AccountSyncDialog
import com.example.ui.components.PasswordRequirementChip
import com.example.util.PasswordValidator
import android.content.Intent
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import com.example.ui.components.XiaomiCompatibilityCard
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.DotMatrixPattern
import com.example.ui.components.NothingCard
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import com.example.ui.theme.LocalCustomTextColor
import com.example.ui.theme.LocalIsDarkTheme

@Composable
fun SettingsScreen(
    currentThemeMode: String,
    hapticFeedbackEnabled: Boolean,
    streakRemindersEnabled: Boolean,
    preLimitAlertsEnabled: Boolean,
    mindfulDelayEnabled: Boolean,
    strictModeEnabled: Boolean,
    userName: String = "Focado",
    currentPassword: () -> String,
    onSaveThemeMode: (String) -> Unit,
    onSaveHapticFeedback: (Boolean) -> Unit,
    onSaveStreakReminders: (Boolean) -> Unit,
    onSavePreLimitAlerts: (Boolean) -> Unit,
    onSaveMindfulDelay: (Boolean) -> Unit,
    onSaveStrictMode: (Boolean) -> Unit,
    onUpdateUserName: (String) -> Unit = {},
    onUpdatePassword: (String) -> Unit,
    onResetToDefaults: () -> Unit,
    onNavigateToStyling: () -> Unit,
    onNavigateToResetSettings: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onBack: () -> Unit
) {
    val accentColor = LocalCustomTextColor.current
    val isDarkTheme = LocalIsDarkTheme.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    var showNameDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showResetDefaultsDialog by remember { mutableStateOf(false) }
    var showPlayStoreBlacklistDialog by remember { mutableStateOf(false) }
    var showWebsiteBlacklistDialog by remember { mutableStateOf(false) }
    var showVpnShieldDialog by remember { mutableStateOf(false) }
    var showDeviceAdminDialog by remember { mutableStateOf(false) }
    var showAccountSyncDialog by remember { mutableStateOf(false) }
    var showXiaomiDialog by remember { mutableStateOf(false) }
    var showBankingSecurityDialog by remember { mutableStateOf(false) }
    var showChangeLockLevelDialog by remember { mutableStateOf(false) }
    var showAbsoluteBlockDialog by remember { mutableStateOf(false) }
    var pendingAbsoluteBlockEnable by remember { mutableStateOf(false) }
    var showJuanPabloDialog by remember { mutableStateOf(false) }

    val userPrefs = remember { com.example.LockInApplication.instance.userPreferences }
    val currentLockLevel by userPrefs.lockLevel.collectAsState()
    val isAbsoluteInstallBlock by userPrefs.isAbsoluteInstallBlock.collectAsState()

    val currentAccount by CloudSyncManager.currentAccount.collectAsState()

    fun triggerHaptic() {
        if (hapticFeedbackEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            try {
                val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(25)
                }
            } catch (_: Exception) {}
        }
    }

    val screenBg = if (isDarkTheme) Color(0xFF101012) else CmfWhite
    val cardBg = if (isDarkTheme) Color(0xFF18181B) else CmfWhite
    val textColor = if (isDarkTheme) Color.White else CmfDark

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
                    .padding(horizontal = 20.dp)
            ) {
                // Top Header Bar
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
                    Text(
                        text = "CONFIGURAÇÕES",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = textColor,
                        letterSpacing = 1.sp
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. HERO CARD INSPIRED BY THE PHOTO (3 Pixel Glyphs: Theme Contrast, Waves, Bell)
                    item {
                        PixelSettingsHeroCard(
                            accentColor = accentColor
                        )
                    }

                    // 2. THEME MODE SEGMENTED CONTROL (Sun, Split/Auto with accent underline, Moon)
                    item {
                        ThemeSegmentedController(
                            selectedMode = currentThemeMode,
                            accentColor = accentColor,
                            onSelectMode = { mode ->
                                triggerHaptic()
                                onSaveThemeMode(mode)
                            }
                        )
                    }

                    // 3. SENSORIAL & NOTIFICAÇÕES (Cards with switches matching the photo)
                    item {
                        SettingsToggleCard(
                            title = "Haptic Feedback",
                            description = "Enable a slight vibration during interactions with the interface and timer events",
                            checked = hapticFeedbackEnabled,
                            accentColor = accentColor,
                            onCheckedChange = { checked ->
                                triggerHaptic()
                                onSaveHapticFeedback(checked)
                            },
                            testTag = "settings_toggle_haptic"
                        )
                    }

                    item {
                        SettingsToggleCard(
                            title = "Streak reminders",
                            description = "An evening nudge if you haven't done at least one minute of session today",
                            checked = streakRemindersEnabled,
                            accentColor = accentColor,
                            onCheckedChange = { checked ->
                                triggerHaptic()
                                onSaveStreakReminders(checked)
                            },
                            testTag = "settings_toggle_streak"
                        )
                    }

                    item {
                        SettingsToggleCard(
                            title = "Alerta Pré-Limite (5 & 2 min)",
                            description = "Notificação suave e pulso háptico quando seu tempo restante estiver quase esgotado",
                            checked = preLimitAlertsEnabled,
                            accentColor = accentColor,
                            onCheckedChange = { checked ->
                                triggerHaptic()
                                onSavePreLimitAlerts(checked)
                            },
                            testTag = "settings_toggle_pre_limit"
                        )
                    }

                    // 4. DISCIPLINA & CONTROLE MENTAL
                    item {
                        SettingsToggleCard(
                            title = "Pausa Consciente (Mindful Delay)",
                            description = "Exigir espera de 30 segundos com contagem de respiração antes de permitir desbloquear",
                            checked = mindfulDelayEnabled,
                            accentColor = accentColor,
                            onCheckedChange = { checked ->
                                triggerHaptic()
                                onSaveMindfulDelay(checked)
                            },
                            testTag = "settings_toggle_mindful"
                        )
                    }

                    item {
                        SettingsToggleCard(
                            title = "Modo Estrito (Strict Mode)",
                            description = "Impede alterar cotas ou desligar restrições enquanto um limite estiver em andamento",
                            checked = strictModeEnabled,
                            accentColor = accentColor,
                            onCheckedChange = { checked ->
                                triggerHaptic()
                                onSaveStrictMode(checked)
                            },
                            testTag = "settings_toggle_strict"
                        )
                    }

                    // 5. ATALHOS RÁPIDOS DE CONFIGURAÇÃO DO SISTEMA
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "AJUSTES AVANÇADOS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CmfMutedText,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                            )

                            SettingsNavigationRow(
                                title = "Nome do Usuário",
                                subtitle = if (userName.isBlank()) "Defina seu nome de foco" else userName,
                                icon = Icons.Default.Person,
                                accentColor = accentColor,
                                onClick = { showNameDialog = true }
                            )

                            SettingsNavigationRow(
                                title = "Estilização da Ilha & Cores",
                                subtitle = "Posição, opacidade, tamanho da cápsula e cor de acento",
                                icon = Icons.Default.ColorLens,
                                accentColor = accentColor,
                                onClick = onNavigateToStyling
                            )

                            SettingsNavigationRow(
                                title = "Tempo de Reset do Telefone",
                                subtitle = "Defina o tempo para desbloquear e reiniciar após o limite esgotar (ex: 2h, padrão 1 dia)",
                                icon = Icons.Default.Sync,
                                accentColor = accentColor,
                                onClick = onNavigateToResetSettings
                            )

                            SettingsNavigationRow(
                                title = "Central de Permissões",
                                subtitle = "Verifique Sobreposição, Dados de Uso e Acessibilidade",
                                icon = Icons.Default.Security,
                                accentColor = accentColor,
                                onClick = onNavigateToPermissions
                            )

                            val isBankingPaused by com.example.service.BankingSecurityHelper.isPausedFlow.collectAsState()
                            SettingsNavigationRow(
                                title = "Proteção & Apps Bancários",
                                subtitle = if (isBankingPaused) "Pausa Ativa (Acesso livre para bancos)" else "Isenção Automática Ativa (100% Seguro para Bancos)",
                                icon = Icons.Default.Shield,
                                accentColor = if (isBankingPaused) Color(0xFFF59E0B) else Color(0xFF22C55E),
                                onClick = { showBankingSecurityDialog = true }
                            )

                            SettingsNavigationRow(
                                title = "Conta & Sincronização em Nuvem",
                                subtitle = if (currentAccount != null) "${currentAccount!!.email} • Backup ativo" else "Conecte Google, Apple, Hotmail, Outlook, Yahoo...",
                                icon = if (currentAccount != null) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                accentColor = if (currentAccount != null) Color(0xFF2E7D32) else accentColor,
                                onClick = { showAccountSyncDialog = true }
                            )

                            SettingsNavigationRow(
                                title = "Senha de Confiança",
                                subtitle = "Altere o código de segurança (Mín. 8, 1 maiúscula, 1 símbolo)",
                                icon = Icons.Default.Lock,
                                accentColor = accentColor,
                                onClick = { showPasswordDialog = true }
                            )

                            SettingsNavigationRow(
                                title = "Nível de Lock (Bloqueio)",
                                subtitle = "Nível atual: ${currentLockLevel.title} • Regras e limites diários de desbloqueio",
                                icon = Icons.Default.Security,
                                accentColor = accentColor,
                                onClick = { showChangeLockLevelDialog = true }
                            )

                            SettingsNavigationRow(
                                title = "Bloqueio de Instalação Absoluta",
                                subtitle = if (isAbsoluteInstallBlock) "ATIVO • Remoção proibida (Desativação exige 5 senhas, frase e token de e-mail)" else "Desativado • Ative para blindar e impedir remoção de apps da lista",
                                icon = Icons.Default.Shield,
                                accentColor = if (isAbsoluteInstallBlock) Color(0xFFEF4444) else accentColor,
                                onClick = {
                                    pendingAbsoluteBlockEnable = !isAbsoluteInstallBlock
                                    showAbsoluteBlockDialog = true
                                }
                            )

                            SettingsNavigationRow(
                                title = "Blacklist da Play Store",
                                subtitle = "Bloqueie a instalação de novos aplicativos na Play Store",
                                icon = Icons.Default.Block,
                                accentColor = accentColor,
                                onClick = { showPlayStoreBlacklistDialog = true }
                            )

                            SettingsNavigationRow(
                                title = "Bloqueio de Sites (Blacklist Web)",
                                subtitle = "Impeça o acesso a sites e domínios em navegadores (Chrome, Samsung...)",
                                icon = Icons.Default.Language,
                                accentColor = accentColor,
                                onClick = { showWebsiteBlacklistDialog = true }
                            )

                            val isVpnRunning by LockInVpnManager.isVpnRunning.collectAsState()
                            val vpnBlockedCount by LockInVpnManager.blockedDnsCount.collectAsState()
                            SettingsNavigationRow(
                                title = "Escudo de Rede & Firewall Local",
                                subtitle = if (isVpnRunning) "Ativo • $vpnBlockedCount bloqueios hoje • 100% no aparelho (Bancos Isentos)" else "Desativado • Filtro DNS nativo e corte de rede para apps esgotados",
                                icon = Icons.Default.Shield,
                                accentColor = if (isVpnRunning) Color(0xFF22C55E) else accentColor,
                                onClick = { showVpnShieldDialog = true }
                            )

                            val isDeviceAdminActive = DeviceAdminManager.isDeviceAdminActive(context)
                            SettingsNavigationRow(
                                title = "Proteção Contra Desinstalação",
                                subtitle = if (isDeviceAdminActive) "Administrador Ativo (Exige 5 senhas + frase)" else "Toque para blindar o app contra desinstalação",
                                icon = Icons.Default.Shield,
                                accentColor = if (isDeviceAdminActive) Color(0xFF2E7D32) else accentColor,
                                onClick = {
                                    if (!isDeviceAdminActive) {
                                        DeviceAdminManager.requestDeviceAdmin(context)
                                    } else {
                                        showDeviceAdminDialog = true
                                    }
                                }
                            )

                            val detectedProfile = remember { DeviceInstructionHelper.getProfile() }
                            SettingsNavigationRow(
                                title = "Como Manter o App Ativo & Início Automático",
                                subtitle = "${detectedProfile.family.displayName} • Travar na aba de recentes e auto start",
                                icon = Icons.Default.PhoneAndroid,
                                accentColor = Color(0xFFFF6900),
                                onClick = { showXiaomiDialog = true }
                            )
                        }
                    }

                    // 6. ÁREA DE CRÉDITOS
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDarkTheme) Color(0xFF1E1E22) else Color(0xFFF3F3F5))
                                .border(1.dp, if (isDarkTheme) Color(0xFF2A2A30) else CmfBorder, RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(accentColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "★",
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ÁREA DE CRÉDITOS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = accentColor,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // 1- Davi Gabriel - DEV principal do Lock In
                                CreditItemRow(
                                    number = "1",
                                    name = "Davi Gabriel",
                                    role = "DEV principal do Lock In",
                                    isDark = isDarkTheme,
                                    textColor = textColor
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // 2- Google AI studio - Principal meio de desenvolvimento
                                CreditItemRow(
                                    number = "2",
                                    name = "Google AI studio",
                                    role = "Principal meio de desenvolvimento",
                                    isDark = isDarkTheme,
                                    textColor = textColor
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // 3- Juan Pablo - Principal Tester e System Breaker.
                                CreditItemRow(
                                    number = "3",
                                    name = "Juan Pablo",
                                    role = "Principal Tester e System Breaker.",
                                    isDark = isDarkTheme,
                                    textColor = textColor,
                                    hasInfoIcon = true,
                                    onClick = { showJuanPabloDialog = true }
                                )
                            }
                        }
                    }

                    // 7. INFO CARD AT THE BOTTOM
                    item {
                        SettingsBottomSplitCard(
                            modifier = Modifier.fillMaxWidth(),
                            title = "Info",
                            subtitle = "Informações e créditos do app",
                            accentColor = accentColor,
                            onClick = { showInfoDialog = true },
                            testTag = "settings_info_card"
                        )
                    }


                    // 7. RESTORE DEFAULTS
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDarkTheme) Color(0xFF1E1E22) else Color(0xFFF3F3F5))
                                .border(1.dp, if (isDarkTheme) Color(0xFF2A2A30) else CmfBorder, RoundedCornerShape(12.dp))
                                .clickable { showResetDefaultsDialog = true }
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.RestartAlt,
                                        contentDescription = null,
                                        tint = CmfMutedText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Restaurar Padrões de Fábrica",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = textColor
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = CmfMutedText,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(36.dp)) }
                }
            }
        }
    }

    // DIALOGS
    if (showNameDialog) {
        ChangeNameDialog(
            currentName = userName,
            accentColor = accentColor,
            onSaveName = { newName ->
                onUpdateUserName(newName)
                showNameDialog = false
            },
            onDismiss = { showNameDialog = false }
        )
    }

    if (showInfoDialog) {
        InfoCreditsDialog(
            accentColor = accentColor,
            onDismiss = { showInfoDialog = false },
            onShowJuanPabloInfo = {
                showJuanPabloDialog = true
            }
        )
    }

    if (showJuanPabloDialog) {
        JuanPabloSpecialThanksDialog(
            accentColor = accentColor,
            onDismiss = { showJuanPabloDialog = false }
        )
    }

    if (showBankingSecurityDialog) {
        BankingSecurityDialog(
            accentColor = accentColor,
            onDismiss = { showBankingSecurityDialog = false }
        )
    }

    if (showXiaomiDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showXiaomiDialog = false }) {
            DeviceInstructionsCard(
                accentColor = accentColor,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            currentPassword = currentPassword(),
            accentColor = accentColor,
            onSaveNewPassword = { newPass ->
                onUpdatePassword(newPass)
                showPasswordDialog = false
            },
            onDismiss = { showPasswordDialog = false }
        )
    }

    if (showResetDefaultsDialog) {
        ResetDefaultsConfirmDialog(
            accentColor = accentColor,
            onConfirm = {
                onResetToDefaults()
                showResetDefaultsDialog = false
            },
            onDismiss = { showResetDefaultsDialog = false }
        )
    }

    if (showChangeLockLevelDialog) {
        ChangeLockLevelDialog(
            currentLevel = currentLockLevel,
            onDismiss = { showChangeLockLevelDialog = false },
            onLevelChanged = { newLevel ->
                userPrefs.setLockLevel(newLevel)
                showChangeLockLevelDialog = false
            }
        )
    }

    if (showAbsoluteBlockDialog) {
        AbsoluteInstallBlockDialog(
            isEnabling = pendingAbsoluteBlockEnable,
            onDismiss = { showAbsoluteBlockDialog = false },
            onStateChanged = { newState ->
                userPrefs.setAbsoluteInstallBlock(newState)
                showAbsoluteBlockDialog = false
            }
        )
    }

    if (showPlayStoreBlacklistDialog) {
        PlayStoreBlacklistDialog(
            accentColor = accentColor,
            trustPassword = currentPassword(),
            onDismiss = { showPlayStoreBlacklistDialog = false }
        )
    }

    if (showWebsiteBlacklistDialog) {
        WebsiteBlacklistDialog(
            accentColor = accentColor,
            trustPassword = currentPassword(),
            onOpenVpnShield = {
                showWebsiteBlacklistDialog = false
                showVpnShieldDialog = true
            },
            onDismiss = { showWebsiteBlacklistDialog = false }
        )
    }

    if (showVpnShieldDialog) {
        VpnShieldDialog(
            accentColor = accentColor,
            onManageWebsites = {
                showVpnShieldDialog = false
                showWebsiteBlacklistDialog = true
            },
            onDismiss = { showVpnShieldDialog = false }
        )
    }

    if (showDeviceAdminDialog) {
        DeviceAdminStatusDialog(
            accentColor = accentColor,
            onDismiss = { showDeviceAdminDialog = false },
            onStartUninstallProtocol = {
                showDeviceAdminDialog = false
                val intent = Intent(context, UninstallProtectionActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        )
    }

    if (showAccountSyncDialog) {
        AccountSyncDialog(
            accentColor = accentColor,
            onDismiss = { showAccountSyncDialog = false }
        )
    }
}

// -------------------------------------------------------------------------
// HERO CARD WITH 3 PIXEL ART GLYPHS (Moon/Split Circle, Waves, Bell)
// -------------------------------------------------------------------------
@Composable
private fun PixelSettingsHeroCard(
    accentColor: Color
) {
    val isDark = com.example.ui.theme.LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF1E1E22) else Color(0xFFF4F4F6)
    val cardBorder = if (isDark) Color(0xFF2C2C32) else Color(0xFFE2E2E6)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
            .padding(vertical = 24.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Pixel Art Split Moon / Contrast Glyph
            PixelMoonGlyph(accentColor = accentColor, modifier = Modifier.size(56.dp, 64.dp))

            // 2. Pixel Art Wavy Vibration Waves
            PixelWavesGlyph(accentColor = accentColor, modifier = Modifier.size(44.dp, 64.dp))

            // 3. Pixel Art Notification Bell
            PixelBellGlyph(accentColor = accentColor, modifier = Modifier.size(52.dp, 64.dp))
        }
    }
}

@Composable
private fun PixelMoonGlyph(accentColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val pixelSize = size.width / 9f
        // 9 cols x 13 rows matrix
        // Left arc outline, right filled
        val matrix = arrayOf(
            "   XXX   ",
            "  XOOOO  ",
            " XOOOOOO ",
            "XOOOOOOO ",
            "XOOOOOOO ",
            "XOOOOOOO ",
            "XOOOOOOO ",
            "XOOOOOOO ",
            "XOOOOOOO ",
            "XOOOOOOO ",
            " XOOOOOO ",
            "  XOOOO  ",
            "   XXX   "
        )
        for (r in matrix.indices) {
            val rowStr = matrix[r]
            for (c in rowStr.indices) {
                val char = rowStr[c]
                if (char == 'X' || char == 'O') {
                    drawRect(
                        color = accentColor,
                        topLeft = Offset(c * pixelSize, r * pixelSize),
                        size = Size(pixelSize - 0.5f, pixelSize - 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PixelWavesGlyph(accentColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val pixelSize = size.width / 8f
        // Two wavy lines side by side
        val matrix = arrayOf(
            " X   X  ",
            "  X   X ",
            "   X   X",
            "  X   X ",
            " X   X  ",
            "X   X   ",
            " X   X  ",
            "  X   X ",
            "   X   X",
            "  X   X ",
            " X   X  ",
            "X   X   ",
            " X   X  "
        )
        for (r in matrix.indices) {
            val rowStr = matrix[r]
            for (c in rowStr.indices) {
                if (rowStr[c] == 'X') {
                    drawRect(
                        color = accentColor,
                        topLeft = Offset(c * pixelSize, r * pixelSize),
                        size = Size(pixelSize - 0.5f, pixelSize - 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PixelBellGlyph(accentColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val pixelSize = size.width / 11f
        val matrix = arrayOf(
            "    XX    ",
            "    XX    ",
            "   XXXX   ",
            "  X    X  ",
            "  X    X  ",
            "  X    X  ",
            " X      X ",
            " X      X ",
            "XXXXXXXXXX",
            "    XX    ",
            "   XXXX   "
        )
        for (r in matrix.indices) {
            val rowStr = matrix[r]
            for (c in rowStr.indices) {
                if (rowStr[c] == 'X') {
                    drawRect(
                        color = accentColor,
                        topLeft = Offset(c * pixelSize, r * pixelSize),
                        size = Size(pixelSize - 0.5f, pixelSize - 0.5f)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// THEME SEGMENTED CONTROLLER (Sun, Split/Auto, Moon)
// -------------------------------------------------------------------------
@Composable
private fun ThemeSegmentedController(
    selectedMode: String,
    accentColor: Color,
    onSelectMode: (String) -> Unit
) {
    val isDark = com.example.ui.theme.LocalIsDarkTheme.current
    val barBg = if (isDark) Color(0xFF1E1E22) else Color(0xFFF4F4F6)
    val barBorder = if (isDark) Color(0xFF2C2C32) else Color(0xFFE2E2E6)
    val selectedBg = if (isDark) Color(0xFF2B2B32) else Color(0xFFFFFFFF)
    val selectedBorder = if (isDark) Color(0xFF3E3E48) else Color(0xFFE2E2E6)
    val selectedIconTint = if (isDark) Color.White else CmfDark
    val unselectedIconTint = if (isDark) Color(0xFF8E8E98) else Color(0xFF71717A)
    val unselectedTextTint = if (isDark) Color(0xFF8E8E98) else Color(0xFF71717A)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(barBg)
            .border(1.dp, barBorder, RoundedCornerShape(16.dp))
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                Triple("LIGHT", Icons.Default.LightMode, "Claro"),
                Triple("SYSTEM", Icons.Default.HourglassBottom, "Auto"),
                Triple("DARK", Icons.Default.DarkMode, "Escuro")
            ).forEach { (mode, icon, label) ->
                val isSelected = selectedMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) selectedBg else Color.Transparent)
                        .then(if (isSelected) Modifier.border(1.dp, selectedBorder, RoundedCornerShape(12.dp)) else Modifier)
                        .clickable { onSelectMode(mode) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) selectedIconTint else unselectedIconTint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(2.dp)
                                    .background(accentColor, CircleShape)
                            )
                        } else {
                            Text(
                                text = label.uppercase(),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = unselectedTextTint
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// SETTINGS TOGGLE CARD (with Nothing OS style Switch + "0" indicator)
// -------------------------------------------------------------------------
@Composable
private fun SettingsToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF18181B) else CmfWhite
    val cardBorder = if (isDark) Color(0xFF27272A) else CmfBorder
    val titleColor = if (isDark) Color.White else CmfDark

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = titleColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = CmfMutedText,
                    lineHeight = 15.sp
                )
            }

            // Custom Nothing OS Switch with '0' / 'I' binary dot below
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                NothingSwitch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    accentColor = accentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (checked) "I" else "0",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = if (checked) accentColor else CmfMutedText
                )
            }
        }
    }
}

@Composable
fun NothingSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 18.dp else 2.dp,
        animationSpec = spring(stiffness = 500f),
        label = "switch_thumb"
    )

    Box(
        modifier = modifier
            .width(42.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) Color(0xFF221A16) else Color(0xFF27272A))
            .border(
                1.dp,
                if (checked) accentColor.copy(alpha = 0.6f) else Color(0xFF3F3F46),
                RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (checked) accentColor else Color(0xFF71717A))
        )
    }
}

// -------------------------------------------------------------------------
// NAVIGATION ROW (Item de navegação rápida)
// -------------------------------------------------------------------------
@Composable
private fun SettingsNavigationRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF18181B) else CmfWhite
    val cardBorder = if (isDark) Color(0xFF27272A) else CmfBorder
    val titleColor = if (isDark) Color.White else CmfDark

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF222226) else Color(0xFFF3F3F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = titleColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = CmfMutedText,
                        lineHeight = 13.sp
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = CmfMutedText,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

// -------------------------------------------------------------------------
// TWO BOTTOM SPLIT CARDS (Matching the Photo: "You are a Premium User" & "Info")
// -------------------------------------------------------------------------
@Composable
private fun SettingsBottomSplitCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF18181B) else CmfWhite
    val cardBorder = if (isDark) Color(0xFF27272A) else CmfBorder
    val titleColor = if (isDark) Color.White else CmfDark

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = titleColor,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = CmfMutedText,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = CmfMutedText,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// DIALOG: ALTERAR NOME DO USUÁRIO
// -------------------------------------------------------------------------
@Composable
private fun ChangeNameDialog(
    currentName: String,
    accentColor: Color,
    onSaveName: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameText by remember { mutableStateOf(currentName) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val isDark = LocalIsDarkTheme.current
    val dialogBg = if (isDark) Color(0xFF18181B) else Color(0xFFFFFFFF)
    val dialogBorder = if (isDark) Color(0xFF2E2E34) else Color(0xFFE2E2E6)
    val titleColor = if (isDark) Color.White else Color(0xFF18181B)
    val inputBg = if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F6)

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape(18.dp))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "SEU NOME",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = titleColor
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Personalize como você é identificado no app e nas notificações de foco.",
                    fontSize = 11.sp,
                    color = CmfMutedText,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nameText,
                    onValueChange = {
                        nameText = it
                        errorText = null
                    },
                    label = { Text("Nome de Exibição", color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = titleColor,
                        unfocusedTextColor = titleColor,
                        cursorColor = accentColor,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = if (isDark) Color(0xFF3F3F46) else Color(0xFFD4D4D8),
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A),
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg
                    )
                )

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorText ?: "", fontSize = 10.sp, color = Color(0xFFFF4400))
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("CANCELAR", fontSize = 11.sp, color = if (isDark) Color.Gray else Color(0xFF71717A), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val trimmed = nameText.trim()
                            if (trimmed.isEmpty()) {
                                errorText = "O nome não pode ficar em branco."
                                return@Button
                            }
                            onSaveName(trimmed)
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("SALVAR", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// DIALOG: INFO & CREDITS
// -------------------------------------------------------------------------
@Composable
private fun InfoCreditsDialog(
    accentColor: Color,
    onDismiss: () -> Unit,
    onShowJuanPabloInfo: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val dialogBg = if (isDark) Color(0xFF18181B) else Color(0xFFFFFFFF)
    val dialogBorder = if (isDark) Color(0xFF2E2E34) else Color(0xFFE2E2E6)
    val titleColor = if (isDark) Color.White else Color(0xFF18181B)
    val bodyColor = if (isDark) Color(0xFFC7C7CF) else Color(0xFF4A4A52)
    val creditBoxBg = if (isDark) Color(0xFF222226) else Color(0xFFF4F4F6)

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape(18.dp))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF2B2B30) else Color(0xFFE4E4E7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isDark) Color.White else Color(0xFF18181B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "LOCK IN • DIGITAL WELLBEING",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = titleColor
                        )
                        Text(
                            text = "Versão 2.4.0 (CMF Nothing OS Design)",
                            fontSize = 10.sp,
                            color = CmfMutedText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Criado com arquitetura 100% nativa em Jetpack Compose, Kotlin e Room Database. Projetado sob a premissa de que sua atenção é o seu recurso mais valioso.",
                    fontSize = 11.sp,
                    color = bodyColor,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(creditBoxBg)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "ÁREA DE CRÉDITOS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = accentColor,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        CreditItemRow(
                            number = "1",
                            name = "Davi Gabriel",
                            role = "DEV principal do Lock In",
                            isDark = isDark,
                            textColor = titleColor
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        CreditItemRow(
                            number = "2",
                            name = "Google AI studio",
                            role = "Principal meio de desenvolvimento",
                            isDark = isDark,
                            textColor = titleColor
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        CreditItemRow(
                            number = "3",
                            name = "Juan Pablo",
                            role = "Principal Tester e System Breaker.",
                            isDark = isDark,
                            textColor = titleColor,
                            hasInfoIcon = true,
                            onClick = onShowJuanPabloInfo
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF2C2C32) else Color(0xFFE4E4E7)
                    )
                ) {
                    Text(
                        text = "FECHAR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) Color.White else Color(0xFF18181B)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// DIALOG: ALTERAR SENHA DE CONFIANÇA
// -------------------------------------------------------------------------
// -------------------------------------------------------------------------
// DIALOG: ALTERAR SENHA DE CONFIANÇA (COM RECUPERAÇÃO VIA E-MAIL)
// -------------------------------------------------------------------------
@Composable
private fun ChangePasswordDialog(
    currentPassword: String,
    accentColor: Color,
    onSaveNewPassword: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val account by CloudSyncManager.currentAccount.collectAsState()
    val registeredEmail = account?.email ?: "usuario@lockin.app"

    var isForgotMode by remember { mutableStateOf(false) }
    var useEmergencyPhrase by remember { mutableStateOf(false) }

    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // Email 6-digit code recovery states
    var generatedCode by remember { mutableStateOf("") }
    var codeInput by remember { mutableStateOf("") }
    var isCodeVerified by remember { mutableStateOf(false) }
    var resendCooldown by remember { mutableStateOf(0) }

    // Emergency phrase
    val emergencyPhraseTarget = "Estou seguro que quero redefinir minha senha"
    var emergencyPhraseInput by remember { mutableStateOf("") }
    var isPhraseVerified by remember { mutableStateOf(false) }

    fun sendVerificationCode() {
        val code = String.format("%06d", (100000..999999).random())
        generatedCode = code
        resendCooldown = 30
        android.widget.Toast.makeText(
            context,
            "Código de verificação enviado para $registeredEmail: $code",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            kotlinx.coroutines.delay(1000L)
            resendCooldown -= 1
        }
    }

    val isNewPassValid = PasswordValidator.isValid(newPass)
    val isAuthorizedToSave = if (isForgotMode) {
        if (useEmergencyPhrase) isPhraseVerified else isCodeVerified
    } else {
        oldPass == currentPassword && oldPass.isNotEmpty()
    }

    val dialogBg = if (isDark) Color(0xFF18181B) else CmfWhite
    val dialogBorder = if (isDark) Color(0xFF2E2E34) else CmfBorder
    val textColor = if (isDark) Color.White else CmfDark
    val subtextColor = if (isDark) Color(0xFFD1D5DB) else Color(0xFF52525B)

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(dialogBg)
                .border(1.2.dp, dialogBorder, RoundedCornerShape(18.dp))
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isForgotMode) "RECUPERAR SENHA" else "ALTERAR SENHA DE SEGURANÇA",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = textColor
                    )

                    if (isForgotMode) {
                        TextButton(
                            onClick = {
                                isForgotMode = false
                                errorText = null
                            }
                        ) {
                            Text("Voltar", fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!isForgotMode) {
                    // MODO NORMAL: Digitar Senha Atual
                    Text("Senha Atual", fontSize = 10.sp, color = subtextColor, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = oldPass,
                        onValueChange = {
                            oldPass = it
                            errorText = null
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = if (isDark) Color(0xFF3F3F46) else Color(0xFFD1D5DB)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                isForgotMode = true
                                errorText = null
                                sendVerificationCode()
                            }
                        ) {
                            Text(
                                text = "Esqueci minha senha",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } else {
                    // MODO ESQUECI MINHA SENHA
                    if (!useEmergencyPhrase) {
                        // Verificação via Código de 6 Dígitos
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor.copy(alpha = 0.12f))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "CÓDIGO DE RECUPERAÇÃO ENVIADO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = accentColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Enviamos um código de 6 dígitos para o e-mail: $registeredEmail",
                                    fontSize = 11.sp,
                                    color = subtextColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Código de Verificação (6 Dígitos)", fontSize = 10.sp, color = subtextColor, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = codeInput,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() }.take(6)
                                    codeInput = filtered
                                    errorText = null
                                    if (filtered == generatedCode && filtered.isNotEmpty()) {
                                        isCodeVerified = true
                                    }
                                },
                                placeholder = { Text("Ex: 123456") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                    focusedBorderColor = if (isCodeVerified) Color(0xFF22C55E) else accentColor,
                                    unfocusedBorderColor = if (isCodeVerified) Color(0xFF22C55E) else (if (isDark) Color(0xFF3F3F46) else Color(0xFFD1D5DB))
                                )
                            )

                            Button(
                                onClick = { sendVerificationCode() },
                                enabled = resendCooldown == 0,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2C2C32) else Color(0xFFE5E7EB))
                            ) {
                                Text(
                                    text = if (resendCooldown > 0) "${resendCooldown}s" else "Reenviar",
                                    fontSize = 10.sp,
                                    color = textColor,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        if (isCodeVerified) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "✓ Código validado com sucesso! Defina sua nova senha abaixo.",
                                fontSize = 10.sp,
                                color = Color(0xFF22C55E),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        TextButton(
                            onClick = { useEmergencyPhrase = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Sem acesso ao e-mail? Usar Frase de Foco", fontSize = 9.sp, color = subtextColor)
                        }
                    } else {
                        // Fallback: Frase de Emergência
                        Text(
                            text = "Digite exatamente a frase de compromisso para redefinir:",
                            fontSize = 10.5.sp,
                            color = subtextColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"$emergencyPhraseTarget\"",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = emergencyPhraseInput,
                            onValueChange = {
                                emergencyPhraseInput = it
                                errorText = null
                                if (it.trim().equals(emergencyPhraseTarget, ignoreCase = true)) {
                                    isPhraseVerified = true
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = if (isPhraseVerified) Color(0xFF22C55E) else accentColor,
                                unfocusedBorderColor = if (isDark) Color(0xFF3F3F46) else Color(0xFFD1D5DB)
                            )
                        )

                        TextButton(
                            onClick = { useEmergencyPhrase = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Voltar para código por e-mail", fontSize = 9.sp, color = accentColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Nova Senha
                Text("Nova Senha (Mín. 12, 1 Maiúscula, 1 Símbolo)", fontSize = 10.sp, color = subtextColor, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = newPass,
                    onValueChange = {
                        newPass = it
                        errorText = null
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = if (isDark) Color(0xFF3F3F46) else Color(0xFFD1D5DB)
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Requirement Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PasswordRequirementChip(
                        text = "12+ carac.",
                        isValid = PasswordValidator.hasMinLength(newPass)
                    )
                    PasswordRequirementChip(
                        text = "1 Maiúscula (A-Z)",
                        isValid = PasswordValidator.hasUppercase(newPass)
                    )
                    PasswordRequirementChip(
                        text = "1 Símbolo (!@#)",
                        isValid = PasswordValidator.hasSpecialSymbol(newPass)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Confirmar Nova Senha
                Text("Confirmar Nova Senha", fontSize = 10.sp, color = subtextColor, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = confirmPass,
                    onValueChange = {
                        confirmPass = it
                        errorText = null
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = if (isDark) Color(0xFF3F3F46) else Color(0xFFD1D5DB)
                    )
                )

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorText!!, color = Color.Red, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2C2C32) else Color(0xFFE5E7EB))
                    ) {
                        Text("CANCELAR", fontSize = 10.sp, color = textColor)
                    }

                    Button(
                        onClick = {
                            if (!isForgotMode && oldPass != currentPassword) {
                                errorText = "Senha atual incorreta."
                                return@Button
                            }
                            if (isForgotMode && !useEmergencyPhrase && !isCodeVerified) {
                                if (codeInput != generatedCode) {
                                    errorText = "Código de verificação incorreto."
                                    return@Button
                                }
                            }
                            if (isForgotMode && useEmergencyPhrase && !isPhraseVerified) {
                                errorText = "Digite a frase de compromisso corretamente."
                                return@Button
                            }
                            val validationError = PasswordValidator.getValidationError(newPass)
                            if (validationError != null) {
                                errorText = validationError
                                return@Button
                            }
                            if (newPass != confirmPass) {
                                errorText = "As senhas não coincidem."
                                return@Button
                            }
                            onSaveNewPassword(newPass)
                            android.widget.Toast.makeText(context, "Nova senha de confiança salva com sucesso!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        enabled = isNewPassValid && confirmPass.isNotEmpty() && isAuthorizedToSave
                    ) {
                        Text("SALVAR", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// DIALOG: CONFIRMAR RESTAURAÇÃO DE PADRÕES
// -------------------------------------------------------------------------
@Composable
private fun ResetDefaultsConfirmDialog(
    accentColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF18181B))
                .border(1.dp, Color(0xFF2E2E34), RoundedCornerShape(18.dp))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "RESTAURAR PADRÕES?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Todas as preferências de tema, vibração, alertas e ilha flutuante serão revertidas aos valores de fábrica.",
                    fontSize = 11.sp,
                    color = CmfMutedText,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C32))
                    ) {
                        Text("CANCELAR", fontSize = 10.sp, color = Color.White)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        Text("RESTAURAR", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// DIALOG: STATUS DA PROTEÇÃO CONTRA DESINSTALAÇÃO
// -------------------------------------------------------------------------
@Composable
private fun DeviceAdminStatusDialog(
    accentColor: Color,
    onDismiss: () -> Unit,
    onStartUninstallProtocol: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val dialogBg = if (isDark) Color(0xFF18181B) else Color(0xFFFFFFFF)
    val dialogBorder = if (isDark) Color(0xFF2E2E34) else Color(0xFFE2E2E6)
    val titleColor = if (isDark) Color.White else Color(0xFF18181B)
    val bodyColor = if (isDark) Color(0xFFC7C7CF) else Color(0xFF4A4A52)
    val boxBg = if (isDark) Color(0xFF222226) else Color(0xFFF4F4F6)

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1B5E20)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "BLINDAGEM ATIVA",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = titleColor
                        )
                        Text(
                            text = "Administrador do Dispositivo Ativado",
                            fontSize = 10.sp,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "O Lock In está protegido contra desinstalação. Para remover o aplicativo, você deve cumprir obrigatoriamente o protocolo de segurança:",
                    fontSize = 11.sp,
                    color = bodyColor,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(boxBg)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "• Digitar a Senha de Confiança 5 vezes consecutivas (sem errar).\n• Digitar manualmente a frase exata 'estou seguro de desinstalar o Lock in'.\n• Proibido copiar e colar.",
                        fontSize = 10.sp,
                        color = if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100),
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF2C2C32) else Color(0xFFE4E4E7)
                        )
                    ) {
                        Text("FECHAR", fontSize = 10.sp, color = if (isDark) Color.White else Color(0xFF18181B))
                    }

                    Button(
                        onClick = onStartUninstallProtocol,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("DESINSTALAR...", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// DIALOG: SEGURANÇA E ISENÇÃO DE APPS BANCÁRIOS
// -------------------------------------------------------------------------
@Composable
private fun BankingSecurityDialog(
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val isPaused by com.example.service.BankingSecurityHelper.isPausedFlow.collectAsState()
    val isCaixaDetected = remember { com.example.service.BankingSecurityHelper.isCaixaInstalled(context) }
    val isUsbDebugging = remember { com.example.service.BankingSecurityHelper.isUsbDebuggingEnabled(context) }

    val dialogBg = if (isDark) Color(0xFF18181B) else Color(0xFFFFFFFF)
    val dialogBorder = if (isDark) Color(0xFF2E2E34) else Color(0xFFE2E2E6)
    val titleColor = if (isDark) Color.White else Color(0xFF18181B)
    val bodyColor = if (isDark) Color(0xFFC7C7CF) else Color(0xFF4A4A52)
    val cardBoxBg = if (isDark) Color(0xFF222226) else Color(0xFFF4F4F6)

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape(18.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CENTRAL DE SEGURANÇA BANCÁRIA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = titleColor
                        )
                        Text(
                            text = if (isPaused) "MODO BANCO ATIVO (ACESSO LIVRE)" else "ISENÇÃO & COMPATIBILIDADE",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPaused) Color(0xFFFF9800) else Color(0xFF10B981)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // CARD ESPECÍFICO DA CAIXA ECONÔMICA FEDERAL
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF1E2838) else Color(0xFFEFF6FF))
                        .border(1.2.dp, Color(0xFF3B82F6), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🏦", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DIAGNÓSTICO: BANCO CAIXA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF3B82F6)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF10B981).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isCaixaDetected) "DETECTADO" else "SUPORTADO",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "A Caixa possui o sistema antifraude mais rigoroso do Brasil. Os pacotes oficiais (br.com.gabba.Caixa, Caixa Tem, FGTS) foram cadastrados e contam com isolamento total.",
                            fontSize = 10.sp,
                            color = bodyColor,
                            lineHeight = 14.sp
                        )
                    }
                }

                // ALERTA DE DEPURAÇÃO USB (CAUSA PRINCIPAL DE ERRO NA CAIXA)
                if (isUsbDebugging) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DEPURAÇÃO USB / DESENVOLVEDOR ATIVA",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFEF4444)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "O app da Caixa bloqueia logins automaticamente quando a 'Depuração USB' está ligada no celular. Desative-a para liberar o acesso.",
                                fontSize = 9.5.sp,
                                color = bodyColor,
                                lineHeight = 13.5.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth().height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("DESATIVAR DEPURAÇÃO NAS CONFIGURAÇÕES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(cardBoxBg)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "MOTIVOS DO BLOQUEIO & SOLUÇÕES APLICADAS:",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "1. Permissão de Acessibilidade: O módulo de segurança da Caixa analisa se outros apps escutam sua tela. O Modo Banco do Lock In isola o serviço para comprovar zero leitura.\n\n" +
                                   "2. Janela Flutuante (Ilha): O Lock In oculta instantaneamente qualquer overlay flutuante ao abrir apps de bancos.\n\n" +
                                   "3. Conexão VPN: O tráfego da Caixa é 100% desvinculado e navega na internet nativa limpa do aparelho.",
                            fontSize = 9.5.sp,
                            color = bodyColor,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // CONTROLES DE MODO BANCO
                if (isPaused) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFF9800).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Modo Banco Ativo: Acessibilidade e monitoramento estão isolados para suas transações bancárias.",
                            fontSize = 10.sp,
                            color = if (isDark) Color(0xFFFFCC80) else Color(0xFFE65100),
                            lineHeight = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            com.example.service.BankingSecurityHelper.resumeProtectionImmediately(context)
                            android.widget.Toast.makeText(context, "Proteção Lock In reativada", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("REATIVAR PROTEÇÃO AGORA", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                com.example.service.BankingSecurityHelper.pauseForBankingAccess(context, 10)
                                android.widget.Toast.makeText(context, "Modo Banco ativado por 10 min. Acesso liberado para a Caixa!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                                contentColor = Color(0xFF10B981)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ATIVAR MODO BANCO (10 MINUTOS)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                        }

                        Button(
                            onClick = {
                                com.example.service.BankingSecurityHelper.pauseForBankingAccess(context, 30)
                                android.widget.Toast.makeText(context, "Modo Banco ativado por 30 min. Acesso liberado para a Caixa!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3B82F6).copy(alpha = 0.12f),
                                contentColor = Color(0xFF3B82F6)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ATIVAR MODO BANCO (30 MINUTOS)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF2C2C32) else Color(0xFFE4E4E7)
                    )
                ) {
                    Text("FECHAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF18181B))
                }
            }
        }
    }
}

@Composable
private fun CreditItemRow(
    number: String,
    name: String,
    role: String,
    isDark: Boolean,
    textColor: Color,
    hasInfoIcon: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 4.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp)
    }

    Row(
        modifier = clickableModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isDark) Color(0xFF2A2A30) else Color(0xFFE4E4E8)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = textColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = textColor
                )
                Text(
                    text = role,
                    fontSize = 10.sp,
                    color = CmfMutedText,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        if (hasInfoIcon) {
            IconButton(
                onClick = { onClick?.invoke() },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Informações sobre $name",
                    tint = LocalCustomTextColor.current,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun JuanPabloSpecialThanksDialog(
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val dialogBg = if (isDark) Color(0xFF18181B) else Color(0xFFFFFFFF)
    val titleColor = if (isDark) Color.White else Color(0xFF18181B)
    val bodyColor = if (isDark) Color(0xFFE4E4E7) else Color(0xFF27272A)
    val quoteBoxBg = if (isDark) Color(0xFF222226) else Color(0xFFF4F4F6)

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(dialogBg)
                .border(1.5.dp, accentColor, RoundedCornerShape(18.dp))
                .padding(22.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "AGRADECIMENTO ESPECIAL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = accentColor,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Juan Pablo",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = titleColor
                )

                Text(
                    text = "Principal Tester e System Breaker",
                    fontSize = 10.sp,
                    color = CmfMutedText,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(quoteBoxBg)
                        .border(1.dp, if (isDark) Color(0xFF33333A) else Color(0xFFE2E2E6), RoundedCornerShape(10.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Agradecimentos especiais para Juan Pablo. Sem ele o Lock In não teria a qualidade que tem hoje e iversas funcionalidades não existiriam. Sem ele o Lock In estaria com mal funcionamento e praticamente não haveria forma de usá-lo. -- Obrigado Juan, por me ajudar tanto! --",
                        fontSize = 11.5.sp,
                        color = bodyColor,
                        lineHeight = 17.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "FECHAR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
