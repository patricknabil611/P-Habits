package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "habit_entries",
    indices = [
        Index(value = ["habitId", "dateString"], unique = true),
        Index(value = ["uuid"], unique = true)
    ]
)
data class HabitEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val habitUuid: String = "",
    val habitId: Long,
    val dateString: String, // Format: "yyyy-MM-dd"
    val value: Float,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
