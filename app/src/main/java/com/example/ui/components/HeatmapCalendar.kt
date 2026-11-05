package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.DateUtils
import com.example.util.HeatmapDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun YearlyHeatmapGrid(
    heatmapDays: List<HeatmapDay>,
    habitColor: Color,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var selectedDay by remember { mutableStateOf<HeatmapDay?>(null) }

    // Scroll to the end (today) on initial layout
    LaunchedEffect(heatmapDays.size) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    // Organize days into weeks (columns) of 7 days (rows)
    // Group by (week offset from start)
    val sorted = remember(heatmapDays) { heatmapDays.sortedBy { it.date } }
    val weeks = remember(sorted) {
        if (sorted.isEmpty()) emptyList()
        else {
            val list = mutableListOf<MutableList<HeatmapDay?>>()
            var currentWeek = MutableList<HeatmapDay?>(7) { null }

            for (day in sorted) {
                // DayOfWeek 1..7 (Mon=1..Sun=7) -> index 0..6
                val dayIndex = day.date.dayOfWeek.value - 1
                if (dayIndex == 0 && currentWeek.any { it != null }) {
                    list.add(currentWeek)
                    currentWeek = MutableList(7) { null }
                }
                currentWeek[dayIndex] = day
            }
            if (currentWeek.any { it != null }) {
                list.add(currentWeek)
            }
            list
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(vertical = 4.dp)
        ) {
            // Day-of-week labels column on left
            Column(
                modifier = Modifier.padding(end = 6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val labels = listOf("M", "", "W", "", "F", "", "S")
                labels.forEach { label ->
                    Box(
                        modifier = Modifier.size(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            // Columns of weeks
            weeks.forEach { weekDays ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(horizontal = 1.5.dp)
                ) {
                    for (i in 0..6) {
                        val day = weekDays[i]
                        if (day != null) {
                            val fraction = day.completionFraction
                            val squareColor = when {
                                fraction <= 0f -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                fraction < 0.35f -> habitColor.copy(alpha = 0.35f)
                                fraction < 0.70f -> habitColor.copy(alpha = 0.65f)
                                fraction < 1.0f -> habitColor.copy(alpha = 0.85f)
                                else -> habitColor
                            }

                            val isSelected = selectedDay?.date == day.date
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(squareColor)
                                    .clickable { selectedDay = day }
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = RoundedCornerShape(3.dp)
                                    )
                            )
                        } else {
                            // Blank padding cell
                            Box(modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // Legend and day details strip
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (selectedDay != null) {
                val d = selectedDay!!
                val dateStr = d.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                val valStr = if (d.target > 1f) {
                    "${d.value.toInt()}/${d.target.toInt()} (${(d.completionFraction * 100).toInt()}%)"
                } else {
                    if (d.value >= 1f) "Completed" else "Not done"
                }
                Text(
                    text = "$dateStr: $valStr",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "Tap any cell for details",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Intensity Legend (Less -> More)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text("Less", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
                listOf(0f, 0.3f, 0.65f, 1f).forEach { frac ->
                    val c = if (frac <= 0f) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else habitColor.copy(alpha = frac)
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(c)
                    )
                }
                Text("More", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
            }
        }
    }
}
