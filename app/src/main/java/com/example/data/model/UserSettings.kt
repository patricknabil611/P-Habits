package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey
    val id: Int = 1,
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK, AMOLED
    val dynamicColor: Boolean = true,
    val accentColor: String = "#10B981", // Preset accent if dynamic color is off
    val cellShape: String = "ROUNDED_SQUARE", // CIRCLE, ROUNDED_SQUARE, SQUARE
    val cellSize: String = "NORMAL", // COMPACT, NORMAL, LARGE
    val doneStyle: String = "FILLED", // FILLED, CHECK_ICON
    val notDoneStyle: String = "FADED", // EMPTY, X_MARK, FADED
    val countDisplayStyle: String = "PROGRESS_RING", // NUMBER_ONLY, NUMBER_TARGET, PROGRESS_RING
    val chartStyle: String = "BAR", // BAR, LINE
    val showGridLines: Boolean = true,
    val showBarValues: Boolean = true,
    val heatmapColorMode: String = "HABIT_COLOR", // HABIT_COLOR, SINGLE_ACCENT
    val firstDayOfWeek: String = "MONDAY", // MONDAY, SUNDAY, SATURDAY
    val is24HourTime: Boolean = true,
    val showStreaksOnHome: Boolean = true,
    val showArchived: Boolean = false,
    val customColorPalette: String = "#10B981,#3B82F6,#8B5CF6,#EC4899,#F59E0B,#EF4444,#06B6D4,#84CC16,#6366F1,#F97316",
    val autoBackupEnabled: Boolean = false,
    val lastBackupTimestamp: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)
