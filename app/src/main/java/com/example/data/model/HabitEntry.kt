package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_entries",
    indices = [
        Index(value = ["habitId", "dateString"], unique = true)
    ]
)
data class HabitEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitId: Long,
    val dateString: String, // Format: "yyyy-MM-dd"
    val value: Float,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
