package com.example.ui.components
import com.example.ui.theme.BlockyFontFamily

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import com.example.ui.theme.LocalCustomTextColor
import kotlin.math.roundToInt

@Composable
fun FloatingIslandCapsule(
    timeText: String = "08:42",
    badgeText: String = "SHORTS",
    sizeMode: String = "MEDIUM",
    opacity: Float = 0.95f,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalCustomTextColor.current

    val (paddingH, paddingV, fontSize, badgeSize) = when (sizeMode) {
        "SMALL" -> Quad(8.dp, 4.dp, 12.sp, 8.sp)
        "LARGE" -> Quad(16.dp, 10.dp, 18.sp, 11.sp)
        else -> Quad(12.dp, 6.dp, 15.sp, 9.sp) // MEDIUM
    }

    Row(
        modifier = modifier
            .alpha(opacity.coerceIn(0.2f, 1.0f))
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(CmfWhite)
            .border(1.dp, CmfBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = paddingH, vertical = paddingV),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Dot indicator
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = badgeText.uppercase(),
                fontSize = badgeSize,
                fontWeight = FontWeight.Bold,
                fontFamily = BlockyFontFamily,
                color = CmfMutedText,
                letterSpacing = 0.5.sp
            )
            Text(
                text = timeText,
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                fontFamily = BlockyFontFamily,
                color = accentColor,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color(0xFF2A2A30))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(2.5.dp)
                        .background(accentColor)
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun InteractiveIslandCanvas(
    sizeMode: String,
    opacity: Float,
    posXRatio: Float,
    posYRatio: Float,
    onPositionChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFAFAFA))
            .border(1.dp, CmfBorder, RoundedCornerShape(12.dp))
    ) {
        DotMatrixPattern(modifier = Modifier.fillMaxSize(), spacingDp = 18f)

        Text(
            text = "ÁREA DE ARRASTO • POSICIONE A ILHA",
            fontSize = 9.sp,
            fontFamily = BlockyFontFamily,
            color = CmfMutedText,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )

        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()

        var currentX by remember(posXRatio) { mutableFloatStateOf(maxWidthPx * posXRatio) }
        var currentY by remember(posYRatio) { mutableFloatStateOf(maxHeightPx * posYRatio) }

        FloatingIslandCapsule(
            timeText = "09:30",
            badgeText = "LOCK IN",
            sizeMode = sizeMode,
            opacity = opacity,
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = currentX.coerceIn(0f, (maxWidthPx - 260f).coerceAtLeast(0f)).roundToInt(),
                        y = currentY.coerceIn(0f, (maxHeightPx - 100f).coerceAtLeast(0f)).roundToInt()
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        currentX = (currentX + dragAmount.x).coerceIn(0f, maxWidthPx - 200f)
                        currentY = (currentY + dragAmount.y).coerceIn(0f, maxHeightPx - 80f)
                        val ratioX = (currentX / maxWidthPx).coerceIn(0f, 1f)
                        val ratioY = (currentY / maxHeightPx).coerceIn(0f, 1f)
                        onPositionChanged(ratioX, ratioY)
                    }
                }
        )
    }
}
