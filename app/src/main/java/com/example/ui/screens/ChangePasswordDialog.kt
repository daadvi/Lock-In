package com.example.ui.screens

import android.app.KeyguardManager
import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.ManualEntryTextField
import com.example.util.CryptoPasswordHelper
import com.example.util.PasswordValidator
import kotlinx.coroutines.delay

/** Recovery authenticates the device owner through Android; no locally displayed email codes. */
@Composable
internal fun ChangePasswordDialog(
    currentPassword: String,
    accentColor: Color,
    onSaveNewPassword: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dialogHeight = 640.dp
    val keyguard = remember(context) { context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }
    var recovering by rememberSaveable { mutableStateOf(false) }
    var authorizedUntil by rememberSaveable { mutableLongStateOf(0L) }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(authorizedUntil) {
        if (authorizedUntil > 0L) {
            delay((authorizedUntil - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
            authorizedUntil = 0L
            error = "A confirmação expirou. Confirme sua identidade novamente."
        }
    }
    fun isAuthorized(): Boolean = authorizedUntil - SystemClock.elapsedRealtime() in 1L..120_000L &&
        (!recovering || keyguard.isDeviceSecure)
    Dialog(onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Surface(modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp).heightIn(max = dialogHeight).testTag("password_dialog"),
                shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (recovering) "RECUPERAR SENHA" else "SENHA DE CONFIANÇA", style = MaterialTheme.typography.titleMedium)
                if (recovering) {
                    Text("Confirme o PIN, padrão ou senha de bloqueio do Android para definir uma nova senha de confiança. Nenhum código é enviado por e-mail.",
                        style = MaterialTheme.typography.bodyMedium)
                    if (authorizedUntil > SystemClock.elapsedRealtime()) {
                        Text("Identidade confirmada. Defina e salve sua nova senha em até 2 minutos.",
                            color = accentColor, modifier = Modifier.testTag("password_recovery_verified"))
                    } else {
                        DeviceCredentialConfirmationButton(
                            keyguard = keyguard,
                            onStarting = { error = null; authorizedUntil = 0L },
                            onResult = { confirmed ->
                                if (confirmed) {
                                    authorizedUntil = SystemClock.elapsedRealtime() + 120_000L
                                    error = null
                                } else {
                                    authorizedUntil = 0L
                                    error = "Verificação cancelada ou não concluída. Sua senha não foi alterada."
                                }
                            },
                            onError = { error = it }
                        )
                    }
                    TextButton(onClick = { recovering = false; authorizedUntil = 0L; error = null }) { Text("Voltar para senha atual") }
                } else if (!isAuthorized()) {
                    ManualEntryTextField(oldPassword, { oldPassword = it; error = null },
                        modifier = Modifier.fillMaxWidth().testTag("password_current"), label = { Text("Senha atual") },
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
                    Button(onClick = {
                        if (currentPassword.isNotEmpty() && CryptoPasswordHelper.verifyPassword(oldPassword, currentPassword)) {
                            oldPassword = ""
                            authorizedUntil = SystemClock.elapsedRealtime() + 120_000L
                            error = null
                        } else error = "Senha atual incorreta."
                    }, enabled = oldPassword.isNotEmpty() && currentPassword.isNotEmpty() &&
                        CryptoPasswordHelper.verifyPassword(oldPassword, currentPassword),
                        modifier = Modifier.fillMaxWidth().testTag("password_current_verify")) { Text("Verificar senha atual") }
                    TextButton(onClick = { recovering = true; authorizedUntil = 0L; oldPassword = ""; error = null },
                        modifier = Modifier.testTag("password_forgot")) { Text("Esqueci minha senha") }
                }
                if (isAuthorized()) {
                    Text(if (recovering) "Identidade confirmada. Defina sua nova senha em até 2 minutos." else "Senha atual confirmada. Defina sua nova senha em até 2 minutos.",
                        style = MaterialTheme.typography.bodySmall, color = accentColor, modifier = Modifier.testTag("password_authorized"))
                    Text("Use 12 ou mais caracteres, uma letra maiúscula e um símbolo. A senha diferencia maiúsculas e minúsculas.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ManualEntryTextField(newPassword, { newPassword = it; error = null },
                        modifier = Modifier.fillMaxWidth().testTag("password_new"), label = { Text("Nova senha") },
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
                    ManualEntryTextField(confirmation, { confirmation = it; error = null },
                        modifier = Modifier.fillMaxWidth().testTag("password_confirm"), label = { Text("Confirmar nova senha") },
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("password_error")) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    if (isAuthorized()) {
                        Button(onClick = {
                            if (!isAuthorized()) { error = "Confirme sua identidade antes de salvar."; return@Button }
                            val validationError = PasswordValidator.getValidationError(newPassword)
                            if (validationError != null) { error = validationError; return@Button }
                            if (newPassword != confirmation) { error = "As senhas não coincidem."; return@Button }
                            authorizedUntil = 0L
                            onSaveNewPassword(newPassword)
                        }, enabled = PasswordValidator.isValid(newPassword) && newPassword == confirmation,
                            modifier = Modifier.weight(1f).testTag("password_save")) { Text("Salvar") }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun DeviceCredentialConfirmationButton(
    keyguard: KeyguardManager,
    onStarting: () -> Unit,
    onResult: (Boolean) -> Unit,
    onError: (String) -> Unit
) {
    var awaitingCredential by rememberSaveable { mutableStateOf(false) }
    val credentialLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        awaitingCredential = false
        onResult(result.resultCode == android.app.Activity.RESULT_OK && keyguard.isDeviceSecure)
    }
    Button(onClick = {
        onStarting()
        if (!keyguard.isDeviceSecure) {
            onError("Este aparelho não tem PIN, padrão ou senha de bloqueio configurado. A recuperação não pode ser autorizada.")
            return@Button
        }
        @Suppress("DEPRECATION")
        val intent = keyguard.createConfirmDeviceCredentialIntent("Recuperar senha do Lock In", "Confirme sua identidade para alterar a senha de confiança.")
        if (intent == null) {
            onError("A verificação do Android não está disponível neste aparelho. Sua senha permanece inalterada.")
            return@Button
        }
        try {
            awaitingCredential = true
            credentialLauncher.launch(intent)
        } catch (_: Exception) {
            awaitingCredential = false
            onError("Não foi possível abrir a verificação do Android. Tente novamente.")
        }
    }, enabled = !awaitingCredential, modifier = Modifier.fillMaxWidth().testTag("password_recovery_verify")) {
        Text(if (awaitingCredential) "Aguardando Android…" else "Confirmar identidade no Android")
    }
}
