package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.example.ui.components.TextInputProtection

val LocalCustomTextColor = compositionLocalOf { CmfDefaultOrange }
val LocalIsDarkTheme = compositionLocalOf { false }

private val CmfColorScheme = lightColorScheme(
    primary = CmfDefaultOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFF0EB),
    onPrimaryContainer = CmfDefaultOrange,
    secondary = CmfDark,
    onSecondary = Color.White,
    background = Color(0xFFF7F7F9),
    onBackground = Color(0xFF111827),
    surface = CmfWhite,
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFFE5E7EB),
    outlineVariant = Color(0xFFD1D5DB)
)

private val CmfDarkColorScheme = darkColorScheme(
    primary = CmfDefaultOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2E1508),
    onPrimaryContainer = Color(0xFFFF7A33),
    secondary = Color(0xFFF3F4F6),
    onSecondary = Color(0xFF111827),
    background = Color(0xFF0F0F12),
    onBackground = Color(0xFFF4F4F5),
    surface = Color(0xFF18181C),
    onSurface = Color(0xFFF4F4F5),
    surfaceVariant = Color(0xFF202026),
    onSurfaceVariant = Color(0xFFD1D5DB),
    outline = Color(0xFF383844),
    outlineVariant = Color(0xFF282832)
)

@Composable
fun LockInTheme(
    customTextColor: Color = CmfDefaultOrange,
    themeMode: String = "LIGHT",
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemDark
    }

    val baseScheme = if (isDark) CmfDarkColorScheme else CmfColorScheme
    val dynamicScheme = baseScheme.copy(
        primary = customTextColor,
        primaryContainer = customTextColor.copy(alpha = if (isDark) 0.2f else 0.12f),
        onPrimaryContainer = customTextColor
    )

    val platformHaptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val gatedHaptics = androidx.compose.runtime.remember(platformHaptics, context) {
        object : androidx.compose.ui.hapticfeedback.HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: androidx.compose.ui.hapticfeedback.HapticFeedbackType) {
                if (com.example.util.AppHaptics.isEnabled(context)) {
                    platformHaptics.performHapticFeedback(hapticFeedbackType)
                }
            }
        }
    }
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(
        LocalCustomTextColor provides customTextColor,
        LocalIsDarkTheme provides isDark,
        androidx.compose.ui.platform.LocalHapticFeedback provides gatedHaptics
    ) {
        MaterialTheme(
            colorScheme = dynamicScheme,
            typography = Typography
        ) {
            TextInputProtection {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                    content = content
                )
            }
        }
    }
}
