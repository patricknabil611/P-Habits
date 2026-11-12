package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.HabitEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitEntryDao {
    @Query("SELECT * FROM habit_entries WHERE habitId = :habitId AND isDeleted = 0 ORDER BY dateString ASC")
    fun getEntriesForHabit(habitId: Long): Flow<List<HabitEntry>>

    @Query("SELECT * FROM habit_entries WHERE habitId = :habitId AND isDeleted = 0 ORDER BY dateString ASC")
    suspend fun getEntriesForHabitOnce(habitId: Long): List<HabitEntry>

    @Query("SELECT * FROM habit_entries WHERE dateString = :dateString AND isDeleted = 0")
    fun getEntriesForDate(dateString: String): Flow<List<HabitEntry>>

    @Query("SELECT * FROM habit_entries WHERE dateString BETWEEN :startDate AND :endDate AND isDeleted = 0")
    fun getEntriesBetweenDates(startDate: String, endDate: String): Flow<List<HabitEntry>>

    @Query("SELECT * FROM habit_entries WHERE habitId = :habitId AND dateString = :dateString LIMIT 1")
    suspend fun getEntry(habitId: Long, dateString: String): HabitEntry?

    @Query("SELECT * FROM habit_entries WHERE uuid = :uuid LIMIT 1")
    suspend fun getEntryByUuid(uuid: String): HabitEntry?

    @Query("SELECT * FROM habit_entries WHERE isDeleted = 0")
    fun getAllEntries(): Flow<List<HabitEntry>>

    @Query("SELECT * FROM habit_entries WHERE isDeleted = 0")
    suspend fun getAllEntriesOnce(): List<HabitEntry>

    @Query("SELECT * FROM habit_entries")
    suspend fun getAllEntriesIncludingDeletedOnce(): List<HabitEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: HabitEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<HabitEntry>)

    @Query("UPDATE habit_entries SET isDeleted = 1, updatedAt = :updatedAt WHERE habitId = :habitId AND dateString = :dateString")
    suspend fun softDeleteEntry(habitId: Long, dateString: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE habit_entries SET isDeleted = 1, updatedAt = :updatedAt WHERE habitId = :habitId")
    suspend fun softDeleteEntriesForHabit(habitId: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM habit_entries WHERE habitId = :habitId AND dateString = :dateString")
    suspend fun deleteEntry(habitId: Long, dateString: String)

    @Query("DELETE FROM habit_entries WHERE habitId = :habitId")
    suspend fun deleteEntriesForHabit(habitId: Long)

    @Query("DELETE FROM habit_entries WHERE habitUuid LIKE 'sample-habit-%'")
    suspend fun deleteSampleEntries()

    @Query("DELETE FROM habit_entries")
    suspend fun deleteAllEntries()
}
