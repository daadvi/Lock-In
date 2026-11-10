package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.util.AndroidPermissionAccess
import com.example.util.AppPermission
import com.example.util.PermissionAccess
import com.example.util.PermissionSnapshot
import com.example.util.PermissionStatus
import com.example.ui.tutorial.tutorialTarget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal val AppPermission.title: String get() = when (this) {
    AppPermission.ACCESSIBILITY -> "Acessibilidade"
    AppPermission.USAGE -> "Acesso ao uso dos apps"
    AppPermission.OVERLAY -> "Sobrepor a outros apps"
    AppPermission.NOTIFICATIONS -> "Notificações"
    AppPermission.BATTERY -> "Funcionamento em segundo plano"
    AppPermission.DEVICE_ADMIN -> "Proteção contra desinstalação"
    AppPermission.LOCK_SCREEN -> "Exibição na tela de bloqueio"
}

private val AppPermission.explanation: String get() = when (this) {
    AppPermission.ACCESSIBILITY -> "Permite identificar o app aberto e ler elementos das telas para aplicar bloqueios de Reels, Shorts e instalações."
    AppPermission.USAGE -> "Permite consultar o tempo de uso dos aplicativos e acompanhar seus limites."
    AppPermission.OVERLAY -> "Permite mostrar o cronômetro flutuante e os avisos de bloqueio sobre outros aplicativos."
    AppPermission.NOTIFICATIONS -> "Mostra os avisos de limite, lembretes e o status do monitoramento."
    AppPermission.BATTERY -> "Reduz as interrupções do monitoramento causadas pela economia de bateria. Pode aumentar o consumo de bateria."
    AppPermission.DEVICE_ADMIN -> "Adiciona uma etapa de confirmação antes de desinstalar o Lock In. Você pode revogar esse acesso nas configurações do Android."
    AppPermission.LOCK_SCREEN -> "Permite exibir avisos em tela cheia quando o aparelho estiver bloqueado."
}

private val AppPermission.instructions: String get() = when (this) {
    AppPermission.ACCESSIBILITY -> "Abra Acessibilidade → Apps instalados ou Serviços baixados → Lock In. Ative o serviço e confirme. Depois, volte para cá."
    AppPermission.USAGE -> "Selecione Lock In e ative Permitir acesso ao uso. Depois, volte para cá."
    AppPermission.OVERLAY -> "Selecione Lock In, se aparecer uma lista, e ative Permitir sobreposição ou Exibir sobre outros apps."
    AppPermission.NOTIFICATIONS -> "Toque em Permitir no aviso do Android. Se já recusou, abra as configurações e ative as notificações do Lock In."
    AppPermission.BATTERY -> "Na lista de otimização de bateria, escolha Todos os apps → Lock In → Não otimizar. Os nomes podem variar conforme o aparelho."
    AppPermission.DEVICE_ADMIN -> "Leia os recursos apresentados pelo Android e toque em Ativar este administrador do dispositivo."
    AppPermission.LOCK_SCREEN -> "Ative Permitir avisos em tela cheia para o Lock In e volte para cá."
}

/** One permission journey shared by first sign-in and the settings screen. */
@Composable
fun PermissionSetup(
    modifier: Modifier = Modifier,
    onContinue: (() -> Unit)? = null,
    requireEssentialPermissions: Boolean = false,
    onPermissionsChanged: (PermissionSnapshot) -> Unit = {},
    permissionAccess: PermissionAccess? = null
) {
    val context = LocalContext.current
    val access = permissionAccess ?: remember(context) { AndroidPermissionAccess(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val onChanged by rememberUpdatedState(onPermissionsChanged)
    var snapshot by remember { mutableStateOf<PermissionSnapshot?>(null) }
    var refreshing by remember { mutableStateOf(true) }
    var refreshVersion by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    var requested by rememberSaveable { mutableStateOf<String?>(null) }
    var showRestrictedHelp by rememberSaveable { mutableStateOf(false) }
    var showDeviceHelp by rememberSaveable { mutableStateOf(false) }
    var showContinueWarning by rememberSaveable { mutableStateOf(false) }
    var notificationsRequested by rememberSaveable { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshVersion++
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshVersion++
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(access, refreshVersion) {
        refreshing = true
        loadFailed = false
        try {
            val updated = withContext(Dispatchers.IO) { access.read() }
            snapshot = updated
            val previousRequest = requested?.let { AppPermission.valueOf(it) }
            if (previousRequest != null) {
                if (updated.isGranted(previousRequest)) {
                    message = "${previousRequest.title}: ativada."
                    expanded = updated.nextPending?.name
                } else {
                    message = "${previousRequest.title}: ainda não ativada. Você pode tentar novamente."
                }
                requested = null
            } else if (expanded == null) {
                expanded = updated.nextPending?.name
            }
            onChanged(updated)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            loadFailed = true
            message = "Não foi possível verificar as permissões. Toque em Verificar novamente."
        } finally {
            refreshing = false
        }
    }

    fun configure(permission: AppPermission) {
        requested = permission.name
        expanded = permission.name
        message = null
        val opened = try {
            if (permission == AppPermission.NOTIFICATIONS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !notificationsRequested && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationsRequested = true
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                true
            } else access.open(permission)
        } catch (_: Exception) { false }
        if (!opened) {
            requested = null
            message = "Não foi possível abrir essa tela. Abra Configurações → Aplicativos → Lock In no Android e tente novamente."
        }
    }

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyColumn(
            modifier = Modifier.weight(1f).testTag("permission_list"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            item(key = "progress") {
                Card(modifier = Modifier.fillMaxWidth().tutorialTarget("permissions"), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Prepare o Lock In", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Ative cada acesso no Android e volte para cá. A lista será atualizada automaticamente.", style = MaterialTheme.typography.bodyMedium)
                        val current = snapshot
                        if (current != null) {
                            Text("${current.grantedCount} de ${current.statuses.size} autorizações ativadas",
                                modifier = Modifier.testTag("permission_progress"), fontWeight = FontWeight.Bold)
                            LinearProgressIndicator(
                                progress = { current.grantedCount.toFloat() / current.statuses.size.coerceAtLeast(1) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("${current.statuses.keys.count { it.essential } - current.missingEssential.size} de ${current.statuses.keys.count { it.essential }} acessos essenciais prontos",
                                style = MaterialTheme.typography.bodySmall)
                            current.nextPending?.let { next ->
                                Text("Próxima: ${next.title}", style = MaterialTheme.typography.bodyMedium)
                                Button(onClick = { configure(next) }, enabled = !refreshing && !loadFailed,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("permission_next")) {
                                    Text("Configurar próxima permissão")
                                }
                            }
                        }
                        if (refreshing) Text("Verificando permissões…", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { refreshVersion++ }, enabled = !refreshing,
                            modifier = Modifier.testTag("permission_refresh")) { Text("Verificar novamente") }
                    }
                }
            }
            message?.let { notice ->
                item(key = "feedback") {
                    Text(notice, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("permission_feedback"))
                }
            }
            if (snapshot?.restrictedSettingsApplicable == true && snapshot?.isGranted(AppPermission.ACCESSIBILITY) == false) {
                item(key = "restricted") {
                    OutlinedCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Configurações restritas", style = MaterialTheme.typography.titleMedium)
                            Text("Instalou por APK e o Android bloqueou a acessibilidade? Libere esse acesso nas informações do Lock In.",
                                style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(onClick = { showRestrictedHelp = true }, modifier = Modifier.fillMaxWidth().testTag("restricted_settings_help")) {
                                Text("Ver como permitir")
                            }
                        }
                    }
                }
            }
            for (essential in listOf(true, false)) {
                val permissions = snapshot?.statuses?.keys.orEmpty().filter { it.essential == essential }
                if (permissions.isNotEmpty()) {
                    item(key = "heading_$essential") {
                        Text(if (essential) "Essenciais para os bloqueios" else "Recursos adicionais",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(permissions, key = { it.name }) { permission ->
                        val status = snapshot!!.statuses.getValue(permission)
                        PermissionSetupCard(permission, status, expanded == permission.name,
                            onExpand = { expanded = if (expanded == permission.name) null else permission.name },
                            onConfigure = { configure(permission) },
                            enabled = !refreshing && !loadFailed,
                            onRestrictedHelp = if (permission == AppPermission.ACCESSIBILITY && snapshot!!.restrictedSettingsApplicable) {
                                { showRestrictedHelp = true }
                            } else null)
                    }
                }
            }
            item(key = "device_help") {
                TextButton(onClick = { showDeviceHelp = !showDeviceHelp }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (showDeviceHelp) "Ocultar ajuda do meu aparelho" else "Ajuda para o meu aparelho")
                }
                if (showDeviceHelp) DeviceInstructionsCard(accentColor = MaterialTheme.colorScheme.primary)
            }
        }
        if (onContinue != null) {
            val missingEssential = snapshot?.missingEssential.orEmpty()
            val requiredForContinue = requireEssentialPermissions && missingEssential.isNotEmpty()
            if (requiredForContinue) {
                Text("O tutorial continua depois que os acessos essenciais forem ativados.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("permission_tutorial_requirement"))
            } else if (missingEssential.isNotEmpty()) {
                Text("Você pode continuar e terminar a configuração depois. Os bloqueios dependem dos acessos essenciais.",
                    style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = {
                    if (missingEssential.isEmpty()) onContinue()
                    else if (!requireEssentialPermissions) showContinueWarning = true
                },
                enabled = snapshot != null && !refreshing && !loadFailed && !requiredForContinue,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("onboarding_permissions_next_button")
            ) { Text("Continuar para o nível de bloqueio") }
        }
    }

    if (showRestrictedHelp) {
        AlertDialog(
            onDismissRequest = { showRestrictedHelp = false },
            title = { Text("Permitir configurações restritas") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("1. Abra as informações do Lock In pelo botão abaixo.")
                    Text("2. Toque nos três pontos (⋮) e em Permitir configurações restritas. Confirme com a senha ou biometria, se solicitado.")
                    Text("3. Volte ao Lock In e toque em Ativar acessibilidade.")
                    Text("Essa opção pode não aparecer em todos os aparelhos. Se não aparecer, tente ativar a acessibilidade diretamente.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (!access.openAppDetails()) {
                        message = "Não foi possível abrir as informações do app. Acesse Configurações → Aplicativos → Lock In."
                        showRestrictedHelp = false
                    }
                }, modifier = Modifier.testTag("restricted_open_details")) { Text("Informações do app") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestrictedHelp = false
                    configure(AppPermission.ACCESSIBILITY)
                }, modifier = Modifier.testTag("restricted_open_accessibility")) { Text("Ativar acessibilidade") }
            }
        )
    }
    if (showContinueWarning) {
        AlertDialog(
            onDismissRequest = { showContinueWarning = false },
            title = { Text("Continuar com funções limitadas?") },
            text = { Text("Ainda faltam: ${snapshot?.missingEssential.orEmpty().joinToString { it.title }}. Você poderá ativar esses acessos em Configurações → Permissões do Lock In.") },
            confirmButton = {
                TextButton(onClick = { showContinueWarning = false; onContinue?.invoke() },
                    modifier = Modifier.testTag("permission_continue_anyway")) { Text("Continuar mesmo assim") }
            },
            dismissButton = { TextButton(onClick = { showContinueWarning = false }) { Text("Configurar agora") } }
        )
    }
}

@Composable
private fun PermissionSetupCard(
    permission: AppPermission,
    status: PermissionStatus,
    expanded: Boolean,
    enabled: Boolean,
    onExpand: () -> Unit,
    onConfigure: () -> Unit,
    onRestrictedHelp: (() -> Unit)?
) {
    val granted = status == PermissionStatus.GRANTED
    Card(
        modifier = Modifier.fillMaxWidth().testTag("permission_${permission.name}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onExpand).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(permission.title, style = MaterialTheme.typography.titleMedium)
                Text(when (status) {
                    PermissionStatus.GRANTED -> "Ativada"
                    PermissionStatus.PENDING -> "Pendente"
                    PermissionStatus.UNKNOWN -> "Não foi possível verificar"
                }, modifier = Modifier.testTag("permission_status_${permission.name}"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            if (granted) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
        }
        if (expanded) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(permission.explanation, style = MaterialTheme.typography.bodyMedium)
                Text(permission.instructions, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onConfigure, enabled = enabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("permission_open_${permission.name}")) {
                    Text(if (granted) "Gerenciar no Android" else "Configurar ${permission.title.lowercase()}")
                }
                if (onRestrictedHelp != null && !granted) {
                    TextButton(onClick = onRestrictedHelp) { Text("O Android mostrou “Configuração restrita”?") }
                }
            }
        }
    }
}
