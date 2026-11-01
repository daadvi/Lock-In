package com.example.ui.components
import com.example.ui.animation.AnimatedRollingText
import com.example.ui.animation.CmfAnalogLever
import com.example.ui.animation.tactilePress
import com.example.ui.theme.BlockyFontFamily

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import com.example.ui.theme.LocalCustomTextColor

@Composable
fun DigitalClockDisplay(
    totalMinutes: Int,
    onMinutesChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "TEMPO LIMITE",
    subLabel: String? = null
) {
    val accentColor = LocalCustomTextColor.current
    var showTimePickerDialog by remember { mutableStateOf(false) }
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    if (showTimePickerDialog) {
        CmfTimePickerDialog(
            initialHour = hours,
            initialMinute = minutes,
            title = label,
            accentColor = accentColor,
            onConfirm = { h, m ->
                val total = (h * 60 + m).coerceIn(1, 1440)
                onMinutesChanged(total)
                showTimePickerDialog = false
            },
            onDismiss = { showTimePickerDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CmfWhite)
            .border(1.2.dp, CmfBorder, RoundedCornerShape(12.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BlockyFontFamily,
                    color = CmfMutedText,
                    letterSpacing = 1.sp
                )
                if (subLabel != null) {
                    Text(
                        text = subLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = BlockyFontFamily,
                        color = accentColor
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CmfLightGray)
                    .clickable { showTimePickerDialog = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Abrir Relógio",
                        tint = CmfDark,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Relógio",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BlockyFontFamily,
                        color = CmfDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large CMF Digital Clock Numbers
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Hours box
            DigitalSegmentBox(
                digits = String.format("%02d", hours),
                subText = "HORAS",
                textColor = accentColor
            )

            Text(
                text = ":",
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = BlockyFontFamily,
                color = accentColor,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Minutes box
            DigitalSegmentBox(
                digits = String.format("%02d", minutes),
                subText = "MINUTOS",
                textColor = accentColor
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Analog Notched Stepper / Lever (Tactile analog control)
        CmfAnalogLever(
            currentMinutes = totalMinutes,
            onMinutesChanged = onMinutesChanged,
            accentColor = accentColor,
            stepsList = listOf(5, 15, 30, 45, 60, 90, 120)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Stepper Quick Controls: -15m, -5m, +5m, +15m
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickStepButton(
                text = "-15m",
                onClick = { onMinutesChanged((totalMinutes - 15).coerceAtLeast(1)) }
            )
            QuickStepButton(
                text = "-5m",
                onClick = { onMinutesChanged((totalMinutes - 5).coerceAtLeast(1)) }
            )
            QuickStepButton(
                text = "+5m",
                onClick = { onMinutesChanged((totalMinutes + 5).coerceAtMost(720)) }
            )
            QuickStepButton(
                text = "+15m",
                onClick = { onMinutesChanged((totalMinutes + 15).coerceAtMost(720)) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Smooth Slider
        Slider(
            value = totalMinutes.toFloat(),
            onValueChange = { onMinutesChanged(it.toInt().coerceIn(1, 180)) },
            valueRange = 1f..180f,
            steps = 35,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("digital_clock_slider"),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = CmfLightGray
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "1 min",
                fontSize = 11.sp,
                fontFamily = BlockyFontFamily,
                color = CmfMutedText
            )
            Text(
                text = "3 horas",
                fontSize = 11.sp,
                fontFamily = BlockyFontFamily,
                color = CmfMutedText
            )
        }
    }
}

@Composable
private fun DigitalSegmentBox(
    digits: String,
    subText: String,
    textColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(CmfLightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .border(1.dp, CmfBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        AnimatedRollingText(
            text = digits,
            color = textColor,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            style = LocalTextStyle.current.copy(fontSize = 44.sp)
        )
        Text(
            text = subText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = BlockyFontFamily,
            color = CmfMutedText,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun QuickStepButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CmfLightGray)
            .border(1.dp, CmfBorder, RoundedCornerShape(8.dp))
            .tactilePress(scaleDown = 0.93f, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = BlockyFontFamily,
            color = CmfDark
        )
    }
}
