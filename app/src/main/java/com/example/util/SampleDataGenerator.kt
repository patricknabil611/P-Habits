package com.example.util

import com.example.data.model.FrequencyType
import com.example.data.model.Habit
import com.example.data.model.HabitEntry
import com.example.data.model.HabitType
import java.time.LocalDate
import kotlin.random.Random

object SampleDataGenerator {

    fun generateSampleHabits(): List<Habit> {
        val now = System.currentTimeMillis()
        return listOf(
            Habit(
                id = 1L,
                name = "Morning Meditation",
                description = "Mindful breathing and stillness for 10 min",
                type = HabitType.YES_NO.name,
                target = 1f,
                unit = "",
                colorHex = "#10B981", // Emerald
                iconName = "self_improvement",
                frequencyType = FrequencyType.EVERY_DAY.name,
                frequencyDays = "1,2,3,4,5,6,7",
                frequencyTarget = 7,
                reminderEnabled = true,
                reminderHour = 7,
                reminderMinute = 30,
                orderIndex = 0,
                createdAt = now - 60L * 86400000L
            ),
            Habit(
                id = 2L,
                name = "Drink Water",
                description = "Stay hydrated throughout the day",
                type = HabitType.COUNT.name,
                target = 8f,
                unit = "glasses",
                colorHex = "#0284C7", // Sky Blue
                iconName = "water_drop",
                frequencyType = FrequencyType.EVERY_DAY.name,
                frequencyDays = "1,2,3,4,5,6,7",
                frequencyTarget = 7,
                reminderEnabled = true,
                reminderHour = 9,
                reminderMinute = 0,
                orderIndex = 1,
                createdAt = now - 60L * 86400000L
            ),
            Habit(
                id = 3L,
                name = "Read Books",
                description = "Expand knowledge and focus",
                type = HabitType.COUNT.name,
                target = 20f,
                unit = "pages",
                colorHex = "#8B5CF6", // Purple/Violet
                iconName = "menu_book",
                frequencyType = FrequencyType.EVERY_DAY.name,
                frequencyDays = "1,2,3,4,5,6,7",
                frequencyTarget = 7,
                reminderEnabled = true,
                reminderHour = 21,
                reminderMinute = 0,
                orderIndex = 2,
                createdAt = now - 60L * 86400000L
            ),
            Habit(
                id = 4L,
                name = "Strength Workout",
                description = "Gym or bodyweight training session",
                type = HabitType.YES_NO.name,
                target = 1f,
                unit = "",
                colorHex = "#F59E0B", // Amber
                iconName = "fitness_center",
                frequencyType = FrequencyType.SPECIFIC_DAYS.name,
                frequencyDays = "1,3,5,6", // Mon, Wed, Fri, Sat
                frequencyTarget = 4,
                reminderEnabled = true,
                reminderHour = 17,
                reminderMinute = 30,
                orderIndex = 3,
                createdAt = now - 60L * 86400000L
            ),
            Habit(
                id = 5L,
                name = "Evening Walk",
                description = "Brisk outdoor steps before dinner",
                type = HabitType.COUNT.name,
                target = 6000f,
                unit = "steps",
                colorHex = "#F43F5E", // Rose
                iconName = "directions_walk",
                frequencyType = FrequencyType.EVERY_DAY.name,
                frequencyDays = "1,2,3,4,5,6,7",
                frequencyTarget = 7,
                reminderEnabled = false,
                reminderHour = 19,
                reminderMinute = 0,
                orderIndex = 4,
                createdAt = now - 60L * 86400000L
            ),
            Habit(
                id = 6L,
                name = "Code & Learn",
                description = "Kotlin / Compose architecture practice",
                type = HabitType.YES_NO.name,
                target = 1f,
                unit = "",
                colorHex = "#6366F1", // Indigo
                iconName = "code",
                frequencyType = FrequencyType.SPECIFIC_DAYS.name,
                frequencyDays = "1,2,3,4,5", // Mon-Fri
                frequencyTarget = 5,
                reminderEnabled = false,
                reminderHour = 20,
                reminderMinute = 0,
                orderIndex = 5,
                createdAt = now - 60L * 86400000L
            )
        )
    }

    fun generateSampleEntries(habits: List<Habit>, daysBack: Int = 45): List<HabitEntry> {
        val today = DateUtils.today()
        val entries = mutableListOf<HabitEntry>()
        val random = Random(42) // deterministic seed so it's consistent

        for (habit in habits) {
            for (offset in daysBack downTo 0) {
                val date = today.minusDays(offset.toLong())
                val dayOfWeekVal = date.dayOfWeek.value
                val isDue = habit.isFrequencyDay(dayOfWeekVal)

                if (!isDue) continue

                // High completion chance in recent days to show nice streaks
                val completionProbability = when {
                    offset <= 7 -> 0.90
                    offset <= 18 -> 0.85
                    offset <= 30 -> 0.78
                    else -> 0.70
                }

                val isDone = random.nextDouble() < completionProbability

                if (isDone) {
                    val entryValue = if (habit.isCountType) {
                        // either exactly target or slightly more, e.g. 8 glasses or 9 glasses
                        val extra = (random.nextInt(3)) * (if (habit.target >= 100) 500 else 1)
                        habit.target + extra
                    } else {
                        1f
                    }
                    entries.add(
                        HabitEntry(
                            habitId = habit.id,
                            dateString = DateUtils.toDateString(date),
                            value = entryValue
                        )
                    )
                } else {
                    // For count habits, sometimes there's partial progress (e.g. 4/8 glasses)
                    if (habit.isCountType && random.nextDouble() < 0.4) {
                        val partial = if (habit.target >= 1000) {
                            (random.nextInt(3) + 1) * 1500f
                        } else {
                            (random.nextInt(habit.target.toInt().coerceAtLeast(2) - 1) + 1).toFloat()
                        }
                        entries.add(
                            HabitEntry(
                                habitId = habit.id,
                                dateString = DateUtils.toDateString(date),
                                value = partial
                            )
                        )
                    }
                }
            }
        }
        return entries
    }
}
