package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.HabitViewModel
import com.example.ui.details.HabitDetailsScreen
import com.example.ui.editor.HabitEditorDialog
import com.example.ui.home.HomeScreen
import com.example.ui.month.MonthScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.stats.StatsOverviewScreen
import com.example.ui.theme.PHabitsTheme

enum class MainTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    WEEK("Week", Icons.Filled.CalendarViewWeek, Icons.Outlined.CalendarViewWeek, "nav_week"),
    MONTH("Month", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_month"),
    STATS("Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart, "nav_stats"),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: HabitViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            PHabitsTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor,
                accentHex = settings.accentColor
            ) {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: HabitViewModel) {
    val allHabits by viewModel.allHabits.collectAsStateWithLifecycle()
    val activeHabits by viewModel.activeHabits.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val habitsToDisplay = if (settings.showArchived) allHabits else activeHabits

    var currentTab by remember { mutableStateOf(MainTab.WEEK) }
    var selectedHabitIdForDetails by remember { mutableStateOf<Long?>(null) }
    var showCreateHabitDialog by remember { mutableStateOf(false) }

    // Handle back button when on habit details screen
    if (selectedHabitIdForDetails != null) {
        BackHandler {
            selectedHabitIdForDetails = null
        }
    }

    val selectedHabit = allHabits.find { it.id == selectedHabitIdForDetails }

    if (selectedHabit != null) {
        // Habit Details & Analytics Screen
        HabitDetailsScreen(
            habit = selectedHabit,
            entries = entries.filter { it.habitId == selectedHabit.id },
            settings = settings,
            onBack = { selectedHabitIdForDetails = null },
            onSaveHabit = { updated -> viewModel.saveHabit(updated) },
            onDeleteHabit = { id -> viewModel.deleteHabit(id) },
            onToggleArchive = { id, archived -> viewModel.toggleArchive(id, archived) }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    MainTab.values().forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = { Text(tab.title) },
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_transition"
                ) { tab ->
                    when (tab) {
                        MainTab.WEEK -> HomeScreen(
                            habits = habitsToDisplay,
                            entries = entries,
                            settings = settings,
                            onToggleEntry = { id, dateStr -> viewModel.toggleYesNo(id, dateStr) },
                            onIncrementEntry = { id, dateStr, delta, target -> viewModel.incrementCount(id, dateStr, delta, target) },
                            onSetEntry = { id, dateStr, value -> viewModel.setCount(id, dateStr, value) },
                            onOpenHabitDetails = { id -> selectedHabitIdForDetails = id },
                            onAddNewHabit = { showCreateHabitDialog = true },
                            onReorderHabits = { reordered -> viewModel.reorderHabits(reordered) },
                            onLoadSampleData = { viewModel.reloadSampleData() }
                        )

                        MainTab.MONTH -> MonthScreen(
                            habits = habitsToDisplay,
                            entries = entries,
                            settings = settings,
                            onToggleYesNo = { id, dateStr -> viewModel.toggleYesNo(id, dateStr) },
                            onIncrementCount = { id, dateStr, delta, target -> viewModel.incrementCount(id, dateStr, delta, target) },
                            onDecrementCount = { id, dateStr -> viewModel.decrementCount(id, dateStr) },
                            onSetCount = { id, dateStr, value -> viewModel.setCount(id, dateStr, value) }
                        )

                        MainTab.STATS -> StatsOverviewScreen(
                            habits = habitsToDisplay,
                            entries = entries,
                            onOpenHabitDetails = { id -> selectedHabitIdForDetails = id }
                        )

                        MainTab.SETTINGS -> SettingsScreen(
                            settings = settings,
                            habits = allHabits,
                            entries = entries,
                            onUpdateSettings = { updated -> viewModel.updateSettings(updated) },
                            onReloadSampleData = { viewModel.reloadSampleData() },
                            onClearAllData = { viewModel.clearAllData() },
                            onRestoreBackup = { h, e -> viewModel.restoreBackup(h, e) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateHabitDialog) {
        HabitEditorDialog(
            initialHabit = null,
            onDismiss = { showCreateHabitDialog = false },
            onSaveHabit = { newHabit ->
                viewModel.saveHabit(newHabit)
                showCreateHabitDialog = false
            }
        )
    }
}
