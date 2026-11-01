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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.BlockyFontFamily
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.LocalIsDarkTheme
import com.example.util.DeviceInstructionHelper

/**
 * Automatically detects the user's phone manufacturer (Samsung, Xiaomi HyperOS, Xiaomi MIUI,
 * Motorola, Realme/Oppo/OnePlus, or Generic) and displays ONLY the exact step-by-step
 * instructions for that specific device.
 */
@Composable
fun DeviceInstructionsCard(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val profile = remember { DeviceInstructionHelper.getProfile() }

    val cardBg = if (isDark) Color(0xFF16161B) else Color.White
    val borderColor = if (isDark) Color(0xFF2E2E36) else CmfBorder
    val textColor = if (isDark) Color.White else CmfDark
    val subtextColor = if (isDark) Color(0xFFA1A1AA) else CmfMutedText

    NothingCard(
        modifier = modifier.fillMaxWidth().testTag("device_instructions_card"),
        title = "CONFIGURAÇÃO DO SEU DISPOSITIVO",
        badge = profile.family.badge,
        backgroundColor = cardBg,
        borderColor = accentColor.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header showing detected model
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(accentColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.deviceName,
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "Instruções detectadas especificamente para seu modelo",
                        color = subtextColor,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            // SECTION 1: Como manter o app ativo na aba de recentes
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color(0xFF1F1F26) else CmfLightGray, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = profile.recentAppsTitle,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BlockyFontFamily
                    )
                }

                Text(
                    text = "Instruções para fixar o Lock In e impedir o encerramento:",
                    color = subtextColor,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif
                )

                profile.recentAppsSteps.forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${index + 1}. ",
                            color = accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = step,
                            color = textColor,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // SECTION 2: Início Automático (Auto Start)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color(0xFF1F1F26) else CmfLightGray, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = profile.autoStartTitle,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BlockyFontFamily
                    )
                }

                profile.autoStartSections.forEach { section ->
                    Text(
                        text = section.title,
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    section.steps.forEachIndexed { idx, step ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "${idx + 1}. ",
                                color = accentColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = step,
                                color = textColor,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.SansSerif,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Essential battery tip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.08f))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = profile.essentialBatteryTitle,
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = profile.essentialBatteryTip,
                            color = textColor,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Quick button to open device settings directly
            Button(
                onClick = {
                    DeviceInstructionHelper.openDeviceOptimizationSettings(context)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("open_device_settings_btn"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ABRIR CONFIGURAÇÕES DO APARELHO",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BlockyFontFamily
                )
            }
        }
    }
}
