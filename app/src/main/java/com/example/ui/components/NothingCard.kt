package com.example.ui.components
import com.example.ui.animation.tactilePress
import com.example.ui.theme.BlockyFontFamily

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.LocalCustomTextColor

@Composable
fun NothingCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    badge: String? = null,
    backgroundColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    cornerRadius: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val accentColor = LocalCustomTextColor.current
    val resolvedBg = if (backgroundColor != Color.Unspecified) backgroundColor else MaterialTheme.colorScheme.surface
    val resolvedBorder = if (borderColor != Color.Unspecified) borderColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    val clickableModifier = if (onClick != null) {
        modifier.tactilePress(scaleDown = 0.97f, onClick = onClick)
    } else {
        modifier
    }

    Box(
        modifier = clickableModifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(resolvedBg)
            .border(1.dp, resolvedBorder, RoundedCornerShape(cornerRadius))
            .padding(16.dp)
    ) {

        Column(modifier = Modifier.fillMaxWidth()) {
            if (title != null || badge != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (title != null) {
                        Text(
                            text = title.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = BlockyFontFamily,
                            color = CmfMutedText,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    if (title != null && badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                    } else if (title == null) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (badge != null) {
                        Box(
                            modifier = Modifier
                                .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badge.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = BlockyFontFamily,
                                color = accentColor,
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            content()
        }
    }
}
