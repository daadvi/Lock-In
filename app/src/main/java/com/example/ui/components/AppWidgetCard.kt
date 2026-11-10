package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.RestrictedAppEntity
import com.example.ui.animation.tactilePress
import com.example.ui.theme.BlockyFontFamily
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.LocalIsDarkTheme

@Composable
fun AppWidgetCard(
    app: RestrictedAppEntity,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSecs = app.totalAvailableSeconds
    val totalLimitSecs = (app.totalLimitMinutes * 60L).coerceAtLeast(1L)
    val remainingMins = (totalSecs / 60).coerceAtLeast(0)
    val remainingSecs = (totalSecs % 60).coerceAtLeast(0)
    val formattedTime = if (remainingMins >= 60) {
        val h = remainingMins / 60
        val m = remainingMins % 60
        if (m == 0L) "${h}h" else String.format("%dh %02dm", h, m)
    } else {
        String.format("%02d:%02d", remainingMins, remainingSecs)
    }
    val isZero = app.isLimitDepleted || app.isBlocked || (totalSecs <= 0 && app.totalLimitMinutes > 0)
    val isDark = LocalIsDarkTheme.current
    val targetProgress = (totalSecs.toFloat() / totalLimitSecs).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "widget_progress"
    )

    val surfaceBg = MaterialTheme.colorScheme.surface
    val cardBg = if (isZero) (if (isDark) Color(0xFF351215) else Color(0xFFFEE2E2)) else surfaceBg
    val borderCol = if (isZero) Color(0xFFEF4444) else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (isZero) 2.dp else 1.dp
    val textColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(
                width = borderWidth,
                color = borderCol,
                shape = RoundedCornerShape(16.dp)
            )
            .tactilePress(scaleDown = 0.95f, onClick = onClick)
            .padding(14.dp)
            .testTag("app_widget_${app.packageName}")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Real App Icon + Status / Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Real App Icon using AppIconImage
                AppIconImage(
                    packageName = app.packageName,
                    fallbackLetter = app.appName.firstOrNull()?.uppercase() ?: "A",
                    sizeDp = 34.dp
                )

                if (app.isBlocked || isZero) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEF4444), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(9.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "ESGOTADO",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = BlockyFontFamily,
                                color = Color.White
                            )
                        }
                    }
                } else if (app.limitShortVideosOnly) {
                    Box(
                        modifier = Modifier
                            .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SÓ SHORTS",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = BlockyFontFamily,
                            color = accentColor
                        )
                    }
                } else if (!app.useAbsoluteTime) {
                    Box(
                        modifier = Modifier
                            .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "VÍDEOS",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = BlockyFontFamily,
                            color = accentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Middle: App Name
            Text(
                text = app.appName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom: Remaining Time Digital Display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (isZero) "00:00" else formattedTime,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        color = if (isZero) Color(0xFFEF4444) else textColor,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isZero) "esgotado" else "disp.",
                        fontSize = 10.sp,
                        fontFamily = BlockyFontFamily,
                        color = if (isZero) Color(0xFFEF4444) else CmfMutedText,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                if (isZero) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Bloqueio Rígido",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Animated Capacity Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(if (isDark) Color(0xFF27272A) else CmfLightGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(if (isZero) 0f else animatedProgress)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(if (isZero) Color(0xFFEF4444) else accentColor)
                )
            }
        }
    }
}
