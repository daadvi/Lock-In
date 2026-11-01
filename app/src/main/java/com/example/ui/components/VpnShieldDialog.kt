package com.example.ui.components

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.service.BankingSecurityHelper
import com.example.service.LockInVpnManager
import com.example.ui.theme.BlockyFontFamily
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import com.example.ui.theme.LocalIsDarkTheme

@Composable
fun VpnShieldDialog(
    accentColor: Color,
    onManageWebsites: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDarkTheme = LocalIsDarkTheme.current

    val isVpnEnabled by LockInVpnManager.isVpnEnabled.collectAsState()
    val isVpnRunning by LockInVpnManager.isVpnRunning.collectAsState()
    val blockedCount by LockInVpnManager.blockedDnsCount.collectAsState()
    val depletedApps by LockInVpnManager.depletedPackages.collectAsState()

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            LockInVpnManager.setVpnEnabled(context, true)
        }
    }

    val dialogBg = if (isDarkTheme) Color(0xFF16161A) else CmfWhite
    val cardBg = if (isDarkTheme) Color(0xFF202026) else CmfLightGray
    val borderColor = if (isDarkTheme) Color(0xFF2E2E38) else CmfBorder
    val primaryText = if (isDarkTheme) Color.White else CmfDark

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(dialogBg)
                .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    if (isVpnRunning) Color(0xFF22C55E).copy(alpha = 0.15f) else accentColor.copy(alpha = 0.15f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isVpnRunning) Color(0xFF22C55E) else accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ESCUDO DE REDE",
                                color = primaryText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = BlockyFontFamily
                            )
                            Text(
                                text = "Firewall & DNS Local",
                                color = CmfMutedText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_vpn_shield_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Master Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isVpnRunning) "Escudo Ativo e Protegendo" else "Ativar Escudo de Rede",
                            color = primaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = if (isVpnRunning)
                                "Bloqueio de domínios e corte de tráfego ativos"
                            else
                                "Filtra domínios de distração e bloqueia redes de apps esgotados",
                            color = CmfMutedText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Switch(
                        checked = isVpnEnabled,
                        onCheckedChange = { enable ->
                            if (enable) {
                                val prepareIntent = VpnService.prepare(context)
                                if (prepareIntent != null) {
                                    vpnLauncher.launch(prepareIntent)
                                } else {
                                    LockInVpnManager.setVpnEnabled(context, true)
                                }
                            } else {
                                LockInVpnManager.setVpnEnabled(context, false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = if (isVpnRunning) Color(0xFF22C55E) else accentColor,
                            uncheckedThumbColor = primaryText,
                            uncheckedTrackColor = borderColor
                        ),
                        modifier = Modifier.testTag("vpn_shield_master_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real-time Metrics Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardBg)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "BLOQUEIOS HOJE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CmfMutedText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$blockedCount",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = BlockyFontFamily,
                                color = if (isVpnRunning) Color(0xFF22C55E) else primaryText
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardBg)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "APPS ESGOTADOS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CmfMutedText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${depletedApps.size}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = BlockyFontFamily,
                                color = accentColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Banking Safety Guarantee Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDarkTheme) Color(0xFF132219) else Color(0xFFE8F5E9))
                        .border(1.dp, Color(0xFF22C55E).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = Color(0xFF22C55E),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ISENÇÃO BANCÁRIA TOTAL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF22C55E)
                            )
                            Text(
                                text = "50+ bancos (Nubank, Itaú, BB, Caixa, Inter...) nunca passam pelo túnel VPN. Zero interferência.",
                                fontSize = 10.sp,
                                color = if (isDarkTheme) Color(0xFFC8E6C9) else Color(0xFF1B5E20)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Local Device Privacy Notice
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardBg)
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "100% no seu aparelho: nenhum dado, tráfego ou navegação sai do seu celular.",
                            fontSize = 11.sp,
                            color = primaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onManageWebsites()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Gerenciar Sites",
                            color = accentColor,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = primaryText)
                    ) {
                        Text(
                            text = "Entendido",
                            color = if (isDarkTheme) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
