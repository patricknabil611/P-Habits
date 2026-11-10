package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Habit
import com.example.data.model.HabitEntry
import com.example.data.model.UserSettings
import com.example.data.repository.HabitRepository
import com.example.sync.BackupItem
import com.example.sync.CloudBackupManager
import com.example.sync.FirestoreSyncManager
import com.example.sync.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HabitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HabitRepository
    val syncManager: FirestoreSyncManager
    val backupManager: CloudBackupManager

    val allHabits: StateFlow<List<Habit>>
    val activeHabits: StateFlow<List<Habit>>
    val entries: StateFlow<List<HabitEntry>>
    val settings: StateFlow<UserSettings>

    private val _isLoadingData = MutableStateFlow(true)
    val isLoadingData: StateFlow<Boolean> = _isLoadingData.asStateFlow()

    val syncStatus: StateFlow<SyncStatus>
    val lastSyncTime: StateFlow<Long>
    val syncErrorMessage: StateFlow<String?>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = HabitRepository(db.habitDao(), db.habitEntryDao(), db.userSettingsDao())

        syncManager = FirestoreSyncManager(application, repository, viewModelScope)
        backupManager = CloudBackupManager(application, repository)

        syncStatus = syncManager.syncStatus
        lastSyncTime = syncManager.lastSyncTime
        syncErrorMessage = syncManager.syncErrorMessage

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
            try {
                repository.seedSampleDataIfEmpty(application)
            } finally {
                _isLoadingData.value = false
            }
        }
    }

    fun onUserChanged(userId: String?) {
        syncManager.onUserChanged(userId)
        if (userId != null) {
            viewModelScope.launch {
                backupManager.checkDailyAutoBackup(userId)
            }
        }
    }

    fun triggerSync() {
        syncManager.triggerSync()
    }

    fun toggleYesNo(habitId: Long, dateString: String) {
        viewModelScope.launch {
            repository.toggleYesNoEntry(habitId, dateString)
            syncManager.triggerDebouncedSync()
        }
    }

    fun incrementCount(habitId: Long, dateString: String, delta: Float = 1f, target: Float = 1f) {
        viewModelScope.launch {
            repository.incrementCountEntry(habitId, dateString, delta, target)
            syncManager.triggerDebouncedSync()
        }
    }

    fun decrementCount(habitId: Long, dateString: String) {
        viewModelScope.launch {
            val existing = repository.getAllActiveEntriesOnce().find { it.habitId == habitId && it.dateString == dateString }
            val currentVal = existing?.value ?: 0f
            val newVal = (currentVal - 1f).coerceAtLeast(0f)
            repository.setCountEntry(habitId, dateString, newVal)
            syncManager.triggerDebouncedSync()
        }
    }

    fun setCount(habitId: Long, dateString: String, value: Float) {
        viewModelScope.launch {
            repository.setCountEntry(habitId, dateString, value)
            syncManager.triggerDebouncedSync()
        }
    }

    fun saveHabit(habit: Habit) {
        viewModelScope.launch {
            if (habit.id == 0L) {
                repository.saveHabit(habit.copy(orderIndex = allHabits.value.size))
            } else {
                repository.updateHabit(habit)
            }
            syncManager.triggerDebouncedSync()
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
            syncManager.triggerDebouncedSync()
        }
    }

    fun toggleArchive(habitId: Long, isArchived: Boolean) {
        viewModelScope.launch {
            repository.setArchived(habitId, isArchived)
            syncManager.triggerDebouncedSync()
        }
    }

    fun reorderHabits(reorderedList: List<Habit>) {
        viewModelScope.launch {
            repository.reorderHabits(reorderedList)
            syncManager.triggerDebouncedSync()
        }
    }

    fun updateSettings(newSettings: UserSettings) {
        viewModelScope.launch {
            repository.updateSettings(newSettings)
            syncManager.triggerDebouncedSync()
        }
    }

    fun reloadSampleData() {
        viewModelScope.launch {
            backupManager.createEmergencyLocalBackup("pre_sample_reload")
            repository.reloadSampleData()
            syncManager.triggerDebouncedSync()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            // SAFETY: Always create local emergency backup before clearing
            backupManager.createEmergencyLocalBackup("pre_clear_all")
            repository.clearAllData()
            syncManager.triggerDebouncedSync()
        }
    }

    fun restoreBackup(habitsList: List<Habit>, entriesList: List<HabitEntry>) {
        viewModelScope.launch {
            backupManager.createEmergencyLocalBackup("pre_restore")
            repository.restoreBackup(habitsList, entriesList)
            syncManager.triggerDebouncedSync()
        }
    }

    fun createBackup(userId: String, onComplete: (Result<BackupItem>) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.createBackup(userId)
            onComplete(result)
        }
    }

    fun listBackups(userId: String, onComplete: (Result<List<BackupItem>>) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.listBackups(userId)
            onComplete(result)
        }
    }

    fun restoreCloudBackup(backup: BackupItem, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.restoreBackup(backup)
            if (result.isSuccess) {
                syncManager.triggerSync()
            }
            onComplete(result)
        }
    }
}
