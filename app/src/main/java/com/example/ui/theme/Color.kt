package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val EmeraldPrimary = Color(0xFF10B981)
val EmeraldDark = Color(0xFF047857)
val EmeraldLight = Color(0xFFA7F3D0)

val SkyPrimary = Color(0xFF0284C7)
val VioletPrimary = Color(0xFF8B5CF6)
val AmberPrimary = Color(0xFFF59E0B)
val RosePrimary = Color(0xFFF43F5E)
val IndigoPrimary = Color(0xFF6366F1)

val AmoledBlack = Color(0xFF000000)
val AmoledSurface = Color(0xFF0B0D13)
val AmoledSurfaceVariant = Color(0xFF151922)

// Preset color palette for habits
val DefaultHabitPalette = listOf(
    "#10B981", // Emerald
    "#0284C7", // Sky
    "#8B5CF6", // Violet
    "#EC4899", // Pink
    "#F59E0B", // Amber
    "#EF4444", // Red
    "#06B6D4", // Cyan
    "#84CC16", // Lime
    "#6366F1", // Indigo
    "#F97316"  // Orange
)

fun parseHexColor(hexString: String, fallback: Color = EmeraldPrimary): Color {
    return try {
        val cleanHex = hexString.trim().removePrefix("#")
        val colorInt = when (cleanHex.length) {
            6 -> ("FF$cleanHex").toLong(16)
            8 -> cleanHex.toLong(16)
            else -> return fallback
        }
        Color(colorInt)
    } catch (_: Exception) {
        fallback
    }
}

fun Color.toHexString(): String {
    val a = (alpha * 255).toInt()
    val r = (red * 255).toInt()
    val g = (green * 255).toInt()
    val b = (blue * 255).toInt()
    return String.format("#%02X%02X%02X", r, g, b)
}
