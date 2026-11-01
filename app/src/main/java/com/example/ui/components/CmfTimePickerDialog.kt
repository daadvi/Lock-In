package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.BlockyFontFamily
import com.example.ui.theme.CmfMutedText
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Nothing CMF Style Interactive Analog/Digital Clock Dialog.
 * Allows setting ANY custom time (0-23 hours and 0-59 minutes)
 * with an interactive radial dial, digital toggle inputs, and fine steppers.
 */
@Composable
fun CmfTimePickerDialog(
    initialHour: Int = 12,
    initialMinute: Int = 0,
    title: String = "DEFINIR HORÁRIO",
    accentColor: Color,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(initialHour.coerceIn(0, 23)) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }
    var isSelectingHour by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .padding(20.dp)
                .testTag("cmf_time_picker_dialog")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Title
                Text(
                    text = title.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = BlockyFontFamily,
                    color = accentColor,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Big Nothing Digital Display: [ HH ] : [ MM ]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Hour Box
                    val hourBg by animateColorAsState(
                        if (isSelectingHour) accentColor.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        label = "hour_bg"
                    )
                    val hourBorder by animateColorAsState(
                        if (isSelectingHour) accentColor
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        label = "hour_border"
                    )

                    Box(
                        modifier = Modifier
                            .size(width = 82.dp, height = 70.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(hourBg)
                            .border(1.5.dp, hourBorder, RoundedCornerShape(12.dp))
                            .clickable { isSelectingHour = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%02d", selectedHour),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = BlockyFontFamily,
                                color = if (isSelectingHour) accentColor else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "HORA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CmfMutedText
                            )
                        }
                    }

                    Text(
                        text = ":",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Minute Box
                    val minBg by animateColorAsState(
                        if (!isSelectingHour) accentColor.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        label = "min_bg"
                    )
                    val minBorder by animateColorAsState(
                        if (!isSelectingHour) accentColor
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        label = "min_border"
                    )

                    Box(
                        modifier = Modifier
                            .size(width = 82.dp, height = 70.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(minBg)
                            .border(1.5.dp, minBorder, RoundedCornerShape(12.dp))
                            .clickable { isSelectingHour = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%02d", selectedMinute),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = BlockyFontFamily,
                                color = if (!isSelectingHour) accentColor else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "MIN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CmfMutedText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive CMF Radial Clock Face
                CmfRadialClockFace(
                    value = if (isSelectingHour) selectedHour else selectedMinute,
                    maxValue = if (isSelectingHour) 24 else 60,
                    isHourMode = isSelectingHour,
                    accentColor = accentColor,
                    onValueChange = { newVal ->
                        if (isSelectingHour) {
                            selectedHour = newVal % 24
                        } else {
                            selectedMinute = newVal % 60
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Fine Steppers: -5m, -1m, +1m, +5m
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CmfPillButton(label = "-1h") { selectedHour = (selectedHour - 1 + 24) % 24 }
                    CmfPillButton(label = "-5m") { selectedMinute = (selectedMinute - 5 + 60) % 60 }
                    CmfPillButton(label = "+1m") { selectedMinute = (selectedMinute + 1) % 60 }
                    CmfPillButton(label = "+5m") { selectedMinute = (selectedMinute + 5) % 60 }
                    CmfPillButton(label = "+1h") { selectedHour = (selectedHour + 1) % 24 }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: Cancel and Confirm
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
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
                        onClick = { onConfirm(selectedHour, selectedMinute) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("CONFIRMAR", fontFamily = BlockyFontFamily, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun CmfPillButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = BlockyFontFamily,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CmfRadialClockFace(
    value: Int,
    maxValue: Int,
    isHourMode: Boolean,
    accentColor: Color,
    onValueChange: (Int) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Clean, minimalist color palette
    val dialBackgroundColor = if (isDark) Color(0xFF141418) else Color(0xFFF7F7FA)
    val dialBorderColor = if (isDark) Color(0xFF262630) else Color(0xFFE2E2EA)
    val subtleTickColor = if (isDark) Color(0xFF383844) else Color(0xFFD4D4DE)
    val primaryNumColor = if (isDark) Color(0xFFEDEDF2) else Color(0xFF1A1A22)
    val secondaryNumColor = if (isDark) Color(0xFFA0A0B0) else Color(0xFF6E6E7E)

    Box(
        modifier = Modifier
            .size(246.dp)
            .pointerInput(isHourMode) {
                fun processOffset(offset: Offset, sizePx: Float) {
                    val center = sizePx / 2f
                    val dx = offset.x - center
                    val dy = offset.y - center
                    var angle = (atan2(dy.toDouble(), dx.toDouble()) * (180.0 / PI)).toFloat() + 90f
                    if (angle < 0f) angle += 360f

                    val outerR = sizePx / 2f - 24.dp.toPx()
                    val innerR = outerR * 0.65f
                    val midR = (outerR + innerR) / 2f
                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)

                    if (isHourMode) {
                        var h = ((angle / 30f).roundToInt()) % 12
                        if (h == 0) h = 12

                        if (dist < midR) {
                            // Inner circle (24h format): 13..23, and 00
                            val h24 = if (h == 12) 0 else h + 12
                            onValueChange(h24)
                        } else {
                            // Outer circle: 1..12
                            onValueChange(h)
                        }
                    } else {
                        // 60-minute mapping (each minute is 6 degrees)
                        val minute = ((angle / 6f).roundToInt()) % 60
                        onValueChange(minute)
                    }
                }

                detectTapGestures { offset ->
                    processOffset(offset, size.width.toFloat())
                }
            }
            .pointerInput(isHourMode) {
                detectDragGestures { change, _ ->
                    val center = size.width / 2f
                    val dx = change.position.x - center
                    val dy = change.position.y - center
                    var angle = (atan2(dy.toDouble(), dx.toDouble()) * (180.0 / PI)).toFloat() + 90f
                    if (angle < 0f) angle += 360f

                    val outerR = size.width / 2f - 24.dp.toPx()
                    val innerR = outerR * 0.65f
                    val midR = (outerR + innerR) / 2f
                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)

                    if (isHourMode) {
                        var h = ((angle / 30f).roundToInt()) % 12
                        if (h == 0) h = 12

                        if (dist < midR) {
                            val h24 = if (h == 12) 0 else h + 12
                            onValueChange(h24)
                        } else {
                            onValueChange(h)
                        }
                    } else {
                        val minute = ((angle / 6f).roundToInt()) % 60
                        onValueChange(minute)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(246.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.width / 2f - 24.dp.toPx()
            val innerRadius = outerRadius * 0.65f

            // Clean, smooth circular background disc
            drawCircle(
                color = dialBackgroundColor,
                radius = size.width / 2f - 4.dp.toPx(),
                center = center
            )
            // Delicate hairline perimeter border
            drawCircle(
                color = dialBorderColor,
                radius = size.width / 2f - 4.dp.toPx(),
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )

            // Very subtle track ring for outer circle
            drawCircle(
                color = dialBorderColor.copy(alpha = 0.5f),
                radius = outerRadius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )

            if (isHourMode) {
                // Subtle track ring for inner 24h circle
                drawCircle(
                    color = dialBorderColor.copy(alpha = 0.35f),
                    radius = innerRadius,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.8.dp.toPx())
                )
            } else {
                // In minute mode, draw 60 subtle minimalist dots for minutes not divisible by 5
                for (m in 0 until 60) {
                    if (m % 5 != 0) {
                        val dotAngle = (m * 6.0 - 90.0) * (PI / 180.0)
                        val dotX = center.x + outerRadius * cos(dotAngle).toFloat()
                        val dotY = center.y + outerRadius * sin(dotAngle).toFloat()
                        drawCircle(
                            color = subtleTickColor,
                            radius = 1.2.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                    }
                }
            }

            // Target position calculation
            val isInnerSelected = isHourMode && ((value in 13..23) || (value == 0))
            val targetRadius = if (isInnerSelected) innerRadius else outerRadius
            val angleRad = if (isHourMode) {
                val hourForAngle = if (value == 0) 12 else (value % 12)
                (hourForAngle * 30.0 - 90.0) * (PI / 180.0)
            } else {
                (value * 6.0 - 90.0) * (PI / 180.0)
            }

            val thumbX = center.x + targetRadius * cos(angleRad).toFloat()
            val thumbY = center.y + targetRadius * sin(angleRad).toFloat()

            // Slim, sleek pointer line
            drawLine(
                color = accentColor,
                start = center,
                end = Offset(thumbX, thumbY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Minimalist center hub
            drawCircle(
                color = accentColor,
                radius = 4.dp.toPx(),
                center = center
            )
            drawCircle(
                color = dialBackgroundColor,
                radius = 2.dp.toPx(),
                center = center
            )

            // Sleek selection thumb disc
            val thumbRadius = if (isInnerSelected) 13.dp.toPx() else 15.dp.toPx()
            drawCircle(
                color = accentColor,
                radius = thumbRadius,
                center = Offset(thumbX, thumbY)
            )

            // Draw numbers
            if (isHourMode) {
                // 1. Outer Circle: 1..12
                for (i in 1..12) {
                    val numAngle = (i * 30.0 - 90.0) * (PI / 180.0)
                    val numX = center.x + outerRadius * cos(numAngle).toFloat()
                    val numY = center.y + outerRadius * sin(numAngle).toFloat()
                    val isCurrentSelection = !isInnerSelected && (value == i || (value == 0 && i == 12) || (value == 12 && i == 12))

                    val textLayout = textMeasurer.measure(
                        text = AnnotatedString("$i"),
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = if (isCurrentSelection) FontWeight.Black else FontWeight.Bold,
                            fontFamily = BlockyFontFamily,
                            color = if (isCurrentSelection) Color.White else primaryNumColor
                        )
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(numX - textLayout.size.width / 2f, numY - textLayout.size.height / 2f)
                    )
                }

                // 2. Inner Circle: 13..23 and 00
                for (i in 1..12) {
                    val innerVal = if (i == 12) 0 else i + 12
                    val label = if (innerVal == 0) "00" else "$innerVal"
                    val numAngle = (i * 30.0 - 90.0) * (PI / 180.0)
                    val numX = center.x + innerRadius * cos(numAngle).toFloat()
                    val numY = center.y + innerRadius * sin(numAngle).toFloat()
                    val isCurrentSelection = isInnerSelected && (value == innerVal)

                    val textLayout = textMeasurer.measure(
                        text = AnnotatedString(label),
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = if (isCurrentSelection) FontWeight.Black else FontWeight.Medium,
                            fontFamily = BlockyFontFamily,
                            color = if (isCurrentSelection) Color.White else secondaryNumColor
                        )
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(numX - textLayout.size.width / 2f, numY - textLayout.size.height / 2f)
                    )
                }
            } else {
                // Minute Mode: 00, 05, 10, ... 55
                for (m in 0 until 60 step 5) {
                    val numAngle = (m * 6.0 - 90.0) * (PI / 180.0)
                    val numX = center.x + outerRadius * cos(numAngle).toFloat()
                    val numY = center.y + outerRadius * sin(numAngle).toFloat()
                    val isCurrentSelection = (value == m)

                    val label = String.format("%02d", m)
                    val textLayout = textMeasurer.measure(
                        text = AnnotatedString(label),
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = if (isCurrentSelection) FontWeight.Black else FontWeight.Bold,
                            fontFamily = BlockyFontFamily,
                            color = if (isCurrentSelection) Color.White else primaryNumColor
                        )
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(numX - textLayout.size.width / 2f, numY - textLayout.size.height / 2f)
                    )
                }
            }
        }
    }
}
