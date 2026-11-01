package com.example.ui.screens
import com.example.ui.theme.BlockyFontFamily

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import com.example.data.local.FocusRoutine
import com.example.service.FocusTimeManager
import com.example.service.PhoneAppHelper
import com.example.util.AppFilterHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.ui.components.AppIconImage
import com.example.ui.components.CmfTimePickerDialog
import com.example.ui.components.DotMatrixPattern
import com.example.ui.components.NothingCard
import com.example.ui.components.cmfTextFieldColors

import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import com.example.ui.theme.LocalCustomTextColor
import com.example.ui.theme.LocalIsDarkTheme

data class SelectableApp(
    val packageName: String,
    val appName: String,
    val isGame: Boolean
)

@Composable
fun FocusTimeScreen(
    initialTargetPackage: String? = null,
    initialTargetAppName: String? = null,
    onClearTargetApp: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = LocalCustomTextColor.current

    // Tabs: 0 = "BLOQUEIO TOTAL", 1 = "ROTINAS"
    var selectedTab by rememberSaveable { mutableIntStateOf(if (!initialTargetPackage.isNullOrBlank()) 1 else 0) }

    // Direct Focus State
    var durationMinutes by rememberSaveable { mutableIntStateOf(30) }
    val directAllowedApps = remember { mutableStateListOf<String>() }

    // Routines State
    val routines = remember { mutableStateListOf<FocusRoutine>() }
    val expandedAppPackages = remember { mutableStateListOf<String>() }
    var targetAppForNewRoutine by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showAppPickerToCreateRoutine by remember { mutableStateOf(false) }

    // Presets State
    val presetsList = remember { mutableStateListOf<FocusTimeManager.FocusPreset>() }

    // Dialog States
    var showAppPickerForDirect by remember { mutableStateOf(false) }
    var showRoutineTypeChoiceDialog by remember { mutableStateOf(false) }
    var showCreateRoutineDialog by remember { mutableStateOf(false) }
    var showConfirmStartDialog by remember { mutableStateOf(false) }
    var showCustomDurationDialog by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var selectedRoutineId by remember { mutableStateOf<String?>(null) }
    var showAppPickerForRoutineId by remember { mutableStateOf<String?>(null) }
    var timeAdjustDialogState by remember { mutableStateOf<Triple<FocusRoutine, Boolean, String>?>(null) }

    val installedApps = remember { mutableStateListOf<SelectableApp>() }

    // Load installed apps, routines, and presets
    LaunchedEffect(Unit) {
        val existingRoutines = FocusTimeManager.getRoutines(context)
        routines.clear()
        routines.addAll(existingRoutines)
        if (selectedRoutineId == null && routines.isNotEmpty()) {
            selectedRoutineId = routines.first().id
        }

        val loadedPresets = FocusTimeManager.getPresets(context)
        presetsList.clear()
        presetsList.addAll(loadedPresets)

        val existingAllowed = FocusTimeManager.getAllowedPackages(context)
            .filterNot { PhoneAppHelper.isPhoneDialerApp(context, it) }
            .filterNot { AppFilterHelper.isLockInSelfApp(context, it) }
            .take(3)
        directAllowedApps.clear()
        if (existingAllowed.isNotEmpty()) {
            directAllowedApps.addAll(existingAllowed)
            FocusTimeManager.setEmergencyAllowedPackages(context, directAllowedApps.toList())
        } else {
            // Check for installed essential messaging apps like WhatsApp, never Phone!
            val candidateEssential = listOf("com.whatsapp", "com.google.android.apps.messaging", "com.android.mms")
            val pm = context.packageManager
            val found = candidateEssential.firstOrNull { pkg ->
                try { pm.getPackageInfo(pkg, 0); true } catch (_: Exception) { false }
            }
            if (found != null) {
                directAllowedApps.add(found)
                FocusTimeManager.setEmergencyAllowedPackages(context, directAllowedApps.toList())
            }
        }

        val apps = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val list = pm.queryIntentActivities(mainIntent, 0)
            list.mapNotNull { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                val label = resolveInfo.loadLabel(pm).toString()
                if (AppFilterHelper.isLockInSelfApp(context, pkg, label)) {
                    return@mapNotNull null
                }
                // Phone apps must NEVER be selectable as allowed apps (Requirement 2)
                if (PhoneAppHelper.isPhoneDialerApp(context, pkg, label)) {
                    return@mapNotNull null
                }
                val isGame = FocusTimeManager.isGameApp(context, pkg)
                SelectableApp(pkg, label, isGame)
            }.distinctBy { it.packageName }.sortedBy { it.appName }
        }

        installedApps.clear()
        installedApps.addAll(apps)
    }

    LaunchedEffect(initialTargetPackage) {
        if (!initialTargetPackage.isNullOrBlank()) {
            selectedTab = 1
            if (!expandedAppPackages.contains(initialTargetPackage)) {
                expandedAppPackages.add(initialTargetPackage)
            }
            val appName = initialTargetAppName ?: installedApps.firstOrNull { it.packageName == initialTargetPackage }?.appName ?: "App"
            val hasRoutines = routines.any { it.targetPackage == initialTargetPackage }
            if (!hasRoutines) {
                targetAppForNewRoutine = Pair(initialTargetPackage, appName)
                showCreateRoutineDialog = true
            }
        }
    }

    val isDark = LocalIsDarkTheme.current
    val textColor = if (isDark) Color.White else CmfDark
    val tabBg = if (isDark) Color(0xFF1B1B22) else CmfLightGray
    val activeTabBg = if (isDark) Color(0xFF262630) else CmfWhite
    val activeTabTextColor = if (isDark) Color.White else CmfDark

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            DotMatrixPattern(modifier = Modifier.fillMaxSize(), spacingDp = 22f)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("focus_time_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar",
                            tint = textColor
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FOCUS TIME",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        color = textColor,
                        letterSpacing = 1.sp
                    )
                }

                // Nothing OS Tab Segmented Control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(tabBg)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 0) activeTabBg else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "BLOQUEAR AGORA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = BlockyFontFamily,
                            color = if (selectedTab == 0) activeTabTextColor else CmfMutedText
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 1) activeTabBg else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ROTINAS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = BlockyFontFamily,
                            color = if (selectedTab == 1) activeTabTextColor else CmfMutedText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Content
                if (selectedTab == 0) {
                    // --- TAB 1: BLOQUEAR O TELEFONE POR UM DETERMINADO TEMPO ---
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Unbreakable Warning Notice
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFF0ED))
                                    .border(1.5.dp, Color(0xFFFF4400), RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFFF4400),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "BLOQUEIO TOTAL INQUEBRÁVEL",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = BlockyFontFamily,
                                            color = Color(0xFFFF4400),
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "Durante o Focus Time, o aparelho fica 100% inutilizável. NÃO existe como sair nem mesmo com a senha de segurança. Somente os 3 apps de emergência selecionados abaixo poderão ser abertos.",
                                            fontSize = 11.sp,
                                            color = CmfDark,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Duration Selector & Presets
                        item {
                            NothingCard(
                                title = "DURAÇÃO DO BLOQUEIO",
                                badge = "${durationMinutes} MINUTOS"
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formatMinutesToReadable(durationMinutes),
                                            fontSize = 38.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = BlockyFontFamily,
                                            color = accentColor,
                                            letterSpacing = 1.5.sp
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            // Custom Time Button
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(CmfLightGray)
                                                    .border(1.dp, CmfBorder, RoundedCornerShape(8.dp))
                                                    .clickable { showCustomDurationDialog = true }
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Tempo Personalizado",
                                                        tint = CmfDark,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Personalizar",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CmfDark
                                                    )
                                                }
                                            }

                                            // Save as Preset Button
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(CmfLightGray)
                                                    .border(1.dp, CmfBorder, RoundedCornerShape(8.dp))
                                                    .clickable { showSavePresetDialog = true }
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.BookmarkAdd,
                                                        contentDescription = "Criar Preset",
                                                        tint = CmfDark,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Salvar Preset",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CmfDark
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "PRESETS DE TEMPO",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = BlockyFontFamily,
                                        color = CmfMutedText
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Presets Grid / Flow Layout
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        presetsList.chunked(4).forEach { rowItems ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                rowItems.forEach { preset ->
                                                    val isSel = durationMinutes == preset.minutes
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (isSel) accentColor else CmfLightGray)
                                                            .border(
                                                                width = 1.dp,
                                                                color = if (isSel) accentColor else CmfBorder,
                                                                shape = RoundedCornerShape(8.dp)
                                                            )
                                                            .clickable { durationMinutes = preset.minutes }
                                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.Center
                                                        ) {
                                                            Text(
                                                                text = preset.label,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                fontFamily = BlockyFontFamily,
                                                                color = if (isSel) Color.White else CmfDark
                                                            )
                                                            if (!preset.isDefault) {
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Icon(
                                                                    imageVector = Icons.Default.Close,
                                                                    contentDescription = "Excluir preset",
                                                                    tint = if (isSel) Color.White.copy(alpha = 0.8f) else CmfMutedText,
                                                                    modifier = Modifier
                                                                        .size(12.dp)
                                                                        .clickable {
                                                                            FocusTimeManager.deletePreset(context, preset.id)
                                                                            presetsList.remove(preset)
                                                                        }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                // Fill empty space if row has fewer than 4 items
                                                if (rowItems.size < 4) {
                                                    repeat(4 - rowItems.size) {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Quick Adjust Buttons (+15m, +30m, -15m)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(CmfLightGray)
                                                .clickable {
                                                    durationMinutes = (durationMinutes - 15).coerceAtLeast(1)
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("-15m", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CmfDark)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(CmfLightGray)
                                                .clickable {
                                                    durationMinutes = (durationMinutes + 15).coerceAtMost(1440)
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("+15m", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CmfDark)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(CmfLightGray)
                                                .clickable {
                                                    durationMinutes = (durationMinutes + 30).coerceAtMost(1440)
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("+30m", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CmfDark)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(CmfLightGray)
                                                .clickable {
                                                    durationMinutes = (durationMinutes + 60).coerceAtMost(1440)
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("+1h", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CmfDark)
                                        }
                                    }
                                }
                            }
                        }

                        // Allowed Apps Selector Card
                        item {
                            NothingCard(
                                title = "APPS LIBERADOS NO BLOQUEIO (MÁX 3)",
                                badge = "${directAllowedApps.size}/3 LIBERADOS"
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Apps que permanecerão liberados e acessíveis durante o bloqueio (limite máximo rigoroso de 3 aplicativos, ex: WhatsApp, Mensagens, Mapas ou Banco).",
                                        fontSize = 11.sp,
                                        color = CmfMutedText,
                                        lineHeight = 16.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Phone note
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF4F4F6))
                                            .border(1.dp, CmfBorder, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "📞 NOTA: O app de Telefone NÃO pode e não precisa ser selecionado aqui, pois ele já possui um botão permanente preto e branco fixo na tela de bloqueio.",
                                            fontSize = 10.sp,
                                            color = CmfMutedText,
                                            lineHeight = 14.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Useful Apps Indication (Requirement 3: e.g. WhatsApp to communicate if something happens)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(accentColor.copy(alpha = 0.08f))
                                            .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = "💡", fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "INDICAÇÃO DE APPS REALMENTE ÚTEIS:",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = BlockyFontFamily,
                                                    color = accentColor
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Recomendamos selecionar apenas aplicativos essenciais para emergências ou comunicação vital caso algo aconteça (ex: WhatsApp):",
                                                fontSize = 10.sp,
                                                color = CmfDark,
                                                lineHeight = 14.sp
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            val usefulApps = listOf(
                                                Triple("com.whatsapp", "WhatsApp", "Comunicação urgente caso algo aconteça"),
                                                Triple("com.whatsapp.w4b", "WhatsApp Business", "Comunicação essencial de trabalho"),
                                                Triple("com.google.android.apps.maps", "Google Maps", "Navegação e direções"),
                                                Triple("com.ubercab", "Uber", "Transporte e locomoção rápida"),
                                                Triple("com.google.android.apps.messaging", "Mensagens SMS", "Envio e recebimento de códigos")
                                            )

                                            val pm = context.packageManager
                                            val installedUseful = usefulApps.filter { (pkg, _, _) ->
                                                try { pm.getPackageInfo(pkg, 0); true } catch (_: Exception) { false }
                                            }

                                            if (installedUseful.isNotEmpty()) {
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    installedUseful.forEach { (pkg, name, purpose) ->
                                                        val isAlreadyAdded = directAllowedApps.contains(pkg)
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(CmfWhite)
                                                                .border(1.dp, if (isAlreadyAdded) Color(0xFF00C853) else CmfBorder, RoundedCornerShape(8.dp))
                                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                                AppIconImage(packageName = pkg, fallbackLetter = name.first().toString(), sizeDp = 24.dp)
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Column {
                                                                    Text(text = name, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = CmfDark)
                                                                    Text(text = purpose, fontSize = 9.sp, color = CmfMutedText)
                                                                }
                                                            }

                                                            if (isAlreadyAdded) {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(14.dp))
                                                                    Spacer(modifier = Modifier.width(3.dp))
                                                                    Text(text = "LIBERADO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
                                                                }
                                                            } else {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(6.dp))
                                                                        .background(accentColor)
                                                                        .clickable {
                                                                            if (directAllowedApps.size < 3) {
                                                                                directAllowedApps.add(pkg)
                                                                                FocusTimeManager.setEmergencyAllowedPackages(context, directAllowedApps.toList())
                                                                                Toast.makeText(context, "$name adicionado aos apps liberados!", Toast.LENGTH_SHORT).show()
                                                                            } else {
                                                                                Toast.makeText(context, "Limite máximo de 3 apps liberados atingido!", Toast.LENGTH_SHORT).show()
                                                                            }
                                                                        }
                                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                                ) {
                                                                    Text(text = "+ LIBERAR", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Display Allowed Apps List
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        directAllowedApps.forEach { pkg ->
                                            val appInfo = installedApps.firstOrNull { it.packageName == pkg }
                                            val displayName = appInfo?.appName ?: pkg.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: pkg

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(CmfWhite)
                                                    .border(1.dp, CmfBorder, RoundedCornerShape(10.dp))
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    AppIconImage(
                                                        packageName = pkg,
                                                        fallbackLetter = displayName.firstOrNull()?.uppercase() ?: "A",
                                                        sizeDp = 32.dp
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            text = displayName,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = CmfDark
                                                        )
                                                        Text(
                                                            text = "Liberado durante o foco",
                                                            fontSize = 10.sp,
                                                            color = CmfMutedText
                                                        )
                                                    }
                                                }

                                                IconButton(
                                                    onClick = {
                                                        directAllowedApps.remove(pkg)
                                                        FocusTimeManager.setEmergencyAllowedPackages(context, directAllowedApps.toList())
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Remover",
                                                        tint = Color.Gray,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Add Button (strictly up to 3 apps)
                                        if (directAllowedApps.size < 3) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(CmfLightGray.copy(alpha = 0.6f))
                                                    .border(1.dp, CmfBorder, RoundedCornerShape(10.dp))
                                                    .clickable { showAppPickerForDirect = true }
                                                    .padding(vertical = 12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = null,
                                                        tint = accentColor,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "ADICIONAR APP LIBERADO (${directAllowedApps.size}/3)",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = BlockyFontFamily,
                                                        color = accentColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Big Trigger CTA
                        item {
                            Button(
                                onClick = { showConfirmStartDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("start_focus_time_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Black,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "BLOQUEAR DISPOSITIVO AGORA",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = BlockyFontFamily,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(90.dp)) }
                    }
                } else {
                    // --- TAB 2: ROTINAS DE BLOQUEIO SEPARADAS POR APPS (FOTO 1 & REQUISITO 1) ---
                    val appRoutinesMap = routines.filter { it.isAppSpecific }.groupBy { it.targetPackage!! }
                    val generalRoutines = routines.filter { !it.isAppSpecific }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "ROTINAS DE BLOQUEIO POR APP",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = BlockyFontFamily,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Bloqueie aplicativos em momentos específicos do dia",
                                        fontSize = 10.sp,
                                        color = CmfMutedText
                                    )
                                }

                                Button(
                                    onClick = { showRoutineTypeChoiceDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accentColor,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("add_routine_button")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Nova Rotina", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (appRoutinesMap.isEmpty() && generalRoutines.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Nenhuma rotina de bloqueio configurada ainda.",
                                            fontSize = 12.sp,
                                            fontFamily = BlockyFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Selecione um aplicativo para programar momentos do dia em que ele será bloqueado automaticamente.",
                                            fontSize = 10.sp,
                                            fontFamily = BlockyFontFamily,
                                            color = CmfMutedText,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Button(
                                            onClick = { showRoutineTypeChoiceDialog = true },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = accentColor,
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("empty_state_add_routine_button")
                                        ) {
                                            Text("+ Criar Nova Rotina de Bloqueio", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // APPS SPECIFIC ROUTINES (Grouped by App with Expandable Accordion)
                        appRoutinesMap.forEach { (targetPkg, appRoutineList) ->
                            val isExpanded = expandedAppPackages.contains(targetPkg)
                            val appName = appRoutineList.firstOrNull()?.targetAppName
                                ?: installedApps.firstOrNull { it.packageName == targetPkg }?.appName
                                ?: targetPkg.split(".").lastOrNull()
                                ?: targetPkg

                            item(key = targetPkg) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.dp, if (isExpanded) accentColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                        .padding(14.dp)
                                ) {
                                    // App Header Card - Clicking expands/collapses routines
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isExpanded) {
                                                    expandedAppPackages.remove(targetPkg)
                                                } else {
                                                    expandedAppPackages.add(targetPkg)
                                                }
                                            },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            AppIconImage(
                                                packageName = targetPkg,
                                                modifier = Modifier.size(38.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = appName,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = BlockyFontFamily,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(if (appRoutineList.any { it.isEnabled }) accentColor else Color.Gray)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "${appRoutineList.size} rotina(s) de bloqueio",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CmfMutedText,
                                                        fontFamily = BlockyFontFamily
                                                    )
                                                }
                                            }
                                        }

                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (isExpanded) "Recolher" else "Expandir",
                                            tint = if (isExpanded) accentColor else Color.Gray,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    // WHEN EXPANDED: Show all routines of this specific app!
                                    if (isExpanded) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                        Spacer(modifier = Modifier.height(14.dp))

                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            appRoutineList.forEach { r ->
                                                AppRoutineDetailCard(
                                                    routine = r,
                                                    accentColor = accentColor,
                                                    onToggleEnabled = { isEnabled ->
                                                        FocusTimeManager.toggleRoutine(context, r.id, isEnabled)
                                                        val idx = routines.indexOfFirst { it.id == r.id }
                                                        if (idx >= 0) routines[idx] = r.copy(isEnabled = isEnabled)
                                                    },
                                                    onEditTime = { isStart ->
                                                        timeAdjustDialogState = Triple(
                                                            r,
                                                            isStart,
                                                            if (isStart) "Início do Bloqueio: $appName" else "Término do Bloqueio: $appName"
                                                        )
                                                    },
                                                    onToggleDay = { dayNum ->
                                                        val newDays = if (r.daysOfWeek.contains(dayNum)) {
                                                            if (r.daysOfWeek.size > 1) r.daysOfWeek - dayNum else r.daysOfWeek
                                                        } else {
                                                            (r.daysOfWeek + dayNum).sorted()
                                                        }
                                                        val updated = r.copy(daysOfWeek = newDays)
                                                        FocusTimeManager.saveRoutine(context, updated)
                                                        val idx = routines.indexOfFirst { it.id == r.id }
                                                        if (idx >= 0) routines[idx] = updated
                                                    },
                                                    onDelete = {
                                                        FocusTimeManager.deleteRoutine(context, r.id)
                                                        routines.remove(r)
                                                    }
                                                )
                                            }

                                            // CTA to add another routine for THIS app
                                            Button(
                                                onClick = {
                                                    targetAppForNewRoutine = Pair(targetPkg, appName)
                                                    showCreateRoutineDialog = true
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = MaterialTheme.colorScheme.onSurface
                                                ),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = accentColor
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "+ Adicionar outro horário para $appName",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = BlockyFontFamily
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // GENERAL / DEVICE-WIDE ROUTINES (if any)
                        if (generalRoutines.isNotEmpty()) {
                            item {
                                val isGeneralExpanded = expandedAppPackages.contains("__GENERAL__")
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isGeneralExpanded) expandedAppPackages.remove("__GENERAL__") else expandedAppPackages.add("__GENERAL__")
                                            },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Bloqueio Geral do Dispositivo",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = BlockyFontFamily,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${generalRoutines.size} rotina(s) geral",
                                                    fontSize = 10.sp,
                                                    color = CmfMutedText
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = if (isGeneralExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color.Gray
                                        )
                                    }

                                    if (isGeneralExpanded) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        generalRoutines.forEach { r ->
                                            AppRoutineDetailCard(
                                                routine = r,
                                                accentColor = accentColor,
                                                onToggleEnabled = { isEnabled ->
                                                    FocusTimeManager.toggleRoutine(context, r.id, isEnabled)
                                                    val idx = routines.indexOfFirst { it.id == r.id }
                                                    if (idx >= 0) routines[idx] = r.copy(isEnabled = isEnabled)
                                                },
                                                onEditTime = { isStart ->
                                                    timeAdjustDialogState = Triple(r, isStart, "Ajustar Horário Geral")
                                                },
                                                onToggleDay = { dayNum ->
                                                    val newDays = if (r.daysOfWeek.contains(dayNum)) {
                                                        if (r.daysOfWeek.size > 1) r.daysOfWeek - dayNum else r.daysOfWeek
                                                    } else {
                                                        (r.daysOfWeek + dayNum).sorted()
                                                    }
                                                    val updated = r.copy(daysOfWeek = newDays)
                                                    FocusTimeManager.saveRoutine(context, updated)
                                                    val idx = routines.indexOfFirst { it.id == r.id }
                                                    if (idx >= 0) routines[idx] = updated
                                                },
                                                onDelete = {
                                                    FocusTimeManager.deleteRoutine(context, r.id)
                                                    routines.remove(r)
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                targetAppForNewRoutine = null
                                                showCreateRoutineDialog = true
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.onSurface
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = accentColor
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "+ Adicionar outro horário geral",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = BlockyFontFamily
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(90.dp)) }
                    }
                }
            }
        }

        // App Picker Dialog to Create App-Specific Routine
        if (showAppPickerToCreateRoutine) {
            AppExceptionPickerDialog(
                installedApps = installedApps,
                accentColor = accentColor,
                onDismiss = { showAppPickerToCreateRoutine = false },
                onSelectApp = { selectedPkg ->
                    val appName = installedApps.firstOrNull { it.packageName == selectedPkg }?.appName
                        ?: selectedPkg.split(".").lastOrNull()
                        ?: selectedPkg
                    targetAppForNewRoutine = Pair(selectedPkg, appName)
                    showAppPickerToCreateRoutine = false
                    showCreateRoutineDialog = true
                }
            )
        }

        // Choice Dialog between Device-Wide Routine and App-Specific Routine
        if (showRoutineTypeChoiceDialog) {
            AlertDialog(
                onDismissRequest = { showRoutineTypeChoiceDialog = false },
                title = {
                    Text(
                        text = "TIPO DE ROTINA",
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        fontSize = 14.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Escolha o tipo de rotina de bloqueio programado que deseja criar:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Option 1: App Specific
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .clickable {
                                    showRoutineTypeChoiceDialog = false
                                    showAppPickerToCreateRoutine = true
                                }
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "1. Rotina de Apps (Bloqueio de Apps Específicos)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = BlockyFontFamily,
                                    color = accentColor
                                )
                                Text(
                                    text = "Bloqueia aplicativos específicos selecionados (ex: YouTube, Instagram) em horários programados.",
                                    fontSize = 10.sp,
                                    color = CmfMutedText
                                )
                            }
                        }

                        // Option 2: Absolute / Device-Wide
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .clickable {
                                    showRoutineTypeChoiceDialog = false
                                    targetAppForNewRoutine = null
                                    showCreateRoutineDialog = true
                                }
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "2. Rotina Absoluta (Bloqueio Total do Celular)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = BlockyFontFamily,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Bloqueia o celular de forma absoluta durante o horário programado (exceto exceções de emergência).",
                                    fontSize = 10.sp,
                                    color = CmfMutedText
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showRoutineTypeChoiceDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }

        // App Picker Dialog (Ensures NO GAMES allowed as exceptions!)
        if (showAppPickerForDirect) {
            AppExceptionPickerDialog(
                installedApps = installedApps,
                accentColor = accentColor,
                onDismiss = { showAppPickerForDirect = false },
                onSelectApp = { selectedPkg ->
                    if (!directAllowedApps.contains(selectedPkg)) {
                        if (directAllowedApps.size < 3) {
                            directAllowedApps.add(selectedPkg)
                            FocusTimeManager.setEmergencyAllowedPackages(context, directAllowedApps.toList())
                            Toast.makeText(context, "App liberado salvo com sucesso!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Limite máximo de 3 apps liberados atingido!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    showAppPickerForDirect = false
                }
            )
        }

        // Create Routine Dialog
        if (showCreateRoutineDialog) {
            CreateRoutineDialog(
                installedApps = installedApps,
                accentColor = accentColor,
                targetPackage = targetAppForNewRoutine?.first,
                targetAppName = targetAppForNewRoutine?.second,
                onDismiss = {
                    showCreateRoutineDialog = false
                    targetAppForNewRoutine = null
                },
                onSave = { newRoutine ->
                    FocusTimeManager.saveRoutine(context, newRoutine)
                    routines.add(newRoutine)
                    newRoutine.targetPackage?.let { pkg ->
                        if (!expandedAppPackages.contains(pkg)) {
                            expandedAppPackages.add(pkg)
                        }
                    }
                    showCreateRoutineDialog = false
                    targetAppForNewRoutine = null
                    Toast.makeText(context, "Rotina programada com sucesso!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Confirmation dialog before engaging unbreakable phone lock
        if (showConfirmStartDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmStartDialog = false },
                title = {
                    Text(
                        text = "CONFIRMAR FOCUS TIME",
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        fontSize = 14.sp
                    )
                },
                text = {
                    Text(
                        text = "Seu telefone será bloqueado por ${formatMinutesToReadable(durationMinutes)}. Durante esse tempo, NÃO existirá forma de sair, nem mesmo com a sua senha de segurança. Apenas os ${directAllowedApps.size} apps selecionados poderão ser acessados.\n\nDeseja ativar agora?",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = CmfDark
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmStartDialog = false
                            FocusTimeManager.startFocusSession(
                                context = context,
                                durationMinutes = durationMinutes,
                                allowedPackages = directAllowedApps.toList()
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                    ) {
                        Text("Sim, Bloquear", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmStartDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }

        // Custom Duration Dialog (Permite colocar o tempo livremente com o relógio Nothing CMF)
        if (showCustomDurationDialog) {
            val initialH = (durationMinutes / 60).coerceIn(0, 23)
            val initialM = (durationMinutes % 60).coerceIn(0, 59)

            CmfTimePickerDialog(
                initialHour = initialH,
                initialMinute = initialM,
                title = "DEFINIR TEMPO DE BLOQUEIO",
                accentColor = accentColor,
                onConfirm = { h, m ->
                    val total = (h * 60 + m).coerceIn(1, 1440)
                    durationMinutes = total
                    showCustomDurationDialog = false
                },
                onDismiss = { showCustomDurationDialog = false }
            )
        }

        // Save Preset Dialog (Permite criar presets com nome personalizado)
        if (showSavePresetDialog) {
            var presetName by remember {
                mutableStateOf(
                    if (durationMinutes >= 60 && durationMinutes % 60 == 0) "${durationMinutes / 60}h" else "${durationMinutes}m"
                )
            }

            AlertDialog(
                onDismissRequest = { showSavePresetDialog = false },
                title = {
                    Text(
                        text = "CRIAR PRESET",
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        fontSize = 14.sp,
                        color = CmfDark
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Deseja salvar a duração atual (${formatMinutesToReadable(durationMinutes)}) como um preset de acesso rápido?",
                            fontSize = 12.sp,
                            color = CmfMutedText,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = presetName,
                            onValueChange = { if (it.length <= 12) presetName = it },
                            label = { Text("Etiqueta do Preset (ex: 45m, 1h30, Estudo)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = cmfTextFieldColors(accentColor = accentColor)
                        )

                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val finalLabel = presetName.trim().ifEmpty {
                                if (durationMinutes >= 60 && durationMinutes % 60 == 0) "${durationMinutes / 60}h" else "${durationMinutes}m"
                            }
                            FocusTimeManager.savePreset(context, finalLabel, durationMinutes)
                            presetsList.clear()
                            presetsList.addAll(FocusTimeManager.getPresets(context))
                            showSavePresetDialog = false
                            Toast.makeText(context, "Preset '$finalLabel' salvo com sucesso!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Salvar Preset", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSavePresetDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }

        // App Picker Dialog for Routine Emergency Apps (prohibiting games)
        showAppPickerForRoutineId?.let { rId ->
            val targetRoutine = routines.firstOrNull { it.id == rId }
            if (targetRoutine != null) {
                AppExceptionPickerDialog(
                    installedApps = installedApps,
                    accentColor = accentColor,
                    onDismiss = { showAppPickerForRoutineId = null },
                    onSelectApp = { selectedPkg ->
                        if (!targetRoutine.allowedPackages.contains(selectedPkg)) {
                            if (targetRoutine.allowedPackages.size < 3) {
                                val updated = targetRoutine.copy(allowedPackages = (targetRoutine.allowedPackages + selectedPkg).take(3))
                                FocusTimeManager.saveRoutine(context, updated)
                                val idx = routines.indexOfFirst { it.id == rId }
                                if (idx >= 0) routines[idx] = updated
                                Toast.makeText(context, "App liberado salvo na rotina!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Limite máximo de 3 apps liberados atingido!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showAppPickerForRoutineId = null
                    }
                )
            } else {
                showAppPickerForRoutineId = null
            }
        }

        // Time Adjustment Dialog for Routines
        timeAdjustDialogState?.let { (routine, isStart, title) ->
            RoutineTimePickerDialog(
                title = title,
                initialHour = if (isStart) routine.startHour else routine.endHour,
                initialMinute = if (isStart) routine.startMinute else routine.endMinute,
                accentColor = accentColor,
                onDismiss = { timeAdjustDialogState = null },
                onConfirm = { h, m ->
                    val updated = if (isStart) {
                        routine.copy(startHour = h, startMinute = m)
                    } else {
                        routine.copy(endHour = h, endMinute = m)
                    }
                    FocusTimeManager.saveRoutine(context, updated)
                    val idx = routines.indexOfFirst { it.id == routine.id }
                    if (idx >= 0) routines[idx] = updated
                    timeAdjustDialogState = null
                }
            )
        }
    }
}

@Composable
fun AppRoutineDetailCard(
    routine: FocusRoutine,
    accentColor: Color,
    onToggleEnabled: (Boolean) -> Unit,
    onEditTime: (Boolean) -> Unit,
    onToggleDay: (Int) -> Unit,
    onDelete: () -> Unit
) {
    val durationText = formatRoutineDuration(routine.startHour, routine.startMinute, routine.endHour, routine.endMinute)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = routine.name.ifBlank { "Rotina de Bloqueio" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BlockyFontFamily,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = durationText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = BlockyFontFamily,
                            color = accentColor
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${routine.formatTimeRange()} • ${routine.formatDays()}",
                    fontSize = 10.sp,
                    color = CmfMutedText,
                    fontFamily = BlockyFontFamily
                )
            }

            Switch(
                checked = routine.isEnabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accentColor
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .clickable { onEditTime(true) }
                    .padding(vertical = 8.dp, horizontal = 10.dp)
            ) {
                Column {
                    Text(text = "INÍCIO", fontSize = 8.sp, color = CmfMutedText, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format("%02d:%02d", routine.startHour, routine.startMinute),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .clickable { onEditTime(false) }
                    .padding(vertical = 8.dp, horizontal = 10.dp)
            ) {
                Column {
                    Text(text = "TÉRMINO", fontSize = 8.sp, color = CmfMutedText, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format("%02d:%02d", routine.endHour, routine.endMinute),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        RoutineTimelineRuler(
            startHour = routine.startHour,
            startMinute = routine.startMinute,
            endHour = routine.endHour,
            endMinute = routine.endMinute,
            accentColor = accentColor
        )

        Spacer(modifier = Modifier.height(10.dp))

        RoutineWeekdayElevator(
            selectedDays = routine.daysOfWeek,
            accentColor = accentColor,
            onToggleDay = onToggleDay
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Excluir rotina",
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Excluir",
                    color = Color(0xFFE53935),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RoutineWeekdayElevator(
    selectedDays: List<Int>,
    accentColor: Color,
    onToggleDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayDefs = listOf(
        Triple(1, "M", "SEG"),
        Triple(2, "T", "TER"),
        Triple(3, "W", "QUA"),
        Triple(4, "T", "QUI"),
        Triple(5, "F", "SEX"),
        Triple(6, "S", "SÁB"),
        Triple(7, "S", "DOM")
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        dayDefs.forEach { (dayNumber, letter, label) ->
            val isActive = selectedDays.contains(dayNumber)

            // Up when active (0.dp), down when inactive (44.dp)
            val animatedOffset by animateDpAsState(
                targetValue = if (isActive) 0.dp else 44.dp,
                animationSpec = tween(durationMillis = 280),
                label = "day_offset_$dayNumber"
            )

            val animatedColor by animateColorAsState(
                targetValue = if (isActive) accentColor else Color(0xFF68686E),
                animationSpec = tween(durationMillis = 250),
                label = "day_color_$dayNumber"
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onToggleDay(dayNumber) }
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Day letter header
                Text(
                    text = letter,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = BlockyFontFamily,
                    color = if (isActive) accentColor else Color.Gray
                )

                Spacer(modifier = Modifier.height(6.dp))

                // The Vertical Track (Capsule)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEBEBF0))
                        .padding(3.dp)
                ) {
                    // Sliding Rectangle Block:
                    // Retângulos para cima = dias ativados (cor vibrante laranja/accent)
                    // Retângulos para baixo = dias que não entram na rotina (cor cinza chumbo escuro)
                    Box(
                        modifier = Modifier
                            .offset(y = animatedOffset)
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(animatedColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height(2.dp)
                                    .background(Color.White.copy(alpha = if (isActive) 0.85f else 0.4f), RoundedCornerShape(1.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height(2.dp)
                                    .background(Color.White.copy(alpha = if (isActive) 0.85f else 0.4f), RoundedCornerShape(1.dp))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = label,
                    fontSize = 8.sp,
                    fontFamily = BlockyFontFamily,
                    color = if (isActive) CmfDark else Color.Gray,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun RoutineTimelineRuler(
    startHour: Int,
    startMinute: Int,
    endHour: Int,
    endMinute: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF0F0F4))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val totalMinutes = 1440f
                val startM = (startHour * 60 + startMinute).toFloat()
                val endM = (endHour * 60 + endMinute).toFloat()

                // Graduation ticks
                for (h in 0..24 step 2) {
                    val x = (h * 60f / totalMinutes) * size.width
                    val tickHeight = if (h % 6 == 0) size.height * 0.5f else size.height * 0.25f
                    drawLine(
                        color = Color(0xFFC8C8CE),
                        start = Offset(x, size.height - tickHeight),
                        end = Offset(x, size.height),
                        strokeWidth = 1.5f
                    )
                }

                val yBar = size.height * 0.2f
                val barHeight = size.height * 0.45f

                if (startM <= endM) {
                    val x1 = (startM / totalMinutes) * size.width
                    val x2 = (endM / totalMinutes) * size.width
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(x1, yBar),
                        size = Size((x2 - x1).coerceAtLeast(4f), barHeight),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                } else {
                    val x1 = (startM / totalMinutes) * size.width
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(x1, yBar),
                        size = Size(size.width - x1, barHeight),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    val x2 = (endM / totalMinutes) * size.width
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(0f, yBar),
                        size = Size(x2, barHeight),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("00h", fontSize = 8.sp, fontFamily = BlockyFontFamily, color = CmfMutedText)
            Text("06h", fontSize = 8.sp, fontFamily = BlockyFontFamily, color = CmfMutedText)
            Text("12h", fontSize = 8.sp, fontFamily = BlockyFontFamily, color = CmfMutedText)
            Text("18h", fontSize = 8.sp, fontFamily = BlockyFontFamily, color = CmfMutedText)
            Text("24h", fontSize = 8.sp, fontFamily = BlockyFontFamily, color = CmfMutedText)
        }
    }
}

fun formatRoutineDuration(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int): String {
    val startTotal = startHour * 60 + startMinute
    val endTotal = endHour * 60 + endMinute
    val diffMinutes = if (endTotal >= startTotal) {
        endTotal - startTotal
    } else {
        (1440 - startTotal) + endTotal
    }
    val h = diffMinutes / 60
    val m = diffMinutes % 60
    return if (m == 0) "${h}h" else "${h}h ${m}m"
}

@Composable
fun RoutineTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    CmfTimePickerDialog(
        initialHour = initialHour,
        initialMinute = initialMinute,
        title = title,
        accentColor = accentColor,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}


@Composable
fun AppExceptionPickerDialog(
    installedApps: List<SelectableApp>,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSelectApp: (String) -> Unit
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
                    text = "SELECIONAR APP DE EXCEÇÃO",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = BlockyFontFamily,
                    color = CmfDark
                )
                Text(
                    text = "Jogos não podem ser selecionados. O Telefone já possui botão fixo na tela de bloqueio.",
                    fontSize = 10.sp,
                    color = CmfMutedText,
                    fontFamily = BlockyFontFamily
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Useful suggestions chips
                val usefulSuggestions = listOf(
                    "com.whatsapp" to "WhatsApp",
                    "com.whatsapp.w4b" to "WA Business",
                    "com.google.android.apps.maps" to "Mapas",
                    "com.google.android.apps.messaging" to "Mensagens"
                )
                val availableUseful = usefulSuggestions.filter { (pkg, _) ->
                    installedApps.any { it.packageName == pkg }
                }
                if (availableUseful.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 Recomendados:",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = BlockyFontFamily,
                            color = accentColor
                        )
                        availableUseful.forEach { (pkg, name) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor.copy(alpha = 0.12f))
                                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .clickable { onSelectApp(pkg) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "+ $name",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar aplicativo...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                val filtered = remember(installedApps.size, searchQuery) {
                    installedApps.filter {
                        !PhoneAppHelper.isPhoneDialerApp(context, it.packageName, it.appName) &&
                        !AppFilterHelper.isLockInSelfApp(context, it.packageName, it.appName) &&
                        (searchQuery.isBlank() || it.appName.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true))
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (app.isGame) CmfLightGray.copy(alpha = 0.5f) else CmfLightGray)
                                .clickable {
                                    if (app.isGame) {
                                        Toast.makeText(
                                            context,
                                            "Jogos não podem ser colocados como exceção!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        onSelectApp(app.packageName)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIconImage(
                                    packageName = app.packageName,
                                    fallbackLetter = app.appName.firstOrNull()?.uppercase() ?: "A",
                                    sizeDp = 30.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = app.appName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (app.isGame) Color.Gray else CmfDark
                                    )
                                    Text(
                                        text = if (app.isGame) "CATEGORIA: JOGO (BLOQUEADO)" else app.packageName,
                                        fontSize = 9.sp,
                                        fontFamily = BlockyFontFamily,
                                        color = if (app.isGame) Color.Red else CmfMutedText
                                    )
                                }
                            }

                            if (app.isGame) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFFEAEA), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PROIBIDO",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Red,
                                        fontFamily = BlockyFontFamily
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CmfDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Fechar", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun CreateRoutineDialog(
    installedApps: List<SelectableApp>,
    accentColor: Color,
    targetPackage: String? = null,
    targetAppName: String? = null,
    onDismiss: () -> Unit,
    onSave: (FocusRoutine) -> Unit
) {
    val context = LocalContext.current
    val defaultName = if (!targetAppName.isNullOrBlank()) "Bloqueio $targetAppName" else "Foco Noturno"
    var name by remember(targetAppName) { mutableStateOf(defaultName) }
    var startHour by remember { mutableIntStateOf(21) }
    var startMinute by remember { mutableIntStateOf(0) }
    var endHour by remember { mutableIntStateOf(5) }
    var endMinute by remember { mutableIntStateOf(30) }
    var isPickingStartTime by remember { mutableStateOf(false) }
    var isPickingEndTime by remember { mutableStateOf(false) }
    val selectedDays = remember { mutableStateListOf(1, 2, 3, 4, 5) }
    val selectedAllowed = remember { mutableStateListOf("com.google.android.dialer") }
    var showAppPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = if (targetAppName != null) "ROTINA PARA ${targetAppName.uppercase()}" else "NOVA ROTINA DE BLOQUEIO",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = BlockyFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (targetAppName != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Este aplicativo será bloqueado automaticamente nos horários programados.",
                        fontSize = 10.sp,
                        color = CmfMutedText,
                        fontFamily = BlockyFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Rotina") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = cmfTextFieldColors(accentColor = accentColor)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Start Time Picker with Clock Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Início do Bloqueio:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { isPickingStartTime = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Abrir Relógio",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format("%02d:%02d", startHour, startMinute),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = BlockyFontFamily,
                            color = accentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // End Time Picker with Clock Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Término do Bloqueio:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { isPickingEndTime = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Abrir Relógio",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format("%02d:%02d", endHour, endMinute),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = BlockyFontFamily,
                            color = accentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))


                // Day Selection Elevator (Matching reference style)
                Text(
                    text = "DIAS DE BLOQUEIO (RETÂNGULOS PARA CIMA):",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BlockyFontFamily,
                    color = CmfMutedText
                )

                Spacer(modifier = Modifier.height(6.dp))

                RoutineWeekdayElevator(
                    selectedDays = selectedDays,
                    accentColor = accentColor,
                    onToggleDay = { dayNum ->
                        if (selectedDays.contains(dayNum)) {
                            if (selectedDays.size > 1) selectedDays.remove(dayNum)
                        } else {
                            selectedDays.add(dayNum)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Allowed apps row (Max 3) - Only relevant for Device-Wide routine (Foto 1 & Requisito 1)
                if (targetPackage.isNullOrBlank()) {
                    Text(
                        text = "Apps permitidos durante esta rotina (máx 3):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CmfMutedText
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        selectedAllowed.forEach { pkg ->
                            val nameLabel = installedApps.firstOrNull { it.packageName == pkg }?.appName ?: pkg.split(".").lastOrNull() ?: pkg
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CmfLightGray)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = nameLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "x",
                                        fontSize = 11.sp,
                                        color = Color.Red,
                                        modifier = Modifier.clickable { selectedAllowed.remove(pkg) }
                                    )
                                }
                            }
                        }

                        if (selectedAllowed.size < 3) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor)
                                    .clickable { showAppPicker = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("+ Add (${selectedAllowed.size}/3)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CmfLightGray, contentColor = CmfDark)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            val routine = FocusRoutine(
                                name = if (name.isBlank()) defaultName else name,
                                startHour = startHour,
                                startMinute = startMinute,
                                endHour = endHour,
                                endMinute = endMinute,
                                daysOfWeek = selectedDays.sorted(),
                                allowedPackages = selectedAllowed.take(3),
                                targetPackage = targetPackage,
                                targetAppName = targetAppName
                            )
                            onSave(routine)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
                    ) {
                        Text("Salvar")
                    }
                }
            }
        }

        if (showAppPicker) {
            AppExceptionPickerDialog(
                installedApps = installedApps,
                accentColor = accentColor,
                onDismiss = { showAppPicker = false },
                onSelectApp = { pkg ->
                    if (!selectedAllowed.contains(pkg)) {
                        if (selectedAllowed.size < 3) {
                            selectedAllowed.add(pkg)
                        } else {
                            Toast.makeText(context, "Limite máximo de 3 apps liberados atingido!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    showAppPicker = false
                }
            )
        }

        if (isPickingStartTime) {
            CmfTimePickerDialog(
                initialHour = startHour,
                initialMinute = startMinute,
                title = "Início do Bloqueio",
                accentColor = accentColor,
                onConfirm = { h, m ->
                    startHour = h
                    startMinute = m
                    isPickingStartTime = false
                },
                onDismiss = { isPickingStartTime = false }
            )
        }

        if (isPickingEndTime) {
            CmfTimePickerDialog(
                initialHour = endHour,
                initialMinute = endMinute,
                title = "Término do Bloqueio",
                accentColor = accentColor,
                onConfirm = { h, m ->
                    endHour = h
                    endMinute = m
                    isPickingEndTime = false
                },
                onDismiss = { isPickingEndTime = false }
            )
        }
    }
}


private fun formatMinutesToReadable(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0 && m > 0) {
        "${h}h ${m}m"
    } else if (h > 0) {
        "${h}h"
    } else {
        "${m}m"
    }
}
