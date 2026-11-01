package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.LocalIsDarkTheme

/**
 * Clean, solid background with zero dots or points as requested.
 * Provides a minimal, fluid, Nothing OS-inspired solid surface.
 */
@Composable
fun DotMatrixPattern(
    modifier: Modifier = Modifier,
    dotColor: Color = Color.Unspecified,
    spacingDp: Float = 24f,
    dotRadiusDp: Float = 1.2f
) {
    val isDark = LocalIsDarkTheme.current
    val solidBgColor = if (isDark) Color(0xFF0F0F12) else Color(0xFFF7F7F9)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(solidBgColor)
    )
}


