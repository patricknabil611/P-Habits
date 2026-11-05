package com.example

import com.example.data.model.Habit
import com.example.data.model.HabitEntry
import com.example.util.DateUtils
import com.example.util.HabitStrengthCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testHabitStrengthAndStreaks() {
        val habit = Habit(
            id = 1L,
            name = "Morning Meditation",
            target = 1f
        )
        val today = LocalDate.of(2026, 11, 3)
        val entries = (0..9).map { offset ->
            HabitEntry(
                habitId = 1L,
                dateString = DateUtils.toDateString(today.minusDays(offset.toLong())),
                value = 1f
            )
        }

        val analytics = HabitStrengthCalculator.calculateAnalytics(habit, entries, today)
        assertEquals(10, analytics.currentStreak)
        assertTrue(analytics.currentScore > 50f)
        assertEquals(10, analytics.totalCompletions)
    }

    @Test
    fun testDateUtilsWeekDays() {
        val date = LocalDate.of(2026, 11, 3) // Tuesday
        val weekDays = DateUtils.getWeekDays(date)
        assertEquals(7, weekDays.size)
        assertEquals(LocalDate.of(2026, 11, 2), weekDays.first()) // Monday
        assertEquals(LocalDate.of(2026, 11, 8), weekDays.last()) // Sunday
    }
}
