package com.example.ui.screens
import com.example.ui.theme.BlockyFontFamily
import com.example.util.PasswordValidator
import com.example.ui.components.AccountSyncDialog
import com.example.ui.components.PasswordRequirementChip
import com.example.service.CloudSyncManager
import com.example.data.model.EmailProvider
import com.example.LockInApplication
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.service.FloatingIslandService
import com.example.ui.components.DotMatrixPattern
import com.example.ui.components.NothingCard
import com.example.ui.components.XiaomiCompatibilityCard
import com.example.ui.components.cmfTextFieldColors

import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import com.example.ui.theme.LocalCustomTextColor

enum class OnboardingStep {
    EMAIL_AUTH,
    PERMISSIONS,
    LOCK_LEVEL
}

@Composable
fun OnboardingScreen(
    onCompleteRegistration: (name: String, password: String) -> Unit
) {
    val accentColor = LocalCustomTextColor.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentStep by remember { mutableStateOf(OnboardingStep.EMAIL_AUTH) }

    // Registration states established in Email Auth step
    var registeredName by remember { mutableStateOf("") }
    var registeredPassword by remember { mutableStateOf("") }
    var selectedLockLevel by remember { mutableStateOf(com.example.data.model.LockLevel.DETERMINED) }

    // Permissions states
    var hasOverlayPermission by remember { mutableStateOf(false) }
    var hasUsageStatsPermission by remember { mutableStateOf(false) }
    var hasLockScreenPermission by remember { mutableStateOf(false) }
    var hasDeviceAdminPermission by remember { mutableStateOf(false) }

    fun checkPermissions() {
        hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true

        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        hasUsageStatsPermission = mode == AppOpsManager.MODE_ALLOWED
        hasLockScreenPermission = com.example.service.LockScreenPermissionHelper.hasLockScreenPermission(context)
        hasDeviceAdminPermission = com.example.service.DeviceAdminManager.isDeviceAdminActive(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        checkPermissions()
    }

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
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Top Step Progress Indicator
                StepProgressHeader(
                    currentStep = currentStep,
                    accentColor = accentColor,
                    onBackClick = {
                        when (currentStep) {
                            OnboardingStep.PERMISSIONS -> currentStep = OnboardingStep.EMAIL_AUTH
                            OnboardingStep.LOCK_LEVEL -> currentStep = OnboardingStep.PERMISSIONS
                            OnboardingStep.EMAIL_AUTH -> {}
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = "onboarding_step_content"
                ) { step ->
                    when (step) {
                        OnboardingStep.EMAIL_AUTH -> {
                            EmailAuthStepContent(
                                accentColor = accentColor,
                                onAuthSuccess = { name, password ->
                                    registeredName = name
                                    registeredPassword = password
                                    currentStep = OnboardingStep.PERMISSIONS
                                }
                            )
                        }
                        OnboardingStep.PERMISSIONS -> {
                            PermissionsStepContent(
                                accentColor = accentColor,
                                hasOverlay = hasOverlayPermission,
                                hasUsageStats = hasUsageStatsPermission,
                                hasLockScreen = hasLockScreenPermission,
                                hasDeviceAdmin = hasDeviceAdminPermission,
                                onNext = {
                                    currentStep = OnboardingStep.LOCK_LEVEL
                                }
                            )
                        }
                        OnboardingStep.LOCK_LEVEL -> {
                            LockLevelStepContent(
                                accentColor = accentColor,
                                selectedLevel = selectedLockLevel,
                                onSelectLevel = { selectedLockLevel = it },
                                onConfirm = {
                                    com.example.LockInApplication.instance.userPreferences.setLockLevel(selectedLockLevel)
                                    if (hasOverlayPermission) {
                                        com.example.service.FloatingIslandService.showIsland(context)
                                    }
                                    onCompleteRegistration(
                                        registeredName.trim().ifBlank { "Foco" },
                                        registeredPassword
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepProgressHeader(
    currentStep: OnboardingStep,
    accentColor: Color,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (currentStep != OnboardingStep.EMAIL_AUTH) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = CmfDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(36.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                OnboardingStep.EMAIL_AUTH to "1. LOGIN",
                OnboardingStep.PERMISSIONS to "2. PERMISSÕES",
                OnboardingStep.LOCK_LEVEL to "3. NÍVEL DE LOCK"
            ).forEach { (step, label) ->
                val isActive = currentStep == step
                val isDone = currentStep.ordinal > step.ordinal

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                isActive -> accentColor
                                isDone -> Color(0xFF18181B)
                                else -> CmfLightGray
                            }
                        )
                        .padding(horizontal = 7.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BlockyFontFamily,
                        color = if (isActive || isDone) Color.White else CmfMutedText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun LockLevelStepContent(
    accentColor: Color,
    selectedLevel: com.example.data.model.LockLevel,
    onSelectLevel: (com.example.data.model.LockLevel) -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "ESCOLHA SEU NÍVEL DE LOCK",
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            fontFamily = BlockyFontFamily,
            color = CmfDark
        )
        Text(
            text = "Depois de colocar as permissões, escolha a intensidade de bloqueio do Lock In para os seus aplicativos:",
            fontSize = 12.sp,
            color = CmfMutedText,
            lineHeight = 16.sp
        )

        com.example.data.model.LockLevel.entries.forEach { level ->
            val isSelected = level == selectedLevel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) accentColor.copy(alpha = 0.08f) else Color(0xFFF9F9FB))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) accentColor else CmfBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelectLevel(level) }
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
                                    .background(if (isSelected) accentColor else Color(0xFF18181B)),
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
                                text = "Nível ${level.levelNumber}: \"${level.title}\"",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = BlockyFontFamily,
                                color = CmfDark
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "SELECIONADO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = level.description,
                        fontSize = 11.sp,
                        color = CmfDark,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "• Desbloqueios: ${level.maxDailyUnlocksPerApp}x/dia",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CmfDark,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "• Repetições de senha: ${level.passwordRepeatCount}x",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "CONFIRMAR NÍVEL E ENTRAR NO LOCK IN",
                fontWeight = FontWeight.Bold,
                fontFamily = BlockyFontFamily,
                fontSize = 12.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmailAuthStepContent(
    accentColor: Color,
    onAuthSuccess: (name: String, password: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isLoginTab by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var autoRestoreBackup by remember { mutableStateOf(true) }
    var hasBackupFound by remember { mutableStateOf(false) }

    // Offline / Local setup dialog state
    var showOfflineDialog by remember { mutableStateOf(false) }
    var offlineName by remember { mutableStateOf("Foco") }
    var offlinePassword by remember { mutableStateOf("") }
    var offlineConfirmPassword by remember { mutableStateOf("") }
    var offlinePasswordVisible by remember { mutableStateOf(false) }
    var offlineError by remember { mutableStateOf<String?>(null) }

    val quickDomains = listOf("@gmail.com", "@outlook.com", "@hotmail.com", "@icloud.com")

    LaunchedEffect(email) {
        val trimmed = email.trim().lowercase()
        hasBackupFound = trimmed.contains("@") && trimmed.contains(".") && CloudSyncManager.hasCloudBackup(context, trimmed)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(10.dp))

            // Brand & Step Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PRIMEIRO ACESSO • AUTENTICAÇÃO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BlockyFontFamily,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isLoginTab) "LOGIN COM E-MAIL" else "CRIAR CONTA COM E-MAIL",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = BlockyFontFamily,
                color = CmfDark,
                lineHeight = 28.sp,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isLoginTab)
                    "Acesse sua conta para restaurar backups automáticos e manter seus limites sincronizados com segurança."
                else
                    "Cadastre seu e-mail e defina uma senha de confiança para proteger sua rotina de foco e dados de uso.",
                fontSize = 12.sp,
                color = CmfMutedText,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Selector (ENTRAR vs CRIAR CONTA)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE4E4E7))
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isLoginTab) CmfWhite else Color.Transparent)
                        .clickable {
                            isLoginTab = true
                            errorMessage = null
                        }
                        .padding(vertical = 10.dp)
                        .testTag("email_auth_login_tab"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Login,
                            contentDescription = null,
                            tint = if (isLoginTab) CmfDark else CmfMutedText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ENTRAR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = BlockyFontFamily,
                            color = if (isLoginTab) CmfDark else CmfMutedText
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isLoginTab) CmfWhite else Color.Transparent)
                        .clickable {
                            isLoginTab = false
                            errorMessage = null
                        }
                        .padding(vertical = 10.dp)
                        .testTag("email_auth_register_tab"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = if (!isLoginTab) CmfDark else CmfMutedText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CRIAR CONTA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = BlockyFontFamily,
                            color = if (!isLoginTab) CmfDark else CmfMutedText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // E-MAIL FIELD
            Text(
                text = "E-MAIL",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = BlockyFontFamily,
                color = CmfDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                placeholder = { Text("seu.email@exemplo.com", fontSize = 13.sp) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("email_auth_email_input"),
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = cmfTextFieldColors(accentColor = accentColor)
            )

            // Quick domain chips
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickDomains.forEach { domain ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF4F4F5))
                            .border(1.dp, CmfBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                val current = email.trim()
                                email = if (current.contains("@")) {
                                    val prefix = current.substringBefore("@")
                                    prefix + domain
                                } else {
                                    current + domain
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = domain,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = CmfDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // If REGISTER TAB: NAME FIELD
            if (!isLoginTab) {
                Text(
                    text = "SEU NOME OU APELIDO",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BlockyFontFamily,
                    color = CmfDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Ex: Foco Total ou Davi", fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_auth_name_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = cmfTextFieldColors(accentColor = accentColor)
                )

                Spacer(modifier = Modifier.height(14.dp))
            }

            // PASSWORD FIELD
            Text(
                text = if (isLoginTab) "SENHA DE ACESSO" else "SENHA DE CONFIANÇA (MÍN. 12 DÍGITOS)",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = BlockyFontFamily,
                color = CmfDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                placeholder = {
                    Text(
                        if (isLoginTab) "Digite sua senha" else "Ex: Foco@2026",
                        fontSize = 13.sp
                    )
                },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Ocultar" else "Mostrar",
                            tint = CmfMutedText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("email_auth_password_input"),
                shape = RoundedCornerShape(10.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = cmfTextFieldColors(accentColor = accentColor)
            )

            // Password Requirement Chips for Register
            if (!isLoginTab) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PasswordRequirementChip(
                        text = "12+ carac.",
                        isValid = PasswordValidator.hasMinLength(password)
                    )
                    PasswordRequirementChip(
                        text = "1 Maiúscula",
                        isValid = PasswordValidator.hasUppercase(password)
                    )
                    PasswordRequirementChip(
                        text = "1 Símbolo (!@#)",
                        isValid = PasswordValidator.hasSpecialSymbol(password)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // CONFIRM PASSWORD FIELD
                Text(
                    text = "CONFIRMAR SENHA",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BlockyFontFamily,
                    color = CmfDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = null
                    },
                    placeholder = { Text("Repita a senha de confiança", fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (confirmPasswordVisible) "Ocultar" else "Mostrar",
                                tint = CmfMutedText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_auth_confirm_password_input"),
                    shape = RoundedCornerShape(10.dp),
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = cmfTextFieldColors(
                        accentColor = accentColor,
                        isError = confirmPassword.isNotEmpty() && password != confirmPassword
                    )
                )
            }

            // Cloud Backup found card
            if (isLoginTab && hasBackupFound) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE8F5E9))
                        .border(1.dp, Color(0xFF00C853).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = Color(0xFF00C853),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BACKUP NA NUVEM ENCONTRADO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = BlockyFontFamily,
                                color = Color(0xFF1B5E20)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ao entrar, seus limites, rotinas e configurações salvas serão restaurados automaticamente neste dispositivo.",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Error message
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFEBEE))
                        .border(1.dp, Color(0xFFFFCDD2), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFD32F2F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Action Buttons & Offline Mode
        Column {
            // Main Submit Button
            Button(
                onClick = {
                    val cleanEmail = email.trim().lowercase()
                    if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                        errorMessage = "Por favor, digite um endereço de e-mail válido."
                        return@Button
                    }

                    if (isLoginTab) {
                        if (password.isBlank()) {
                            errorMessage = "Digite a sua senha de acesso."
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            try {
                                val loginResult = CloudSyncManager.loginAccount(context, cleanEmail, password)
                                if (loginResult.isSuccess) {
                                    val profile = loginResult.getOrNull()
                                    var finalName = profile?.displayName?.ifBlank { cleanEmail.substringBefore("@") } ?: "Foco"
                                    var finalPassword = password

                                    if (autoRestoreBackup && CloudSyncManager.hasCloudBackup(context, cleanEmail)) {
                                        try {
                                            val restoreResult = CloudSyncManager.restoreDataFromCloud(
                                                context,
                                                cleanEmail
                                            )
                                            if (restoreResult.success) {
                                                val app = context.applicationContext as? LockInApplication ?: LockInApplication.instance
                                                val restoredName = app.userPreferences.userName.value
                                                if (restoredName.isNotBlank()) finalName = restoredName
                                                val restoredPass = app.userPreferences.getTrustPassword()
                                                if (restoredPass.isNotBlank()) finalPassword = restoredPass
                                            }
                                        } catch (_: Exception) {}
                                    }
                                    isLoading = false
                                    onAuthSuccess(finalName, finalPassword)
                                } else {
                                    isLoading = false
                                    errorMessage = loginResult.exceptionOrNull()?.message
                                        ?: "Falha na autenticação. Verifique seu e-mail e senha."
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = e.message ?: "Erro ao autenticar conta."
                            }
                        }
                    } else {
                        // Register flow
                        if (!PasswordValidator.isValid(password)) {
                            errorMessage = "A senha deve ter no mínimo 12 caracteres, 1 letra maiúscula e 1 caractere especial (!@#$)."
                            return@Button
                        }
                        if (password != confirmPassword) {
                            errorMessage = "As senhas não coincidem. Digite a mesma senha na confirmação."
                            return@Button
                        }

                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            try {
                                val chosenName = name.trim().ifBlank { cleanEmail.substringBefore("@") }
                                val regResult = CloudSyncManager.registerAccount(
                                    context = context,
                                    email = cleanEmail,
                                    password = password,
                                    displayName = chosenName
                                )
                                if (regResult.isSuccess) {
                                    isLoading = false
                                    onAuthSuccess(chosenName, password)
                                } else {
                                    isLoading = false
                                    errorMessage = regResult.exceptionOrNull()?.message
                                        ?: "Falha ao registrar conta com e-mail."
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = e.message ?: "Erro ao cadastrar conta."
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("email_auth_submit_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isLoginTab) CmfDark else accentColor)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isLoginTab) Icons.Default.Login else Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isLoginTab) "ENTRAR NA CONTA" else "CRIAR CONTA E CONTINUAR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = BlockyFontFamily,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Offline / Skip option
            OutlinedButton(
                onClick = { showOfflineDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("email_auth_skip_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = CmfDark
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CONTINUAR SEM E-MAIL (MODO LOCAL)",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BlockyFontFamily,
                        color = CmfDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Modal for local setup without email
    if (showOfflineDialog) {
        Dialog(onDismissRequest = { showOfflineDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CmfWhite)
                    .border(1.5.dp, CmfDark, RoundedCornerShape(16.dp))
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
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ACESSO LOCAL (OFFLINE)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = BlockyFontFamily,
                            color = CmfDark
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Você pode usar o Lock In sem associar um e-mail. Para proteger as restrições e desinstalação, defina uma senha de confiança local:",
                        fontSize = 11.5.sp,
                        color = CmfMutedText,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "SEU NOME",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BlockyFontFamily,
                        color = CmfDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = offlineName,
                        onValueChange = { offlineName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = cmfTextFieldColors(accentColor = accentColor)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "SENHA DE CONFIANÇA (MÍN. 12, 1 MAIÚSC., 1 SÍMBOLO)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BlockyFontFamily,
                        color = CmfDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = offlinePassword,
                        onValueChange = {
                            offlinePassword = it
                            offlineError = null
                        },
                        placeholder = { Text("Ex: Foco@2026", fontSize = 12.sp) },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { offlinePasswordVisible = !offlinePasswordVisible }) {
                                Icon(
                                    imageVector = if (offlinePasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = CmfMutedText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        visualTransformation = if (offlinePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = cmfTextFieldColors(accentColor = accentColor)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "CONFIRMAR SENHA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BlockyFontFamily,
                        color = CmfDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = offlineConfirmPassword,
                        onValueChange = {
                            offlineConfirmPassword = it
                            offlineError = null
                        },
                        placeholder = { Text("Repita a senha", fontSize = 12.sp) },
                        singleLine = true,
                        visualTransformation = if (offlinePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = cmfTextFieldColors(accentColor = accentColor)
                    )

                    if (offlineError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = offlineError ?: "",
                            color = Color(0xFFD32F2F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showOfflineDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "VOLTAR",
                                fontSize = 11.sp,
                                fontFamily = BlockyFontFamily,
                                color = CmfDark
                            )
                        }

                        Button(
                            onClick = {
                                if (!PasswordValidator.isValid(offlinePassword)) {
                                    offlineError = "A senha deve ter no mínimo 12 caracteres, 1 maiúscula e 1 símbolo."
                                    return@Button
                                }
                                if (offlinePassword != offlineConfirmPassword) {
                                    offlineError = "As senhas não coincidem."
                                    return@Button
                                }
                                showOfflineDialog = false
                                onAuthSuccess(
                                    offlineName.trim().ifBlank { "Foco" },
                                    offlinePassword
                                )
                            },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text(
                                text = "CONTINUAR",
                                fontSize = 11.sp,
                                fontFamily = BlockyFontFamily,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionsStepContent(
    accentColor: Color,
    hasOverlay: Boolean,
    hasUsageStats: Boolean,
    hasLockScreen: Boolean,
    hasDeviceAdmin: Boolean,
    onNext: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "PERMISSÕES NECESSÁRIAS",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                fontFamily = BlockyFontFamily,
                color = CmfDark,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Para que o cronômetro, a ilha flutuante, a tela de bloqueio e a proteção contra desinstalação funcionem com total segurança, o Android requer as seguintes autorizações:",
                fontSize = 12.sp,
                color = CmfMutedText,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Xiaomi / Redmi System Optimizations Card
            XiaomiCompatibilityCard(
                accentColor = accentColor,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Device Administrator Anti-Uninstall Protection (FIRST & CRITICAL)
            OnboardingPermissionItem(
                title = "Proteção Contra Desinstalação (Administrador)",
                desc = "Impede que o Lock In seja desinstalado sem passar pelo protocolo estrito de segurança (5 senhas consecutivas e frase manual).",
                icon = Icons.Default.Shield,
                isGranted = hasDeviceAdmin,
                accentColor = accentColor,
                buttonText = if (hasDeviceAdmin) "Blindagem Ativa (Autorizado)" else "Ativar Administrador do Dispositivo",
                onClick = {
                    com.example.service.DeviceAdminManager.requestDeviceAdmin(context)
                },
                secondaryButtonText = if (!hasDeviceAdmin) "Configurações" else null,
                onSecondaryClick = {
                    com.example.service.DeviceAdminManager.openDeviceAdminSettings(context)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Overlay / Floating Island
            OnboardingPermissionItem(
                title = "Sobrepor a Outros Apps (Ilha Flutuante)",
                desc = "Exibe a cápsula do cronômetro enquanto você navega e a tela de bloqueio quando o limite zerar.",
                icon = Icons.Default.Layers,
                isGranted = hasOverlay,
                accentColor = accentColor,
                buttonText = if (hasOverlay) "Autorizado" else "Conceder Sobreposição",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                            context.startActivity(intent)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Usage Stats
            OnboardingPermissionItem(
                title = "Acesso aos Dados de Uso",
                desc = "Permite identificar em tempo real o app em execução para subtrair segundos do seu limite.",
                icon = Icons.Default.QueryStats,
                isGranted = hasUsageStats,
                accentColor = accentColor,
                buttonText = if (hasUsageStats) "Autorizado" else "Conceder Acesso a Dados",
                onClick = {
                    try {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Accessibility (For Reels & Shorts detection)
            OnboardingPermissionItem(
                title = "Serviço de Acessibilidade (Reels/Shorts & Play Store)",
                desc = "Detecta vídeos curtos vs longos, Picture-in-Picture e bloqueia instalações na Play Store em tempo real.\n\nInstruções: Configurações > Acessibilidade > Apps Instalados > Ativar 'Lock In'.",
                icon = Icons.Default.Accessibility,
                isGranted = false,
                accentColor = accentColor,
                buttonText = "Configurar Acessibilidade",
                onClick = {
                    try {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Lock Screen Appearance
            OnboardingPermissionItem(
                title = "Aparecer na Tela de Bloqueio",
                desc = "Permite que a tela de bloqueio consciente seja exibida imediatamente mesmo se o aparelho estiver bloqueado.",
                icon = Icons.Default.Lock,
                isGranted = hasLockScreen,
                accentColor = accentColor,
                buttonText = if (hasLockScreen) "Autorizado" else "Permitir na Tela de Bloqueio",
                onClick = {
                    com.example.service.LockScreenPermissionHelper.requestLockScreenPermission(context)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Action Button
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("onboarding_permissions_next_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CmfDark)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "CONCLUIR E INICIAR LOCK IN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BlockyFontFamily,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun OnboardingPermissionItem(
    title: String,
    desc: String,
    icon: ImageVector,
    isGranted: Boolean,
    accentColor: Color,
    buttonText: String,
    onClick: () -> Unit,
    secondaryButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CmfWhite)
            .border(
                1.dp,
                if (isGranted) Color(0xFF00C853).copy(alpha = 0.4f) else CmfBorder,
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) Color(0xFF00C853) else accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        color = CmfDark
                    )
                }

                if (isGranted) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ATIVO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = BlockyFontFamily,
                            color = Color(0xFF00C853)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = desc,
                fontSize = 11.sp,
                color = CmfMutedText,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isGranted) Color(0xFFF3F3F5) else Color(0xFF18181B))
                        .border(
                            1.dp,
                            if (isGranted) CmfBorder else Color.Black,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onClick() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isGranted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF00C853),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = buttonText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = BlockyFontFamily,
                            color = if (isGranted) CmfDark else Color.White
                        )
                    }
                }

                if (secondaryButtonText != null && onSecondaryClick != null && !isGranted) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF4F4F5))
                            .border(1.dp, CmfBorder, RoundedCornerShape(6.dp))
                            .clickable { onSecondaryClick() }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = secondaryButtonText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            color = CmfDark
                        )
                    }
                }
            }
        }
    }
}
