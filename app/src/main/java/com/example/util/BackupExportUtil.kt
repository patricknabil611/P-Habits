package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.model.Habit
import com.example.data.model.HabitEntry
import org.json.JSONArray
import org.json.JSONObject

object BackupExportUtil {

    fun generateCsv(habits: List<Habit>, entries: List<HabitEntry>): String {
        val habitMap = habits.associateBy { it.id }
        val sb = StringBuilder()
        sb.append("Habit ID,Habit Name,Habit Type,Date,Value,Target,Unit,Is Completed\n")

        for (entry in entries.sortedBy { it.dateString }) {
            val habit = habitMap[entry.habitId] ?: continue
            val target = if (habit.isCountType) habit.target else 1f
            val isCompleted = if (entry.value >= target) "YES" else "NO"
            val escapedName = "\"${habit.name.replace("\"", "\"\"")}\""
            sb.append("${habit.id},$escapedName,${habit.type},${entry.dateString},${entry.value},$target,\"${habit.unit}\",$isCompleted\n")
        }
        return sb.toString()
    }

    fun generateBackupJson(habits: List<Habit>, entries: List<HabitEntry>): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val habitsArray = JSONArray()
        for (h in habits) {
            val obj = JSONObject()
            obj.put("id", h.id)
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
            obj.put("createdAt", h.createdAt)
            habitsArray.put(obj)
        }
        root.put("habits", habitsArray)

        val entriesArray = JSONArray()
        for (e in entries) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("habitId", e.habitId)
            obj.put("dateString", e.dateString)
            obj.put("value", e.value.toDouble())
            obj.put("notes", e.notes)
            obj.put("timestamp", e.timestamp)
            entriesArray.put(obj)
        }
        root.put("entries", entriesArray)

        return root.toString(2)
    }

    fun parseBackupJson(jsonString: String): Pair<List<Habit>, List<HabitEntry>> {
        val root = JSONObject(jsonString)
        val habitsList = mutableListOf<Habit>()
        val entriesList = mutableListOf<HabitEntry>()

        val habitsArray = root.optJSONArray("habits")
        if (habitsArray != null) {
            for (i in 0 until habitsArray.length()) {
                val obj = habitsArray.getJSONObject(i)
                val habit = Habit(
                    id = obj.optLong("id", 0),
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
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                habitsList.add(habit)
            }
        }

        val entriesArray = root.optJSONArray("entries")
        if (entriesArray != null) {
            for (i in 0 until entriesArray.length()) {
                val obj = entriesArray.getJSONObject(i)
                val entry = HabitEntry(
                    id = obj.optLong("id", 0),
                    habitId = obj.getLong("habitId"),
                    dateString = obj.getString("dateString"),
                    value = obj.optDouble("value", 1.0).toFloat(),
                    notes = obj.optString("notes", ""),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
                entriesList.add(entry)
            }
        }

        return Pair(habitsList, entriesList)
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
