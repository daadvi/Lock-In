package com.example.ui.components
import com.example.ui.theme.BlockyFontFamily

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.AppGroupEntity
import com.example.ui.screens.InstalledAppItem
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite

@Composable
fun AppGroupConfigDialog(
    group: AppGroupEntity?,
    availableApps: List<InstalledAppItem>,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSaveGroup: (AppGroupEntity) -> Unit,
    onDeleteGroup: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(group?.name ?: "") }
    var limitMinutes by remember { mutableIntStateOf(group?.limitMinutes ?: 15) }
    val selectedPackages = remember {
        mutableStateListOf<String>().apply {
            if (group != null) {
                addAll(group.getPackageList())
            }
        }
    }

    var showAppPicker by remember { mutableStateOf(false) }

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
                            text = if (group == null) "NOVO TEMPO LIMITE EM GRUPO" else "EDITAR TEMPO EM GRUPO",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = BlockyFontFamily,
                            color = CmfDark,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Apps selecionados dividem a mesma cota e ilha flutuante",
                            fontSize = 10.sp,
                            fontFamily = BlockyFontFamily,
                            color = accentColor
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = CmfDark)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Grupo (Ex: Redes Sociais, Jogos)") },
                    placeholder = { Text("Ex: Redes Sociais") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_group_name_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = CmfBorder
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Limit Minutes adjustment
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CmfLightGray.copy(alpha = 0.5f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TEMPO LIMITE COMPARTILHADO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = BlockyFontFamily,
                            color = CmfMutedText
                        )
                        Text(
                            text = "${limitMinutes} minutos",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = BlockyFontFamily,
                            color = accentColor
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(-10, -5, +5, +10).forEach { delta ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CmfWhite)
                                    .border(1.dp, CmfBorder, RoundedCornerShape(6.dp))
                                    .clickable {
                                        limitMinutes = (limitMinutes + delta).coerceIn(1, 480)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (delta > 0) "+$delta" else "$delta",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = BlockyFontFamily,
                                    color = CmfDark
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Apps in group header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "APLICATIVOS NO GRUPO (${selectedPackages.size}):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BlockyFontFamily,
                        color = CmfMutedText
                    )

                    TextButton(onClick = { showAppPicker = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Adicionar", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Apps list
                if (selectedPackages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CmfLightGray.copy(alpha = 0.4f))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum aplicativo adicionado ainda.\nClique em 'Adicionar' para selecionar os apps que dividirão este tempo limite.",
                            fontSize = 11.sp,
                            color = CmfMutedText,
                            fontFamily = BlockyFontFamily,
                            lineHeight = 15.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(selectedPackages) { pkg ->
                                val appItem = availableApps.firstOrNull { it.packageName == pkg }
                                val label = appItem?.appName ?: pkg.split(".").lastOrNull() ?: pkg

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CmfLightGray)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        MiniAppIcon(packageName = pkg, context = context, sizeDp = 22)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CmfDark
                                        )
                                    }

                                    IconButton(
                                        onClick = { selectedPackages.remove(pkg) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remover",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (group != null && onDeleteGroup != null) {
                        IconButton(
                            onClick = {
                                onDeleteGroup(group.id)
                                onDismiss()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Red)
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CmfLightGray, contentColor = CmfDark)
                    ) {
                        Text("Cancelar", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val finalName = name.trim().ifEmpty { "Conjunto" }
                            val newGroup = (group?.copy(
                                name = finalName,
                                packageNamesCsv = selectedPackages.joinToString(","),
                                limitMinutes = limitMinutes,
                                remainingSeconds = if (group.limitMinutes != limitMinutes) limitMinutes * 60L else group.remainingSeconds
                            ) ?: AppGroupEntity(
                                name = finalName,
                                packageNamesCsv = selectedPackages.joinToString(","),
                                limitMinutes = limitMinutes,
                                remainingSeconds = limitMinutes * 60L
                            ))
                            onSaveGroup(newGroup)
                            onDismiss()
                        },
                        enabled = selectedPackages.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
                    ) {
                        Text("Salvar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showAppPicker) {
            GroupAppPickerDialog(
                availableApps = availableApps,
                selectedPackages = selectedPackages,
                accentColor = accentColor,
                onDismiss = { showAppPicker = false },
                onTogglePackage = { pkg ->
                    if (selectedPackages.contains(pkg)) {
                        selectedPackages.remove(pkg)
                    } else {
                        selectedPackages.add(pkg)
                    }
                }
            )
        }
    }
}

@Composable
fun GroupAppPickerDialog(
    availableApps: List<InstalledAppItem>,
    selectedPackages: List<String>,
    accentColor: Color,
    onDismiss: () -> Unit,
    onTogglePackage: (String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CmfWhite)
                .border(1.5.dp, CmfDark, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SELECIONAR APPS PARA O TEMPO EM GRUPO",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = BlockyFontFamily,
                    color = CmfDark
                )
                Text(
                    text = "Selecione os aplicativos que compartilharão o mesmo limite e a mesma ilha flutuante.",
                    fontSize = 10.sp,
                    color = CmfMutedText,
                    fontFamily = BlockyFontFamily
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar aplicativo...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                val filtered = availableApps.filter {
                    searchQuery.isBlank() || it.appName.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered) { app ->
                        val isChosen = selectedPackages.contains(app.packageName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isChosen) accentColor.copy(alpha = 0.15f) else CmfLightGray)
                                .border(
                                    width = if (isChosen) 1.dp else 0.dp,
                                    color = if (isChosen) accentColor else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onTogglePackage(app.packageName) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MiniAppIcon(packageName = app.packageName, context = context, sizeDp = 26)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = app.appName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CmfDark
                                    )
                                    Text(
                                        text = app.packageName,
                                        fontSize = 9.sp,
                                        color = CmfMutedText
                                    )
                                }
                            }

                            if (isChosen) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selecionado",
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
                ) {
                    Text("Concluir Seleção (${selectedPackages.size})", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
