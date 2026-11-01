package com.example.ui.screens
import com.example.ui.theme.BlockyFontFamily

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.CircleShape
import com.example.ui.components.AppGroupWidgetCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.local.AppGroupEntity
import com.example.data.local.RestrictedAppEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.ui.components.AppGroupConfigDialog
import com.example.ui.components.AppIconImage
import com.example.ui.components.DotMatrixPattern
import com.example.ui.components.MiniAppIcon
import com.example.ui.components.WebsiteBlacklistDialog
import com.example.ui.components.cmfTextFieldColors

import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import com.example.ui.theme.LocalCustomTextColor
import com.example.ui.theme.LocalIsDarkTheme

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val isSocialOrVideo: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(
    currentRestrictedApps: List<RestrictedAppEntity>,
    appGroups: List<AppGroupEntity> = emptyList(),
    trustPassword: String = "",
    onSaveApp: (RestrictedAppEntity) -> Unit,
    onRemoveApp: (String) -> Unit,
    onSaveAppGroup: (AppGroupEntity) -> Unit = {},
    onDeleteAppGroup: (String) -> Unit = {},
    onNavigateToAppRoutines: (String, String) -> Unit = { _, _ -> },
    onNavigateToTrustUnlock: (String, String) -> Unit = { _, _ -> },
    onNavigateToWebsites: () -> Unit = {},
    onBack: () -> Unit
) {
    val accentColor = LocalCustomTextColor.current
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Apps Individuais, 1: Tempo Limite em Grupo
    var searchQuery by remember { mutableStateOf("") }
    val installedApps = remember { mutableStateListOf<InstalledAppItem>() }
    var editingGroup by remember { mutableStateOf<AppGroupEntity?>(null) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var showWebsiteBlacklistDialog by remember { mutableStateOf(false) }
    var blockedAppNoticeName by remember { mutableStateOf<String?>(null) }
    var conflictAppGroupNotice by remember { mutableStateOf<Pair<InstalledAppItem, AppGroupEntity>?>(null) }
    var blockedDepletedAppNotice by remember { mutableStateOf<Pair<InstalledAppItem, RestrictedAppEntity>?>(null) }
    var blockedInProgressNoticeApp by remember { mutableStateOf<Pair<InstalledAppItem, RestrictedAppEntity>?>(null) }
    var bankingProtectedNoticeApp by remember { mutableStateOf<InstalledAppItem?>(null) }

    val existingEntitiesByPkg = remember(currentRestrictedApps) {
        currentRestrictedApps
            .filterNot { com.example.util.AppFilterHelper.isLockInSelfApp(context, it.packageName, it.appName) }
            .associateBy { it.packageName }
    }

    // Query installed apps on phone off the main thread to ensure 60fps UI
    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveList = pm.queryIntentActivities(intent, 0)
            resolveList.mapNotNull { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                val label = resolveInfo.loadLabel(pm).toString()
                // Never show Lock in itself in the app limit selection list
                if (com.example.util.AppFilterHelper.isLockInSelfApp(context, pkg, label)) {
                    return@mapNotNull null
                }
                val isKnownSocial = isKnownSocialOrVideoApp(pkg, label)
                InstalledAppItem(
                    packageName = pkg,
                    appName = label,
                    isSocialOrVideo = isKnownSocial
                )
            }.distinctBy { it.packageName }.sortedByDescending { it.isSocialOrVideo }
        }

        installedApps.clear()
        installedApps.addAll(loaded)
    }

    val filteredList = remember(installedApps.size, searchQuery) {
        installedApps.filter {
            !com.example.util.AppFilterHelper.isLockInSelfApp(context, it.packageName, it.appName) &&
                    (searchQuery.isBlank() ||
                            it.appName.contains(searchQuery, ignoreCase = true) ||
                            it.packageName.contains(searchQuery, ignoreCase = true))
        }
    }

    val isDark = LocalIsDarkTheme.current
    val textColor = if (isDark) Color.White else CmfDark

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
                            tint = textColor
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CONTROLE DE APLICATIVOS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        color = textColor,
                        letterSpacing = 1.sp
                    )
                }

                // Tab Selector: Apps Individuais vs Tempo Limite em Grupo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF1E1E22) else CmfLightGray)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selectedTab == 0) accentColor else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 8.dp)
                            .testTag("tab_individual_limits"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "INDIVIDUAIS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = BlockyFontFamily,
                            color = if (selectedTab == 0) Color.White else (if (isDark) Color(0xFFA1A1AA) else CmfDark),
                            letterSpacing = 0.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selectedTab == 1) accentColor else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 8.dp)
                            .testTag("tab_group_limits"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TEMPO EM GRUPO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = BlockyFontFamily,
                                color = if (selectedTab == 1) Color.White else (if (isDark) Color(0xFFA1A1AA) else CmfDark),
                                letterSpacing = 0.5.sp
                            )
                            if (appGroups.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(if (selectedTab == 1) Color.White else accentColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${appGroups.size}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedTab == 1) accentColor else Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))


                if (selectedTab == 0) {
                    // TAB 0: APPS INDIVIDUAIS

                    // Button to block websites in browsers and manage website limits
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF1E1E22) else CmfLightGray)
                            .border(1.2.dp, if (isDark) Color(0xFF2E2E34) else CmfBorder, RoundedCornerShape(12.dp))
                            .clickable { showWebsiteBlacklistDialog = true }
                            .padding(12.dp)
                            .testTag("btn_card_block_websites")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.15f)),
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
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "BLOQUEAR SITES",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = BlockyFontFamily,
                                            color = textColor,
                                            letterSpacing = 0.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "NAVEGADORES",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = BlockyFontFamily,
                                                color = accentColor,
                                                softWrap = false,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Bloquear acesso a sites indesejados",
                                        fontSize = 10.sp,
                                        color = if (isDark) Color(0xFFA1A1AA) else CmfMutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { showWebsiteBlacklistDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("btn_block_websites_action")
                            ) {
                                Text(
                                    text = "BLOQUEAR",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = BlockyFontFamily,
                                    color = Color.White,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar aplicativo instalado...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = CmfMutedText)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("app_search_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = cmfTextFieldColors(accentColor = accentColor)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "SELECIONE OS APPS PARA LIMITAR O USO:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BlockyFontFamily,
                        color = CmfMutedText,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredList, key = { it.packageName }) { appItem ->
                            val existingEntity = existingEntitiesByPkg[appItem.packageName]
                            val isSelected = existingEntity?.isLimited == true
                            val groupForApp = appGroups.firstOrNull { it.containsPackage(appItem.packageName) }
                            val isDepleted = com.example.service.AppBlockEnforcer.isAppDepleted(appItem.packageName) ||
                                    (existingEntity != null && (
                                        existingEntity.isLimitDepleted ||
                                        existingEntity.isBlocked ||
                                        (existingEntity.isLimited && existingEntity.totalLimitMinutes > 0 && (
                                            (existingEntity.useAbsoluteTime && existingEntity.individualRemainingSeconds <= 0L) ||
                                            (existingEntity.limitShortVideosOnly && existingEntity.shortVideoRemainingSeconds <= 0L) ||
                                            (!existingEntity.useAbsoluteTime && !existingEntity.limitShortVideosOnly && (existingEntity.shortVideoRemainingSeconds <= 0L || existingEntity.longVideoRemainingSeconds <= 0L))
                                        ))
                                    ))

                            AppItemRow(
                                item = appItem,
                                existingEntity = existingEntity,
                                isSelected = isSelected,
                                isDepleted = isDepleted,
                                accentColor = accentColor,
                                groupName = groupForApp?.name,
                                onConflictAttempt = {
                                    if (groupForApp != null) {
                                        conflictAppGroupNotice = appItem to groupForApp
                                    }
                                },
                                onDepletedAttempt = {
                                    blockedDepletedAppNotice = appItem to (existingEntity ?: RestrictedAppEntity(
                                        packageName = appItem.packageName,
                                        appName = appItem.appName,
                                        isLimited = true
                                    ))
                                },
                                onBankingAttempt = {
                                    bankingProtectedNoticeApp = appItem
                                },
                                onInProgressAttempt = {
                                    if (existingEntity != null) {
                                        blockedInProgressNoticeApp = appItem to existingEntity
                                    }
                                },
                                onToggleSelect = { selected ->
                                    if (com.example.service.BankingSecurityHelper.isBankingOrFinancialApp(appItem.packageName)) {
                                        bankingProtectedNoticeApp = appItem
                                        return@AppItemRow
                                    }
                                    if (existingEntity?.isUsageInProgress == true) {
                                        blockedInProgressNoticeApp = appItem to existingEntity
                                        return@AppItemRow
                                    }
                                    if (groupForApp != null) {
                                        // App is already part of a shared group time limit: TRIGGER RED POPUP!
                                        conflictAppGroupNotice = appItem to groupForApp
                                        return@AppItemRow
                                    }
                                    if (!selected) {
                                        // ANTI-BYPASS: If app's limit is depleted, user cannot simply uncheck it to regain access!
                                        if (isDepleted) {
                                            blockedDepletedAppNotice = appItem to (existingEntity ?: RestrictedAppEntity(
                                                packageName = appItem.packageName,
                                                appName = appItem.appName,
                                                isLimited = true
                                            ))
                                            return@AppItemRow
                                        }
                                        if (existingEntity != null) {
                                            onSaveApp(existingEntity.copy(isLimited = false))
                                        } else {
                                            onRemoveApp(appItem.packageName)
                                        }
                                    } else {
                                        val isVideo = com.example.service.AppVideoCategoryHelper.hasVideos(appItem.packageName)
                                        val hasShorts = com.example.service.AppVideoCategoryHelper.hasShortVideos(appItem.packageName)
                                        val supportsVideo = isVideo || hasShorts
                                        val initShortM = 15
                                        val initLongM = 45
                                        val initTotalM = if (supportsVideo) (initShortM + initLongM) else 15
                                        val newApp = existingEntity?.copy(isLimited = true) ?: RestrictedAppEntity(
                                            packageName = appItem.packageName,
                                            appName = appItem.appName,
                                            isLimited = true,
                                            isShortVideoPlatform = supportsVideo,
                                            limitShortVideosOnly = false,
                                            limitLongVideosAlso = true,
                                            useAbsoluteTime = !supportsVideo,
                                            hasVideoContent = isVideo,
                                            hasShortVideoContent = hasShorts,
                                            individualLimitMinutes = initTotalM,
                                            individualRemainingSeconds = initTotalM * 60L,
                                            shortVideoLimitMinutes = if (supportsVideo) initShortM else 10,
                                            shortVideoRemainingSeconds = if (supportsVideo) initShortM * 60L else 10 * 60L,
                                            longVideoLimitMinutes = if (supportsVideo) initLongM else 25,
                                            longVideoRemainingSeconds = if (supportsVideo) initLongM * 60L else 25 * 60L
                                        )
                                        onSaveApp(newApp)
                                    }
                                },
                                onUpdateEntity = { updated ->
                                    if (isDepleted) {
                                        blockedDepletedAppNotice = appItem to (existingEntity ?: updated)
                                    } else {
                                        onSaveApp(updated)
                                    }
                                },
                                onNavigateToAppRoutines = onNavigateToAppRoutines,
                                onNavigateToTrustUnlock = onNavigateToTrustUnlock
                            )
                        }

                        item { Spacer(modifier = Modifier.height(90.dp)) }
                    }
                } else {
                    // TAB 1: TEMPO LIMITE EM GRUPO
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        Button(
                            onClick = { showNewGroupDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_group_limit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+ CRIAR TEMPO LIMITE EM GRUPO",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = BlockyFontFamily,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0xFF1E1E22) else CmfLightGray)
                                .border(1.dp, if (isDark) Color(0xFF2E2E34) else CmfBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Os aplicativos em um grupo compartilham o mesmo tempo limite e a mesma ilha flutuante. Se o tempo do grupo esgotar, todos os apps dele são bloqueados juntos.",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFFA1A1AA) else CmfMutedText,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (appGroups.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = CmfMutedText,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "NENHUM TEMPO EM GRUPO",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = BlockyFontFamily,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Selecione vários apps (como Instagram, TikTok e YouTube) para que dividam a mesma cota de tempo.",
                                        fontSize = 11.sp,
                                        color = CmfMutedText,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                contentPadding = PaddingValues(bottom = 120.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(appGroups, key = { it.id }) { group ->
                                    AppGroupWidgetCard(
                                        group = group,
                                        accentColor = accentColor,
                                        onClick = { editingGroup = group },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(90.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }

    // Small Red Pop-up Dialog for Shared Time Limit Conflict
    if (conflictAppGroupNotice != null) {
        val (conflictItem, conflictGroup) = conflictAppGroupNotice!!
        Dialog(onDismissRequest = { conflictAppGroupNotice = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0xFF18181B) else CmfWhite)
                    .border(2.dp, Color(0xFFE53935), RoundedCornerShape(16.dp))
                    .padding(20.dp)
                    .testTag("shared_limit_conflict_popup")
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFE53935).copy(alpha = 0.15f), CircleShape)
                            .border(1.2.dp, Color(0xFFE53935).copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alerta",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "TEMPO LIMITE COMPARTILHADO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        color = Color(0xFFE53935),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "O app \"${conflictItem.appName}\" já está em um tempo limite compartilhado no grupo \"${conflictGroup.name}\".",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Para colocar um limite individual para este app, você terá que primeiro tirar aquele app do tempo limite compartilhado.",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFFA1A1AA) else CmfMutedText,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { conflictAppGroupNotice = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                        ) {
                            Text("ENTENDI", fontSize = 11.sp, fontFamily = BlockyFontFamily)
                        }

                        Button(
                            onClick = {
                                conflictAppGroupNotice = null
                                selectedTab = 1
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE53935),
                                contentColor = Color.White
                            )
                        ) {
                            Text("VER GRUPO", fontSize = 11.sp, fontFamily = BlockyFontFamily)
                        }
                    }
                }
            }
        }
    }

    blockedDepletedAppNotice?.let { (depletedItem, _) ->
        Dialog(onDismissRequest = { blockedDepletedAppNotice = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0xFF1E1E22) else CmfWhite)
                    .border(2.dp, Color(0xFFE53935), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFFE53935).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "APLICATIVO BLOQUEADO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        color = Color(0xFFE53935),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "O limite diário de \"${depletedItem.appName}\" está totalmente esgotado.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Por regras de disciplina do Lock In, você não pode simplesmente desmarcar um aplicativo com limite esgotado para voltar a usá-lo.\n\nEle permanecerá bloqueado até o reset diário de amanhã. Para desbloqueá-lo agora em caso de urgência, use o Desbloqueio de Confiança (exige 5 confirmações de senha e frase).",
                        fontSize = 11.5.sp,
                        color = if (isDark) Color(0xFFA1A1AA) else CmfMutedText,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val pkg = depletedItem.packageName
                                val name = depletedItem.appName
                                blockedDepletedAppNotice = null
                                onNavigateToTrustUnlock(pkg, name)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE53935),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DESBLOQUEAR COM SENHA (5X)", fontSize = 11.sp, fontFamily = BlockyFontFamily)
                        }

                        OutlinedButton(
                            onClick = { blockedDepletedAppNotice = null },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                        ) {
                            Text("VOLTAR AO FOCO", fontSize = 11.sp, fontFamily = BlockyFontFamily)
                        }
                    }
                }
            }
        }
    }

    blockedInProgressNoticeApp?.let { (inProgressItem, _) ->
        Dialog(onDismissRequest = { blockedInProgressNoticeApp = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0xFF1E1E22) else CmfWhite)
                    .border(2.dp, Color(0xFFFF9800), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFFFF9800).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "LIMITE EM ANDAMENTO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        color = Color(0xFFFF9800),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "O uso de \"${inProgressItem.appName}\" já está ativo hoje.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Por regras de disciplina inegociável do Lock In, você não pode alterar ou tentar burlar o limite enquanto ele estiver em andamento.\n\nAguarde o cronômetro chegar a zero. Depois disso, você poderá redefinir o limite apenas 1 vez ao dia mediante o protocolo de segurança (5 senhas + frase).",
                        fontSize = 11.5.sp,
                        color = if (isDark) Color(0xFFA1A1AA) else CmfMutedText,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = { blockedInProgressNoticeApp = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800),
                            contentColor = Color.White
                        )
                    ) {
                        Text("ENTENDIDO", fontSize = 11.sp, fontFamily = BlockyFontFamily)
                    }
                }
            }
        }
    }

    bankingProtectedNoticeApp?.let { bankingItem ->
        Dialog(onDismissRequest = { bankingProtectedNoticeApp = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0xFF1E1E22) else CmfWhite)
                    .border(2.dp, Color(0xFF10B981), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "ISENÇÃO BANCÁRIA TOTAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        color = Color(0xFF10B981),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "\"${bankingItem.appName}\" é um App Financeiro/Bancário",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "O Lock In protege e isenta automaticamente todos os aplicativos bancários e carteiras digitais. Isso impede que os mecanismos de segurança dos bancos (Pix/Fraude) emitam alertas ou bloqueiem seu acesso.",
                        fontSize = 11.5.sp,
                        color = if (isDark) Color(0xFFA1A1AA) else CmfMutedText,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = { bankingProtectedNoticeApp = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        )
                    ) {
                        Text("ENTENDIDO", fontSize = 11.sp, fontFamily = BlockyFontFamily)
                    }
                }
            }
        }
    }

    if (showNewGroupDialog) {
        AppGroupConfigDialog(
            group = null,
            availableApps = installedApps,
            accentColor = accentColor,
            onDismiss = { showNewGroupDialog = false },
            onSaveGroup = { newGroup ->
                // Conflict prevention: clear individual limits for apps in the group
                newGroup.getPackageList().forEach { pkg ->
                    val existing = existingEntitiesByPkg[pkg]
                    if (existing != null && existing.isLimited) {
                        onSaveApp(existing.copy(isLimited = false))
                    }
                }
                onSaveAppGroup(newGroup)
                showNewGroupDialog = false
            }
        )
    }

    editingGroup?.let { currentGroup ->
        AppGroupConfigDialog(
            group = currentGroup,
            availableApps = installedApps,
            accentColor = accentColor,
            onDismiss = { editingGroup = null },
            onSaveGroup = { updatedGroup ->
                // Conflict prevention: clear individual limits for apps in the group
                updatedGroup.getPackageList().forEach { pkg ->
                    val existing = existingEntitiesByPkg[pkg]
                    if (existing != null && existing.isLimited) {
                        onSaveApp(existing.copy(isLimited = false))
                    }
                }
                onSaveAppGroup(updatedGroup)
                editingGroup = null
            },
            onDeleteGroup = { id ->
                onDeleteAppGroup(id)
                editingGroup = null
            }
        )
    }

    if (showWebsiteBlacklistDialog) {
        WebsiteBlacklistDialog(
            accentColor = accentColor,
            trustPassword = trustPassword,
            onDismiss = { showWebsiteBlacklistDialog = false }
        )
    }
}

@Composable
fun AppItemRow(
    item: InstalledAppItem,
    existingEntity: RestrictedAppEntity?,
    isSelected: Boolean,
    isDepleted: Boolean = false,
    accentColor: Color,
    groupName: String? = null,
    onConflictAttempt: (() -> Unit)? = null,
    onDepletedAttempt: (() -> Unit)? = null,
    onBankingAttempt: (() -> Unit)? = null,
    onInProgressAttempt: (() -> Unit)? = null,
    onToggleSelect: (Boolean) -> Unit,
    onUpdateEntity: (RestrictedAppEntity) -> Unit,
    onNavigateToAppRoutines: (packageName: String, appName: String) -> Unit = { _, _ -> },
    onNavigateToTrustUnlock: (packageName: String, appName: String) -> Unit = { _, _ -> }
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    val isBankingApp = com.example.service.BankingSecurityHelper.isBankingOrFinancialApp(item.packageName)
    val isInProgress = existingEntity?.isUsageInProgress == true

    val surfaceBg = MaterialTheme.colorScheme.surface
    val borderCol = when {
        isBankingApp -> Color(0xFF10B981).copy(alpha = 0.5f)
        isInProgress -> Color(0xFFFF9800).copy(alpha = 0.7f)
        isDepleted -> Color(0xFFE53935)
        isSelected -> accentColor
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }
    val textCol = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(surfaceBg)
            .border(
                width = if (isBankingApp || isInProgress || isDepleted || isSelected) 1.5.dp else 1.dp,
                color = borderCol,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable {
                if (isBankingApp) {
                    onBankingAttempt?.invoke()
                } else if (isInProgress) {
                    onInProgressAttempt?.invoke()
                } else if (groupName != null) {
                    onConflictAttempt?.invoke()
                } else if (isDepleted) {
                    onDepletedAttempt?.invoke()
                }
            }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isBankingApp) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.2f))
                        .clickable { onBankingAttempt?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Protegido",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(15.dp)
                    )
                }
            } else if (isInProgress) {
                Checkbox(
                    checked = true,
                    onCheckedChange = { onInProgressAttempt?.invoke() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFFF9800),
                        checkmarkColor = Color.White
                    )
                )
            } else if (groupName != null) {
                Checkbox(
                    checked = false,
                    onCheckedChange = { onConflictAttempt?.invoke() },
                    colors = CheckboxDefaults.colors(
                        uncheckedColor = Color(0xFFE53935),
                        checkedColor = Color(0xFFE53935)
                    )
                )
            } else if (isDepleted) {
                Checkbox(
                    checked = true,
                    onCheckedChange = { onDepletedAttempt?.invoke() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFE53935),
                        checkmarkColor = Color.White
                    )
                )
            } else {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect(it) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = accentColor,
                        checkmarkColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Real App Icon
            AppIconImage(
                packageName = item.packageName,
                fallbackLetter = item.appName.firstOrNull()?.uppercase() ?: "A",
                sizeDp = 38.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.appName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textCol
                    )

                    if (isBankingApp) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(0.8.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .clickable { onBankingAttempt?.invoke() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "ISENÇÃO BANCÁRIA",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = BlockyFontFamily,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    } else if (isInProgress) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFF9800).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(0.8.dp, Color(0xFFFF9800).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .clickable { onInProgressAttempt?.invoke() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HourglassBottom,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "LIMITE EM ANDAMENTO",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = BlockyFontFamily,
                                    color = Color(0xFFFF9800)
                                )
                            }
                        }
                    } else if (isDepleted) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE53935).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(0.8.dp, Color(0xFFE53935).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .clickable { onDepletedAttempt?.invoke() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "BLOQUEADO • ESGOTADO",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = BlockyFontFamily,
                                    color = Color(0xFFE53935)
                                )
                            }
                        }
                    } else if (groupName != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE53935).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(0.8.dp, Color(0xFFE53935).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .clickable { onConflictAttempt?.invoke() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "TEMPO EM GRUPO • $groupName",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = BlockyFontFamily,
                                color = Color(0xFFE53935)
                            )
                        }
                    } else {
                        val hasShorts = com.example.service.AppVideoCategoryHelper.hasShortVideos(item.packageName)
                        val hasVideos = com.example.service.AppVideoCategoryHelper.hasVideos(item.packageName)

                        if (hasShorts) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(accentColor.copy(alpha = 0.14f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "VÍDEOS CURTOS",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = BlockyFontFamily,
                                    color = accentColor
                                )
                            }
                        } else if (hasVideos) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF2B2B2E), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "VÍDEOS",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = BlockyFontFamily,
                                    color = Color.White
                                )
                            }
                        }

                        val canonicalDomain = com.example.util.AppWebsiteMapper.getPrimaryDomainForApp(item.packageName, item.appName)
                        if (isSelected && canonicalDomain != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF3B82F6).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .border(0.8.dp, Color(0xFF3B82F6).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = canonicalDomain,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = BlockyFontFamily,
                                        color = Color(0xFF3B82F6)
                                    )
                                }
                            }
                        }
                    }
                }
                Text(
                    text = item.packageName,
                    fontSize = 11.sp,
                    fontFamily = BlockyFontFamily,
                    color = CmfMutedText,
                    maxLines = 1
                )
            }

            if (isSelected) {
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Configurar",
                        tint = CmfMutedText
                    )
                }
            }
        }

        // Expanded Options for Video Filtering & Absolute Limits
        AnimatedVisibility(visible = isSelected && isExpanded) {
            val entity = existingEntity ?: return@AnimatedVisibility
            val hasShorts = com.example.service.AppVideoCategoryHelper.hasShortVideos(item.packageName)
            val hasVideos = com.example.service.AppVideoCategoryHelper.hasVideos(item.packageName)
            val supportsVideoMode = hasShorts || hasVideos

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                val isDepleted = entity.isLimitDepleted

                if (isDepleted) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE53935).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFFE53935), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Bloqueado",
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "APLICATIVO BLOQUEADO",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = BlockyFontFamily,
                                        color = Color(0xFFE53935)
                                    )
                                    Text(
                                        text = "O limite deste app chegou a zero.",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 14.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    onNavigateToTrustUnlock(entity.packageName, entity.appName)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE53935),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Desbloquear com Senha (5x) e Frase",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (!supportsVideoMode) {
                    // Non-video app (e.g. Camera, Calculator, Gallery, etc.)
                    // Do NOT show short-video option! Show individual absolute time limit for the app.
                    Text(
                        text = "LIMITE DE TEMPO DO APLICATIVO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BlockyFontFamily,
                        color = accentColor
                    )
                    Text(
                        text = "Defina o tempo limite individual absoluto para uso deste app.",
                        fontSize = 11.sp,
                        color = CmfMutedText
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LimitNumberBox(
                        modifier = Modifier.fillMaxWidth(),
                        label = "TEMPO LIMITE DIÁRIO (ABSOLUTO)",
                        minutes = entity.individualLimitMinutes,
                        onIncrement = {
                            if (isDepleted) {
                                onDepletedAttempt?.invoke()
                            } else {
                                val newM = (entity.individualLimitMinutes + 5).coerceAtMost(240)
                                onUpdateEntity(
                                    entity.copy(
                                        individualLimitMinutes = newM,
                                        individualRemainingSeconds = newM * 60L,
                                        useAbsoluteTime = true,
                                        limitShortVideosOnly = false
                                    )
                                )
                            }
                        },
                        onDecrement = {
                            if (isDepleted) {
                                onDepletedAttempt?.invoke()
                            } else {
                                val newM = (entity.individualLimitMinutes - 5).coerceAtLeast(1)
                                onUpdateEntity(
                                    entity.copy(
                                        individualLimitMinutes = newM,
                                        individualRemainingSeconds = newM * 60L,
                                        useAbsoluteTime = true,
                                        limitShortVideosOnly = false
                                    )
                                )
                            }
                        }
                    )
                } else {
                    // App WITH videos / short videos (e.g. YouTube, Instagram, TikTok)
                    // Two mutually exclusive options: Absolute Time OR Short Videos Only
                    Text(
                        text = "MODO DE RESTRIÇÃO DO APLICATIVO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BlockyFontFamily,
                        color = accentColor
                    )
                    Text(
                        text = "Escolha uma das opções abaixo (não podem ser ativadas juntas):",
                        fontSize = 11.sp,
                        color = CmfMutedText
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val isAbsoluteMode = entity.useAbsoluteTime && !entity.limitShortVideosOnly

                    // Option A: Limite de tempo absoluto para o app
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isAbsoluteMode) accentColor.copy(alpha = 0.08f) else Color.Transparent)
                            .border(
                                width = if (isAbsoluteMode) 1.5.dp else 1.dp,
                                color = if (isAbsoluteMode) accentColor else CmfBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                if (isDepleted) {
                                    onDepletedAttempt?.invoke()
                                } else {
                                    onUpdateEntity(
                                        entity.copy(
                                            useAbsoluteTime = true,
                                            limitShortVideosOnly = false
                                        )
                                    )
                                }
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(if (isAbsoluteMode) accentColor else Color.Transparent)
                                .border(2.dp, if (isAbsoluteMode) accentColor else CmfMutedText, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isAbsoluteMode) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Limite de tempo absoluto do app",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Bloqueia o app por completo após o limite diário selecionado.",
                                fontSize = 11.sp,
                                color = CmfMutedText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val isShortsMode = !entity.useAbsoluteTime && entity.limitShortVideosOnly

                    val hasShortsOnly = com.example.service.AppVideoCategoryHelper.hasShortVideosOnly(item.packageName)
                    val hasLongsOnly = com.example.service.AppVideoCategoryHelper.hasLongVideosOnly(item.packageName)
                    val hasBoth = com.example.service.AppVideoCategoryHelper.hasBothShortAndLongVideos(item.packageName) ||
                            (hasShorts && hasVideos && !hasShortsOnly && !hasLongsOnly)

                    val videoOptionTitle = when {
                        hasShortsOnly -> "Limitar vídeos curtos (Reels / Shorts)"
                        hasLongsOnly -> "Limitar vídeos longos"
                        else -> "Limitar vídeos curtos e longos"
                    }
                    val videoOptionSubtitle = when {
                        hasShortsOnly -> "Este aplicativo possui apenas vídeos curtos. Defina a cota de tempo diária."
                        hasLongsOnly -> "Defina a cota diária para reprodução de vídeos longos e streams."
                        else -> "Defina cotas de tempo independentes para Shorts e vídeos longos."
                    }

                    // Option B: Bloquear / Limitar vídeos
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isShortsMode) accentColor.copy(alpha = 0.08f) else Color.Transparent)
                            .border(
                                width = if (isShortsMode) 1.5.dp else 1.dp,
                                color = if (isShortsMode) accentColor else CmfBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                if (isDepleted) {
                                    onDepletedAttempt?.invoke()
                                } else {
                                    onUpdateEntity(
                                        entity.copy(
                                            useAbsoluteTime = false,
                                            limitShortVideosOnly = true
                                        )
                                    )
                                }
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(if (isShortsMode) accentColor else Color.Transparent)
                                .border(2.dp, if (isShortsMode) accentColor else CmfMutedText, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isShortsMode) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = videoOptionTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = videoOptionSubtitle,
                                fontSize = 11.sp,
                                color = CmfMutedText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isAbsoluteMode) {
                        LimitNumberBox(
                            modifier = Modifier.fillMaxWidth(),
                            label = "TEMPO TOTAL DO APP (ABSOLUTO)",
                            minutes = entity.individualLimitMinutes,
                            onIncrement = {
                                if (isDepleted) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Aquele app está bloqueado e o limite só pode ser redefinido quando o app estiver liberado de novo.",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    val newM = (entity.individualLimitMinutes + 5).coerceAtMost(240)
                                    onUpdateEntity(
                                        entity.copy(
                                            individualLimitMinutes = newM,
                                            individualRemainingSeconds = newM * 60L
                                        )
                                    )
                                }
                            },
                            onDecrement = {
                                if (isDepleted) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Aquele app está bloqueado e o limite só pode ser redefinido quando o app estiver liberado de novo.",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    val newM = (entity.individualLimitMinutes - 5).coerceAtLeast(1)
                                    onUpdateEntity(
                                        entity.copy(
                                            individualLimitMinutes = newM,
                                            individualRemainingSeconds = newM * 60L
                                        )
                                    )
                                }
                            },
                            onCustomTime = { customM ->
                                if (isDepleted) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Aquele app está bloqueado e o limite só pode ser redefinido quando o app estiver liberado de novo.",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    val newM = customM.coerceIn(1, 1440)
                                    onUpdateEntity(
                                        entity.copy(
                                            individualLimitMinutes = newM,
                                            individualRemainingSeconds = newM * 60L
                                        )
                                    )
                                }
                            }
                        )
                    } else {
                        // Video mode: Show boxes matching the app's video capabilities
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Show Shorts box if app has short videos (shorts-only or both)
                            if (hasShortsOnly || hasBoth || (!hasLongsOnly && hasShorts)) {
                                LimitNumberBox(
                                    modifier = Modifier.fillMaxWidth(),
                                    label = "TEMPO LIMITE PARA VÍDEOS CURTOS (SHORTS / REELS)",
                                    minutes = entity.shortVideoLimitMinutes,
                                    onIncrement = {
                                        if (isDepleted) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Aquele app está bloqueado e o limite só pode ser redefinido quando o app estiver liberado de novo.",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            val newM = (entity.shortVideoLimitMinutes + 5).coerceAtMost(180)
                                            val totalM = if (!entity.useAbsoluteTime) (newM + entity.longVideoLimitMinutes) else entity.individualLimitMinutes
                                            val totalSec = if (!entity.useAbsoluteTime) (newM * 60L + entity.longVideoRemainingSeconds) else entity.individualRemainingSeconds
                                            onUpdateEntity(
                                                entity.copy(
                                                    shortVideoLimitMinutes = newM,
                                                    shortVideoRemainingSeconds = newM * 60L,
                                                    individualLimitMinutes = totalM,
                                                    individualRemainingSeconds = totalSec
                                                )
                                            )
                                        }
                                    },
                                    onDecrement = {
                                        if (isDepleted) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Aquele app está bloqueado e o limite só pode ser redefinido quando o app estiver liberado de novo.",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            val newM = (entity.shortVideoLimitMinutes - 5).coerceAtLeast(1)
                                            val totalM = if (!entity.useAbsoluteTime) (newM + entity.longVideoLimitMinutes) else entity.individualLimitMinutes
                                            val totalSec = if (!entity.useAbsoluteTime) (newM * 60L + entity.longVideoRemainingSeconds) else entity.individualRemainingSeconds
                                            onUpdateEntity(
                                                entity.copy(
                                                    shortVideoLimitMinutes = newM,
                                                    shortVideoRemainingSeconds = newM * 60L,
                                                    individualLimitMinutes = totalM,
                                                    individualRemainingSeconds = totalSec
                                                )
                                            )
                                        }
                                    },
                                    onCustomTime = { customM ->
                                        if (isDepleted) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Aquele app está bloqueado e o limite só pode ser redefinido quando o app estiver liberado de novo.",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            val newM = customM.coerceIn(1, 1440)
                                            val totalM = if (!entity.useAbsoluteTime) (newM + entity.longVideoLimitMinutes) else entity.individualLimitMinutes
                                            val totalSec = if (!entity.useAbsoluteTime) (newM * 60L + entity.longVideoRemainingSeconds) else entity.individualRemainingSeconds
                                            onUpdateEntity(
                                                entity.copy(
                                                    shortVideoLimitMinutes = newM,
                                                    shortVideoRemainingSeconds = newM * 60L,
                                                    individualLimitMinutes = totalM,
                                                    individualRemainingSeconds = totalSec
                                                )
                                            )
                                        }
                                    }
                                )
                            }

                            // Show Long Video box if app has long videos (longs-only or both)
                            if (hasLongsOnly || hasBoth || (!hasShortsOnly && hasVideos)) {
                                LimitNumberBox(
                                    modifier = Modifier.fillMaxWidth(),
                                    label = "TEMPO LIMITE PARA VÍDEOS LONGOS",
                                    minutes = entity.longVideoLimitMinutes,
                                    onIncrement = {
                                        if (isDepleted) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Aquele app está bloqueado e o limite só pode ser redefinido quando o app estiver liberado de novo.",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            val newM = (entity.longVideoLimitMinutes + 5).coerceAtMost(240)
                                            val totalM = if (!entity.useAbsoluteTime) (entity.shortVideoLimitMinutes + newM) else entity.individualLimitMinutes
                                            val totalSec = if (!entity.useAbsoluteTime) (entity.shortVideoRemainingSeconds + newM * 60L) else entity.individualRemainingSeconds
                                            onUpdateEntity(
                                                entity.copy(
                                                    longVideoLimitMinutes = newM,
                                                    longVideoRemainingSeconds = newM * 60L,
                                                    individualLimitMinutes = totalM,
                                                    individualRemainingSeconds = totalSec
                                                )
                                            )
                                        }
                                    },
                                    onDecrement = {
                                        if (isDepleted) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Aquele app está bloqueado e o limite só pode ser redefinido quando o app estiver liberado de novo.",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            val newM = (entity.longVideoLimitMinutes - 5).coerceAtLeast(1)
                                            val totalM = if (!entity.useAbsoluteTime) (entity.shortVideoLimitMinutes + newM) else entity.individualLimitMinutes
                                            val totalSec = if (!entity.useAbsoluteTime) (entity.shortVideoRemainingSeconds + newM * 60L) else entity.individualRemainingSeconds
                                            onUpdateEntity(
                                                entity.copy(
                                                    longVideoLimitMinutes = newM,
                                                    longVideoRemainingSeconds = newM * 60L,
                                                    individualLimitMinutes = totalM,
                                                    individualRemainingSeconds = totalSec
                                                )
                                            )
                                        }
                                    },
                                    onCustomTime = { customM ->
                                        if (isDepleted) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Aquele app está bloqueado e o limite só pode ser redefinido quando o app estiver liberado de novo.",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            val newM = customM.coerceIn(1, 1440)
                                            val totalM = if (!entity.useAbsoluteTime) (entity.shortVideoLimitMinutes + newM) else entity.individualLimitMinutes
                                            val totalSec = if (!entity.useAbsoluteTime) (entity.shortVideoRemainingSeconds + newM * 60L) else entity.individualRemainingSeconds
                                            onUpdateEntity(
                                                entity.copy(
                                                    longVideoLimitMinutes = newM,
                                                    longVideoRemainingSeconds = newM * 60L,
                                                    individualLimitMinutes = totalM,
                                                    individualRemainingSeconds = totalSec
                                                )
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Option to configure scheduled routine for this specific app
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable {
                            onNavigateToAppRoutines(item.packageName, item.appName)
                        }
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "BLOQUEAR EM HORÁRIOS ESPECÍFICOS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = BlockyFontFamily,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Definir rotina de horários para este app na aba de rotinas",
                                    fontSize = 9.sp,
                                    color = CmfMutedText
                                )
                            }
                        }
                        Text(
                            text = "CONFIGURAR →",
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

@Composable
private fun LimitNumberBox(
    label: String,
    minutes: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onCustomTime: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalCustomTextColor.current
    var showCustomDialog by remember { mutableStateOf(false) }
    var customText by remember(minutes, showCustomDialog) { mutableStateOf(minutes.toString()) }

    if (showCustomDialog && onCustomTime != null) {
        Dialog(onDismissRequest = { showCustomDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TEMPO PERSONALIZADO",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        color = accentColor,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CmfMutedText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = customText,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() }.take(4)
                            customText = filtered
                        },
                        label = { Text("Minutos") },
                        suffix = { Text("min", fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = cmfTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_time_input_field"),
                        textStyle = TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = BlockyFontFamily,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Chips row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5, 10, 15, 20, 30, 45, 60).take(5).forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (customText == preset.toString()) accentColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .clickable { customText = preset.toString() }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${preset}m",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (customText == preset.toString()) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showCustomDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("CANCELAR", fontFamily = BlockyFontFamily, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val parsed = customText.toIntOrNull()?.coerceIn(1, 1440) ?: minutes
                                onCustomTime(parsed)
                                showCustomDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("SALVAR", fontFamily = BlockyFontFamily, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = BlockyFontFamily,
            color = CmfMutedText
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Prominent interactive time badge: clicking allows direct custom time entry!
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(alpha = 0.08f))
                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .clickable {
                    if (onCustomTime != null) {
                        showCustomDialog = true
                    }
                }
                .padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${minutes}m",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = BlockyFontFamily,
                    color = accentColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar tempo personalizado",
                    tint = accentColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Stepper Buttons (- and +) and direct DIGITAR button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onDecrement() }
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("-", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .clickable {
                        if (onCustomTime != null) {
                            showCustomDialog = true
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "DIGITAR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BlockyFontFamily,
                    color = CmfMutedText
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onIncrement() }
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("+", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

private fun isKnownSocialOrVideoApp(pkg: String, name: String): Boolean {
    val lower = (pkg + name).lowercase()
    return lower.contains("youtube") ||
            lower.contains("instagram") ||
            lower.contains("tiktok") ||
            lower.contains("facebook") ||
            lower.contains("twitter") ||
            lower.contains("musically") ||
            lower.contains("reddit") ||
            lower.contains("snapchat") ||
            lower.contains("threads") ||
            lower.contains("twitch") ||
            lower.contains("kwai") ||
            lower.contains("shorts") ||
            lower.contains("video")
}
