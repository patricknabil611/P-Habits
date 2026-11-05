package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.HabitEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitEntryDao {
    @Query("SELECT * FROM habit_entries WHERE habitId = :habitId ORDER BY dateString ASC")
    fun getEntriesForHabit(habitId: Long): Flow<List<HabitEntry>>

    @Query("SELECT * FROM habit_entries WHERE habitId = :habitId ORDER BY dateString ASC")
    suspend fun getEntriesForHabitOnce(habitId: Long): List<HabitEntry>

    @Query("SELECT * FROM habit_entries WHERE dateString = :dateString")
    fun getEntriesForDate(dateString: String): Flow<List<HabitEntry>>

    @Query("SELECT * FROM habit_entries WHERE dateString BETWEEN :startDate AND :endDate")
    fun getEntriesBetweenDates(startDate: String, endDate: String): Flow<List<HabitEntry>>

    @Query("SELECT * FROM habit_entries WHERE habitId = :habitId AND dateString = :dateString LIMIT 1")
    suspend fun getEntry(habitId: Long, dateString: String): HabitEntry?

    @Query("SELECT * FROM habit_entries")
    fun getAllEntries(): Flow<List<HabitEntry>>

    @Query("SELECT * FROM habit_entries")
    suspend fun getAllEntriesOnce(): List<HabitEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: HabitEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<HabitEntry>)

    @Query("DELETE FROM habit_entries WHERE habitId = :habitId AND dateString = :dateString")
    suspend fun deleteEntry(habitId: Long, dateString: String)

    @Query("DELETE FROM habit_entries WHERE habitId = :habitId")
    suspend fun deleteEntriesForHabit(habitId: Long)

    @Query("DELETE FROM habit_entries")
    suspend fun deleteAllEntries()
}
