package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private fun createCustomDarkScheme(accent: Color, isAmoled: Boolean) = darkColorScheme(
    primary = accent,
    onPrimary = Color.Black,
    primaryContainer = accent.copy(alpha = 0.25f),
    onPrimaryContainer = Color.White,
    secondary = accent.copy(alpha = 0.85f),
    onSecondary = Color.Black,
    tertiary = Color(0xFF67E8F9),
    background = if (isAmoled) AmoledBlack else Color(0xFF0F172A),
    onBackground = Color(0xFFF1F5F9),
    surface = if (isAmoled) AmoledSurface else Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = if (isAmoled) AmoledSurfaceVariant else Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569)
)

private fun createCustomLightScheme(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.15f),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = accent.copy(alpha = 0.85f),
    onSecondary = Color.White,
    tertiary = Color(0xFF0284C7),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun PHabitsTheme(
    themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK", "AMOLED"
    dynamicColor: Boolean = true,
    accentHex: String = "#10B981",
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode.uppercase()) {
        "LIGHT" -> false
        "DARK", "AMOLED" -> true
        else -> isSystemDark
    }
    val isAmoled = themeMode.uppercase() == "AMOLED"
    val accent = parseHexColor(accentHex, EmeraldPrimary)

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val base = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (isAmoled) {
                base.copy(
                    background = AmoledBlack,
                    surface = AmoledSurface,
                    surfaceVariant = AmoledSurfaceVariant
                )
            } else {
                base
            }
        }
        isDark -> createCustomDarkScheme(accent, isAmoled)
        else -> createCustomLightScheme(accent)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
