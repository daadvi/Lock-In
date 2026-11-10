package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.data.model.LockLevel

data class LockLevelColors(val accent: Color, val end: Color) {
    fun background(selected: Boolean): Brush = Brush.horizontalGradient(
        listOf(accent.copy(alpha = if (selected) .20f else .09f), end.copy(alpha = if (selected) .18f else .06f)))
}

@Composable
fun lockLevelColors(level: LockLevel): LockLevelColors {
    val dark = LocalIsDarkTheme.current
    return when (level) {
        LockLevel.BASIC -> LockLevelColors(if (dark) Color(0xFF73DDB0) else Color(0xFF167653), Color(0xFF48B9A0))
        LockLevel.DETERMINED -> LockLevelColors(if (dark) Color(0xFFF0C35A) else Color(0xFF8D620D), Color(0xFFD39A28))
        LockLevel.DEV_DREAM -> LockLevelColors(if (dark) Color(0xFFFF873E) else Color(0xFFB64312), Color(0xFFE33D48))
    }
}
