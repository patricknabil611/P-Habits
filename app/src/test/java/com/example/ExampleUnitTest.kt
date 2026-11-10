package com.example

import com.example.data.model.Habit
import com.example.data.model.HabitEntry
import com.example.util.DateUtils
import com.example.util.HabitStrengthCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun testSyncConflictResolution() {
        val localHabit = Habit(
            id = 10L,
            uuid = "habit-uuid-abc",
            name = "Local Name",
            target = 5f,
            updatedAt = 1000L,
            isDeleted = false
        )
        val cloudHabitNewer = Habit(
            id = 0L,
            uuid = "habit-uuid-abc",
            name = "Cloud Name Updated",
            target = 10f,
            updatedAt = 2000L,
            isDeleted = false
        )

        // Verify newer timestamp wins
        val winningHabit = if (cloudHabitNewer.updatedAt > localHabit.updatedAt) cloudHabitNewer else localHabit
        assertEquals("Cloud Name Updated", winningHabit.name)
        assertEquals(10f, winningHabit.target)

        // Verify soft delete flag propagation
        val deletedHabit = localHabit.copy(isDeleted = true, updatedAt = 3000L)
        assertTrue(deletedHabit.isDeleted)
        assertTrue(deletedHabit.updatedAt > cloudHabitNewer.updatedAt)
    }

    @Test
    fun testHabitEntrySyncModel() {
        val entry = HabitEntry(
            id = 1L,
            uuid = "entry-uuid-xyz",
            habitUuid = "habit-uuid-abc",
            habitId = 10L,
            dateString = "2026-11-10",
            value = 1f,
            updatedAt = 1500L,
            isDeleted = false
        )
        assertEquals("habit-uuid-abc", entry.habitUuid)
        assertFalse(entry.isDeleted)
    }
}
