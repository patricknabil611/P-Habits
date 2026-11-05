package com.example.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Habit
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CountInputDialog(
    habit: Habit,
    date: LocalDate,
    currentValue: Float,
    onDismiss: () -> Unit,
    onSaveValue: (Float) -> Unit,
    onReset: () -> Unit
) {
    var textValue by remember {
        mutableStateOf(if (currentValue % 1f == 0f) currentValue.toInt().toString() else currentValue.toString())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Daily Target: ${if (habit.target % 1f == 0f) habit.target.toInt() else habit.target} ${habit.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("Count value (${habit.unit.ifBlank { "times" }})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("count_input_field")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick presets buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 5).forEach { addVal ->
                        OutlinedButton(
                            onClick = {
                                val cur = textValue.toFloatOrNull() ?: 0f
                                val newVal = cur + addVal
                                textValue = if (newVal % 1f == 0f) newVal.toInt().toString() else newVal.toString()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+$addVal")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = textValue.toFloatOrNull() ?: 0f
                    onSaveValue(value)
                },
                modifier = Modifier.testTag("count_dialog_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onReset,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("count_dialog_reset_button")
                ) {
                    Text("Reset to 0")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
