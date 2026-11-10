package com.example.ui.screens
import com.example.ui.tutorial.tutorialTarget
import com.example.ui.tutorial.LocalTutorialController
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

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.FloatingIslandService
import com.example.ui.components.DotMatrixPattern
import com.example.ui.components.NothingCard
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
    val onboardingState: OnboardingState = androidx.lifecycle.viewmodel.compose.viewModel()
    var currentStep by onboardingState.step
    var registeredName by onboardingState.name
    var registeredPassword by onboardingState.password
    var selectedLockLevel by onboardingState.lockLevel
    val tutorial = LocalTutorialController.current
    val tutorialPreferences = remember(context) { context.getSharedPreferences("lock_in_tutorial", 0) }

    // The first-use guide begins on its own unless the person explicitly hid it.
    LaunchedEffect(tutorial) {
        if (tutorial != null && tutorial.chapter == null &&
            !tutorialPreferences.getBoolean("completed", false) &&
            !tutorialPreferences.getBoolean("hidden", false)
        ) {
            tutorial.start(chapter = 0, guided = true)
        }
    }
    LaunchedEffect(currentStep, tutorial) {
        if (currentStep != OnboardingStep.EMAIL_AUTH) tutorial?.markCredentialsComplete()
    }

    androidx.activity.compose.BackHandler(enabled = currentStep != OnboardingStep.EMAIL_AUTH) {
        currentStep = if (currentStep == OnboardingStep.LOCK_LEVEL) OnboardingStep.PERMISSIONS else OnboardingStep.EMAIL_AUTH
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

                com.example.ui.tutorial.TutorialEntry(guidedOnboarding = true)

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
                    modifier = Modifier.weight(1f).tutorialTarget(when (currentStep) {
                        OnboardingStep.PERMISSIONS -> "onboarding_permissions_content"
                        OnboardingStep.LOCK_LEVEL -> "levels"
                        else -> "onboarding_content"
                    }),
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
                            com.example.ui.components.PermissionSetup(
                                requireEssentialPermissions = tutorial?.guidedOnboarding == true && tutorial.visible,
                                onPermissionsChanged = { snapshot ->
                                    tutorial?.updateEssentialPermissions(snapshot.missingEssential.isEmpty())
                                },
                                onContinue = {
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
                                    if (Settings.canDrawOverlays(context)) {
                                        com.example.service.FloatingIslandService.showIsland(context)
                                    }
                                    tutorial?.markLockLevelComplete()
                                    onCompleteRegistration(
                                        registeredName.trim().ifBlank { "Foco" },
                                        registeredPassword
                                    )
                                    registeredPassword = ""
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
    Column(Modifier.tutorialTarget("overview"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (currentStep != OnboardingStep.EMAIL_AUTH) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar para a etapa anterior",
                        tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            Column(Modifier.weight(1f)) {
                Text("Etapa ${currentStep.ordinal + 1} de 3", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    when (currentStep) {
                        OnboardingStep.EMAIL_AUTH -> "Sua conta"
                        OnboardingStep.PERMISSIONS -> "Permissões"
                        OnboardingStep.LOCK_LEVEL -> "Nível de bloqueio"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        androidx.compose.material3.LinearProgressIndicator(
            progress = { (currentStep.ordinal + 1) / 3f },
            modifier = Modifier.fillMaxWidth(),
            color = accentColor
        )
    }
}

@Composable
internal fun LockLevelStepContent(
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
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Escolha a intensidade de bloqueio do Lock In. A quantidade de repetições da senha também vale para desbloquear o dispositivo, desinstalar o Lock In e desativar suas proteções:",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )

        com.example.data.model.LockLevel.entries.forEach { level ->
            val isSelected = level == selectedLevel
            val palette = com.example.ui.theme.lockLevelColors(level)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.background(isSelected))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = palette.accent.copy(alpha = if (isSelected) 1f else .45f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .selectable(selected = isSelected, role = Role.RadioButton) { onSelectLevel(level) }
                    .testTag("lock_level_${level.id}")
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(palette.accent.copy(alpha = .18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${level.levelNumber}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = palette.accent,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Nível ${level.levelNumber}: \"${level.title}\"",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = BlockyFontFamily,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(palette.accent.copy(alpha = .18f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "SELECIONADO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = palette.accent
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = level.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
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
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "• Repetições de senha: ${level.passwordRepeatCount}x",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accent,
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
        hasBackupFound = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            trimmed.contains("@") && trimmed.contains(".") && CloudSyncManager.hasCloudBackup(context, trimmed)
        }
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
                    .testTag("email_auth_email_input").tutorialTarget("account"),
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
                                    val finalPassword = password

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
