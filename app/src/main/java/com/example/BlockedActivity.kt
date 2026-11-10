package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.example.service.InstallationBlockerHelper
import com.example.ui.components.LockInLogoIcon
import com.example.ui.theme.BlockyFontFamily
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import com.example.ui.theme.LockInTheme
import com.example.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.delay
import java.util.Calendar

class BlockedActivity : ComponentActivity() {

    private val packageNameState = mutableStateOf<String?>(null)
    private val appNameState = mutableStateOf("Rede Social")
    private val blockedReasonState = mutableStateOf<String?>(null)
    private val isDepletedLimitState = mutableStateOf(false)
    private val isBlacklistRestrictedState = mutableStateOf(false)

    private fun extractIntentData(srcIntent: Intent?) {
        if (srcIntent == null) return

        val pkg = srcIntent.getStringExtra(EXTRA_PACKAGE_NAME)
            ?: srcIntent.getStringExtra("EXTRA_PACKAGE_NAME")
            ?: srcIntent.getStringExtra(EXTRA_PROHIBITED_PACKAGE)

        val appName = srcIntent.getStringExtra(EXTRA_APP_NAME)
            ?: srcIntent.getStringExtra("EXTRA_APP_NAME")
            ?: srcIntent.getStringExtra(EXTRA_BLOCKED_TARGET)
            ?: if (InstallationBlockerHelper.isInstallerPackage(pkg)) {
                InstallationBlockerHelper.DEFAULT_APP_NAME
            } else "Rede Social"

        val reason = srcIntent.getStringExtra(EXTRA_BLOCKED_REASON)
            ?: srcIntent.getStringExtra("EXTRA_BLOCKED_REASON")
            ?: if (InstallationBlockerHelper.isInstallerPackage(pkg)) {
                InstallationBlockerHelper.DEFAULT_BLOCKED_REASON
            } else null

        val isDepleted = srcIntent.getBooleanExtra(EXTRA_IS_DEPLETED_LIMIT, false) ||
                srcIntent.getBooleanExtra("EXTRA_IS_DEPLETED_LIMIT", false)

        val isExplicitBlacklist = srcIntent.getBooleanExtra(EXTRA_IS_BLACKLIST_RESTRICTED, false) ||
                srcIntent.getBooleanExtra("EXTRA_IS_BLACKLIST_RESTRICTED", false)

        val isBlacklistMatch = (pkg != null && com.example.service.PlayStoreBlacklistManager.isAppOrPackageBlacklisted(this, pkg) != null) ||
                (reason?.contains("blacklist", ignoreCase = true) == true) ||
                (reason?.contains("lista de bloqueio", ignoreCase = true) == true) ||
                (reason?.contains("proibido", ignoreCase = true) == true) ||
                (appName.contains("Blacklist", ignoreCase = true)) ||
                (appName.contains("Proibido", ignoreCase = true))

        val resolvedPkg = if (!pkg.isNullOrBlank() && pkg != packageName) {
            pkg
        } else {
            com.example.service.PlayStoreBlacklistManager.findInstalledPackageForBlacklist(this, appName)
        }

        packageNameState.value = resolvedPkg ?: pkg
        appNameState.value = appName
        blockedReasonState.value = reason
        isDepletedLimitState.value = isDepleted
        isBlacklistRestrictedState.value = isExplicitBlacklist || isBlacklistMatch
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractIntentData(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (com.example.service.DeviceAdminManager.isUninstallAuthorized(this)) {
            finish()
            return
        }
        enableEdgeToEdge()
        extractIntentData(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        // Dismiss the target blocked app process and window immediately
        val targetBlockedPackage = packageNameState.value
        if (!targetBlockedPackage.isNullOrBlank()) {
            try {
                com.example.service.LockInAccessibilityService.closeAndDismissApp(this, targetBlockedPackage)
            } catch (_: Exception) {}
        }

        // Tapjacking / overlay snooping protection
        window.decorView.filterTouchesWhenObscured = true

        // Strict security: Back press must go to home launcher, NEVER back into the blocked app
        val goHomeAction = {
            val pkgToDismiss = packageNameState.value
            try {
                com.example.service.LockInAccessibilityService.closeAndDismissApp(this, pkgToDismiss)
            } catch (_: Exception) {}
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            finishAndRemoveTask()
        }

        onBackPressedDispatcher.addCallback(this) {
            goHomeAction()
        }

        val onUninstallAction: (String?) -> Unit = { targetPkg ->
            val pkgToUninstall = if (!targetPkg.isNullOrBlank()) {
                targetPkg
            } else {
                packageNameState.value ?: com.example.service.PlayStoreBlacklistManager.findInstalledPackageForBlacklist(this, appNameState.value)
            }

            if (!pkgToUninstall.isNullOrBlank()) {
                try {
                    com.example.service.LockInAccessibilityService.allowTemporaryUninstall(pkgToUninstall)
                    val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
                        data = Uri.parse("package:$pkgToUninstall")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(uninstallIntent)
                } catch (_: Exception) {
                    try {
                        com.example.service.LockInAccessibilityService.allowTemporaryUninstall(pkgToUninstall)
                        val detailsIntent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$pkgToUninstall")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(detailsIntent)
                    } catch (_: Exception) {}
                }
            } else {
                try {
                    val manageAppsIntent = Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(manageAppsIntent)
                } catch (_: Exception) {
                    goHomeAction()
                }
            }
        }

        val prefs = LockInApplication.instance.userPreferences
        val customColor = try {
            Color(prefs.textColorHex.value.toColorInt())
        } catch (_: Exception) {
            Color(0xFFFF5500)
        }

        setContent {
            val themeMode by prefs.themeMode.collectAsStateWithLifecycle()
            LockInTheme(customTextColor = customColor, themeMode = themeMode) {
                BlockedScreen(
                    blockedTarget = appNameState.value,
                    prohibitedPackage = packageNameState.value,
                    blockedReason = blockedReasonState.value,
                    isDepletedLimit = isDepletedLimitState.value,
                    isBlacklistRestricted = isBlacklistRestrictedState.value,
                    accentColor = customColor,
                    onGoHome = goHomeAction,
                    onOpenLockIn = {
                        val lockInIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(lockInIntent)
                        finish()
                    },
                    onUninstallApp = onUninstallAction
                )
            }
        }
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        if (isInMultiWindowMode) {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            finishAndRemoveTask()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            finishAndRemoveTask()
        }
    }

    companion object {
        const val EXTRA_BLOCKED_TARGET = "extra_blocked_target"
        const val EXTRA_PROHIBITED_PACKAGE = "extra_prohibited_package"

        const val EXTRA_PACKAGE_NAME = "EXTRA_PACKAGE_NAME"
        const val EXTRA_APP_NAME = "EXTRA_APP_NAME"
        const val EXTRA_BLOCKED_REASON = "EXTRA_BLOCKED_REASON"

        const val EXTRA_IS_DEPLETED_LIMIT = "EXTRA_IS_DEPLETED_LIMIT"
        const val EXTRA_IS_BLACKLIST_RESTRICTED = "EXTRA_IS_BLACKLIST_RESTRICTED"

        val MOTIVATIONAL_PHRASES = listOf(
            "Veja o que está pendente",
            "Você não precisa estar sempre no telefone",
            "Concentre-se no que realmente importa agora",
            "O tempo que você economiza hoje constrói o seu amanhã",
            "Respire fundo e viva o momento presente",
            "Verifique suas tarefas prioritárias para hoje",
            "Que tal mandar uma mensagem para alguém especial ou ler um livro?",
            "Você tem controle sobre o seu tempo",
            "Desconecte-se para se reconectar",
            "Domine a si mesmo antes de tentar dominar o mundo"
        )
    }
}

private fun calculateSecondsUntilReset(context: android.content.Context): Long {
    return try {
        if (com.example.service.ScreenTimeManager.isLockoutActive(context)) {
            return com.example.service.ScreenTimeManager.getSecondsUntilReset()
        }

        val app = context.applicationContext as? LockInApplication ?: LockInApplication.instance
        val prefs = app.userPreferences
        val lastReset = prefs.getLastResetEpoch()
        val intervalHours = prefs.resetIntervalHours.value
        val intervalMillis = intervalHours * 3600_000L
        val now = System.currentTimeMillis()

        val nextResetEpoch = if (lastReset > 0L) {
            lastReset + intervalMillis
        } else {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 24)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        }
        val diffSec = (nextResetEpoch - now) / 1000L
        diffSec.coerceAtLeast(0L)
    } catch (_: Exception) {
        3600L
    }
}

@Composable
fun BlockedScreen(
    blockedTarget: String,
    prohibitedPackage: String? = null,
    blockedReason: String? = null,
    isDepletedLimit: Boolean = false,
    isBlacklistRestricted: Boolean = false,
    accentColor: Color,
    onGoHome: () -> Unit,
    onOpenLockIn: () -> Unit,
    onUninstallApp: (String?) -> Unit = {}
) {
    // Intercept back key and back gesture to send directly to Home launcher
    BackHandler {
        onGoHome()
    }

    val context = LocalContext.current
    val isDarkTheme = LocalIsDarkTheme.current
    val screenBg = if (isDarkTheme) Color(0xFF101012) else CmfWhite
    val cardBg = if (isDarkTheme) Color(0xFF18181B) else CmfWhite
    val cardBorder = if (isDarkTheme) Color(0xFF27272A) else CmfBorder
    val textColor = if (isDarkTheme) Color.White else CmfDark
    val subtextColor = if (isDarkTheme) Color(0xFFA1A1AA) else CmfMutedText
    val innerCardBg = if (isDarkTheme) Color(0xFF202024) else Color(0xFFF9F9F9)

    // User requirement:
    // If an app in the Play Store blacklist is installed or opened:
    // Show a pop-up saying it's blocked, with no unlock, no password, no phrase,
    // and the only available action is the button to uninstall the app.
    if (isBlacklistRestricted) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = screenBg
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .widthIn(max = 440.dp)
                        .background(cardBg, RoundedCornerShape(20.dp))
                        .border(1.5.dp, Color(0xFFE53935).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFFE53935).copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, Color(0xFFE53935).copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Aplicativo Bloqueado",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE53935).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFE53935).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PLAY STORE • BLOQUEIO TOTAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = BlockyFontFamily,
                            color = Color(0xFFE53935),
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = blockedTarget,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(innerCardBg, RoundedCornerShape(12.dp))
                            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Este aplicativo está na sua lista de bloqueio da Play Store e está totalmente bloqueado para ser usado.\n\nNão é possível entrar no aplicativo, não há opção de desbloqueio e não há função de senha ou frase de emergência.\n\nA única opção é desinstalar o aplicativo no botão abaixo.",
                                fontSize = 13.sp,
                                color = subtextColor,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            if (!prohibitedPackage.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Pacote: $prohibitedPackage",
                                    fontSize = 11.sp,
                                    color = subtextColor.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons: Only Uninstall button and Return to Home button
                    Button(
                        onClick = { onUninstallApp(prohibitedPackage) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("blocked_uninstall_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Desinstalar",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "DESINSTALAR APLICATIVO",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (com.example.service.FriendsPartyManager.isJoined.value) {
                        Button(
                            onClick = {
                                val lockInIntent = Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    putExtra("OPEN_SCREEN", "FRIENDS_PANEL")
                                }
                                context.startActivity(lockInIntent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("blocked_open_friends_panel_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF27272A),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = "Friends Panel",
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFFD71920)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "VER NO FRIENDS PANEL",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedButton(
                        onClick = onGoHome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("blocked_go_home_blacklist_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = textColor
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Início",
                            modifier = Modifier.size(18.dp),
                            tint = subtextColor
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Voltar para a Tela Inicial",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = textColor
                        )
                    }
                }
            }
        }
        return
    }
    val randomQuote = remember {
        BlockedActivity.MOTIVATIONAL_PHRASES.random()
    }
    val isScreenTimeLock = remember(blockedTarget) {
        blockedTarget.contains("Tela", ignoreCase = true)
    }
    val isInstallationBlocked = remember(blockedTarget, prohibitedPackage, blockedReason) {
        InstallationBlockerHelper.isInstallerPackage(prohibitedPackage) ||
        !blockedReason.isNullOrBlank() ||
        blockedTarget.contains("Play Store", ignoreCase = true) ||
        blockedTarget.contains("Blacklist", ignoreCase = true) ||
        blockedTarget.contains("Instalação", ignoreCase = true)
    }

    var secondsToReset by remember { mutableLongStateOf(calculateSecondsUntilReset(context)) }
    var tickPulse by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            secondsToReset = calculateSecondsUntilReset(context)
            tickPulse = !tickPulse
            delay(1000L)
        }
    }

    val hours = secondsToReset / 3600
    val minutes = (secondsToReset % 3600) / 60
    val seconds = secondsToReset % 60
    val clockCountdownText = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    val quotes = BlockedActivity.MOTIVATIONAL_PHRASES
    var phraseIndex by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000L)
            phraseIndex = (phraseIndex + 1) % quotes.size
        }
    }
    val currentQuote = quotes[phraseIndex]

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = screenBg
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row with Logo on top-left and Orange Warning in top-center (Foto 3 request)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo do Lock in no canto superior esquerdo
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(accentColor.copy(alpha = 0.12f), CircleShape)
                            .border(1.dp, accentColor.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        LockInLogoIcon(
                            size = 22.dp,
                            tint = accentColor
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Mensagem laranja no centro superior
                    Text(
                        text = if (isDepletedLimit) "Você já excedeu o limite de uso deste app" else (blockedReason ?: "Acesso restrito pelo Lock In"),
                        color = Color(0xFFFF5500),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(40.dp)) // Balanço visual simétrico
                }

                // Center Reality Check Card com frase que sempre se alterna no centro (Foto 3)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardBg, RoundedCornerShape(16.dp))
                        .border(1.5.dp, cardBorder, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isDepletedLimit) "$blockedTarget • 00:00 ESGOTADO" else "$blockedTarget • BLOQUEADO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = BlockyFontFamily,
                            color = accentColor,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Frase central alternada com animação fluida
                    androidx.compose.animation.AnimatedContent(
                        targetState = currentQuote,
                        transitionSpec = {
                            androidx.compose.animation.fadeIn() togetherWith androidx.compose.animation.fadeOut()
                        },
                        label = "MotivationalQuote"
                    ) { quote ->
                        Text(
                            text = "\"$quote\"",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = BlockyFontFamily,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(innerCardBg, RoundedCornerShape(10.dp))
                            .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = if (isDepletedLimit) {
                                "O seu limite programado para hoje chegou a zero. O tempo economizado aqui devolve o controle do seu dia."
                            } else {
                                blockedReason ?: "O acesso a este aplicativo foi temporariamente suspenso de acordo com suas regras e rotinas no Lock In."
                            },
                            fontSize = 12.sp,
                            color = subtextColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            lineHeight = 16.sp
                        )
                    }
                }

                // Bottom Actions: Botão 'Voltar para o mundo real' (Foto 3)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onGoHome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("blocked_go_home_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Início",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Voltar para o mundo real",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenLockIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("blocked_open_lock_in_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = textColor
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Desbloqueio",
                            modifier = Modifier.size(16.dp),
                            tint = subtextColor
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Desbloqueio de Confiança (Lock in)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}
