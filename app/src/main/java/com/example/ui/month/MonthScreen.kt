package com.example.ui.month

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Habit
import com.example.data.model.HabitEntry
import com.example.data.model.UserSettings
import com.example.ui.theme.parseHexColor
import com.example.util.DateUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MonthScreen(
    habits: List<Habit>,
    entries: List<HabitEntry>,
    settings: UserSettings,
    onToggleYesNo: (Long, String) -> Unit,
    onIncrementCount: (Long, String, Float, Float) -> Unit,
    onDecrementCount: (Long, String) -> Unit,
    onSetCount: (Long, String, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = remember { DateUtils.today() }
    val firstDay = remember(settings.firstDayOfWeek) { DateUtils.parseFirstDayOfWeek(settings.firstDayOfWeek) }

    var selectedHabitFilterId by remember { mutableStateOf<Long?>(null) } // null = All Habits
    var isHeatmapMode by remember { mutableStateOf(false) } // Toggle heatmap mode vs dots/chips mode
    var selectedDateForSheet by remember { mutableStateOf<LocalDate?>(null) }

    val daysInGrid = remember(currentYearMonth, firstDay) {
        DateUtils.getMonthCalendarDays(currentYearMonth, firstDay)
    }

    // Pre-group entries by date string
    val entriesByDate = remember(entries) {
        entries.groupBy { it.dateString }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                actions = {
                    // Heatmap mode toggle
                    IconButton(
                        onClick = { isHeatmapMode = !isHeatmapMode },
                        modifier = Modifier.testTag("toggle_heatmap_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Toggle Heatmap Mode",
                            tint = if (isHeatmapMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Jump to today
                    val isCurrentMonth = currentYearMonth == YearMonth.now()
                    if (!isCurrentMonth) {
                        IconButton(
                            onClick = { currentYearMonth = YearMonth.now() },
                            modifier = Modifier.testTag("month_jump_today_button")
                        ) {
                            Icon(Icons.Default.Today, contentDescription = "This Month")
                        }
                    }

                    // Month navigation
                    IconButton(
                        onClick = { currentYearMonth = currentYearMonth.minusMonths(1) },
                        modifier = Modifier.testTag("prev_month_button")
                    ) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Previous Month", modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { currentYearMonth = currentYearMonth.plusMonths(1) },
                        modifier = Modifier.testTag("next_month_button")
                    ) {
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next Month", modifier = Modifier.size(16.dp))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Chips at top: "All Habits" and individual habits
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedHabitFilterId == null,
                    onClick = { selectedHabitFilterId = null },
                    label = { Text("All Habits (${habits.size})") },
                    modifier = Modifier.testTag("filter_all_habits")
                )
                habits.forEach { habit ->
                    val habitColor = parseHexColor(habit.colorHex)
                    FilterChip(
                        selected = selectedHabitFilterId == habit.id,
                        onClick = {
                            selectedHabitFilterId = if (selectedHabitFilterId == habit.id) null else habit.id
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(habitColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(habit.name)
                            }
                        },
                        modifier = Modifier.testTag("filter_habit_${habit.id}")
                    )
                }
            }

            // Day of Week Header row (M, T, W, T, F, S, S)
            val dayHeaders = remember(firstDay) {
                (0..6).map { firstDay.plus(it.toLong()) }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                dayHeaders.forEach { dow ->
                    Text(
                        text = dow.name.take(3),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Month Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(daysInGrid, key = { it.toString() }) { date ->
                    val isCurrentMonth = date.month == currentYearMonth.month
                    val isToday = date == today
                    val dateStr = DateUtils.toDateString(date)
                    val dayEntries = entriesByDate[dateStr] ?: emptyList()

                    val displayedHabits = if (selectedHabitFilterId != null) {
                        habits.filter { it.id == selectedHabitFilterId }
                    } else habits

                    // Calculate day completion stats
                    val completedHabits = displayedHabits.filter { h ->
                        val entry = dayEntries.find { it.habitId == h.id }
                        val v = entry?.value ?: 0f
                        val target = if (h.isCountType) h.target else 1f
                        v >= target
                    }

                    val completionFraction = if (displayedHabits.isNotEmpty()) {
                        completedHabits.size.toFloat() / displayedHabits.size
                    } else 0f

                    // Cell Background styling
                    val cellBgColor = if (isHeatmapMode && isCurrentMonth && completionFraction > 0f) {
                        val accentColor = if (settings.heatmapColorMode == "HABIT_COLOR" && displayedHabits.size == 1) {
                            parseHexColor(displayedHabits.first().colorHex)
                        } else MaterialTheme.colorScheme.primary

                        accentColor.copy(alpha = (0.15f + completionFraction * 0.7f).coerceIn(0.15f, 0.9f))
                    } else if (isToday) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    } else if (isCurrentMonth) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    } else {
                        Color.Transparent // Outside current month
                    }

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(cellBgColor)
                            .then(
                                if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                else Modifier
                            )
                            .clickable(enabled = isCurrentMonth) {
                                selectedDateForSheet = date
                            }
                            .padding(4.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (isCurrentMonth) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = "${date.dayOfMonth}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isHeatmapMode && completionFraction > 0.6f) Color.White
                                    else if (isToday) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )

                                if (!isHeatmapMode) {
                                    // Dots and Count Chips
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        displayedHabits.forEach { habit ->
                                            val entry = dayEntries.find { it.habitId == habit.id }
                                            val v = entry?.value ?: 0f
                                            val target = if (habit.isCountType) habit.target else 1f
                                            val habitColor = parseHexColor(habit.colorHex)

                                            if (v > 0f) {
                                                if (habit.isCountType) {
                                                    // Count number chip
                                                    Text(
                                                        text = "${v.toInt()}",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = habitColor
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 1.dp)
                                                    )
                                                } else {
                                                    // Yes/No colored dot
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(habitColor)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Heatmap mode shows completion percentage or count
                                    if (completionFraction > 0f) {
                                        Text(
                                            text = "${(completionFraction * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (completionFraction > 0.5f) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            // Faded day number for outside month
                            Text(
                                text = "${date.dayOfMonth}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Bottom sheet on day tap
    if (selectedDateForSheet != null) {
        DayHabitsBottomSheet(
            date = selectedDateForSheet!!,
            habits = habits,
            entries = entries,
            onDismiss = { selectedDateForSheet = null },
            onToggleYesNo = onToggleYesNo,
            onIncrementCount = onIncrementCount,
            onDecrementCount = onDecrementCount,
            onSetCount = onSetCount
        )
    }
}
