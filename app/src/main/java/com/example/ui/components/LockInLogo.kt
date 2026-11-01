package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlockyFontFamily

/**
 * Minimalist geometric lock/focus icon embodying CMF Nothing OS aesthetics:
 * A clean modern padlock integrated with a central focal tick/dot.
 */
@Composable
fun LockInLogoIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val strokeWidth = (w * 0.08f).coerceAtLeast(1.5f)
        val bracketLen = w * 0.22f
        val bracketInset = w * 0.04f

        // 1. Four Viewfinder Focus Corner Brackets
        // Top-Left bracket
        drawLine(color = tint, start = Offset(bracketInset, bracketInset + bracketLen), end = Offset(bracketInset, bracketInset), strokeWidth = strokeWidth)
        drawLine(color = tint, start = Offset(bracketInset, bracketInset), end = Offset(bracketInset + bracketLen, bracketInset), strokeWidth = strokeWidth)

        // Top-Right bracket
        drawLine(color = tint, start = Offset(w - bracketInset - bracketLen, bracketInset), end = Offset(w - bracketInset, bracketInset), strokeWidth = strokeWidth)
        drawLine(color = tint, start = Offset(w - bracketInset, bracketInset), end = Offset(w - bracketInset, bracketInset + bracketLen), strokeWidth = strokeWidth)

        // Bottom-Left bracket
        drawLine(color = tint, start = Offset(bracketInset, h - bracketInset - bracketLen), end = Offset(bracketInset, h - bracketInset), strokeWidth = strokeWidth)
        drawLine(color = tint, start = Offset(bracketInset, h - bracketInset), end = Offset(bracketInset + bracketLen, h - bracketInset), strokeWidth = strokeWidth)

        // Bottom-Right bracket
        drawLine(color = tint, start = Offset(w - bracketInset - bracketLen, h - bracketInset), end = Offset(w - bracketInset, h - bracketInset), strokeWidth = strokeWidth)
        drawLine(color = tint, start = Offset(w - bracketInset, h - bracketInset - bracketLen), end = Offset(w - bracketInset, h - bracketInset), strokeWidth = strokeWidth)

        // 2. Padlock Shackle (upper arch)
        val shackleWidth = w * 0.40f
        val shackleHeight = h * 0.34f
        val shackleLeft = (w - shackleWidth) / 2f
        val shackleTop = h * 0.16f

        drawRoundRect(
            color = tint,
            topLeft = Offset(shackleLeft, shackleTop),
            size = Size(shackleWidth, shackleHeight),
            cornerRadius = CornerRadius(shackleWidth / 2f, shackleWidth / 2f),
            style = Stroke(width = strokeWidth * 1.1f)
        )

        // 3. Padlock Body (bottom rounded rectangle)
        val bodyWidth = w * 0.58f
        val bodyHeight = h * 0.44f
        val bodyLeft = (w - bodyWidth) / 2f
        val bodyTop = h * 0.40f
        val bodyCorner = w * 0.12f

        drawRoundRect(
            color = tint,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(bodyCorner, bodyCorner)
        )

        // 4. Embedded Clock / Timer Dial inside the lock body
        val clockCenter = Offset(w / 2f, bodyTop + bodyHeight * 0.50f)
        val clockRadius = bodyHeight * 0.32f

        // Clock background circle
        drawCircle(
            color = Color.Black.copy(alpha = 0.85f),
            radius = clockRadius,
            center = clockCenter
        )

        // Active countdown timer arc
        drawArc(
            color = tint,
            startAngle = -90f,
            sweepAngle = 240f,
            useCenter = false,
            topLeft = Offset(clockCenter.x - clockRadius * 0.75f, clockCenter.y - clockRadius * 0.75f),
            size = Size(clockRadius * 1.5f, clockRadius * 1.5f),
            style = Stroke(width = strokeWidth * 0.9f)
        )

        // Center pivot dot & clock hand pointing to 2 o'clock
        drawCircle(
            color = Color.White,
            radius = strokeWidth * 0.7f,
            center = clockCenter
        )
        drawLine(
            color = Color.White,
            start = clockCenter,
            end = Offset(clockCenter.x + clockRadius * 0.45f, clockCenter.y - clockRadius * 0.45f),
            strokeWidth = strokeWidth * 0.75f
        )
    }
}

/**
 * Complete header branding: Minimalist logo icon + "LOCK IN" text in blocky typography.
 */
@Composable
fun LockInBranding(
    modifier: Modifier = Modifier,
    iconSize: Dp = 20.dp,
    textSize: TextUnit = 15.sp,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    iconTint: Color = Color(0xFFFF5500)
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LockInLogoIcon(
            size = iconSize,
            tint = iconTint
        )
        Text(
            text = "LOCK IN",
            fontSize = textSize,
            fontWeight = FontWeight.Black,
            fontFamily = BlockyFontFamily,
            color = textColor,
            letterSpacing = 2.5.sp
        )
    }
}
