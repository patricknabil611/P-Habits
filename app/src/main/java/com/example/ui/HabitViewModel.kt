package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Habit
import com.example.data.model.HabitEntry
import com.example.data.model.UserSettings
import com.example.data.repository.HabitRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HabitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HabitRepository

    val allHabits: StateFlow<List<Habit>>
    val activeHabits: StateFlow<List<Habit>>
    val entries: StateFlow<List<HabitEntry>>
    val settings: StateFlow<UserSettings>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = HabitRepository(db.habitDao(), db.habitEntryDao(), db.userSettingsDao())

        allHabits = repository.allHabits.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        activeHabits = repository.activeHabits.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        entries = repository.allEntries.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        settings = repository.userSettings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    fun toggleYesNo(habitId: Long, dateString: String) {
        viewModelScope.launch {
            repository.toggleYesNoEntry(habitId, dateString)
        }
    }

    fun incrementCount(habitId: Long, dateString: String, delta: Float = 1f, target: Float = 1f) {
        viewModelScope.launch {
            repository.incrementCountEntry(habitId, dateString, delta, target)
        }
    }

    fun decrementCount(habitId: Long, dateString: String) {
        viewModelScope.launch {
            val existing = repository.getAllEntriesOnce().find { it.habitId == habitId && it.dateString == dateString }
            val currentVal = existing?.value ?: 0f
            val newVal = (currentVal - 1f).coerceAtLeast(0f)
            repository.setCountEntry(habitId, dateString, newVal)
        }
    }

    fun setCount(habitId: Long, dateString: String, value: Float) {
        viewModelScope.launch {
            repository.setCountEntry(habitId, dateString, value)
        }
    }

    fun saveHabit(habit: Habit) {
        viewModelScope.launch {
            if (habit.id == 0L) {
                repository.saveHabit(habit.copy(orderIndex = allHabits.value.size))
            } else {
                repository.updateHabit(habit)
            }
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
        }
    }

    fun toggleArchive(habitId: Long, isArchived: Boolean) {
        viewModelScope.launch {
            repository.setArchived(habitId, isArchived)
        }
    }

    fun reorderHabits(reorderedList: List<Habit>) {
        viewModelScope.launch {
            repository.reorderHabits(reorderedList)
        }
    }

    fun updateSettings(newSettings: UserSettings) {
        viewModelScope.launch {
            repository.updateSettings(newSettings)
        }
    }

    fun reloadSampleData() {
        viewModelScope.launch {
            repository.reloadSampleData()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    fun restoreBackup(habitsList: List<Habit>, entriesList: List<HabitEntry>) {
        viewModelScope.launch {
            repository.restoreBackup(habitsList, entriesList)
        }
    }
}
