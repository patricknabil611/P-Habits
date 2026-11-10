package com.example.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.R
import com.example.data.model.Habit
import com.example.data.model.HabitEntry
import com.example.data.model.UserSettings
import com.example.data.repository.HabitRepository
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class SyncStatus {
    SYNCED,
    SYNCING,
    OFFLINE,
    ERROR,
    DISABLED
}

class FirestoreSyncManager(
    private val context: Context,
    private val repository: HabitRepository,
    private val scope: CoroutineScope
) {
    private val TAG = "FirestoreSyncManager"

    private val firestore: FirebaseFirestore by lazy {
        val dbId = try {
            context.getString(R.string.firestore_database_id)
        } catch (_: Exception) {
            null
        }
        if (!dbId.isNullOrEmpty()) {
            Firebase.firestore(dbId)
        } else {
            Firebase.firestore
        }
    }

    private val _syncStatus = MutableStateFlow(SyncStatus.DISABLED)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long>(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _syncErrorMessage = MutableStateFlow<String?>(null)
    val syncErrorMessage: StateFlow<String?> = _syncErrorMessage.asStateFlow()

    private var currentUserId: String? = null
    private var debounceJob: Job? = null
    private var isOnline = true

    init {
        monitorNetwork()
    }

    private fun monitorNetwork() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            isOnline = true
            return
        }

        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        isOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline = true
                Log.d(TAG, "Network became available")
                if (currentUserId != null && _syncStatus.value == SyncStatus.OFFLINE) {
                    triggerSync()
                }
            }

            override fun onLost(network: Network) {
                isOnline = false
                Log.d(TAG, "Network lost")
                if (currentUserId != null) {
                    _syncStatus.value = SyncStatus.OFFLINE
                }
            }
        })
    }

    fun onUserChanged(userId: String?) {
        currentUserId = userId
        if (userId == null) {
            _syncStatus.value = SyncStatus.DISABLED
        } else {
            if (!isOnline) {
                _syncStatus.value = SyncStatus.OFFLINE
            } else {
                triggerSync()
            }
        }
    }

    fun triggerDebouncedSync() {
        if (currentUserId == null) return
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(1500) // 1.5s debounce
            triggerSync()
        }
    }

    fun triggerSync() {
        val uid = currentUserId ?: run {
            _syncStatus.value = SyncStatus.DISABLED
            return
        }

        if (!isOnline) {
            _syncStatus.value = SyncStatus.OFFLINE
            return
        }

        scope.launch {
            performSync(uid)
        }
    }

    private suspend fun performSync(userId: String) = withContext(Dispatchers.IO) {
        try {
            _syncStatus.value = SyncStatus.SYNCING
            _syncErrorMessage.value = null

            val userDocRef = firestore.collection("users").document(userId)

            // Ensure user root doc exists
            userDocRef.set(
                mapOf(
                    "userId" to userId,
                    "lastSeen" to System.currentTimeMillis()
                )
            ).await()

            // 1. SYNC HABITS
            syncHabits(userId)

            // 2. SYNC ENTRIES
            syncEntries(userId)

            // 3. SYNC SETTINGS
            syncSettings(userId)

            _lastSyncTime.value = System.currentTimeMillis()
            _syncStatus.value = SyncStatus.SYNCED
            Log.d(TAG, "Sync completed successfully for user $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}", e)
            _syncErrorMessage.value = e.localizedMessage ?: "Sync error"
            _syncStatus.value = SyncStatus.ERROR
        }
    }

    private suspend fun syncHabits(userId: String) {
        val habitsCol = firestore.collection("users").document(userId).collection("habits")

        // Fetch remote habits
        val remoteSnap = habitsCol.get().await()
        val remoteHabitsMap = remoteSnap.documents.mapNotNull { parseHabitFromDoc(it) }.associateBy { it.uuid }

        // Fetch local habits (including soft-deleted)
        val localHabits = repository.getAllHabitsIncludingDeletedOnce()
        val localHabitsMap = localHabits.associateBy { it.uuid }

        // Merge Remote -> Local
        for ((uuid, remoteHabit) in remoteHabitsMap) {
            val localHabit = localHabitsMap[uuid]
            if (localHabit == null) {
                // If remote is not deleted, insert to local
                if (!remoteHabit.isDeleted) {
                    repository.insertOrUpdateHabitFromSync(remoteHabit.copy(id = 0))
                }
            } else {
                // If remote has newer updatedAt, update local
                if (remoteHabit.updatedAt > localHabit.updatedAt) {
                    repository.insertOrUpdateHabitFromSync(
                        remoteHabit.copy(id = localHabit.id)
                    )
                }
            }
        }

        // Merge Local -> Remote
        for ((uuid, localHabit) in localHabitsMap) {
            val remoteHabit = remoteHabitsMap[uuid]
            if (remoteHabit == null) {
                // Not in remote yet, upload
                habitsCol.document(uuid).set(habitToMap(localHabit)).await()
            } else if (localHabit.updatedAt > remoteHabit.updatedAt) {
                // Local is newer, upload
                habitsCol.document(uuid).set(habitToMap(localHabit)).await()
            }
        }
    }

    private suspend fun syncEntries(userId: String) {
        val entriesCol = firestore.collection("users").document(userId).collection("habit_entries")

        // Fetch remote entries
        val remoteSnap = entriesCol.get().await()
        val remoteEntriesMap = remoteSnap.documents.mapNotNull { parseEntryFromDoc(it) }.associateBy { it.uuid }

        // Fetch local entries
        val localEntries = repository.getAllEntriesIncludingDeletedOnce()
        val localEntriesMap = localEntries.associateBy { it.uuid }

        // Map habit uuid to local id
        val localHabits = repository.getAllHabitsIncludingDeletedOnce().associateBy { it.uuid }

        // Remote -> Local
        for ((uuid, remoteEntry) in remoteEntriesMap) {
            val localEntry = localEntriesMap[uuid]
            if (localEntry == null) {
                if (!remoteEntry.isDeleted) {
                    val targetHabit = localHabits[remoteEntry.habitUuid]
                    val localHabitId = targetHabit?.id ?: remoteEntry.habitId
                    repository.insertOrUpdateEntryFromSync(
                        remoteEntry.copy(id = 0, habitId = localHabitId)
                    )
                }
            } else {
                if (remoteEntry.updatedAt > localEntry.updatedAt) {
                    repository.insertOrUpdateEntryFromSync(
                        remoteEntry.copy(id = localEntry.id, habitId = localEntry.habitId)
                    )
                }
            }
        }

        // Local -> Remote
        for ((uuid, localEntry) in localEntriesMap) {
            val remoteEntry = remoteEntriesMap[uuid]
            if (remoteEntry == null) {
                entriesCol.document(uuid).set(entryToMap(localEntry)).await()
            } else if (localEntry.updatedAt > remoteEntry.updatedAt) {
                entriesCol.document(uuid).set(entryToMap(localEntry)).await()
            }
        }
    }

    private suspend fun syncSettings(userId: String) {
        val settingsDocRef = firestore.collection("users").document(userId).collection("settings").document("current")
        val snap = settingsDocRef.get().await()
        val localSettings = repository.getSettingsOnce()

        if (snap.exists()) {
            val remoteUpdatedAt = snap.getLong("updatedAt") ?: 0L
            if (remoteUpdatedAt > localSettings.updatedAt) {
                // Remote is newer, update local
                val updatedSettings = localSettings.copy(
                    themeMode = snap.getString("themeMode") ?: localSettings.themeMode,
                    dynamicColor = snap.getBoolean("dynamicColor") ?: localSettings.dynamicColor,
                    accentColor = snap.getString("accentColor") ?: localSettings.accentColor,
                    cellShape = snap.getString("cellShape") ?: localSettings.cellShape,
                    cellSize = snap.getString("cellSize") ?: localSettings.cellSize,
                    doneStyle = snap.getString("doneStyle") ?: localSettings.doneStyle,
                    notDoneStyle = snap.getString("notDoneStyle") ?: localSettings.notDoneStyle,
                    countDisplayStyle = snap.getString("countDisplayStyle") ?: localSettings.countDisplayStyle,
                    chartStyle = snap.getString("chartStyle") ?: localSettings.chartStyle,
                    firstDayOfWeek = snap.getString("firstDayOfWeek") ?: localSettings.firstDayOfWeek,
                    is24HourTime = snap.getBoolean("is24HourTime") ?: localSettings.is24HourTime,
                    showStreaksOnHome = snap.getBoolean("showStreaksOnHome") ?: localSettings.showStreaksOnHome,
                    showArchived = snap.getBoolean("showArchived") ?: localSettings.showArchived,
                    updatedAt = remoteUpdatedAt
                )
                repository.insertOrUpdateSettingsFromSync(updatedSettings)
            } else if (localSettings.updatedAt > remoteUpdatedAt) {
                // Local is newer, upload
                settingsDocRef.set(settingsToMap(localSettings)).await()
            }
        } else {
            // No remote settings yet, upload local
            settingsDocRef.set(settingsToMap(localSettings)).await()
        }
    }

    private fun habitToMap(h: Habit): Map<String, Any?> = mapOf(
        "uuid" to h.uuid,
        "name" to h.name,
        "description" to h.description,
        "type" to h.type,
        "target" to h.target.toDouble(),
        "unit" to h.unit,
        "colorHex" to h.colorHex,
        "iconName" to h.iconName,
        "frequencyType" to h.frequencyType,
        "frequencyDays" to h.frequencyDays,
        "frequencyTarget" to h.frequencyTarget,
        "reminderEnabled" to h.reminderEnabled,
        "reminderHour" to h.reminderHour,
        "reminderMinute" to h.reminderMinute,
        "reminderDays" to h.reminderDays,
        "orderIndex" to h.orderIndex,
        "isArchived" to h.isArchived,
        "isDeleted" to h.isDeleted,
        "createdAt" to h.createdAt,
        "updatedAt" to h.updatedAt
    )

    private fun parseHabitFromDoc(doc: DocumentSnapshot): Habit? {
        val uuid = doc.getString("uuid") ?: doc.id
        val name = doc.getString("name") ?: return null
        return Habit(
            id = 0,
            uuid = uuid,
            name = name,
            description = doc.getString("description") ?: "",
            type = doc.getString("type") ?: "YES_NO",
            target = (doc.getDouble("target") ?: 1.0).toFloat(),
            unit = doc.getString("unit") ?: "",
            colorHex = doc.getString("colorHex") ?: "#10B981",
            iconName = doc.getString("iconName") ?: "check_circle",
            frequencyType = doc.getString("frequencyType") ?: "EVERY_DAY",
            frequencyDays = doc.getString("frequencyDays") ?: "1,2,3,4,5,6,7",
            frequencyTarget = (doc.getLong("frequencyTarget") ?: 7L).toInt(),
            reminderEnabled = doc.getBoolean("reminderEnabled") ?: false,
            reminderHour = (doc.getLong("reminderHour") ?: 20L).toInt(),
            reminderMinute = (doc.getLong("reminderMinute") ?: 0L).toInt(),
            reminderDays = doc.getString("reminderDays") ?: "1,2,3,4,5,6,7",
            orderIndex = (doc.getLong("orderIndex") ?: 0L).toInt(),
            isArchived = doc.getBoolean("isArchived") ?: false,
            isDeleted = doc.getBoolean("isDeleted") ?: false,
            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
            updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
        )
    }

    private fun entryToMap(e: HabitEntry): Map<String, Any?> = mapOf(
        "uuid" to e.uuid,
        "habitUuid" to e.habitUuid,
        "dateString" to e.dateString,
        "value" to e.value.toDouble(),
        "notes" to e.notes,
        "timestamp" to e.timestamp,
        "updatedAt" to e.updatedAt,
        "isDeleted" to e.isDeleted
    )

    private fun parseEntryFromDoc(doc: DocumentSnapshot): HabitEntry? {
        val uuid = doc.getString("uuid") ?: doc.id
        val dateString = doc.getString("dateString") ?: return null
        val habitUuid = doc.getString("habitUuid") ?: ""
        return HabitEntry(
            id = 0,
            uuid = uuid,
            habitUuid = habitUuid,
            habitId = 0,
            dateString = dateString,
            value = (doc.getDouble("value") ?: 0.0).toFloat(),
            notes = doc.getString("notes") ?: "",
            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
            updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
            isDeleted = doc.getBoolean("isDeleted") ?: false
        )
    }

    private fun settingsToMap(s: UserSettings): Map<String, Any?> = mapOf(
        "themeMode" to s.themeMode,
        "dynamicColor" to s.dynamicColor,
        "accentColor" to s.accentColor,
        "cellShape" to s.cellShape,
        "cellSize" to s.cellSize,
        "doneStyle" to s.doneStyle,
        "notDoneStyle" to s.notDoneStyle,
        "countDisplayStyle" to s.countDisplayStyle,
        "chartStyle" to s.chartStyle,
        "firstDayOfWeek" to s.firstDayOfWeek,
        "is24HourTime" to s.is24HourTime,
        "showStreaksOnHome" to s.showStreaksOnHome,
        "showArchived" to s.showArchived,
        "updatedAt" to s.updatedAt
    )
}
