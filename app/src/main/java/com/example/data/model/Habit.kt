package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class HabitType {
    YES_NO,
    COUNT
}

enum class FrequencyType {
    EVERY_DAY,
    SPECIFIC_DAYS,
    TIMES_PER_WEEK,
    TIMES_PER_MONTH
}

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val type: String = HabitType.YES_NO.name,
    val target: Float = 1f,
    val unit: String = "",
    val colorHex: String = "#10B981", // Emerald default
    val iconName: String = "check_circle",
    val frequencyType: String = FrequencyType.EVERY_DAY.name,
    val frequencyDays: String = "1,2,3,4,5,6,7", // 1=Mon .. 7=Sun
    val frequencyTarget: Int = 7,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val reminderDays: String = "1,2,3,4,5,6,7",
    val orderIndex: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isCountType: Boolean get() = type == HabitType.COUNT.name

    fun isFrequencyDay(dayOfWeekValue: Int): Boolean {
        return when (frequencyType) {
            FrequencyType.EVERY_DAY.name -> true
            FrequencyType.SPECIFIC_DAYS.name -> {
                val days = frequencyDays.split(",").mapNotNull { it.trim().toIntOrNull() }
                dayOfWeekValue in days
            }
            else -> true
        }
    }
}
