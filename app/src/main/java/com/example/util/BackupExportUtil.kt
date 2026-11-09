package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.model.Habit
import com.example.data.model.HabitEntry
import com.example.data.model.UserSettings
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object BackupExportUtil {

    fun generateCsv(habits: List<Habit>, entries: List<HabitEntry>): String {
        val habitMap = habits.associateBy { it.id }
        val sb = StringBuilder()
        sb.append("Habit ID,Habit Name,Habit Type,Date,Value,Target,Unit,Is Completed\n")

        for (entry in entries.filter { !it.isDeleted }.sortedBy { it.dateString }) {
            val habit = habitMap[entry.habitId] ?: continue
            if (habit.isDeleted) continue
            val target = if (habit.isCountType) habit.target else 1f
            val isCompleted = if (entry.value >= target) "YES" else "NO"
            val escapedName = "\"${habit.name.replace("\"", "\"\"")}\""
            sb.append("${habit.id},$escapedName,${habit.type},${entry.dateString},${entry.value},$target,\"${habit.unit}\",$isCompleted\n")
        }
        return sb.toString()
    }

    fun generateBackupJson(
        habits: List<Habit>,
        entries: List<HabitEntry>,
        settings: UserSettings? = null
    ): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())

        val habitsArray = JSONArray()
        for (h in habits) {
            val obj = JSONObject()
            obj.put("id", h.id)
            obj.put("uuid", h.uuid)
            obj.put("name", h.name)
            obj.put("description", h.description)
            obj.put("type", h.type)
            obj.put("target", h.target.toDouble())
            obj.put("unit", h.unit)
            obj.put("colorHex", h.colorHex)
            obj.put("iconName", h.iconName)
            obj.put("frequencyType", h.frequencyType)
            obj.put("frequencyDays", h.frequencyDays)
            obj.put("frequencyTarget", h.frequencyTarget)
            obj.put("reminderEnabled", h.reminderEnabled)
            obj.put("reminderHour", h.reminderHour)
            obj.put("reminderMinute", h.reminderMinute)
            obj.put("reminderDays", h.reminderDays)
            obj.put("orderIndex", h.orderIndex)
            obj.put("isArchived", h.isArchived)
            obj.put("isDeleted", h.isDeleted)
            obj.put("createdAt", h.createdAt)
            obj.put("updatedAt", h.updatedAt)
            habitsArray.put(obj)
        }
        root.put("habits", habitsArray)

        val entriesArray = JSONArray()
        for (e in entries) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("uuid", e.uuid)
            obj.put("habitUuid", e.habitUuid)
            obj.put("habitId", e.habitId)
            obj.put("dateString", e.dateString)
            obj.put("value", e.value.toDouble())
            obj.put("notes", e.notes)
            obj.put("timestamp", e.timestamp)
            obj.put("updatedAt", e.updatedAt)
            obj.put("isDeleted", e.isDeleted)
            entriesArray.put(obj)
        }
        root.put("entries", entriesArray)

        if (settings != null) {
            val setObj = JSONObject()
            setObj.put("themeMode", settings.themeMode)
            setObj.put("dynamicColor", settings.dynamicColor)
            setObj.put("accentColor", settings.accentColor)
            setObj.put("cellShape", settings.cellShape)
            setObj.put("cellSize", settings.cellSize)
            setObj.put("doneStyle", settings.doneStyle)
            setObj.put("notDoneStyle", settings.notDoneStyle)
            setObj.put("countDisplayStyle", settings.countDisplayStyle)
            setObj.put("chartStyle", settings.chartStyle)
            setObj.put("firstDayOfWeek", settings.firstDayOfWeek)
            setObj.put("is24HourTime", settings.is24HourTime)
            setObj.put("showStreaksOnHome", settings.showStreaksOnHome)
            setObj.put("showArchived", settings.showArchived)
            root.put("settings", setObj)
        }

        return root.toString(2)
    }

    fun parseBackupJson(jsonString: String): Triple<List<Habit>, List<HabitEntry>, UserSettings?> {
        val root = JSONObject(jsonString)
        val habitsList = mutableListOf<Habit>()
        val entriesList = mutableListOf<HabitEntry>()

        val habitsArray = root.optJSONArray("habits")
        if (habitsArray != null) {
            for (i in 0 until habitsArray.length()) {
                val obj = habitsArray.getJSONObject(i)
                val rawUuid = obj.optString("uuid", "")
                val uuid = if (rawUuid.isNotEmpty()) rawUuid else UUID.randomUUID().toString()
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                val updatedAt = obj.optLong("updatedAt", createdAt)

                val habit = Habit(
                    id = obj.optLong("id", 0),
                    uuid = uuid,
                    name = obj.getString("name"),
                    description = obj.optString("description", ""),
                    type = obj.optString("type", "YES_NO"),
                    target = obj.optDouble("target", 1.0).toFloat(),
                    unit = obj.optString("unit", ""),
                    colorHex = obj.optString("colorHex", "#10B981"),
                    iconName = obj.optString("iconName", "check_circle"),
                    frequencyType = obj.optString("frequencyType", "EVERY_DAY"),
                    frequencyDays = obj.optString("frequencyDays", "1,2,3,4,5,6,7"),
                    frequencyTarget = obj.optInt("frequencyTarget", 7),
                    reminderEnabled = obj.optBoolean("reminderEnabled", false),
                    reminderHour = obj.optInt("reminderHour", 20),
                    reminderMinute = obj.optInt("reminderMinute", 0),
                    reminderDays = obj.optString("reminderDays", "1,2,3,4,5,6,7"),
                    orderIndex = obj.optInt("orderIndex", i),
                    isArchived = obj.optBoolean("isArchived", false),
                    isDeleted = obj.optBoolean("isDeleted", false),
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )
                habitsList.add(habit)
            }
        }

        val entriesArray = root.optJSONArray("entries")
        if (entriesArray != null) {
            for (i in 0 until entriesArray.length()) {
                val obj = entriesArray.getJSONObject(i)
                val rawUuid = obj.optString("uuid", "")
                val uuid = if (rawUuid.isNotEmpty()) rawUuid else UUID.randomUUID().toString()
                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                val updatedAt = obj.optLong("updatedAt", timestamp)

                val entry = HabitEntry(
                    id = obj.optLong("id", 0),
                    uuid = uuid,
                    habitUuid = obj.optString("habitUuid", ""),
                    habitId = obj.getLong("habitId"),
                    dateString = obj.getString("dateString"),
                    value = obj.optDouble("value", 1.0).toFloat(),
                    notes = obj.optString("notes", ""),
                    timestamp = timestamp,
                    updatedAt = updatedAt,
                    isDeleted = obj.optBoolean("isDeleted", false)
                )
                entriesList.add(entry)
            }
        }

        var settings: UserSettings? = null
        val setObj = root.optJSONObject("settings")
        if (setObj != null) {
            settings = UserSettings(
                themeMode = setObj.optString("themeMode", "SYSTEM"),
                dynamicColor = setObj.optBoolean("dynamicColor", true),
                accentColor = setObj.optString("accentColor", "#10B981"),
                cellShape = setObj.optString("cellShape", "ROUNDED_SQUARE"),
                cellSize = setObj.optString("cellSize", "NORMAL"),
                doneStyle = setObj.optString("doneStyle", "FILLED"),
                notDoneStyle = setObj.optString("notDoneStyle", "FADED"),
                countDisplayStyle = setObj.optString("countDisplayStyle", "PROGRESS_RING"),
                chartStyle = setObj.optString("chartStyle", "BAR"),
                firstDayOfWeek = setObj.optString("firstDayOfWeek", "MONDAY"),
                is24HourTime = setObj.optBoolean("is24HourTime", true),
                showStreaksOnHome = setObj.optBoolean("showStreaksOnHome", true),
                showArchived = setObj.optBoolean("showArchived", false)
            )
        }

        return Triple(habitsList, entriesList, settings)
    }

    fun shareText(context: Context, text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}
