package com.example.ui.screens
import com.example.ui.theme.BlockyFontFamily

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.window.Dialog
import com.example.ui.components.RestrictedSettingsCard
import com.example.util.RestrictedSettingsHelper
import com.example.ui.components.DotMatrixPattern
import com.example.ui.components.NothingCard
import com.example.ui.components.XiaomiCompatibilityCard
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import com.example.ui.theme.LocalCustomTextColor
import com.example.ui.theme.LocalIsDarkTheme
import androidx.compose.material.icons.filled.ScreenLockPortrait

@Composable
fun PermissionsScreen(
    onBack: () -> Unit
) {
    val accentColor = LocalCustomTextColor.current
    val context = LocalContext.current

    var hasOverlayPermission by remember { mutableStateOf(false) }
    var hasUsageStatsPermission by remember { mutableStateOf(false) }
    var hasLockScreenPermission by remember { mutableStateOf(false) }
    var hasDeviceAdminPermission by remember { mutableStateOf(false) }
    var showRestrictedSettingsDialog by remember { mutableStateOf(false) }

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

        if (hasOverlayPermission) {
            try {
                com.example.service.FloatingIslandService.ensureStarted(context)
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        checkPermissions()
    }

    val isDarkTheme = LocalIsDarkTheme.current
    val screenBg = if (isDarkTheme) Color(0xFF101012) else CmfWhite
    val cardBg = if (isDarkTheme) Color(0xFF18181B) else CmfWhite
    val borderCol = if (isDarkTheme) Color(0xFF27272A) else CmfBorder
    val textColor = if (isDarkTheme) Color.White else CmfDark
    val subtextColor = if (isDarkTheme) Color(0xFFA1A1AA) else CmfMutedText

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
                            tint = textColor
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PERMISSÕES DO SISTEMA",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        color = textColor,
                        letterSpacing = 1.sp
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        NothingCard(
                            title = "INTEGRAÇÃO DO ANDROID",
                            badge = "SISTEMA"
                        ) {
                            Text(
                                text = "Para monitorar com precisão o tempo gasto nos apps, exibir a ilha flutuante e identificar vídeos curtos (Shorts/Reels), o Lock in necessita das seguintes autorizações do Android:",
                                fontSize = 12.sp,
                                color = textColor,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Xiaomi / Redmi Dedicated Compatibility Card
                    item {
                        XiaomiCompatibilityCard(
                            accentColor = accentColor
                        )
                    }

                    // 1. Accessibility Service
                    item {
                        PermissionItemCard(
                            title = "Serviço de Acessibilidade",
                            description = "Permite identificar vídeos curtos/longos (Shorts, Reels), Picture-in-Picture e monitorar a Google Play Store/instaladores para bloquear instalações proibidas em tempo real.\n\nInstruções de Ativação:\n1. Toque em 'Configurar Acessibilidade' abaixo.\n2. Procure por 'Apps Instalados' ou 'Serviços Baixados'.\n3. Toque em 'Lock In' (ou 'Lock In - Bloqueador de Instalação').\n4. Ative o serviço e confirme.",
                            icon = Icons.Default.Accessibility,
                            isGranted = true, // We will give button to open settings
                            accentColor = accentColor,
                            buttonText = "Configurar Acessibilidade",
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            }
                        )
                    }

                    // 2. Overlay / Floating Island
                    item {
                        PermissionItemCard(
                            title = "Sobrepor a Outros Apps (Ilha Flutuante)",
                            description = "Necessário para manter a cápsula flutuante visível no canto da tela enquanto você utiliza seus aplicativos.",
                            icon = Icons.Default.Layers,
                            isGranted = hasOverlayPermission,
                            accentColor = accentColor,
                            buttonText = if (hasOverlayPermission) "Permissão Concedida" else "Conceder Sobreposição",
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }

                    // 3. Usage Stats
                    item {
                        PermissionItemCard(
                            title = "Acesso aos Dados de Uso",
                            description = "Permite ao Android reportar o tempo ativo de aplicativos em segundo plano para auditoria dos limites.",
                            icon = Icons.Default.QueryStats,
                            isGranted = hasUsageStatsPermission,
                            accentColor = accentColor,
                            buttonText = if (hasUsageStatsPermission) "Permissão Concedida" else "Conceder Acesso a Dados",
                            onClick = {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                context.startActivity(intent)
                            }
                        )
                    }

                    // 4. Lock Screen Appearance
                    item {
                        PermissionItemCard(
                            title = "Exibir na Tela de Bloqueio",
                            description = "Permite exibir telas conscientes de bloqueio mesmo quando o aparelho estiver com a tela bloqueada ou em descanso (Full Screen Intent / Sobreposição na tela de bloqueio).",
                            icon = Icons.Default.Security,
                            isGranted = hasLockScreenPermission,
                            accentColor = accentColor,
                            buttonText = if (hasLockScreenPermission) "Permissão Concedida" else "Solicitar Acesso na Tela de Bloqueio",
                            onClick = {
                                com.example.service.LockScreenPermissionHelper.requestLockScreenPermission(context)
                            }
                        )
                    }

                    // 5. Anti-Uninstall Device Administrator Protection
                    item {
                        PermissionItemCard(
                            title = "Proteção Contra Desinstalação (Administrador do Dispositivo)",
                            description = "Impede que o Lock In seja desinstalado sem passar pelo protocolo de segurança (5 senhas consecutivas e confirmação por frase manual).",
                            icon = Icons.Default.Security,
                            isGranted = hasDeviceAdminPermission,
                            accentColor = accentColor,
                            buttonText = if (hasDeviceAdminPermission) "Proteção Blindada Ativa" else "Ativar Administrador do Dispositivo",
                            onClick = {
                                com.example.service.DeviceAdminManager.requestDeviceAdmin(context)
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }

        if (showRestrictedSettingsDialog) {
            Dialog(onDismissRequest = { showRestrictedSettingsDialog = false }) {
                RestrictedSettingsCard(
                    accentColor = accentColor,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

@Composable
fun PermissionItemCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    accentColor: Color,
    buttonText: String,
    onClick: () -> Unit
) {
    val isDarkTheme = LocalIsDarkTheme.current
    val cardBg = if (isDarkTheme) Color(0xFF18181B) else CmfWhite
    val cardBorder = if (isGranted) accentColor.copy(alpha = 0.5f) else (if (isDarkTheme) Color(0xFF27272A) else CmfBorder)
    val titleColor = if (isDarkTheme) Color.White else CmfDark
    val descColor = if (isDarkTheme) Color(0xFFA1A1AA) else CmfMutedText
    val btnGrantedBg = if (isDarkTheme) Color(0xFF27272A) else CmfLightGray
    val btnGrantedText = if (isDarkTheme) Color.White else CmfDark

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = description,
            fontSize = 11.sp,
            color = descColor,
            lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGranted) btnGrantedBg else accentColor,
                contentColor = if (isGranted) btnGrantedText else Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = buttonText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
