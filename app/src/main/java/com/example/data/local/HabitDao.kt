package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Habit
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isDeleted = 0 ORDER BY orderIndex ASC, id ASC")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE isArchived = 0 AND isDeleted = 0 ORDER BY orderIndex ASC, id ASC")
    fun getActiveHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id AND isDeleted = 0")
    fun getHabitById(id: Long): Flow<Habit?>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitByIdOnce(id: Long): Habit?

    @Query("SELECT * FROM habits WHERE uuid = :uuid LIMIT 1")
    suspend fun getHabitByUuid(uuid: String): Habit?

    @Query("SELECT * FROM habits WHERE isDeleted = 0")
    suspend fun getAllActiveHabitsOnce(): List<Habit>

    @Query("SELECT * FROM habits")
    suspend fun getAllHabitsIncludingDeletedOnce(): List<Habit>

    @Query("SELECT COUNT(*) FROM habits WHERE isDeleted = 0")
    suspend fun getActiveHabitsCount(): Int

    @Query("SELECT COUNT(*) FROM habits")
    suspend fun getTotalHabitCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<Habit>)

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("UPDATE habits SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteHabitById(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE habits SET isDeleted = 1, updatedAt = :updatedAt WHERE uuid = :uuid")
    suspend fun softDeleteHabitByUuid(uuid: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: Long)

    @Query("UPDATE habits SET isArchived = :isArchived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE habits SET orderIndex = :newIndex, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateOrderIndex(id: Long, newIndex: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM habits WHERE uuid LIKE 'sample-habit-%'")
    suspend fun deleteSampleHabits()

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()
}
