package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.model.InstallApprovalRequest
import com.example.data.model.LeaveApprovalRequest
import com.example.data.model.PartyMember
import com.example.data.model.PartyState
import com.example.data.model.RequestStatus
import com.example.service.FriendsPartyManager
import com.example.ui.components.FocusRankingPodiumCard
import com.example.ui.components.LiveActivityFeedCard
import com.example.ui.theme.BlockyFontFamily
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import com.example.ui.theme.LocalIsDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsPanelScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val isDarkTheme = LocalIsDarkTheme.current

    val screenBg = if (isDarkTheme) Color(0xFF101012) else CmfWhite
    val cardBg = if (isDarkTheme) Color(0xFF18181B) else CmfWhite
    val cardBorder = if (isDarkTheme) Color(0xFF27272A) else CmfBorder
    val textColor = if (isDarkTheme) Color.White else CmfDark
    val subtextColor = if (isDarkTheme) Color(0xFFA1A1AA) else CmfMutedText
    val accentRed = Color(0xFFD71920) // Nothing Brand Red
    val accentGreen = Color(0xFF10B981)

    val partyState by FriendsPartyManager.partyState.collectAsState()
    val isJoined by FriendsPartyManager.isJoined.collectAsState()
    val syncStatus by FriendsPartyManager.syncStatus.collectAsState()
    val myUserId = remember { FriendsPartyManager.getMyUserId() }

    var joinCodeInput by remember { mutableStateOf("") }
    var joinError by remember { mutableStateOf<String?>(null) }
    var bannerMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        FriendsPartyManager.refreshMyStats(context)
        FriendsPartyManager.startCloudSyncLoop(context.applicationContext)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentRed.copy(alpha = 0.15f))
                                .border(1.dp, accentRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = "Friends Panel",
                                tint = accentRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "FRIENDS PANEL",
                                fontFamily = BlockyFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp,
                                color = textColor
                            )
                            Text(
                                text = if (isJoined) syncStatus else "RESPONSABILIDADE MÚTUA",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isJoined) accentGreen else subtextColor
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("friends_panel_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = textColor
                        )
                    }
                },
                actions = {
                    if (isJoined) {
                        IconButton(
                            onClick = {
                                FriendsPartyManager.refreshMyStats(context)
                                Toast.makeText(context, "Sincronizando com seus amigos...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("friends_panel_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Atualizar",
                                tint = textColor
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = screenBg)
            )
        },
        containerColor = screenBg,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .testTag("friends_panel_content"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Informative feedback banner
            if (bannerMessage != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF26262B))
                            .border(1.dp, Color(0xFF3F3F46), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Aviso",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = bannerMessage ?: "",
                                fontSize = 12.sp,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { bannerMessage = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fechar",
                                    tint = subtextColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (!isJoined || partyState == null) {
                // ==========================================
                // VIEW A: NOT CONNECTED (JOIN OR CREATE)
                // ==========================================
                item {
                    NotConnectedHeroCard(
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        textColor = textColor,
                        subtextColor = subtextColor,
                        accentRed = accentRed,
                        onCreateParty = {
                            val code = FriendsPartyManager.createParty(context)
                            Toast.makeText(context, "Grupo criado com sucesso! Código: $code", Toast.LENGTH_LONG).show()
                        }
                    )
                }

                item {
                    JoinWithCodeCard(
                        code = joinCodeInput,
                        onCodeChange = { input ->
                            joinCodeInput = input.uppercase().trim()
                            joinError = null
                        },
                        error = joinError,
                        onJoin = {
                            val normalized = FriendsPartyManager.normalizeCode(joinCodeInput)
                            if (normalized.length < 4) {
                                joinError = "Digite um código válido de grupo (ex: LCK-4829)"
                            } else {
                                val success = FriendsPartyManager.joinParty(context, normalized)
                                if (success) {
                                    Toast.makeText(context, "Conectado ao grupo $normalized!", Toast.LENGTH_SHORT).show()
                                } else {
                                    joinError = "Código inválido ou indisponível."
                                }
                            }
                        },
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        textColor = textColor,
                        subtextColor = subtextColor,
                        accentRed = accentRed
                    )
                }

                item {
                    HowItWorksInfoCard(
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        textColor = textColor,
                        subtextColor = subtextColor,
                        accentRed = accentRed
                    )
                }
            } else {
                // ==========================================
                // VIEW B: IN PARTY (REAL LIVE CONNECTION)
                // ==========================================
                val state = partyState!!
                val isHost = state.hostUserId == myUserId

                // Party Header Pill
                item {
                    PartyHeaderCard(
                        state = state,
                        isHost = isHost,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        textColor = textColor,
                        subtextColor = subtextColor,
                        accentRed = accentRed,
                        onCopyCode = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("Código Grupo Lock In", state.partyCode))
                            Toast.makeText(context, "Código copiado: ${state.partyCode}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Active Install Approval Requests Alert
                val pendingInstallRequests = state.pendingInstallRequests.filter { it.status == RequestStatus.PENDING }
                if (pendingInstallRequests.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "SOLICITAÇÕES DE INSTALAÇÃO PENDENTES",
                                fontFamily = BlockyFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = accentRed,
                                letterSpacing = 1.sp
                            )

                            pendingInstallRequests.forEach { req ->
                                InstallRequestCard(
                                    request = req,
                                    isMyRequest = req.requesterId == myUserId,
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    textColor = textColor,
                                    subtextColor = subtextColor,
                                    accentRed = accentRed,
                                    accentGreen = accentGreen,
                                    onApprove = {
                                        FriendsPartyManager.respondToInstallRequest(context, req.requestId, approved = true)
                                        Toast.makeText(context, "Instalação permitida por 10 minutos!", Toast.LENGTH_SHORT).show()
                                    },
                                    onReject = {
                                        FriendsPartyManager.respondToInstallRequest(context, req.requestId, approved = false)
                                        Toast.makeText(context, "Instalação recusada com sucesso.", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }

                // Active Leave Approval Requests Alert
                val pendingLeaveRequests = state.pendingLeaveRequests.filter { it.status == RequestStatus.PENDING }
                if (pendingLeaveRequests.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "SOLICITAÇÕES DE DESPAREAMENTO",
                                fontFamily = BlockyFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFF59E0B),
                                letterSpacing = 1.sp
                            )

                            pendingLeaveRequests.forEach { req ->
                                LeaveRequestCard(
                                    request = req,
                                    isMyRequest = req.requesterId == myUserId,
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    textColor = textColor,
                                    subtextColor = subtextColor,
                                    onApprove = {
                                        FriendsPartyManager.respondToLeaveRequest(context, req.requestId, approved = true)
                                        Toast.makeText(context, "Saída autorizada.", Toast.LENGTH_SHORT).show()
                                    },
                                    onReject = {
                                        FriendsPartyManager.respondToLeaveRequest(context, req.requestId, approved = false)
                                        Toast.makeText(context, "Saída recusada.", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }

                // 1. Focus Leaderboard & Podium (1º, 2º, 3º lugar)
                item {
                    FocusRankingPodiumCard(
                        members = state.members,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        textColor = textColor,
                        subtextColor = subtextColor,
                        accentRed = accentRed
                    )
                }

                // 2. Real-Time Focus Alerts & Activity Feed (Alertas de Vacilo)
                item {
                    LiveActivityFeedCard(
                        alerts = state.alerts,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        textColor = textColor,
                        subtextColor = subtextColor,
                        accentRed = accentRed,
                        onCheerAll = {
                            FriendsPartyManager.sendCheer(context, "toda a Party")
                            Toast.makeText(context, "Você mandou força para todos os aliados! 💪", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Privacy Control Banner for Current User
                item {
                    val myMember = state.members.firstOrNull { it.userId == myUserId }
                    val isSharing = myMember?.shareDetailedScreenTime ?: FriendsPartyManager.isSharingDetailedScreenTime(context)
                    PartyPrivacyControlCard(
                        isSharing = isSharing,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        textColor = textColor,
                        subtextColor = subtextColor,
                        accentColor = accentRed,
                        onToggleSharing = { enabled ->
                            FriendsPartyManager.setSharingDetailedScreenTime(context, enabled)
                            Toast.makeText(
                                context,
                                if (enabled) "Tempo de tela e apps agora visíveis para amigos! 👁️" else "Tempo de tela e apps agora privados no grupo! 🔒",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }

                // Section Title: Members & Real-Time Stats
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AMIGOS CONECTADOS (${state.members.size})",
                            fontFamily = BlockyFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = textColor,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Members List
                items(state.members) { member ->
                    MemberStatsCard(
                        member = member,
                        isMe = member.userId == myUserId,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        textColor = textColor,
                        subtextColor = subtextColor,
                        accentRed = accentRed,
                        accentGreen = accentGreen
                    )
                }

                // Leave Party / Unpair Section
                item {
                    LeavePartySection(
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        textColor = textColor,
                        subtextColor = subtextColor,
                        accentRed = accentRed,
                        hasPendingInstall = pendingInstallRequests.isNotEmpty(),
                        onLeaveClicked = {
                            val (allowed, msg) = FriendsPartyManager.requestLeaveParty(context)
                            bannerMessage = msg
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }

            // Bottom space for dock
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun NotConnectedHeroCard(
    cardBg: Color,
    cardBorder: Color,
    textColor: Color,
    subtextColor: Color,
    accentRed: Color,
    onCreateParty: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accentRed.copy(alpha = 0.15f))
                        .border(1.dp, accentRed.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Proteção de Amigos",
                        tint = accentRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "PAINEL DE AMIGOS",
                        fontFamily = BlockyFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = textColor
                    )
                    Text(
                        text = "Responsabilidade Mútua & Foco",
                        fontSize = 12.sp,
                        color = subtextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Conecte dois ou mais aparelhos usando um código de grupo. Vocês acompanharão o tempo de tela uns dos outros, apps usados e quantidade de respeito aos limites em tempo real.\n\n" +
                        "🔒 Se qualquer usuário tentar instalar um app bloqueado, os outros amigos recebem uma notificação imediata e a instalação só será permitida se um amigo autorizar!",
                fontSize = 13.sp,
                color = subtextColor,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCreateParty,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("friends_create_party_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Criar",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CRIAR NOVO GRUPO (PARTY)",
                    fontFamily = BlockyFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun JoinWithCodeCard(
    code: String,
    onCodeChange: (String) -> Unit,
    error: String?,
    onJoin: () -> Unit,
    cardBg: Color,
    cardBorder: Color,
    textColor: Color,
    subtextColor: Color,
    accentRed: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "ENTRAR COM CÓDIGO",
                fontFamily = BlockyFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = textColor,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Insira o código gerado pelo seu amigo (ex: LCK-4829)",
                fontSize = 12.sp,
                color = subtextColor
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = code,
                onValueChange = onCodeChange,
                placeholder = { Text("LCK-0000", color = subtextColor.copy(alpha = 0.6f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("friends_code_input_field"),
                shape = RoundedCornerShape(12.dp),
                isError = error != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentRed,
                    unfocusedBorderColor = cardBorder,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                ),
                singleLine = true
            )

            if (error != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = error,
                    fontSize = 11.sp,
                    color = accentRed
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onJoin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("friends_join_party_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = textColor,
                    contentColor = if (textColor == Color.White) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Conectar",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CONECTAR AO GRUPO",
                    fontFamily = BlockyFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun HowItWorksInfoCard(
    cardBg: Color,
    cardBorder: Color,
    textColor: Color,
    subtextColor: Color,
    accentRed: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "COMO FUNCIONA A PROTEÇÃO:",
                fontFamily = BlockyFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = accentRed
            )
            Text(
                text = "1. Crie o grupo e envie o código para seu amigo.\n" +
                        "2. Ambos verão em tempo real o tempo de tela e porcentagem de respeito.\n" +
                        "3. Ninguém pode instalar apps proibidos sem a autorização do outro.\n" +
                        "4. Ninguém pode desparear ou sair do grupo sem a aprovação do amigo.",
                fontSize = 12.sp,
                color = subtextColor,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun PartyHeaderCard(
    state: PartyState,
    isHost: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textColor: Color,
    subtextColor: Color,
    accentRed: Color,
    onCopyCode: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = state.partyName.uppercase(),
                        fontFamily = BlockyFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = textColor
                    )
                    Text(
                        text = "${state.members.size} amigos conectados",
                        fontSize = 11.sp,
                        color = subtextColor
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isHost) accentRed.copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f))
                        .border(1.dp, if (isHost) accentRed.copy(alpha = 0.4f) else Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isHost) "LÍDER DA PARTY" else "MEMBRO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BlockyFontFamily,
                        color = if (isHost) accentRed else Color(0xFF10B981)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Code Display Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF222226))
                    .border(1.dp, Color(0xFF333338), RoundedCornerShape(12.dp))
                    .clickable { onCopyCode() }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("friends_copy_code_pill")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CÓDIGO DO GRUPO (COMPARTILHE COM SEU AMIGO)",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = subtextColor
                        )
                        Text(
                            text = state.partyCode,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "COPIAR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentRed
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar",
                            tint = accentRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallRequestCard(
    request: InstallApprovalRequest,
    isMyRequest: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textColor: Color,
    subtextColor: Color,
    accentRed: Color,
    accentGreen: Color,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accentRed.copy(alpha = 0.08f))
            .border(1.5.dp, accentRed, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("install_request_card_${request.requestId}")
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentRed.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alerta",
                        tint = accentRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isMyRequest) "SUA SOLICITAÇÃO DE INSTALAÇÃO" else "PEDIDO DE INSTALAÇÃO DE AMIGO",
                        fontFamily = BlockyFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = accentRed,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "App Bloqueado: ${request.targetAppName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isMyRequest) {
                    "Você solicitou a instalação de ${request.targetAppName}. Aguardando autorização de um amigo do grupo para liberar por 10 minutos.\n\n⚠️ Lembrete: Você não pode sair da party enquanto a solicitação estiver em andamento."
                } else {
                    "${request.requesterName} está tentando instalar ${request.targetAppName} (que está na lista de bloqueio). Ele só poderá instalar se você autorizar!"
                },
                fontSize = 12.sp,
                color = subtextColor,
                lineHeight = 17.sp
            )

            if (!isMyRequest) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("friends_approve_install_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Aprovar", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PERMITIR (10m)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onReject,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("friends_reject_install_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Recusar", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RECUSAR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaveRequestCard(
    request: LeaveApprovalRequest,
    isMyRequest: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textColor: Color,
    subtextColor: Color,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF59E0B).copy(alpha = 0.08f))
            .border(1.5.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("leave_request_card_${request.requestId}")
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Saída",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isMyRequest) "SEU PEDIDO DE DESPAREAMENTO" else "PEDIDO DE SAÍDA DE AMIGO",
                        fontFamily = BlockyFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = Color(0xFFF59E0B)
                    )
                    Text(
                        text = if (isMyRequest) "Aguardando aprovação de um amigo..." else "${request.requesterName} quer sair do grupo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isMyRequest) {
                    "Para evitar despareamentos impulsivos, sua saída só será concluída se um amigo do grupo aprovar."
                } else {
                    "${request.requesterName} pediu para sair da party e desparear. Você autoriza a saída?"
                },
                fontSize = 12.sp,
                color = subtextColor
            )

            if (!isMyRequest) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("friends_approve_leave_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("PERMITIR SAÍDA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onReject,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("friends_reject_leave_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("NEGAR SAÍDA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemberStatsCard(
    member: PartyMember,
    isMe: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textColor: Color,
    subtextColor: Color,
    accentRed: Color,
    accentGreen: Color
) {
    val context = LocalContext.current
    val usedMinutes = (member.screenTimeSecondsToday / 60L).toInt()
    val limitMinutes = member.screenTimeLimitMinutes
    val progress = (usedMinutes.toFloat() / limitMinutes.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
            .padding(18.dp)
            .testTag("member_stats_card_${member.userId}")
    ) {
        Column {
            // Member Identity Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isMe) accentRed.copy(alpha = 0.15f) else Color(0xFF3B82F6).copy(alpha = 0.15f))
                            .border(1.dp, if (isMe) accentRed.copy(alpha = 0.4f) else Color(0xFF3B82F6).copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.displayName.take(1).uppercase(),
                            fontWeight = FontWeight.Black,
                            fontFamily = BlockyFontFamily,
                            fontSize = 16.sp,
                            color = if (isMe) accentRed else Color(0xFF3B82F6)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = member.displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = textColor
                            )
                            if (isMe) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(Você)",
                                    fontSize = 11.sp,
                                    color = accentRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = if (member.isOnline) "🟢 Conectado" else "⚪ Offline",
                            fontSize = 11.sp,
                            color = if (member.isOnline) accentGreen else subtextColor
                        )
                    }
                }

                // Respect Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF27272A))
                        .border(1.dp, Color(0xFF3F3F46), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${member.respectPercentage}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (member.respectPercentage >= 85) accentGreen else Color(0xFFF59E0B)
                        )
                        Text(
                            text = "RESPEITO",
                            fontSize = 8.sp,
                            fontFamily = BlockyFontFamily,
                            color = subtextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val isDark = LocalIsDarkTheme.current
            val canViewDetailed = isMe || member.shareDetailedScreenTime

            // Screen Time Progress Row (or Privacy Locked Box if hidden by friend)
            if (canViewDetailed) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TEMPO DE TELA",
                                fontSize = 10.sp,
                                fontFamily = BlockyFontFamily,
                                color = subtextColor
                            )
                            if (isMe) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (member.shareDetailedScreenTime) "• COMPARTILHADO" else "• PRIVADO",
                                    fontSize = 8.5.sp,
                                    fontFamily = BlockyFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = if (member.shareDetailedScreenTime) accentGreen else Color(0xFFF59E0B)
                                )
                            }
                        }
                        Text(
                            text = "${usedMinutes / 60}h ${usedMinutes % 60}m / ${limitMinutes / 60}h ${limitMinutes % 60}m",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (progress >= 1f) accentRed else if (progress > 0.8f) Color(0xFFF59E0B) else accentGreen,
                        trackColor = Color(0xFF27272A)
                    )
                }
            } else {
                // Privacy Locked Box for friend who opted not to share screen time
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF202024) else Color(0xFFF4F4F6))
                        .border(1.dp, if (isDark) cardBorder else CmfBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6B7280).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Privado",
                                tint = if (isDark) Color(0xFF9CA3AF) else Color(0xFF4B5563),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TEMPO DE TELA E APPS PRIVADOS",
                                fontFamily = BlockyFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Este aliado optou por não exibir o tempo de tela total e uso individual de apps.",
                                fontSize = 10.sp,
                                color = subtextColor,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }

            if (isMe) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color(0xFF202024) else Color(0xFFF4F4F6))
                        .border(1.dp, if (isDark) cardBorder else CmfBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (member.shareDetailedScreenTime) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Privacidade",
                            tint = if (member.shareDetailedScreenTime) accentGreen else Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "COMPARTILHAR TEMPO TOTAL & APPS",
                                fontSize = 9.5.sp,
                                fontFamily = BlockyFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                text = if (member.shareDetailedScreenTime)
                                    "Visível para aliados no Friends Panel"
                                else
                                    "Oculto: aliados só veem seu Streak e Respeito",
                                fontSize = 9.sp,
                                color = subtextColor
                            )
                        }
                    }

                    Switch(
                        checked = member.shareDetailedScreenTime,
                        onCheckedChange = { isChecked ->
                            FriendsPartyManager.setSharingDetailedScreenTime(context, isChecked)
                            android.widget.Toast.makeText(
                                context,
                                if (isChecked) "Estatísticas detalhadas agora visíveis para amigos! 👁️" else "Estatísticas detalhadas ocultadas dos amigos! 🔒",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentGreen,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFF6B7280)
                        ),
                        modifier = Modifier.testTag("member_privacy_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val pillBg = if (isDark) Color(0xFF202024) else Color(0xFFF4F4F6)
            val pillBorder = if (isDark) cardBorder else CmfBorder
            val respectHours = member.respectMinutesToday / 60
            val respectMins = member.respectMinutesToday % 60
            val respectTimeFormatted = if (respectHours > 0) "${respectHours}h ${respectMins}m" else "${respectMins}m"

            // Stats Quick Pills: Streak, Daily Respect Time & Violations
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(pillBg)
                        .border(1.dp, pillBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔥", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(text = "STREAK", fontSize = 8.sp, fontFamily = BlockyFontFamily, color = subtextColor)
                            Text(text = "${member.streakDays} dias", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = textColor)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(pillBg)
                        .border(1.dp, pillBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⏱️", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(text = "RESPEITO HOJE", fontSize = 8.sp, fontFamily = BlockyFontFamily, color = subtextColor)
                            Text(text = respectTimeFormatted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = textColor)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(pillBg)
                        .border(1.dp, pillBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🛡️", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(text = "VIOLAÇÕES", fontSize = 8.sp, fontFamily = BlockyFontFamily, color = subtextColor)
                            Text(text = "${member.violationsToday}", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = textColor)
                        }
                    }
                }
            }

            // Top Apps Used (only visible if member allows detailed sharing or is self)
            if (canViewDetailed && member.topApps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "APPS MAIS USADOS HOJE",
                    fontSize = 10.sp,
                    fontFamily = BlockyFontFamily,
                    color = subtextColor
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    member.topApps.forEach { app ->
                        val appMin = (app.usageSeconds / 60L).toInt()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(pillBg)
                                .border(1.dp, pillBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = app.appName,
                                    fontSize = 11.sp,
                                    color = textColor,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${appMin}m",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = accentRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Quick Cheer & Nudge Actions for friends
            if (!isMe) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            FriendsPartyManager.sendCheer(context, member.displayName)
                            android.widget.Toast.makeText(context, "Você mandou força para ${member.displayName}! 💪", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(38.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("💪 Mandar Força", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            FriendsPartyManager.sendNudge(context, member.displayName)
                            android.widget.Toast.makeText(context, "Você cobrou foco de ${member.displayName}! ⏰", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(38.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("⏰ Cobrar Foco", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun LeavePartySection(
    cardBg: Color,
    cardBorder: Color,
    textColor: Color,
    subtextColor: Color,
    accentRed: Color,
    hasPendingInstall: Boolean,
    onLeaveClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column {
            Text(
                text = "SAIR DA PARTY / DESPAREAMENTO",
                fontFamily = BlockyFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = textColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (hasPendingInstall) {
                    "⚠️ Bloqueado: Há uma solicitação de instalação pendente. O Lock In proíbe sair da party enquanto a requisição estiver em andamento!"
                } else {
                    "Para evitar despareamentos por impulso, a saída exige aprovação de outro membro do grupo."
                },
                fontSize = 11.sp,
                color = if (hasPendingInstall) accentRed else subtextColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onLeaveClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("friends_request_leave_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasPendingInstall) Color(0xFF3F3F46) else accentRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Sair",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SOLICITAR SAÍDA / DESPAREAMENTO",
                    fontFamily = BlockyFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun PartyPrivacyControlCard(
    isSharing: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textColor: Color,
    subtextColor: Color,
    accentColor: Color,
    onToggleSharing: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(1.2.dp, cardBorder, RoundedCornerShape(18.dp))
            .padding(18.dp)
            .testTag("party_privacy_control_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                            .background(if (isSharing) Color(0xFF22C55E).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSharing) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Privacidade",
                            tint = if (isSharing) Color(0xFF22C55E) else Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PRIVACIDADE DO TEMPO DE TELA",
                            fontFamily = BlockyFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = textColor,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (isSharing) "Compartilhando tempo total e apps" else "Tempo total e apps privados",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSharing) Color(0xFF22C55E) else Color(0xFFF59E0B)
                        )
                    }
                }

                Switch(
                    checked = isSharing,
                    onCheckedChange = onToggleSharing,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF22C55E),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFF6B7280)
                    ),
                    modifier = Modifier.testTag("party_privacy_main_toggle")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Escolha se os outros membros da Party podem ver quanto tempo de tela absoluto você usou hoje e quanto tempo usou em cada aplicativo individual.",
                fontSize = 11.5.sp,
                color = subtextColor,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) Color(0xFF202024) else Color(0xFFF4F4F6))
                    .border(1.dp, if (isDark) Color(0xFF33333C) else CmfBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🛡️", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "A sequência diária (Streak) e o tempo de respeito aos limites continuam sempre visíveis para manter o compromisso mútuo.",
                        fontSize = 10.5.sp,
                        color = subtextColor,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

