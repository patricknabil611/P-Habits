package com.example.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Habit
import com.example.data.model.HabitEntry
import com.example.ui.components.getHabitIcon
import com.example.ui.theme.parseHexColor
import com.example.util.DateUtils
import com.example.util.HabitStrengthCalculator

data class HabitPerformanceItem(
    val habit: Habit,
    val streak: Int,
    val strengthScore: Float,
    val isDoneToday: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsOverviewScreen(
    habits: List<Habit>,
    entries: List<HabitEntry>,
    onOpenHabitDetails: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { DateUtils.today() }
    val todayStr = remember { DateUtils.todayString() }

    val activeHabits = remember(habits) { habits.filter { !it.isArchived } }
    val entryMap = remember(entries) { entries.associateBy { "${it.habitId}_${it.dateString}" } }

    // 1. Today's progress
    val dueTodayHabits = remember(activeHabits, today) {
        activeHabits.filter { it.isFrequencyDay(today.dayOfWeek.value) }
    }
    val completedTodayHabits = remember(dueTodayHabits, entryMap, todayStr) {
        dueTodayHabits.filter { h ->
            val v = entryMap["${h.id}_$todayStr"]?.value ?: 0f
            val target = if (h.isCountType) h.target else 1f
            v >= target
        }
    }
    val todayFraction = if (dueTodayHabits.isNotEmpty()) {
        completedTodayHabits.size.toFloat() / dueTodayHabits.size
    } else 0f

    // 2. Weekly completion rate (past 7 days)
    val past7Days = remember(today) { (0..6).map { today.minusDays(it.toLong()) } }
    var weeklyCompleted = 0
    var weeklyDue = 0
    past7Days.forEach { d ->
        val dStr = DateUtils.toDateString(d)
        activeHabits.forEach { h ->
            if (h.isFrequencyDay(d.dayOfWeek.value)) {
                weeklyDue++
                val v = entryMap["${h.id}_$dStr"]?.value ?: 0f
                val target = if (h.isCountType) h.target else 1f
                if (v >= target) weeklyCompleted++
            }
        }
    }
    val weeklyRate = if (weeklyDue > 0) (weeklyCompleted.toFloat() / weeklyDue * 100f) else 0f

    // 3. Monthly completion rate (past 30 days)
    val past30Days = remember(today) { (0..29).map { today.minusDays(it.toLong()) } }
    var monthlyCompleted = 0
    var monthlyDue = 0
    past30Days.forEach { d ->
        val dStr = DateUtils.toDateString(d)
        activeHabits.forEach { h ->
            if (h.isFrequencyDay(d.dayOfWeek.value)) {
                monthlyDue++
                val v = entryMap["${h.id}_$dStr"]?.value ?: 0f
                val target = if (h.isCountType) h.target else 1f
                if (v >= target) monthlyCompleted++
            }
        }
    }
    val monthlyRate = if (monthlyDue > 0) (monthlyCompleted.toFloat() / monthlyDue * 100f) else 0f

    // 4. Performance rankings (best habits & needs attention)
    val performanceItems = remember(activeHabits, entries, today) {
        activeHabits.map { h ->
            val hEntries = entries.filter { it.habitId == h.id }
            val hEntriesMap = hEntries.associateBy { it.dateString }
            val streak = HabitStrengthCalculator.calculateStreaks(h, hEntriesMap, today).currentStreak
            val score = HabitStrengthCalculator.calculateAnalytics(h, hEntries, today, 60).currentScore
            val isDone = (entryMap["${h.id}_$todayStr"]?.value ?: 0f) >= (if (h.isCountType) h.target else 1f)
            HabitPerformanceItem(h, streak, score, isDone)
        }
    }

    val bestHabits = remember(performanceItems) {
        performanceItems.sortedByDescending { it.strengthScore * 0.7f + it.streak * 0.3f }
    }
    val needsAttentionHabits = remember(performanceItems) {
        performanceItems.filter { !it.isDoneToday }.sortedBy { it.strengthScore }
    }

    val animatedTodayProgress = remember { Animatable(0f) }
    LaunchedEffect(todayFraction) {
        animatedTodayProgress.animateTo(todayFraction, tween(600))
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Stats Overview", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Today's Progress Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val progress = animatedTodayProgress.value
                        val primary = MaterialTheme.colorScheme.primary
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 9.dp.toPx()
                            val radius = (size.minDimension - stroke) / 2
                            val center = Offset(size.width / 2, size.height / 2)

                            drawCircle(
                                color = primary.copy(alpha = 0.15f),
                                radius = radius,
                                style = Stroke(stroke)
                            )
                            if (progress > 0f) {
                                drawArc(
                                    color = primary,
                                    startAngle = -90f,
                                    sweepAngle = progress * 360f,
                                    useCenter = false,
                                    topLeft = Offset(center.x - radius, center.y - radius),
                                    size = Size(radius * 2, radius * 2),
                                    style = Stroke(stroke, cap = StrokeCap.Round)
                                )
                            }
                        }
                        Text(
                            text = "${(todayFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column {
                        Text(
                            text = "Today's Check-ins",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${completedTodayHabits.size} of ${dueTodayHabits.size} habits completed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val statusMsg = when {
                            todayFraction >= 1f -> "All done for today! Fantastic!"
                            todayFraction >= 0.5f -> "More than halfway there!"
                            else -> "Keep going, you've got this!"
                        }
                        Text(
                            text = statusMsg,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Rates Row: 7-Day & 30-Day Completion Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("7-Day Rate", style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${weeklyRate.toInt()}%",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { weeklyRate / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("30-Day Rate", style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${monthlyRate.toInt()}%",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { monthlyRate / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Best Performing Habits Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFC107))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Best Performing Habits",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    bestHabits.take(4).forEachIndexed { index, item ->
                        val habitColor = parseHexColor(item.habit.colorHex)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onOpenHabitDetails(item.habit.id) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${index + 1}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(26.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(habitColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getHabitIcon(item.habit.iconName),
                                    contentDescription = null,
                                    tint = habitColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = item.habit.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${item.streak}d",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFFF9800)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = habitColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${item.strengthScore.toInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = habitColor
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Needs Attention Card
            if (needsAttentionHabits.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pending Today",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        needsAttentionHabits.take(4).forEach { item ->
                            val habitColor = parseHexColor(item.habit.colorHex)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onOpenHabitDetails(item.habit.id) }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(habitColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getHabitIcon(item.habit.iconName),
                                        contentDescription = null,
                                        tint = habitColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = item.habit.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = if (item.habit.isCountType) "0/${item.habit.target.toInt()} ${item.habit.unit}" else "Not done",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Quick App Overview Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${activeHabits.size} Active Habits • ${entries.size} Total Check-ins",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
