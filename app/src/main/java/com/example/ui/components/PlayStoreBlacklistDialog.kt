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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.LockLevel
import com.example.service.PlayStoreBlacklistManager
import com.example.ui.theme.BlockyFontFamily
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayStoreBlacklistDialog(
    accentColor: Color,
    trustPassword: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val blacklistedApps by PlayStoreBlacklistManager.blacklistedApps.collectAsState()
    val isEnabled by PlayStoreBlacklistManager.isBlacklistEnabled.collectAsState()

    val userPrefs = remember { com.example.LockInApplication.instance.userPreferences }
    val isAbsoluteInstallBlock by userPrefs.isAbsoluteInstallBlock.collectAsState()
    val currentLockLevel by userPrefs.lockLevel.collectAsState()
    // No nível Sonho do Dev o bloqueio absoluto nunca pode ser desativado: a lista é permanente.
    val isAbsoluteBlockPermanent = currentLockLevel == LockLevel.DEV_DREAM
    var showAbsoluteBlockAlert by remember { mutableStateOf(false) }

    var newAppNameInput by remember { mutableStateOf("") }
    var itemToRemove by remember { mutableStateOf<String?>(null) }
    var passwordConfirmInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CmfWhite)
                .border(1.5.dp, CmfDark, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "BLACKLIST DA PLAY STORE",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = BlockyFontFamily,
                            color = CmfDark,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "BLOQUEIA A INSTALAÇÃO DE NOVOS APPS",
                            fontSize = 9.sp,
                            fontFamily = BlockyFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = CmfDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle Master Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isEnabled) accentColor.copy(alpha = 0.08f) else Color(0xFFF7F7F9))
                        .border(1.dp, if (isEnabled) accentColor.copy(alpha = 0.3f) else CmfBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PROTEÇÃO DA PLAY STORE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = BlockyFontFamily,
                            color = CmfDark
                        )
                        Text(
                            text = if (isEnabled) "Bloqueio ativo para apps na blacklist." else "Proteção temporariamente desativada.",
                            fontSize = 10.sp,
                            color = CmfMutedText
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { checked ->
                            PlayStoreBlacklistManager.setBlacklistEnabled(context, checked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentColor
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE3F2FD))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Proteção inteligente com tolerância a erros de digitação (ex: tictoc, freefair, insta). Bloqueia a instalação e o uso em tempo real.",
                        fontSize = 10.sp,
                        color = Color(0xFF0D47A1),
                        lineHeight = 13.sp
                    )
                }

                if (isAbsoluteInstallBlock) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFEBEE))
                            .border(1.dp, Color(0xFFEF5350), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAbsoluteBlockPermanent)
                                "BLOQUEIO ABSOLUTO PERMANENTE (NÍVEL SONHO DO DEV): é proibido remover apps desta lista. Nenhum app que você adicionar pode ser retirado neste nível."
                            else
                                "BLOQUEIO ABSOLUTO ATIVO: É proibido remover apps desta lista sem antes desativar o modo nas Configurações.",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828),
                            lineHeight = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Input to add a new app
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newAppNameInput,
                        onValueChange = { newAppNameInput = it },
                        placeholder = { Text("Nome do app (ex: TikTok, Kwai)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (newAppNameInput.isNotBlank()) {
                                PlayStoreBlacklistManager.addApp(context, newAppNameInput)
                                newAppNameInput = ""
                            }
                        }),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("blacklist_app_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            if (newAppNameInput.isNotBlank()) {
                                PlayStoreBlacklistManager.addApp(context, newAppNameInput)
                                newAppNameInput = ""
                            }
                        },
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("add_blacklist_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp),
                        enabled = newAppNameInput.isNotBlank()
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Adicionar", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "AO BLOQUEAR UM APP, O SITE OFICIAL DELE TAMBÉM FICA BLOQUEADO EM TODOS OS NAVEGADORES.",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BlockyFontFamily,
                    color = accentColor
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Popular suggestions chips
                Text(
                    text = "SUGESTÕES RÁPIDAS PARA BLOQUEAR:",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BlockyFontFamily,
                    color = CmfMutedText
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlayStoreBlacklistManager.POPULAR_SUGGESTIONS.take(6).forEach { suggestion ->
                        val isAlreadyAdded = blacklistedApps.any { it.equals(suggestion, ignoreCase = true) }
                        if (!isAlreadyAdded) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CmfLightGray)
                                    .clickable {
                                        PlayStoreBlacklistManager.addApp(context, suggestion)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "+ $suggestion",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = CmfDark
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Blacklisted Apps List
                Text(
                    text = "APPS PROIBIDOS DE INSTALAÇÃO (${blacklistedApps.size}):",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = BlockyFontFamily,
                    color = CmfDark
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (blacklistedApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF7F7F9))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum app na Blacklist. Adicione acima os aplicativos da Play Store que você quer impedir de baixar!",
                            fontSize = 11.sp,
                            color = CmfMutedText,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(blacklistedApps) { appName ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF4F4F6))
                                    .border(1.dp, CmfBorder, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = appName.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = BlockyFontFamily,
                                        color = CmfDark
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (isAbsoluteInstallBlock) {
                                            showAbsoluteBlockAlert = true
                                        } else {
                                            itemToRemove = appName
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isAbsoluteInstallBlock) Icons.Default.Lock else Icons.Default.Close,
                                        contentDescription = if (isAbsoluteInstallBlock) "Bloqueado pelo Modo Absoluto" else "Remover",
                                        tint = if (isAbsoluteInstallBlock) Color(0xFFEF4444) else CmfMutedText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CmfDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("CONCLUÍDO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    // Alert shown when user tries to remove an app while Absolute Install Block is active
    if (showAbsoluteBlockAlert) {
        Dialog(onDismissRequest = { showAbsoluteBlockAlert = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CmfWhite)
                    .border(2.dp, Color(0xFFEF4444), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "BLOQUEIO ABSOLUTO ATIVO",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        color = Color(0xFFEF4444),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isAbsoluteBlockPermanent)
                            "Você está no nível Sonho do Dev. O Bloqueio Absoluto não pode ser desativado neste nível e nenhum aplicativo pode ser removido da lista de bloqueio de instalação."
                        else
                            "A opção 'Bloqueio de instalação absoluta' está ativada nas Configurações.\n\nNão há forma de tirar nenhum app da lista de bloqueio de instalação enquanto este modo estiver ativo.\n\nPara desbloquear remoções, você deve desativar esse modo nas Configurações cumprindo o desafio de 3 etapas com o código enviado ao seu e-mail.",
                        fontSize = 11.sp,
                        color = CmfDark,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showAbsoluteBlockAlert = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("ENTENDIDO", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // High-friction security challenge to remove an app from Blacklist
    if (itemToRemove != null) {
        Dialog(
            onDismissRequest = {
                itemToRemove = null
                passwordConfirmInput = ""
                passwordError = null
            },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CmfWhite)
            ) {
                HighFrictionSecurityChallengeFlow(
                    challengeType = HighFrictionChallengeType.UNBLOCK_INSTALLATION,
                    targetItemName = itemToRemove,
                    correctPassword = trustPassword,
                    accentColor = accentColor,
                    onCancel = {
                        itemToRemove = null
                        passwordConfirmInput = ""
                        passwordError = null
                    },
                    onSuccess = {
                        val toRemove = itemToRemove
                        if (toRemove != null) {
                            PlayStoreBlacklistManager.removeApp(context, toRemove)
                        }
                        itemToRemove = null
                        passwordConfirmInput = ""
                        passwordError = null
                    }
                )
            }
        }
    }
}
