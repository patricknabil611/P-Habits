package com.example.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FrequencyType
import com.example.data.model.Habit
import com.example.data.model.HabitType
import com.example.ui.components.ColorPaletteSelector
import com.example.ui.components.HabitIconsList
import com.example.ui.components.getHabitIcon
import com.example.ui.theme.DefaultHabitPalette
import com.example.ui.theme.parseHexColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitEditorDialog(
    initialHabit: Habit?,
    onDismiss: () -> Unit,
    onSaveHabit: (Habit) -> Unit,
    onDeleteHabit: ((Long) -> Unit)? = null,
    onToggleArchive: ((Long, Boolean) -> Unit)? = null
) {
    val isEditMode = initialHabit != null

    var name by remember { mutableStateOf(initialHabit?.name ?: "") }
    var description by remember { mutableStateOf(initialHabit?.description ?: "") }
    var habitType by remember { mutableStateOf(initialHabit?.type ?: HabitType.YES_NO.name) }
    var targetText by remember {
        mutableStateOf(
            if (initialHabit?.isCountType == true) {
                if (initialHabit.target % 1f == 0f) initialHabit.target.toInt().toString() else initialHabit.target.toString()
            } else "8"
        )
    }
    var unitText by remember { mutableStateOf(initialHabit?.unit ?: "times") }
    var selectedColorHex by remember { mutableStateOf(initialHabit?.colorHex ?: "#10B981") }
    var selectedIconName by remember { mutableStateOf(initialHabit?.iconName ?: "check_circle") }

    var frequencyType by remember { mutableStateOf(initialHabit?.frequencyType ?: FrequencyType.EVERY_DAY.name) }
    var selectedWeekdays by remember {
        mutableStateOf(
            initialHabit?.frequencyDays?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet()
                ?: setOf(1, 2, 3, 4, 5, 6, 7)
        )
    }
    var frequencyTarget by remember { mutableIntStateOf(initialHabit?.frequencyTarget ?: 3) }

    var reminderEnabled by remember { mutableStateOf(initialHabit?.reminderEnabled ?: false) }
    var reminderHour by remember { mutableIntStateOf(initialHabit?.reminderHour ?: 20) }
    var reminderMinute by remember { mutableIntStateOf(initialHabit?.reminderMinute ?: 0) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(if (isEditMode) "Edit Habit" else "New Habit")
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("editor_back_button")) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    val targetVal = targetText.toFloatOrNull()?.coerceAtLeast(1f) ?: 1f
                                    val daysCsv = selectedWeekdays.sorted().joinToString(",")
                                    val habitToSave = (initialHabit ?: Habit(name = name)).copy(
                                        name = name.trim(),
                                        description = description.trim(),
                                        type = habitType,
                                        target = if (habitType == HabitType.COUNT.name) targetVal else 1f,
                                        unit = if (habitType == HabitType.COUNT.name) unitText.trim() else "",
                                        colorHex = selectedColorHex,
                                        iconName = selectedIconName,
                                        frequencyType = frequencyType,
                                        frequencyDays = if (daysCsv.isNotBlank()) daysCsv else "1,2,3,4,5,6,7",
                                        frequencyTarget = frequencyTarget,
                                        reminderEnabled = reminderEnabled,
                                        reminderHour = reminderHour,
                                        reminderMinute = reminderMinute
                                    )
                                    onSaveHabit(habitToSave)
                                }
                            },
                            enabled = name.isNotBlank(),
                            modifier = Modifier.padding(end = 8.dp).testTag("editor_save_button")
                        ) {
                            Text("Save")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Section 1: Basic Info
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit Name *") },
                    placeholder = { Text("e.g. Read Books, Morning Run, Water") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("habit_name_field")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Question / Description (Optional)") },
                    placeholder = { Text("e.g. Did you read for 20 mins today?") },
                    modifier = Modifier.fillMaxWidth().testTag("habit_desc_field")
                )

                // Section 2: Habit Type Selection
                Text("Habit Type", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = habitType == HabitType.YES_NO.name,
                        onClick = { habitType = HabitType.YES_NO.name },
                        label = { Text("Yes / No") },
                        modifier = Modifier.weight(1f).testTag("type_yes_no_chip")
                    )
                    FilterChip(
                        selected = habitType == HabitType.COUNT.name,
                        onClick = { habitType = HabitType.COUNT.name },
                        label = { Text("Count (Numeric)") },
                        modifier = Modifier.weight(1f).testTag("type_count_chip")
                    )
                }

                // If Count type: target and unit fields
                AnimatedVisibility(visible = habitType == HabitType.COUNT.name) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = targetText,
                            onValueChange = { targetText = it },
                            label = { Text("Daily Target") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("habit_target_field")
                        )
                        OutlinedTextField(
                            value = unitText,
                            onValueChange = { unitText = it },
                            label = { Text("Unit label") },
                            placeholder = { Text("glasses, pages, min") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("habit_unit_field")
                        )
                    }
                }

                // Section 3: Color Palette
                Text("Color", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                ColorPaletteSelector(
                    selectedColorHex = selectedColorHex,
                    onColorSelected = { selectedColorHex = it },
                    customPalette = DefaultHabitPalette
                )

                // Section 4: Icon Selector
                Text("Icon", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HabitIconsList.forEach { iconItem ->
                        val isSelected = selectedIconName == iconItem.name
                        val habitColor = parseHexColor(selectedColorHex)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) habitColor.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) habitColor else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedIconName = iconItem.name }
                                .testTag("icon_option_${iconItem.name}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconItem.icon,
                                contentDescription = iconItem.label,
                                tint = if (isSelected) habitColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Section 5: Frequency
                Text("Frequency", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = frequencyType == FrequencyType.EVERY_DAY.name,
                        onClick = { frequencyType = FrequencyType.EVERY_DAY.name },
                        label = { Text("Every Day") }
                    )
                    FilterChip(
                        selected = frequencyType == FrequencyType.SPECIFIC_DAYS.name,
                        onClick = { frequencyType = FrequencyType.SPECIFIC_DAYS.name },
                        label = { Text("Specific Days") }
                    )
                    FilterChip(
                        selected = frequencyType == FrequencyType.TIMES_PER_WEEK.name,
                        onClick = { frequencyType = FrequencyType.TIMES_PER_WEEK.name },
                        label = { Text("Times / Week") }
                    )
                    FilterChip(
                        selected = frequencyType == FrequencyType.TIMES_PER_MONTH.name,
                        onClick = { frequencyType = FrequencyType.TIMES_PER_MONTH.name },
                        label = { Text("Times / Month") }
                    )
                }

                if (frequencyType == FrequencyType.SPECIFIC_DAYS.name) {
                    val daysLabels = listOf(
                        1 to "M", 2 to "T", 3 to "W", 4 to "T", 5 to "F", 6 to "S", 7 to "S"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        daysLabels.forEach { (dayNum, label) ->
                            val isSelected = selectedWeekdays.contains(dayNum)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        selectedWeekdays = if (isSelected) {
                                            if (selectedWeekdays.size > 1) selectedWeekdays - dayNum else selectedWeekdays
                                        } else {
                                            selectedWeekdays + dayNum
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else if (frequencyType == FrequencyType.TIMES_PER_WEEK.name || frequencyType == FrequencyType.TIMES_PER_MONTH.name) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (frequencyType == FrequencyType.TIMES_PER_WEEK.name) "$frequencyTarget times per week" else "$frequencyTarget times per month",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { if (frequencyTarget > 1) frequencyTarget-- }) { Text("-") }
                            OutlinedButton(onClick = { frequencyTarget++ }) { Text("+") }
                        }
                    }
                }

                // Section 6: Reminder
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Reminder Notification", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = if (reminderEnabled) String.format("%02d:%02d", reminderHour, reminderMinute) else "Off",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { reminderEnabled = it },
                                modifier = Modifier.testTag("reminder_switch")
                            )
                        }

                        if (reminderEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Time: ", style = MaterialTheme.typography.bodyMedium)
                                listOf(7, 9, 12, 18, 20, 21).forEach { hr ->
                                    FilterChip(
                                        selected = reminderHour == hr && reminderMinute == 0,
                                        onClick = {
                                            reminderHour = hr
                                            reminderMinute = 0
                                        },
                                        label = { Text(String.format("%02d:00", hr)) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 7: Danger Zone (Archive & Delete)
                if (isEditMode && initialHabit != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onToggleArchive?.invoke(initialHabit.id, !initialHabit.isArchived)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (initialHabit.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (initialHabit.isArchived) "Unarchive" else "Archive")
                        }

                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f).testTag("editor_delete_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showDeleteConfirmDialog && initialHabit != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Habit?") },
            text = { Text("Are you sure you want to delete '${initialHabit.name}' and all its history? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteHabit?.invoke(initialHabit.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
