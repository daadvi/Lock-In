package com.example.ui.components

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.PartyAlert
import com.example.data.model.PartyMember
import com.example.service.FriendsPartyManager
import com.example.ui.theme.BlockyFontFamily
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import com.example.ui.theme.LocalIsDarkTheme

@Composable
fun FocusRankingPodiumCard(
    members: List<PartyMember>,
    cardBg: Color,
    cardBorder: Color,
    textColor: Color,
    subtextColor: Color,
    accentRed: Color,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val sorted = members.sortedWith(
        compareByDescending<PartyMember> { it.respectPercentage }
            .thenByDescending { it.streakDays }
            .thenBy { it.screenTimeSecondsToday }
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(1.2.dp, cardBorder, RoundedCornerShape(18.dp))
            .padding(18.dp)
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
                            .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Ranking",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "RANKING DE FOCO DA PARTY",
                            fontFamily = BlockyFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = textColor,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Pódio baseado em aderência, streak e tempo de respeito",
                            fontSize = 10.sp,
                            color = subtextColor
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "LEADERBOARD",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BlockyFontFamily,
                        color = Color(0xFFF59E0B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Podium Top 3 Items
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sorted.take(3).forEachIndexed { index, member ->
                    val (medal, medalColor, positionLabel) = when (index) {
                        0 -> Triple("🥇", Color(0xFFF59E0B), "1º LUGAR")
                        1 -> Triple("🥈", Color(0xFF9CA3AF), "2º LUGAR")
                        else -> Triple("🥉", Color(0xFFCD7F32), "3º LUGAR")
                    }

                    val respectHours = member.respectMinutesToday / 60
                    val respectMins = member.respectMinutesToday % 60
                    val respectTimeFormatted = if (respectHours > 0) "${respectHours}h ${respectMins}m" else "${respectMins}m"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF222228) else Color(0xFFF4F4F6))
                            .border(1.dp, if (index == 0) medalColor.copy(alpha = 0.6f) else cardBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
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
                                Text(
                                    text = medal,
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = member.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = textColor
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = positionLabel,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = medalColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "🔥 ${member.streakDays}d streak",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentRed,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "⏱️ $respectTimeFormatted de respeito",
                                            fontSize = 10.sp,
                                            color = subtextColor,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }

                            // Score / Respect %
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(medalColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${member.respectPercentage}%",
                                    fontWeight = FontWeight.Black,
                                    fontFamily = BlockyFontFamily,
                                    fontSize = 12.sp,
                                    color = medalColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveActivityFeedCard(
    alerts: List<PartyAlert>,
    cardBg: Color,
    cardBorder: Color,
    textColor: Color,
    subtextColor: Color,
    accentRed: Color,
    onCheerAll: () -> Unit,
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
                            .background(accentRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Alertas",
                            tint = accentRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ALERTAS DE VACILO & FEED",
                            fontFamily = BlockyFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = textColor,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Notificações instantâneas quando um aliado estoura o limite",
                            fontSize = 10.sp,
                            color = subtextColor
                        )
                    }
                }

                TextButton(onClick = onCheerAll) {
                    Text("💪 Força Geral", fontSize = 10.sp, color = accentRed, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (alerts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color(0xFF222228) else Color(0xFFF4F4F6))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎯 Nenhum vacilo hoje! Todos os aliados estão no foco total.",
                        fontSize = 11.sp,
                        color = subtextColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    alerts.take(5).forEach { alert ->
                        val (icon, bgCol, borderCol, tagCol, tagLabel) = when (alert.alertType) {
                            "OVER_LIMIT" -> Tuple5(
                                Icons.Default.Warning,
                                if (isDark) Color(0xFF331416) else Color(0xFFFEE2E2),
                                Color(0xFFEF4444).copy(alpha = 0.5f),
                                Color(0xFFEF4444),
                                "VACILO"
                            )
                            "CHEER" -> Tuple5(
                                Icons.Default.NotificationsActive,
                                if (isDark) Color(0xFF132B1C) else Color(0xFFDCFCE7),
                                Color(0xFF22C55E).copy(alpha = 0.5f),
                                Color(0xFF22C55E),
                                "FORÇA"
                            )
                            else -> Tuple5(
                                Icons.Default.NotificationsActive,
                                if (isDark) Color(0xFF2A2213) else Color(0xFFFEF3C7),
                                Color(0xFFF59E0B).copy(alpha = 0.5f),
                                Color(0xFFF59E0B),
                                "COBRANÇA"
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(bgCol)
                                .border(1.dp, borderCol, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(tagCol, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = tagLabel,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${alert.memberName} ${alert.message}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textColor,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val a: A,
    val b: B,
    val c: C,
    val d: D,
    val e: E
)
