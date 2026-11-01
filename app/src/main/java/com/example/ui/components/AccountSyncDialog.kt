package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.EmailProvider
import com.example.service.CloudSyncManager
import com.example.ui.theme.BlockyFontFamily
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import com.example.util.PasswordValidator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountSyncDialog(
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentAccount by CloudSyncManager.currentAccount.collectAsState()
    val isSyncing by CloudSyncManager.isSyncing.collectAsState()
    val lastSyncEpoch by CloudSyncManager.lastSyncEpoch.collectAsState()
    val syncMessage by CloudSyncManager.syncMessage.collectAsState()

    var isRegisterTab by remember { mutableStateOf(false) }
    var selectedProvider by remember { mutableStateOf(EmailProvider.GOOGLE) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusSuccessMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
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
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (currentAccount != null) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "CONTA & NUVEM",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = BlockyFontFamily,
                                color = CmfDark,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "BACKUP E SINCRONIZAÇÃO AUTOMÁTICA",
                                fontSize = 8.5.sp,
                                fontFamily = BlockyFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = CmfDark)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (currentAccount != null) {
                    // =========================================================
                    // VIEW 1: USER IS LOGGED IN
                    // =========================================================
                    val account = currentAccount!!

                    // Profile Header Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF7F7FA))
                            .border(1.dp, CmfBorder, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(accentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = account.email.take(1).uppercase(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = BlockyFontFamily,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account.displayName.uppercase(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = BlockyFontFamily,
                                    color = CmfDark
                                )
                                Text(
                                    text = account.email,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CmfMutedText
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${account.provider.displayName} • Nuvem Conectada",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Backup Content Breakdown
                    Text(
                        text = "ITENS PROTEGIDOS EM NUVEM:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        color = CmfDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF9F9FB))
                            .border(1.dp, CmfBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SyncItemRow("✓ Limites de tela do celular e horários de reset")
                        SyncItemRow("✓ Aplicativos bloqueados e limites de vídeo (Shorts/Longo)")
                        SyncItemRow("✓ Sites proibidos no navegador")
                        SyncItemRow("✓ Rotinas de foco (Focus Time agendados)")
                        SyncItemRow("✓ Blacklist de apps proibidos da Play Store")
                        SyncItemRow("✓ Senha de confiança e preferências de personalização")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Last sync status
                    val dateFormatted = if (lastSyncEpoch > 0) {
                        SimpleDateFormat("dd/MM/yyyy 'às' HH:mm:ss", Locale.getDefault()).format(Date(lastSyncEpoch))
                    } else {
                        "Pendente"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ÚLTIMO BACKUP:",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CmfMutedText
                        )
                        Text(
                            text = dateFormatted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CmfDark
                        )
                    }

                    if (statusSuccessMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = statusSuccessMessage ?: "",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sync & Restore Buttons
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val result = CloudSyncManager.syncDataToCloud(context)
                                if (result.success) {
                                    statusSuccessMessage = "✓ Sincronizado com sucesso na nuvem!"
                                    Toast.makeText(context, "Dados salvos na nuvem!", Toast.LENGTH_SHORT).show()
                                } else {
                                    errorMessage = result.message
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sync_now_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SINCRONIZANDO...", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = BlockyFontFamily)
                        } else {
                            Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SINCRONIZAR AGORA", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = BlockyFontFamily)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                val result = CloudSyncManager.restoreDataFromCloud(context, account.email)
                                if (result.success) {
                                    statusSuccessMessage = "✓ Dados restaurados da nuvem com sucesso!"
                                    Toast.makeText(context, "Dados restaurados com sucesso!", Toast.LENGTH_SHORT).show()
                                } else {
                                    errorMessage = result.message
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("restore_cloud_button"),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isSyncing
                    ) {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp), tint = CmfDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RESTAURAR BACKUP DA NUVEM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CmfDark)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            CloudSyncManager.signOut(context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CmfLightGray, contentColor = CmfDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SAIR DA CONTA", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                } else {
                    // =========================================================
                    // VIEW 2: LOGIN OR CREATE CLOUD ACCOUNT
                    // =========================================================
                    Text(
                        text = "CONTA E BACKUP EM NUVEM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        color = CmfDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Protegido por senha: seu backup, rotinas e limites ficam seguros na nuvem. Ninguém pode acessar seus dados sem a sua senha.",
                        fontSize = 10.sp,
                        color = CmfMutedText,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab selector: ENTRAR vs CRIAR CONTA
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CmfLightGray)
                            .padding(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (!isRegisterTab) accentColor else Color.Transparent)
                                .clickable {
                                    isRegisterTab = false
                                    errorMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ENTRAR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = BlockyFontFamily,
                                color = if (!isRegisterTab) Color.White else CmfDark
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isRegisterTab) accentColor else Color.Transparent)
                                .clickable {
                                    isRegisterTab = true
                                    errorMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "CRIAR CONTA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = BlockyFontFamily,
                                color = if (isRegisterTab) Color.White else CmfDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Provider selector chips
                    Text(
                        text = "SELECIONE SEU PROVEDOR DE E-MAIL:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BlockyFontFamily,
                        color = CmfDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            EmailProvider.GOOGLE,
                            EmailProvider.APPLE,
                            EmailProvider.HOTMAIL,
                            EmailProvider.OUTLOOK,
                            EmailProvider.YAHOO,
                            EmailProvider.PROTON,
                            EmailProvider.CUSTOM
                        ).forEach { provider ->
                            val isSelected = selectedProvider == provider
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) accentColor else CmfLightGray)
                                    .clickable {
                                        selectedProvider = provider
                                        if (provider.defaultDomain.isNotBlank()) {
                                            val username = if (emailInput.contains("@")) {
                                                emailInput.substringBefore("@").trim()
                                            } else {
                                                emailInput.trim()
                                            }
                                            if (username.isNotEmpty()) {
                                                emailInput = "$username@${provider.defaultDomain}"
                                            }
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = provider.displayName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) Color.White else CmfDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Email Field
                    Text(
                        text = "E-MAIL:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CmfDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            selectedProvider = EmailProvider.fromEmail(it)
                        },
                        placeholder = {
                            Text(
                                if (selectedProvider.defaultDomain.isNotBlank())
                                    "seunome@${selectedProvider.defaultDomain}"
                                else "seu.email@dominio.com"
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        trailingIcon = {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = CmfMutedText)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("account_email_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Password Field
                    Text(
                        text = if (isRegisterTab) "CRIE SUA SENHA (MÍNIMO 6 DÍGITOS):" else "SENHA DA CONTA:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CmfDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        placeholder = { Text("••••••••") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = CmfMutedText
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("account_password_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    if (isRegisterTab) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "CONFIRMAR SENHA:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CmfDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = confirmPasswordInput,
                            onValueChange = { confirmPasswordInput = it },
                            placeholder = { Text("••••••••") },
                            singleLine = true,
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = CmfMutedText
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("account_confirm_password_input"),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val cleanEmail = emailInput.trim().lowercase()
                    val hasExistingBackup = remember(cleanEmail) {
                        if (cleanEmail.contains("@") && cleanEmail.contains(".")) {
                            CloudSyncManager.hasCloudBackup(context, cleanEmail)
                        } else false
                    }

                    if (!isRegisterTab && hasExistingBackup) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE8F5E9))
                                .border(1.dp, Color(0xFF81C784), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Backup existente encontrado para este e-mail. Digite a senha cadastrada para autenticar e restaurar seus dados.",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1B5E20)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F),
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    val isEmailValid = cleanEmail.contains("@") && cleanEmail.contains(".")
                    val isFormValid = if (isRegisterTab) {
                        isEmailValid && passwordInput.length >= 6 && confirmPasswordInput == passwordInput
                    } else {
                        isEmailValid && passwordInput.isNotEmpty()
                    }

                    Button(
                        onClick = {
                            if (!isEmailValid) {
                                errorMessage = "Por favor, digite um e-mail válido."
                                return@Button
                            }

                            if (isRegisterTab) {
                                if (passwordInput.length < 6) {
                                    errorMessage = "A senha deve ter pelo menos 6 dígitos."
                                    return@Button
                                }
                                if (passwordInput != confirmPasswordInput) {
                                    errorMessage = "As senhas não coincidem. Digite a mesma senha nos dois campos."
                                    return@Button
                                }
                            } else {
                                if (passwordInput.isBlank()) {
                                    errorMessage = "Digite sua senha para acessar sua conta."
                                    return@Button
                                }
                            }

                            errorMessage = null
                            coroutineScope.launch {
                                if (isRegisterTab) {
                                    val res = CloudSyncManager.registerAccount(
                                        context = context,
                                        email = cleanEmail,
                                        password = passwordInput,
                                        provider = selectedProvider
                                    )
                                    res.onSuccess {
                                        Toast.makeText(context, "Conta criada com sucesso! Backup em nuvem ativado.", Toast.LENGTH_LONG).show()
                                    }.onFailure { err ->
                                        errorMessage = err.message ?: "Erro ao criar conta."
                                    }
                                } else {
                                    val res = CloudSyncManager.loginAccount(
                                        context = context,
                                        email = cleanEmail,
                                        password = passwordInput
                                    )
                                    res.onSuccess {
                                        Toast.makeText(context, "Login realizado! Dados sincronizados com sucesso.", Toast.LENGTH_LONG).show()
                                    }.onFailure { err ->
                                        errorMessage = err.message ?: "Erro ao entrar na conta."
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_or_create_account_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp),
                        enabled = isFormValid && !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AUTENTICANDO...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = if (isRegisterTab) Icons.Default.Person else Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRegisterTab) "CRIAR CONTA PROTEGIDA" else "ENTRAR COM SENHA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = BlockyFontFamily
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncItemRow(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        color = CmfDark,
        fontFamily = FontFamily.Monospace
    )
}

@Composable
fun PasswordRequirementChip(
    text: String,
    isValid: Boolean
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isValid) Color(0xFFE8F5E9) else Color(0xFFF3F3F5))
            .border(
                1.dp,
                if (isValid) Color(0xFF81C784) else Color(0xFFE0E0E0),
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isValid) "✓ $text" else "• $text",
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isValid) Color(0xFF2E7D32) else CmfMutedText
            )
        }
    }
}
