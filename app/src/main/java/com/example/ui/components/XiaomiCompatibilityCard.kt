package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Backward compatibility wrapper forwarding directly to the unified [DeviceInstructionsCard],
 * which automatically detects Samsung, Xiaomi (HyperOS/MIUI), Motorola, Realme, etc.,
 * and displays ONLY the exact device-specific instructions.
 */
@Composable
fun XiaomiCompatibilityCard(
    accentColor: Color,
    modifier: Modifier = Modifier,
    isDeviceXiaomi: Boolean = true
) {
    DeviceInstructionsCard(
        accentColor = accentColor,
        modifier = modifier
    )
}
