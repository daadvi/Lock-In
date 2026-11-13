package com.example.ui.screens

import com.example.ui.tutorial.tutorialTarget
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.MaterialTheme
import com.example.util.AppHaptics
import com.example.service.FocusReminder
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.AppBlocking
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Language
import com.example.data.model.LockLevel
import com.example.service.DeviceAdminManager
import com.example.service.CloudSyncManager
import com.example.UninstallProtectionActivity
import com.example.ui.components.PlayStoreBlacklistDialog
import com.example.ui.components.WebsiteBlacklistDialog
import com.example.ui.components.VpnShieldDialog
import com.example.ui.components.ChangeLockLevelDialog
import com.example.ui.components.AbsoluteInstallBlockDialog
import com.example.service.LockInVpnManager
import com.example.ui.components.DeviceInstructionsCard
import com.example.util.DeviceInstructionHelper
import com.example.ui.components.AccountSyncDialog
import android.content.Intent
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.DotMatrixPattern
import com.example.ui.components.NothingCard
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.LocalCustomTextColor
import com.example.ui.theme.LocalIsDarkTheme

@Composable
fun SettingsScreen(
    currentThemeMode: String,
    hapticFeedbackEnabled: Boolean,
    streakRemindersEnabled: Boolean,
    preLimitAlertsEnabled: Boolean,
    mindfulDelayEnabled: Boolean,
    userName: String = "Focado",
    currentPassword: () -> String,
    onSaveThemeMode: (String) -> Unit,
    onSaveHapticFeedback: (Boolean) -> Unit,
    onSaveStreakReminders: (Boolean) -> Unit,
    onSavePreLimitAlerts: (Boolean) -> Unit,
    onSaveMindfulDelay: (Boolean) -> Unit,
    onUpdateUserName: (String) -> Unit = {},
    onUpdatePassword: (String) -> Unit,
    onResetToDefaults: () -> Unit,
    onNavigateToStyling: () -> Unit,
    onNavigateToResetSettings: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onBack: () -> Unit
) {
    val accentColor = LocalCustomTextColor.current
    val context = LocalContext.current

    var showNameDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
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
    val currentLockLevel by userPrefs.lockLevel.collectAsStateWithLifecycle()
    val isAbsoluteInstallBlock by userPrefs.isAbsoluteInstallBlock.collectAsStateWithLifecycle()

    val currentAccount by CloudSyncManager.currentAccount.collectAsStateWithLifecycle()

    fun triggerHaptic() = AppHaptics.tap(context)
    fun setHaptic(enabled: Boolean) {
        onSaveHapticFeedback(enabled)
        if (enabled) triggerHaptic()
    }
    fun setReminder(enabled: Boolean) {
        triggerHaptic()
        onSaveStreakReminders(enabled)
    }

    var notificationsAllowed by remember { mutableStateOf(FocusReminder.notificationsAllowed(context)) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsAllowed = FocusReminder.notificationsAllowed(context)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val tutorial = com.example.ui.tutorial.LocalTutorialController.current
    val gridState = rememberLazyGridState()
    val isBankingPaused by com.example.service.BankingSecurityHelper.isPausedFlow.collectAsStateWithLifecycle()
    val isVpnRunning by LockInVpnManager.isVpnRunning.collectAsStateWithLifecycle()
    val vpnBlockedCount by LockInVpnManager.blockedDnsCount.collectAsStateWithLifecycle()
    var isDeviceAdminActive by remember { mutableStateOf(DeviceAdminManager.isDeviceAdminActive(context)) }
    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) isDeviceAdminActive = DeviceAdminManager.isDeviceAdminActive(context)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val detectedProfile = remember { DeviceInstructionHelper.getProfile() }
    val sections = listOf(
        "FOCO E ALERTAS" to listOf(
            SettingsTile("haptic", "Vibração ao tocar", "Retorno suave ao usar os controles.", Icons.Default.Vibration,
                checked = hapticFeedbackEnabled, onClick = { setHaptic(!hapticFeedbackEnabled) }),
            SettingsTile("streak", "Lembrete de foco", "Revise sua sequência às 20h. Pausado durante o foco.", Icons.Default.NotificationsActive,
                checked = streakRemindersEnabled, onClick = { setReminder(!streakRemindersEnabled) }),
            SettingsTile("pre_limit", "Alerta pré-limite", "Avisos quando faltarem 5 e 2 minutos.", Icons.Default.Timer,
                checked = preLimitAlertsEnabled, onClick = { triggerHaptic(); onSavePreLimitAlerts(!preLimitAlertsEnabled) }),
            SettingsTile("mindful", "Pausa consciente", "Respire por 30 segundos antes de desbloquear.", Icons.Default.SelfImprovement,
                checked = mindfulDelayEnabled, onClick = { triggerHaptic(); onSaveMindfulDelay(!mindfulDelayEnabled) })
        ),
        "SUA CONTA E APARÊNCIA" to listOf(
            SettingsTile("name", "Seu nome", userName.ifBlank { "Defina seu nome de foco" }, Icons.Default.Person,
                onClick = { showNameDialog = true }),
            SettingsTile("island", "Ilha e cores", "Posição, tamanho, transparência e cor de destaque.", Icons.Default.ColorLens,
                target = "island", onClick = onNavigateToStyling),
            SettingsTile("account", "Conta e backup", currentAccount?.email ?: "Salve suas configurações neste aparelho.", Icons.Default.CloudSync,
                target = "account", onClick = { showAccountSyncDialog = true }),
            SettingsTile("password", "Senha de confiança", "Alterar senha ou recuperar o acesso.", Icons.Default.Key,
                target = "password", onClick = { showPasswordDialog = true })
        ),
        "BLOQUEIOS E PROTEÇÕES" to listOf(
            SettingsTile("levels", "Nível de Lock", currentLockLevel.title + " • Regras de desbloqueio.", Icons.Default.Layers,
                target = "levels", onClick = { showChangeLockLevelDialog = true }),
            SettingsTile("absolute", "Bloqueio absoluto",
                when {
                    currentLockLevel == LockLevel.DEV_DREAM -> "Permanente • Nível Sonho do Dev: nada sai da lista."
                    isAbsoluteInstallBlock -> "Ativo • Protege a lista de instalação."
                    else -> "Reforce o bloqueio da instalação de apps."
                }, Icons.Default.Lock,
                onClick = { pendingAbsoluteBlockEnable = !isAbsoluteInstallBlock; showAbsoluteBlockDialog = true }),
            SettingsTile("play_store", "Apps da Play Store", "Escolha os apps cuja instalação será bloqueada.", Icons.Default.Shop,
                target = "play_store", onClick = { showPlayStoreBlacklistDialog = true }),
            SettingsTile("websites", "Sites bloqueados", "Gerencie os domínios e sites restritos.", Icons.Default.Language,
                target = "websites", onClick = { showWebsiteBlacklistDialog = true }),
            SettingsTile("network", "Filtro de rede", if (isVpnRunning) "Ativo • $vpnBlockedCount bloqueios de DNS hoje." else "Configure o filtro de DNS local.", Icons.Default.Wifi,
                onClick = { showVpnShieldDialog = true }),
            SettingsTile("uninstall", "Desinstalação", if (isDeviceAdminActive) "Proteção ativa • Senha e frases de confirmação." else "Configure a proteção contra remoção.", Icons.Default.DeleteForever,
                target = "uninstall", onClick = { if (isDeviceAdminActive) showDeviceAdminDialog = true else DeviceAdminManager.requestDeviceAdmin(context) })
        ),
        "DISPOSITIVO" to listOf(
            SettingsTile("permissions", "Central de permissões", "Confira os acessos necessários para o Lock In.", Icons.Default.Tune,
                target = "permissions", onClick = onNavigateToPermissions),
            SettingsTile("reset", "Tempo de reset", "Quando liberar e reiniciar o limite do aparelho.", Icons.Default.Sync,
                target = "reset", onClick = onNavigateToResetSettings),
            SettingsTile("banking", "Apps bancários", if (isBankingPaused) "Modo banco pausado." else "Compatibilidade e pausa da proteção.", Icons.Default.AccountBalance,
                onClick = { showBankingSecurityDialog = true }),
            SettingsTile("background", "Manter app ativo", detectedProfile.family.displayName + " • Bateria e início automático.", Icons.Default.PhoneAndroid,
                onClick = { showXiaomiDialog = true })
        ),
        "SOBRE E PREFERÊNCIAS" to listOf(
            SettingsTile("info_card", "Informações e créditos", "Conheça o Lock In e quem ajudou a criá-lo.", Icons.Default.Info,
                onClick = { showInfoDialog = true }),
            SettingsTile("defaults", "Restaurar preferências", "Redefina tema, alertas e pausa consciente.", Icons.Default.RestartAlt,
                onClick = { showResetDefaultsDialog = true })
        )
    )
    val showNotificationNotice = streakRemindersEnabled && !notificationsAllowed
    val tutorialIndexes = buildMap {
        var index = if (showNotificationNotice) 3 else 2
        sections.forEach { (_, tiles) ->
            index++ // section heading
            tiles.forEach { tile -> if (tile.target.isNotEmpty()) put(tile.target, index); index++ }
        }
    }
    LaunchedEffect(tutorial?.target, showNotificationNotice) {
        tutorialIndexes[tutorial?.target]?.let { gridState.scrollToItem(it) }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            DotMatrixPattern(Modifier.fillMaxSize())
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") }
                    Column {
                        Text("CONFIGURAÇÕES", fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("Seu Lock In, do seu jeito", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    // Two columns on phones; wider/landscape windows gain space without a horizontal scroll.
                    val columns = if (maxWidth >= 700.dp) 4 else 2
                    LazyVerticalGrid(columns = GridCells.Fixed(columns), state = gridState,
                        modifier = Modifier.fillMaxSize().testTag("settings_content"),
                        contentPadding = PaddingValues(bottom = 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item(key = "hero", span = { GridItemSpan(maxLineSpan) }) {
                            PixelSettingsHeroCard(accentColor, currentThemeMode, hapticFeedbackEnabled, streakRemindersEnabled,
                                onTheme = { triggerHaptic(); onSaveThemeMode(when(currentThemeMode) { "LIGHT" -> "SYSTEM"; "SYSTEM" -> "DARK"; else -> "LIGHT" }) },
                                onHaptic = { setHaptic(!hapticFeedbackEnabled) }, onReminder = { setReminder(!streakRemindersEnabled) })
                        }
                        item(key = "theme", span = { GridItemSpan(maxLineSpan) }) {
                            ThemeSegmentedController(currentThemeMode, accentColor) { triggerHaptic(); onSaveThemeMode(it) }
                        }
                        if (showNotificationNotice) item(key = "notification_notice", span = { GridItemSpan(maxLineSpan) }) {
                            NothingCard(title = "NOTIFICAÇÕES DESATIVADAS") {
                                Text("Permita notificações para receber o lembrete.", fontSize = 13.sp)
                                TextButton(onClick = onNavigateToPermissions, modifier = Modifier.testTag("settings_reminder_permissions")) { Text("Abrir central de permissões") }
                            }
                        }
                        sections.forEach { (title, tiles) ->
                            item(key = title, span = { GridItemSpan(maxLineSpan) }) {
                                Text(title, Modifier.padding(top = 12.dp, bottom = 2.dp), fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                            }
                            tiles.forEach { tile -> item(key = tile.key) { SettingsGridTile(tile, accentColor) } }
                        }
                        item(key = "tutorial", span = { GridItemSpan(maxLineSpan) }) { com.example.ui.tutorial.TutorialEntry() }
                    }
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
            passwordRepeats = currentLockLevel.passwordRepeatCount,
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
    accentColor: Color,
    themeMode: String,
    hapticEnabled: Boolean,
    reminderEnabled: Boolean,
    onTheme: () -> Unit,
    onHaptic: () -> Unit,
    onReminder: () -> Unit
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    NothingCard {
        Text("DO SEU JEITO", fontFamily = FontFamily.Monospace, fontSize = 11.sp,
            letterSpacing = 1.sp, color = muted)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PixelShortcut("Tema", when (themeMode) { "DARK" -> "Escuro"; "SYSTEM" -> "Sistema"; else -> "Claro" },
                onTheme, Modifier.weight(1f).testTag("settings_hero_theme")) {
                PixelMoonGlyph(accentColor, Modifier.size(32.dp, 48.dp))
            }
            PixelShortcut("Vibração", if (hapticEnabled) "Ativada" else "Desativada", onHaptic,
                Modifier.weight(1f).testTag("settings_hero_haptic")) {
                PixelWavesGlyph(if (hapticEnabled) accentColor else muted, Modifier.size(32.dp, 48.dp))
            }
            PixelShortcut("Lembrete", if (reminderEnabled) "20h" else "Desativado", onReminder,
                Modifier.weight(1f).testTag("settings_hero_reminder")) {
                PixelBellGlyph(if (reminderEnabled) accentColor else muted, Modifier.size(36.dp, 48.dp))
            }
        }
    }
}

@Composable
private fun PixelShortcut(
    label: String, state: String, onClick: () -> Unit, modifier: Modifier,
    glyph: @Composable () -> Unit
) {
    Column(modifier.clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        .semantics { stateDescription = state }
        .clickable(role = Role.Button, onClickLabel = "Alterar $label", onClick = onClick)
        .padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        glyph()
        Spacer(Modifier.height(12.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Text(state, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(Triple("LIGHT", Icons.Default.LightMode, "Claro"),
            Triple("SYSTEM", Icons.Default.BrightnessAuto, "Sistema"),
            Triple("DARK", Icons.Default.DarkMode, "Escuro")).forEach { (mode, icon, label) ->
            val selected = selectedMode == mode
            Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelectMode(mode) })
                .testTag("settings_theme_$mode").padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, null, tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp))
                Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

// -------------------------------------------------------------------------
// SETTINGS TOGGLE CARD (with Nothing OS style Switch + "0" indicator)
// -------------------------------------------------------------------------
@Composable
fun NothingSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 18.dp else 0.dp,
        animationSpec = spring(stiffness = 500f), label = "switch_thumb"
    )
    Box(modifier.width(44.dp).height(26.dp).clip(RoundedCornerShape(13.dp))
        .background(if (checked) accentColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant)
        .border(1.dp, if (checked) accentColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline,
            RoundedCornerShape(13.dp))
        .then(if (onCheckedChange != null) Modifier.toggleable(checked, role = Role.Switch,
            onValueChange = onCheckedChange) else Modifier)
        .padding(4.dp), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.offset(x = thumbOffset).size(18.dp).clip(CircleShape)
            .background(if (checked) accentColor else MaterialTheme.colorScheme.onSurfaceVariant))
    }
}

private data class SettingsTile(
    val key: String, val title: String, val summary: String, val icon: ImageVector,
    val target: String = "", val checked: Boolean? = null, val onClick: () -> Unit
)

@Composable
private fun SettingsGridTile(tile: SettingsTile, accentColor: Color) {
    val shape = RoundedCornerShape(16.dp)
    val tag = if (tile.checked != null) "settings_toggle_${tile.key}" else "settings_${tile.key}"
    val action = if (tile.checked != null) Modifier.toggleable(tile.checked, role = Role.Switch, onValueChange = { tile.onClick() })
        else Modifier.clickable(role = Role.Button, onClick = tile.onClick)
    Column(Modifier.fillMaxWidth().heightIn(min = 168.dp).tutorialTarget(tile.target).testTag(tag)
        .clip(shape).background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape).then(action).padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Icon(tile.icon, null, tint = accentColor, modifier = Modifier.size(22.dp))
            if (tile.checked != null) NothingSwitch(tile.checked, null, accentColor)
            else Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(tile.title, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        Text(tile.summary, fontSize = 12.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

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
                            text = "Versão ${com.example.BuildConfig.VERSION_NAME}",
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
                    text = "Tema, vibração, alertas e ilha serão redefinidos. A pausa consciente e o modo estrito serão desativados. Suas senhas e listas de apps não serão apagadas.",
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
    passwordRepeats: Int,
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
                        text = "• Digitar a Senha de Confiança $passwordRepeats vez${if (passwordRepeats > 1) "es consecutivas" else ""} (sem errar).\n• Completar as frases de confirmação exibidas no protocolo.\n• Proibido copiar e colar.",
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
    val isPaused by com.example.service.BankingSecurityHelper.isPausedFlow.collectAsStateWithLifecycle()
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
