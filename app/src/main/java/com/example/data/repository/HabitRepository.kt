package com.example.data.repository

import android.content.Context
import com.example.data.local.HabitDao
import com.example.data.local.HabitEntryDao
import com.example.data.local.UserSettingsDao
import com.example.data.model.Habit
import com.example.data.model.HabitEntry
import com.example.data.model.UserSettings
import com.example.util.SampleDataGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class HabitRepository(
    private val context: Context,
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

    suspend fun getAllActiveEntriesOnce(): List<HabitEntry> {
        return habitEntryDao.getAllEntriesOnce()
    }

    suspend fun getAllEntriesIncludingDeletedOnce(): List<HabitEntry> {
        return habitEntryDao.getAllEntriesIncludingDeletedOnce()
    }

    suspend fun getAllActiveHabitsOnce(): List<Habit> {
        return habitDao.getAllActiveHabitsOnce()
    }

    suspend fun getAllHabitsIncludingDeletedOnce(): List<Habit> {
        return habitDao.getAllHabitsIncludingDeletedOnce()
    }

    suspend fun getHabitByIdOnce(id: Long): Habit? {
        return habitDao.getHabitByIdOnce(id)
    }

    suspend fun getHabitByUuid(uuid: String): Habit? {
        return habitDao.getHabitByUuid(uuid)
    }

    suspend fun getEntryByUuid(uuid: String): HabitEntry? {
        return habitEntryDao.getEntryByUuid(uuid)
    }

    suspend fun getSettingsOnce(): UserSettings {
        return userSettingsDao.getSettingsOnce() ?: UserSettings()
    }

    suspend fun updateSettings(settings: UserSettings) {
        userSettingsDao.insertSettings(settings.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleYesNoEntry(habitId: Long, dateString: String) {
        val now = System.currentTimeMillis()
        val habit = habitDao.getHabitByIdOnce(habitId)
        val habitUuid = habit?.uuid ?: ""
        val existing = habitEntryDao.getEntry(habitId, dateString)

        if (existing == null || existing.isDeleted || existing.value <= 0f) {
            val entryUuid = existing?.uuid?.ifEmpty { null } ?: UUID.randomUUID().toString()
            habitEntryDao.insertEntry(
                HabitEntry(
                    id = existing?.id ?: 0,
                    uuid = entryUuid,
                    habitUuid = habitUuid,
                    habitId = habitId,
                    dateString = dateString,
                    value = 1f,
                    timestamp = now,
                    updatedAt = now,
                    isDeleted = false
                )
            )
        } else {
            habitEntryDao.softDeleteEntry(habitId, dateString, now)
        }
    }

    suspend fun incrementCountEntry(habitId: Long, dateString: String, delta: Float = 1f, target: Float = 1f) {
        val now = System.currentTimeMillis()
        val habit = habitDao.getHabitByIdOnce(habitId)
        val habitUuid = habit?.uuid ?: ""
        val existing = habitEntryDao.getEntry(habitId, dateString)
        val currentVal = if (existing != null && !existing.isDeleted) existing.value else 0f
        val newVal = currentVal + delta
        val entryUuid = existing?.uuid?.ifEmpty { null } ?: UUID.randomUUID().toString()

        habitEntryDao.insertEntry(
            HabitEntry(
                id = existing?.id ?: 0,
                uuid = entryUuid,
                habitUuid = habitUuid,
                habitId = habitId,
                dateString = dateString,
                value = newVal,
                timestamp = existing?.timestamp ?: now,
                updatedAt = now,
                isDeleted = false
            )
        )
    }

    suspend fun setCountEntry(habitId: Long, dateString: String, value: Float) {
        val now = System.currentTimeMillis()
        val habit = habitDao.getHabitByIdOnce(habitId)
        val habitUuid = habit?.uuid ?: ""
        val existing = habitEntryDao.getEntry(habitId, dateString)

        if (value <= 0f) {
            if (existing != null) {
                habitEntryDao.softDeleteEntry(habitId, dateString, now)
            }
        } else {
            val entryUuid = existing?.uuid?.ifEmpty { null } ?: UUID.randomUUID().toString()
            habitEntryDao.insertEntry(
                HabitEntry(
                    id = existing?.id ?: 0,
                    uuid = entryUuid,
                    habitUuid = habitUuid,
                    habitId = habitId,
                    dateString = dateString,
                    value = value,
                    timestamp = existing?.timestamp ?: now,
                    updatedAt = now,
                    isDeleted = false
                )
            )
        }
    }

    suspend fun saveHabit(habit: Habit): Long {
        val now = System.currentTimeMillis()
        val safeUuid = habit.uuid.ifEmpty { UUID.randomUUID().toString() }
        val toSave = habit.copy(
            uuid = safeUuid,
            updatedAt = now,
            isDeleted = false
        )
        return habitDao.insertHabit(toSave)
    }

    suspend fun updateHabit(habit: Habit) {
        val now = System.currentTimeMillis()
        val safeUuid = habit.uuid.ifEmpty { UUID.randomUUID().toString() }
        habitDao.insertHabit(
            habit.copy(
                uuid = safeUuid,
                updatedAt = now
            )
        )
    }

    suspend fun deleteHabit(habitId: Long) {
        val now = System.currentTimeMillis()
        habitEntryDao.softDeleteEntriesForHabit(habitId, now)
        habitDao.softDeleteHabitById(habitId, now)
    }

    suspend fun setArchived(habitId: Long, isArchived: Boolean) {
        habitDao.setArchived(habitId, isArchived, System.currentTimeMillis())
    }

    suspend fun reorderHabits(habits: List<Habit>) {
        val now = System.currentTimeMillis()
        habits.forEachIndexed { index, habit ->
            habitDao.updateOrderIndex(habit.id, index, now)
        }
    }

    suspend fun seedSampleDataIfEmpty(context: Context) {
        val prefs = context.getSharedPreferences("p_habits_prefs", Context.MODE_PRIVATE)
        val hasLaunchedBefore = prefs.getBoolean("has_launched_before", false)

        val currentSettings = userSettingsDao.getSettingsOnce()
        if (currentSettings == null) {
            userSettingsDao.insertSettings(UserSettings())
        }

        // Strictly check if the app has ever launched before AND if there are 0 habits in database
        val totalCount = habitDao.getTotalHabitCount()
        if (!hasLaunchedBefore && totalCount == 0) {
            val sampleHabits = SampleDataGenerator.generateSampleHabits()
            habitDao.insertHabits(sampleHabits)
            val sampleEntries = SampleDataGenerator.generateSampleEntries(sampleHabits, daysBack = 50)
            habitEntryDao.insertEntries(sampleEntries)
        }

        // Mark has launched before so sample data will NEVER re-seed automatically on future launches
        prefs.edit().putBoolean("has_launched_before", true).apply()
    }

    suspend fun hasSampleData(): Boolean {
        val habits = habitDao.getAllHabitsIncludingDeletedOnce()
        return habits.any { it.uuid.startsWith("sample-habit-") && !it.isDeleted }
    }

    suspend fun deleteSampleData() {
        val sampleHabits = habitDao.getAllHabitsIncludingDeletedOnce().filter { it.uuid.startsWith("sample-habit-") }
        for (sample in sampleHabits) {
            habitEntryDao.deleteEntriesForHabit(sample.id)
            habitDao.deleteHabitById(sample.id)
        }
        habitEntryDao.deleteSampleEntries()
        habitDao.deleteSampleHabits()
    }

    fun setAutoSyncStarted(started: Boolean) {
        val prefs = context.getSharedPreferences("p_habits_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_sync_started", started).apply()
    }

    fun isAutoSyncStarted(): Boolean {
        val prefs = context.getSharedPreferences("p_habits_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("auto_sync_started", false)
    }

    suspend fun reloadSampleData() {
        val now = System.currentTimeMillis()
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

    // Direct inserts for sync engine
    suspend fun insertOrUpdateHabitFromSync(habit: Habit) {
        habitDao.insertHabit(habit)
    }

    suspend fun insertOrUpdateEntryFromSync(entry: HabitEntry) {
        habitEntryDao.insertEntry(entry)
    }

    suspend fun insertOrUpdateSettingsFromSync(settings: UserSettings) {
        userSettingsDao.insertSettings(settings)
    }
}
