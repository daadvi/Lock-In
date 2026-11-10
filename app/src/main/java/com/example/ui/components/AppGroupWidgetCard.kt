package com.example.ui.components
import com.example.ui.animation.tactilePress
import com.example.ui.theme.BlockyFontFamily

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.data.local.AppGroupEntity
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite

@Composable
fun AppGroupWidgetCard(
    group: AppGroupEntity,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val remainingMins = (group.remainingSeconds / 60).coerceAtLeast(0)
    val remainingSecs = (group.remainingSeconds % 60).coerceAtLeast(0)
    val formattedTime = String.format("%02d:%02d", remainingMins, remainingSecs)
    val isZero = group.remainingSeconds <= 0
    val packageList = group.getPackageList()
    val totalLimitSecs = (group.limitMinutes * 60).coerceAtLeast(1)
    val targetProgress = (group.remainingSeconds.toFloat() / totalLimitSecs).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "group_progress"
    )

    val surfaceBg = MaterialTheme.colorScheme.surface
    val borderCol = if (isZero) accentColor else MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceBg)
            .border(
                width = if (isZero) 1.5.dp else 1.dp,
                color = borderCol,
                shape = RoundedCornerShape(16.dp)
            )
            .tactilePress(scaleDown = 0.95f, onClick = onClick)
            .padding(14.dp)
            .testTag("app_group_widget_${group.id}")
    ) {

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Miniature app icons cluster + Set badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Miniature app icons stack/row
                Row(
                    horizontalArrangement = Arrangement.spacedBy((-6).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    packageList.take(3).forEach { pkg ->
                        MiniAppIcon(packageName = pkg, context = context)
                    }
                    if (packageList.size > 3) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(CmfDark)
                                .border(1.5.dp, CmfWhite, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+${packageList.size - 3}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Badge
                Box(
                    modifier = Modifier
                        .background(
                            if (isZero) accentColor else accentColor.copy(alpha = 0.12f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    if (isZero) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(9.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "BLOQUEADO",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = BlockyFontFamily,
                                color = Color.White
                            )
                        }
                    } else {
                        Text(
                            text = "CONJUNTO",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = BlockyFontFamily,
                            color = accentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Middle: App Set Name
            Text(
                text = group.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = BlockyFontFamily,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${packageList.size} apps inclusos",
                fontSize = 10.sp,
                fontFamily = BlockyFontFamily,
                color = CmfMutedText
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom: Remaining shared time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formattedTime,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        color = if (isZero) accentColor else textColor,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isZero) "esgotado" else "disp.",
                        fontSize = 10.sp,
                        fontFamily = BlockyFontFamily,
                        color = if (isZero) accentColor else CmfMutedText,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                if (isZero) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Desbloquear",
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MiniAppIcon(packageName: String, context: Context, sizeDp: Int = 24) {
    AppIconImage(
        packageName = packageName,
        sizeDp = sizeDp.dp,
        cornerRadius = (sizeDp / 2).dp
    )
}

