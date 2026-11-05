package com.example.data.repository

import com.example.data.local.HabitDao
import com.example.data.local.HabitEntryDao
import com.example.data.local.UserSettingsDao
import com.example.data.model.Habit
import com.example.data.model.HabitEntry
import com.example.data.model.UserSettings
import com.example.util.SampleDataGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HabitRepository(
    private val habitDao: HabitDao,
    private val habitEntryDao: HabitEntryDao,
    private val userSettingsDao: UserSettingsDao
) {
    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()
    val activeHabits: Flow<List<Habit>> = habitDao.getActiveHabits()
    val allEntries: Flow<List<HabitEntry>> = habitEntryDao.getAllEntries()

    val userSettings: Flow<UserSettings> = userSettingsDao.getSettings().map {
        it ?: UserSettings()
    }

    fun getEntriesForHabit(habitId: Long): Flow<List<HabitEntry>> {
        return habitEntryDao.getEntriesForHabit(habitId)
    }

    suspend fun getEntriesForHabitOnce(habitId: Long): List<HabitEntry> {
        return habitEntryDao.getEntriesForHabitOnce(habitId)
    }

    suspend fun getAllEntriesOnce(): List<HabitEntry> {
        return habitEntryDao.getAllEntriesOnce()
    }

    suspend fun getSettingsOnce(): UserSettings {
        return userSettingsDao.getSettingsOnce() ?: UserSettings()
    }

    suspend fun updateSettings(settings: UserSettings) {
        userSettingsDao.insertSettings(settings)
    }

    suspend fun toggleYesNoEntry(habitId: Long, dateString: String) {
        val existing = habitEntryDao.getEntry(habitId, dateString)
        if (existing == null || existing.value <= 0f) {
            habitEntryDao.insertEntry(
                HabitEntry(
                    habitId = habitId,
                    dateString = dateString,
                    value = 1f
                )
            )
        } else {
            habitEntryDao.deleteEntry(habitId, dateString)
        }
    }

    suspend fun incrementCountEntry(habitId: Long, dateString: String, delta: Float = 1f, target: Float = 1f) {
        val existing = habitEntryDao.getEntry(habitId, dateString)
        val currentValue = existing?.value ?: 0f
        val newValue = currentValue + delta
        habitEntryDao.insertEntry(
            HabitEntry(
                id = existing?.id ?: 0,
                habitId = habitId,
                dateString = dateString,
                value = newValue
            )
        )
    }

    suspend fun setCountEntry(habitId: Long, dateString: String, value: Float) {
        if (value <= 0f) {
            habitEntryDao.deleteEntry(habitId, dateString)
        } else {
            val existing = habitEntryDao.getEntry(habitId, dateString)
            habitEntryDao.insertEntry(
                HabitEntry(
                    id = existing?.id ?: 0,
                    habitId = habitId,
                    dateString = dateString,
                    value = value
                )
            )
        }
    }

    suspend fun saveHabit(habit: Habit): Long {
        return habitDao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habitId: Long) {
        habitEntryDao.deleteEntriesForHabit(habitId)
        habitDao.deleteHabitById(habitId)
    }

    suspend fun setArchived(habitId: Long, isArchived: Boolean) {
        habitDao.setArchived(habitId, isArchived)
    }

    suspend fun reorderHabits(habits: List<Habit>) {
        habits.forEachIndexed { index, habit ->
            habitDao.updateOrderIndex(habit.id, index)
        }
    }

    suspend fun seedSampleDataIfEmpty() {
        val currentSettings = userSettingsDao.getSettingsOnce()
        if (currentSettings == null) {
            userSettingsDao.insertSettings(UserSettings())
        }

        val habitsOnce = habitDao.getActiveHabits()
        // Check if there are any habits already
        val existingHabit = habitDao.getHabitByIdOnce(1L)
        if (existingHabit == null) {
            val sampleHabits = SampleDataGenerator.generateSampleHabits()
            habitDao.insertHabits(sampleHabits)
            val sampleEntries = SampleDataGenerator.generateSampleEntries(sampleHabits, daysBack = 50)
            habitEntryDao.insertEntries(sampleEntries)
        }
    }

    suspend fun reloadSampleData() {
        habitEntryDao.deleteAllEntries()
        habitDao.deleteAllHabits()
        val sampleHabits = SampleDataGenerator.generateSampleHabits()
        habitDao.insertHabits(sampleHabits)
        val sampleEntries = SampleDataGenerator.generateSampleEntries(sampleHabits, daysBack = 50)
        habitEntryDao.insertEntries(sampleEntries)
    }

    suspend fun clearAllData() {
        habitEntryDao.deleteAllEntries()
        habitDao.deleteAllHabits()
    }

    suspend fun restoreBackup(habits: List<Habit>, entries: List<HabitEntry>) {
        habitEntryDao.deleteAllEntries()
        habitDao.deleteAllHabits()
        habitDao.insertHabits(habits)
        habitEntryDao.insertEntries(entries)
    }
}
