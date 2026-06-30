package com.example.data

import androidx.compose.ui.graphics.Color

data class ThemeOption(
    val name: String,
    val color: Color,
    val description: String
)

object ThemeRepository {
    val themes = listOf(
        ThemeOption("Deep Black", Color(0xFF0A0A0A), "Pure OLED black. Battery friendly"),
        ThemeOption("Ink Black", Color(0xFF0D0F14), "Slight blue tint. Softer than black"),
        ThemeOption("Charcoal", Color(0xFF111318), "Samsung One UI style"),
        ThemeOption("Slate Dark", Color(0xFF15161A), "Professional"),
        ThemeOption("Graphite", Color(0xFF1A1B1F), "Google Pixel style"),
        ThemeOption("Navy Dark", Color(0xFF0B1020), "Rich depth"),
        ThemeOption("Steel Blue", Color(0xFF0F141D), "Apple + Nothing OS"),
        ThemeOption("Forest Dark", Color(0xFF0E120F), "For reading"),
        ThemeOption("Cocoa Dark", Color(0xFF1A1410), "Luxury leather"),
        ThemeOption("Stone Dark", Color(0xFF222325), "Material 3")
    )
}
