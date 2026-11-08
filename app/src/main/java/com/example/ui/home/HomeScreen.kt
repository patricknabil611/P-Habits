package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Habit
import com.example.data.model.HabitEntry
import com.example.data.model.UserSettings
import com.example.ui.components.getHabitIcon
import com.example.ui.theme.parseHexColor
import com.example.util.DateUtils
import com.example.util.HabitStrengthCalculator
import com.example.util.HapticUtil
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    habits: List<Habit>,
    entries: List<HabitEntry>,
    settings: UserSettings,
    onToggleEntry: (Long, String) -> Unit,
    onIncrementEntry: (Long, String, Float, Float) -> Unit,
    onSetEntry: (Long, String, Float) -> Unit,
    onOpenHabitDetails: (Long) -> Unit,
    onAddNewHabit: () -> Unit,
    onReorderHabits: (List<Habit>) -> Unit,
    onLoadSampleData: () -> Unit,
    onEditHabit: (Habit) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var anchorDate by remember { mutableStateOf(DateUtils.today()) }
    val today = remember { DateUtils.today() }
    val firstDay = remember(settings.firstDayOfWeek) { DateUtils.parseFirstDayOfWeek(settings.firstDayOfWeek) }
    val currentWeekDays = remember(anchorDate, firstDay) { DateUtils.getWeekDays(anchorDate, firstDay) }

    // Dialog state for count habit editing
    var countDialogHabit by remember { mutableStateOf<Pair<Habit, LocalDate>?>(null) }
    var isReorderMode by remember { mutableStateOf(false) }

    // Map entries for quick O(1) lookup: key = "${habitId}_${dateString}"
    val entryMap = remember(entries) {
        entries.associateBy { "${it.habitId}_${it.dateString}" }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "P Habits",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        // Week range subtitle chip with navigation arrows: < [Nov 1 - Nov 7] >
                        val weekStartStr = currentWeekDays.first().format(DateTimeFormatter.ofPattern("MMM d"))
                        val weekEndStr = currentWeekDays.last().format(DateTimeFormatter.ofPattern("MMM d"))

                        IconButton(
                            onClick = { anchorDate = anchorDate.minusWeeks(1) },
                            modifier = Modifier.size(32.dp).testTag("prev_week_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBackIosNew,
                                contentDescription = "Previous week",
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "$weekStartStr - $weekEndStr",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = { anchorDate = anchorDate.plusWeeks(1) },
                            modifier = Modifier.size(32.dp).testTag("next_week_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForwardIos,
                                contentDescription = "Next week",
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                },
                actions = {
                    // Quick "Today" jump button if not current week
                    val isCurrentWeek = currentWeekDays.contains(today)
                    if (!isCurrentWeek) {
                        IconButton(
                            onClick = { anchorDate = today },
                            modifier = Modifier.testTag("jump_today_button")
                        ) {
                            Icon(Icons.Default.Today, contentDescription = "Jump to Today")
                        }
                    }

                    // Reorder toggle button
                    IconButton(
                        onClick = { isReorderMode = !isReorderMode },
                        modifier = Modifier.testTag("toggle_reorder_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Reorder,
                            contentDescription = "Reorder habits",
                            tint = if (isReorderMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNewHabit,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_habit")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Habit")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Week Navigator Header
            WeekHeaderRow(
                weekDays = currentWeekDays,
                today = today,
                settings = settings
            )

            if (habits.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Start Tracking Your Habits",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Build streaks, score habit strength, and stay consistent offline.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            TextButton(
                                onClick = onLoadSampleData,
                                modifier = Modifier.testTag("load_sample_habits_button")
                            ) {
                                Text("Load Sample Habits")
                            }
                        }
                    }
                }
            } else {
                // List of habit rows with consistent 12dp spacing and bottom padding for FAB
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 104.dp)
                ) {
                    itemsIndexed(habits, key = { _, h -> h.id }) { index, habit ->
                        val habitEntries = remember(entries, habit.id) {
                            entries.filter { it.habitId == habit.id }
                        }
                        val habitEntriesMap = remember(habitEntries) {
                            habitEntries.associateBy { it.dateString }
                        }
                        val streakInfo = remember(habit, habitEntriesMap, today) {
                            HabitStrengthCalculator.calculateStreaks(habit, habitEntriesMap, today)
                        }

                        HabitRow(
                            habit = habit,
                            weekDays = currentWeekDays,
                            today = today,
                            entryMap = entryMap,
                            streak = streakInfo.currentStreak,
                            settings = settings,
                            isReorderMode = isReorderMode,
                            onMoveUp = {
                                if (index > 0) {
                                    val list = habits.toMutableList()
                                    val item = list.removeAt(index)
                                    list.add(index - 1, item)
                                    onReorderHabits(list)
                                }
                            },
                            onMoveDown = {
                                if (index < habits.size - 1) {
                                    val list = habits.toMutableList()
                                    val item = list.removeAt(index)
                                    list.add(index + 1, item)
                                    onReorderHabits(list)
                                }
                            },
                            onHabitClick = { onOpenHabitDetails(habit.id) },
                            onEditHabit = onEditHabit,
                            onCellClick = { date ->
                                val dateStr = DateUtils.toDateString(date)
                                if (habit.isCountType) {
                                    HapticUtil.lightTick(context)
                                    onIncrementEntry(habit.id, dateStr, 1f, habit.target)
                                } else {
                                    HapticUtil.successTick(context)
                                    onToggleEntry(habit.id, dateStr)
                                }
                            },
                            onCellLongClick = { date ->
                                countDialogHabit = Pair(habit, date)
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog for custom count input or reset
    if (countDialogHabit != null) {
        val (habit, date) = countDialogHabit!!
        val dateStr = DateUtils.toDateString(date)
        val curVal = entryMap["${habit.id}_$dateStr"]?.value ?: 0f

        CountInputDialog(
            habit = habit,
            date = date,
            currentValue = curVal,
            onDismiss = { countDialogHabit = null },
            onSaveValue = { value ->
                onSetEntry(habit.id, dateStr, value)
                countDialogHabit = null
            },
            onReset = {
                onSetEntry(habit.id, dateStr, 0f)
                countDialogHabit = null
            }
        )
    }
}

@Composable
fun WeekHeaderRow(
    weekDays: List<LocalDate>,
    today: LocalDate,
    settings: UserSettings,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Empty leading column matching the 56dp habit icon column
            Box(modifier = Modifier.width(56.dp))

            Spacer(modifier = Modifier.width(8.dp))

            // 7 Days header columns sharing remaining width equally
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                weekDays.forEach { date ->
                    val isToday = date == today
                    val dayName = date.format(DateTimeFormatter.ofPattern("EE")).take(2)
                    val dayNum = date.dayOfMonth.toString()

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isToday) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dayNum,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 13.sp,
                                    fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Normal
                                ),
                                color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitRow(
    habit: Habit,
    weekDays: List<LocalDate>,
    today: LocalDate,
    entryMap: Map<String, HabitEntry>,
    streak: Int,
    settings: UserSettings,
    isReorderMode: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onHabitClick: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    onCellClick: (LocalDate) -> Unit,
    onCellLongClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val habitColor = parseHexColor(habit.colorHex)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onHabitClick,
                onLongClick = { onEditHabit(habit) }
            ),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading 56dp column: habit icon (or reorder arrows when in reorder mode)
            Box(
                modifier = Modifier.width(56.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isReorderMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = onMoveUp,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move Up",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onMoveDown,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move Down",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(habitColor.copy(alpha = 0.2f))
                            .clickable { onEditHabit(habit) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getHabitIcon(habit.iconName),
                            contentDescription = "Edit ${habit.name}",
                            tint = habitColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 7 Days Cells sharing remaining width equally
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                weekDays.forEach { date ->
                    val dateStr = DateUtils.toDateString(date)
                    val entry = entryMap["${habit.id}_$dateStr"]
                    val value = entry?.value ?: 0f

                    HabitCell(
                        habit = habit,
                        date = date,
                        value = value,
                        isToday = date == today,
                        settings = settings,
                        onClick = { onCellClick(date) },
                        onLongClick = { onCellLongClick(date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
