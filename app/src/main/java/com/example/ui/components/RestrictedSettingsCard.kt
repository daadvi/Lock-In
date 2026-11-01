package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlockyFontFamily
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import com.example.ui.theme.LocalIsDarkTheme
import com.example.util.RestrictedSettingsHelper

@Composable
fun RestrictedSettingsCard(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val isApplicable = RestrictedSettingsHelper.isRestrictedSettingsApplicable()

    val cardBg = if (isDark) Color(0xFF18181B) else CmfWhite
    val borderCol = if (isDark) Color(0xFF27272A) else CmfBorder
    val textColor = if (isDark) Color.White else CmfDark
    val subtextColor = if (isDark) Color(0xFFD1D5DB) else Color(0xFF52525B)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.5.dp, if (isApplicable) accentColor.copy(alpha = 0.6f) else borderCol, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "CONFIGURAÇÕES RESTRITAS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = BlockyFontFamily,
                            color = textColor,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (RestrictedSettingsHelper.isXiaomiDevice()) "Xiaomi • MIUI / HyperOS • Android 13+" else "Android 13+ (API 33+) • Proteção do Sistema",
                            fontSize = 10.sp,
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontFamily = BlockyFontFamily
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Se ao tentar ativar a Acessibilidade o Android exibir o aviso \"Configuração restrita - Para sua segurança, esta configuração não está disponível no momento\", siga os 4 passos:",
                fontSize = 11.5.sp,
                color = subtextColor,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Step 1
            RestrictedStepItem(
                stepNumber = "1",
                title = "Toque em 'Abrir Configurações do App' abaixo",
                subtitle = "Isso abrirá a tela oficial de Informações do App do Lock In no Android.",
                accentColor = accentColor,
                textColor = textColor,
                subtextColor = subtextColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Step 2
            RestrictedStepItem(
                stepNumber = "2",
                title = "Toque nos 3 Pontinhos (⋮) no topo direito",
                subtitle = "No canto superior direito da tela de informações do aplicativo.",
                accentColor = accentColor,
                textColor = textColor,
                subtextColor = subtextColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Step 3
            RestrictedStepItem(
                stepNumber = "3",
                title = "Selecione 'Permitir configurações restritas'",
                subtitle = "O sistema solicitará sua biometria ou senha do aparelho para desbloquear.",
                accentColor = accentColor,
                textColor = textColor,
                subtextColor = subtextColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Step 4
            RestrictedStepItem(
                stepNumber = "4",
                title = "Volte ao Lock In e ative a Acessibilidade",
                subtitle = "O botão ficará 100% destravado e funcional sem bloqueios do sistema.",
                accentColor = accentColor,
                textColor = textColor,
                subtextColor = subtextColor
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        RestrictedSettingsHelper.openAppDetailsSettings(context)
                    },
                    modifier = Modifier.weight(1f).height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ABRIR CONFIG. DO APP",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BlockyFontFamily,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = {
                        RestrictedSettingsHelper.openAccessibilitySettings(context)
                    },
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "ACESSABILIDADE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BlockyFontFamily,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun RestrictedStepItem(
    stepNumber: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    textColor: Color,
    subtextColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(accentColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = subtextColor,
                lineHeight = 14.sp
            )
        }
    }
}
