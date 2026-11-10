package com.example.ui.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LockInLogoIcon
import com.example.ui.theme.BlockyFontFamily
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Tactile spring press animation modifier:
 * Compresses slightly with spring physics when touched, giving an industrial physical button feel.
 */
fun Modifier.tactilePress(
    scaleDown: Float = 0.96f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tactile_scale"
    )

    val mod = this.scale(scale)
    if (onClick != null) {
        mod.clickable(
            interactionSource = interactionSource,
            indication = ripple(color = Color.Black.copy(alpha = 0.15f)),
            onClick = {
                com.example.util.AppHaptics.tap(context)
                onClick()
            }
        )
    } else {
        mod
    }
}

/**
 * Animated rolling text:
 * Rolls numbers vertically when text changes (like a split-flap or digital segment clock).
 */
@Composable
fun AnimatedRollingText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontFamily: FontFamily = BlockyFontFamily,
    fontWeight: FontWeight = FontWeight.Bold,
    letterSpacing: androidx.compose.ui.unit.TextUnit = 0.sp
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            val isAscending = targetState > initialState
            val enterSpec = spring<IntOffset>(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
            val exitSpec = spring<IntOffset>(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )

            if (isAscending) {
                (slideInVertically(animationSpec = enterSpec) { height -> height } + fadeIn())
                    .togetherWith(slideOutVertically(animationSpec = exitSpec) { height -> -height } + fadeOut())
            } else {
                (slideInVertically(animationSpec = enterSpec) { height -> -height } + fadeIn())
                    .togetherWith(slideOutVertically(animationSpec = exitSpec) { height -> height } + fadeOut())
            }.using(SizeTransform(clip = false))
        },
        label = "rolling_text_transition",
        modifier = modifier
    ) { targetText ->
        Text(
            text = targetText,
            style = style,
            color = color,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            letterSpacing = letterSpacing
        )
    }
}

/**
 * Slide to Action Button (Deslize para Iniciar / Bloquear):
 * Tactile pill slider with draggable knob, animated chevron trails, and spring snap.
 */
@Composable
fun CmfSlideActionButton(
    text: String,
    accentColor: Color,
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    thumbSize: Dp = 46.dp
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val thumbSizePx = with(density) { thumbSize.toPx() }
    val offsetX = remember { Animatable(0f) }
    var maxDragPx by remember { mutableFloatStateOf(1f) }

    // Pulsing chevron animation
    val infiniteTransition = rememberInfiniteTransition(label = "chevron_pulse")
    val chevronAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chevron_alpha"
    )

    fun triggerHapticFeedback() = com.example.util.AppHaptics.tap(context)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(CmfDark)
            .border(1.2.dp, CmfBorder, RoundedCornerShape(height / 2))
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidthPx = with(density) { maxWidth.toPx() }
        val paddingPx = with(density) { 10.dp.toPx() }
        maxDragPx = (totalWidthPx - thumbSizePx - paddingPx).coerceAtLeast(1f)

        // Track background label + chevrons
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(
                    (1f - (offsetX.value / maxDragPx) * 1.5f).coerceIn(0f, 1f)
                )
            ) {
                Text(
                    text = text.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BlockyFontFamily,
                    color = CmfWhite,
                    letterSpacing = 1.5.sp
                )
                Row(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .alpha(chevronAlpha)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Draggable Knob (Industrial Thumb with LockIn icon)
        val dragFraction = (offsetX.value / maxDragPx).coerceIn(0f, 1f)
        val knobColor = if (dragFraction > 0.8f) accentColor else CmfWhite

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(knobColor)
                .border(
                    width = 1.dp,
                    color = if (dragFraction > 0.8f) accentColor else CmfBorder,
                    shape = CircleShape
                )
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        coroutineScope.launch {
                            val newOffset = (offsetX.value + delta).coerceIn(0f, maxDragPx)
                            offsetX.snapTo(newOffset)
                        }
                    },
                    onDragStopped = {
                        if (offsetX.value >= maxDragPx * 0.75f) {
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    maxDragPx,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                                triggerHapticFeedback()
                                onConfirmed()
                                // Snap back after triggering
                                offsetX.animateTo(
                                    0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        } else {
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            LockInLogoIcon(
                size = 18.dp,
                tint = if (dragFraction > 0.8f) Color.White else accentColor
            )
        }
    }
}

/**
 * Concentric pulsing radar/focus aura:
 * Used on active lock / focus screens to give a subtle breathing aura around the LockIn icon.
 */
@Composable
fun PulsingFocusAura(
    color: Color,
    modifier: Modifier = Modifier,
    maxRadius: Dp = 100.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "focus_aura")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, delayMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )

    Canvas(modifier = modifier.size(maxRadius * 2)) {
        val centerOffset = Offset(size.width / 2f, size.height / 2f)

        // First ring
        val r1 = (size.width / 2f) * pulse1
        val alpha1 = (1f - pulse1).coerceIn(0f, 0.4f)
        drawCircle(
            color = color.copy(alpha = alpha1),
            radius = r1,
            center = centerOffset,
            style = Stroke(width = 2.dp.toPx())
        )

        // Second ring
        val r2 = (size.width / 2f) * pulse2
        val alpha2 = (1f - pulse2).coerceIn(0f, 0.4f)
        drawCircle(
            color = color.copy(alpha = alpha2),
            radius = r2,
            center = centerOffset,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

/**
 * Analog Mechanical Stepper / Lever:
 * An industrial vertical notched lever slider inspired by analog controls.
 * Allows quick snapping or smooth dragging to adjust minutes with tactile notches.
 */
@Composable
fun CmfAnalogLever(
    currentMinutes: Int,
    onMinutesChanged: (Int) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    stepsList: List<Int> = listOf(5, 10, 15, 30, 45, 60, 90, 120)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Current index in stepsList or closest
    val closestIndex = stepsList.indexOfFirst { it >= currentMinutes }.let { if (it == -1) stepsList.size - 1 else it }
    var selectedIndex by remember(currentMinutes) { mutableStateOf(closestIndex) }

    fun triggerTick() = com.example.util.AppHaptics.tap(context)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CmfLightGray.copy(alpha = 0.5f))
            .border(1.dp, CmfBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            stepsList.forEachIndexed { index, minutes ->
                val isSelected = index == selectedIndex
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.08f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "notch_scale"
                )

                Box(
                    modifier = Modifier
                        .scale(scale)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) accentColor else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) accentColor else CmfBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            selectedIndex = index
                            triggerTick()
                            onMinutesChanged(minutes)
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (minutes >= 60) "${minutes / 60}h" else "${minutes}m",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        fontFamily = BlockyFontFamily,
                        color = if (isSelected) Color.White else CmfDark
                    )
                }
            }
        }
    }
}
