package com.example.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.LocalCustomTextColor

/**
 * Standard text field colors ensuring high contrast and readability across
 * both Light and Dark modes. Completely resolves font visibility issues
 * where input text appeared too faint or washed out.
 */
@Composable
fun cmfTextFieldColors(
    accentColor: Color = LocalCustomTextColor.current,
    isError: Boolean = false
): TextFieldColors {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = onSurface,
        unfocusedTextColor = onSurface,
        disabledTextColor = onSurface.copy(alpha = 0.4f),
        focusedPlaceholderColor = onSurfaceVariant.copy(alpha = 0.8f),
        unfocusedPlaceholderColor = onSurfaceVariant.copy(alpha = 0.8f),
        focusedBorderColor = if (isError) Color.Red else accentColor,
        unfocusedBorderColor = if (isError) Color.Red else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        focusedContainerColor = surface,
        unfocusedContainerColor = surfaceVariant.copy(alpha = 0.4f),
        cursorColor = accentColor,
        focusedLeadingIconColor = onSurfaceVariant,
        unfocusedLeadingIconColor = onSurfaceVariant,
        focusedTrailingIconColor = onSurfaceVariant,
        unfocusedTrailingIconColor = onSurfaceVariant
    )
}
