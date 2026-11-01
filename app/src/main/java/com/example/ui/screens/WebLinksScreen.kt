package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.RestrictedAppEntity
import com.example.data.local.RestrictedWebsiteEntity
import com.example.ui.components.DotMatrixPattern
import com.example.ui.components.NothingCard
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import com.example.ui.theme.LocalCustomTextColor

@Composable
fun WebLinksScreen(
    websites: List<RestrictedWebsiteEntity>,
    restrictedApps: List<RestrictedAppEntity>,
    onAddWebsite: (RestrictedWebsiteEntity) -> Unit,
    onDeleteWebsite: (RestrictedWebsiteEntity) -> Unit,
    onBack: () -> Unit
) {
    val accentColor = LocalCustomTextColor.current

    var newUrlInput by remember { mutableStateOf("") }
    var showRedundancyAlert by remember { mutableStateOf(false) }
    var redundantAppInfo by remember { mutableStateOf("") }
    var pendingDomainToAdd by remember { mutableStateOf("") }

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
                            tint = CmfDark
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LINKS DA WEB LIMITADOS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = CmfDark,
                        letterSpacing = 1.sp
                    )
                }

                // Add URL Input Card
                NothingCard(
                    title = "ADICIONAR SITE OU DOMÍNIO",
                    badge = "NOVO LINK"
                ) {
                    Column {
                        Text(
                            text = "Limite o uso de sites da web em navegadores (Chrome, Samsung Internet, etc.) caso não tenham app ou queira reforçar:",
                            fontSize = 11.sp,
                            color = CmfMutedText,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newUrlInput,
                                onValueChange = { newUrlInput = it },
                                placeholder = { Text("Ex: reddit.com") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("web_link_input"),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = CmfBorder,
                                    focusedContainerColor = CmfWhite,
                                    unfocusedContainerColor = CmfLightGray.copy(alpha = 0.3f)
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    val cleaned = cleanDomain(newUrlInput)
                                    if (cleaned.isNotBlank()) {
                                        // Check if already matches an installed app
                                        val matchedApp = restrictedApps.firstOrNull { app ->
                                            app.associatedWebsites.contains(cleaned, ignoreCase = true) ||
                                                    cleaned.contains(app.appName.lowercase().replace(" ", ""))
                                        }

                                        if (matchedApp != null) {
                                            // Redundancy detected! Alert user
                                            redundantAppInfo = "${matchedApp.appName} (${matchedApp.packageName})"
                                            pendingDomainToAdd = cleaned
                                            showRedundancyAlert = true
                                        } else {
                                            onAddWebsite(
                                                RestrictedWebsiteEntity(
                                                    domain = cleaned,
                                                    limitMinutes = 15,
                                                    remainingSeconds = 15 * 60L
                                                )
                                            )
                                            newUrlInput = ""
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("add_web_link_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Adicionar")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Synchronization note
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF9F5), RoundedCornerShape(8.dp))
                        .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sincronização automática: ao limitar um app (ex: Facebook), o site correspondente (facebook.com) também é bloqueado pelo mesmo timer.",
                            fontSize = 11.sp,
                            color = CmfDark,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "SITES ATUALMENTE MONITORADOS (${websites.size}):",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CmfMutedText
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(websites, key = { it.id }) { web ->
                        WebItemRow(
                            website = web,
                            accentColor = accentColor,
                            onDelete = { onDeleteWebsite(web) }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }

        // Redundancy Alert Dialog
        if (showRedundancyAlert) {
            AlertDialog(
                onDismissRequest = { showRedundancyAlert = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "APP JÁ CONFIGURADO",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = CmfDark
                        )
                    }
                },
                text = {
                    Text(
                        text = "O site '$pendingDomainToAdd' já possui o aplicativo correspondente ($redundantAppInfo) selecionado no Lock in!\n\nO Lock in sincroniza o site e o app automaticamente para evitar redundâncias e manter o mesmo tempo limite.",
                        fontSize = 13.sp,
                        color = CmfDark,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onAddWebsite(
                                RestrictedWebsiteEntity(
                                    domain = pendingDomainToAdd,
                                    limitMinutes = 15,
                                    remainingSeconds = 15 * 60L
                                )
                            )
                            newUrlInput = ""
                            showRedundancyAlert = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Adicionar Mesmo Assim", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRedundancyAlert = false }) {
                        Text("Cancelar", color = CmfMutedText, fontSize = 12.sp)
                    }
                },
                containerColor = CmfWhite,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun WebItemRow(
    website: RestrictedWebsiteEntity,
    accentColor: Color,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CmfWhite)
            .border(1.dp, CmfBorder, RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = website.domain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CmfDark
                )
                Text(
                    text = if (website.isBlocked) "Bloqueado • 00:00" else "${website.remainingSeconds / 60}m restantes",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (website.isBlocked) accentColor else CmfMutedText
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remover",
                tint = CmfMutedText
            )
        }
    }
}

private fun cleanDomain(input: String): String {
    return input.trim()
        .replace("https://", "")
        .replace("http://", "")
        .replace("www.", "")
        .split("/")
        .firstOrNull() ?: ""
}
