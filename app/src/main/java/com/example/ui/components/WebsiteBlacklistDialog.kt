package com.example.ui.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.service.LockInVpnManager
import com.example.service.WebsiteBlacklistManager
import com.example.ui.theme.BlockyFontFamily
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import com.example.ui.theme.LocalIsDarkTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WebsiteBlacklistDialog(
    accentColor: Color,
    trustPassword: String,
    onOpenVpnShield: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDarkTheme = LocalIsDarkTheme.current
    val blacklistedWebsites by WebsiteBlacklistManager.blacklistedWebsites.collectAsState()
    val isEnabled by WebsiteBlacklistManager.isBlacklistEnabled.collectAsState()
    val isVpnRunning by LockInVpnManager.isVpnRunning.collectAsState()

    var newSiteInput by remember { mutableStateOf("") }
    var itemToRemove by remember { mutableStateOf<String?>(null) }
    var passwordConfirmInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val dialogBg = if (isDarkTheme) Color(0xFF16161A) else CmfWhite
    val cardBg = if (isDarkTheme) Color(0xFF202026) else CmfLightGray
    val borderColor = if (isDarkTheme) Color(0xFF2E2E38) else CmfBorder
    val primaryText = if (isDarkTheme) Color.White else CmfDark

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(dialogBg)
                .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(accentColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "BLOQUEIO DE SITES",
                                color = primaryText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = BlockyFontFamily
                            )
                            Text(
                                text = "Navegadores & Rede",
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
                            .testTag("close_website_blacklist_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Master Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardBg)
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ativar bloqueio de sites",
                            color = primaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = if (isEnabled) "Sites da lista são bloqueados em navegadores" else "Bloqueio de sites desativado",
                            color = CmfMutedText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { WebsiteBlacklistManager.setBlacklistEnabled(context, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = primaryText,
                            uncheckedTrackColor = borderColor
                        )
                    )
                }

                // VPN Shield Integration Banner
                if (onOpenVpnShield != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isVpnRunning) Color(0xFF22C55E).copy(alpha = 0.12f) else accentColor.copy(alpha = 0.1f))
                            .clickable { onOpenVpnShield() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isVpnRunning) Color(0xFF22C55E) else accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isVpnRunning) "Escudo DNS Ativo (Bloqueio sem brechas)" else "Reforçar com Escudo DNS Local",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isVpnRunning) Color(0xFF22C55E) else accentColor
                            )
                        }
                        Text(
                            text = "CONFIGURAR >",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isVpnRunning) Color(0xFF22C55E) else accentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Add website input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newSiteInput,
                        onValueChange = { newSiteInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("website_blacklist_input"),
                        placeholder = {
                            Text("Ex: bet365.com, tiktok.com, kwai.com...", fontSize = 13.sp, color = CmfMutedText)
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = primaryText,
                            unfocusedTextColor = primaryText,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = borderColor
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (newSiteInput.isNotBlank()) {
                                    WebsiteBlacklistManager.addWebsite(context, newSiteInput)
                                    newSiteInput = ""
                                }
                            }
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newSiteInput.isNotBlank()) {
                                WebsiteBlacklistManager.addWebsite(context, newSiteInput)
                                newSiteInput = ""
                            }
                        },
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("add_website_blacklist_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Adicionar", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Popular suggestions
                Text(
                    text = "SUGESTÕES RÁPIDAS:",
                    color = CmfMutedText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BlockyFontFamily
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WebsiteBlacklistManager.POPULAR_SUGGESTIONS.take(6).forEach { suggestion ->
                        val alreadyAdded = blacklistedWebsites.contains(WebsiteBlacklistManager.cleanDomain(suggestion))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (alreadyAdded) accentColor.copy(alpha = 0.2f) else cardBg)
                                .border(1.dp, if (alreadyAdded) accentColor else borderColor, RoundedCornerShape(8.dp))
                                .clickable {
                                    if (!alreadyAdded) {
                                        WebsiteBlacklistManager.addWebsite(context, suggestion)
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "+ $suggestion",
                                color = if (alreadyAdded) accentColor else primaryText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // List of blacklisted websites
                Text(
                    text = "SITES NA LISTA DE BLOQUEIO (${blacklistedWebsites.size}):",
                    color = CmfMutedText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BlockyFontFamily
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (blacklistedWebsites.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(cardBg)
                            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum site bloqueado no momento.\nAdicione domínios acima para bloquear o acesso.",
                            color = CmfMutedText,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(blacklistedWebsites, key = { it }) { site ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(cardBg)
                                    .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = site,
                                        color = primaryText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (trustPassword.isNotBlank()) {
                                            itemToRemove = site
                                            passwordConfirmInput = ""
                                            passwordError = null
                                        } else {
                                            WebsiteBlacklistManager.removeWebsite(context, site)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("delete_site_${site}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remover",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Password confirmation sub-dialog when removing an item
                AnimatedVisibility(visible = itemToRemove != null) {
                    val toRemove = itemToRemove ?: return@AnimatedVisibility
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDarkTheme) Color(0xFF261D14) else Color(0xFFFFF3E0))
                            .border(1.dp, Color(0xFFFF9800), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Confirmar remoção de '$toRemove'",
                            color = Color(0xFFFF9800),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "Digite sua senha de confiança para desbloquear este site:",
                            color = primaryText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ManualEntryTextField(
                            value = passwordConfirmInput,
                            onValueChange = {
                                passwordConfirmInput = it
                                passwordError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Senha de Confiança", fontSize = 12.sp) },
                            singleLine = true,
                            isError = passwordError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = primaryText,
                                unfocusedTextColor = primaryText
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        if (passwordError != null) {
                            Text(
                                text = passwordError!!,
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { itemToRemove = null },
                                colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Cancelar", color = primaryText, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (passwordConfirmInput == trustPassword) {
                                        WebsiteBlacklistManager.removeWebsite(context, toRemove)
                                        itemToRemove = null
                                    } else {
                                        passwordError = "Senha incorreta!"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Remover", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
