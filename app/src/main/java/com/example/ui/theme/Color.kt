package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// CMF Nothing OS Palette
val CmfWhite = Color(0xFFFFFFFF)
val CmfOffWhite = Color(0xFFF7F7F8)
val CmfCardBg = Color(0xFFFFFFFF)
val CmfBorder = Color(0xFFE5E5E5)
val CmfBorderHighlight = Color(0xFFCCCCCC)
val CmfDotGrid = Color(0xFFE0E0E0)
val CmfDark = Color(0xFF161616)
val CmfMutedText = Color(0xFF757575)
val CmfLightGray = Color(0xFFF0F0F0)

// Default CMF Orange
val CmfDefaultOrange = Color(0xFFFF5500)

// Configurable Text & Accent Colors
val TextColorPresets = listOf(
    NamedColor("CMF Orange", Color(0xFFFF5500), "#FF5500"),
    NamedColor("Vibrant Red", Color(0xFFE53935), "#E53935"),
    NamedColor("Pure Black", Color(0xFF111111), "#111111"),
    NamedColor("Electric Blue", Color(0xFF0066FF), "#0066FF"),
    NamedColor("Neon Volt", Color(0xFFC6FF00), "#C6FF00"),
    NamedColor("Acid Green", Color(0xFF00E676), "#00E676"),
    NamedColor("Sunset Peach", Color(0xFFFF7043), "#FF7043"),
    NamedColor("Deep Violet", Color(0xFF7C4DFF), "#7C4DFF")
)

data class NamedColor(
    val name: String,
    val color: Color,
    val hex: String
)
