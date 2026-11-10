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
    val solidBgColor = androidx.compose.material3.MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(solidBgColor)
    )
}


