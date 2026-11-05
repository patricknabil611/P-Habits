package com.example.util

import com.example.data.model.Habit
import com.example.data.model.HabitEntry
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class StreakInfo(
    val currentStreak: Int,
    val bestStreak: Int,
    val topStreaks: List<StreakInterval>
)

data class StreakInterval(
    val length: Int,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class HabitScorePoint(
    val date: LocalDate,
    val score: Float, // 0.0 to 1.0
    val value: Float,
    val isCompleted: Boolean
)

data class HabitAnalytics(
    val currentScore: Float, // 0 to 100%
    val currentStreak: Int,
    val bestStreak: Int,
    val topStreaks: List<StreakInterval>,
    val totalCompletions: Int,
    val totalEligibleDays: Int,
    val completionRate: Float, // 0 to 100%
    val weekdayCounts: Map<DayOfWeek, Int>,
    val historyScores: List<HabitScorePoint>,
    val yearHeatmap: List<HeatmapDay>
)

data class HeatmapDay(
    val date: LocalDate,
    val value: Float,
    val target: Float,
    val completionFraction: Float // 0f to 1f
)

data class PeriodSummary(
    val label: String,
    val completedCount: Int,
    val totalTarget: Float,
    val actualSum: Float,
    val rate: Float
)

object HabitStrengthCalculator {

    /**
     * Exponential moving average decay factor, similar to Loop Habit Tracker.
     * With decay = 0.92, a single miss does not wipe out your score, but consistent
     * effort steadily builds strength up to 100%.
     */
    private const val DECAY_FACTOR = 0.92f

    fun calculateAnalytics(
        habit: Habit,
        entries: List<HabitEntry>,
        upToDate: LocalDate = DateUtils.today(),
        historyDays: Int = 365
    ): HabitAnalytics {
        val entryMap = entries.associateBy { it.dateString }
        val startDate = upToDate.minusDays(historyDays.toLong() - 1)

        var currentEma = 0f
        val scorePoints = mutableListOf<HabitScorePoint>()
        val heatmapDays = mutableListOf<HeatmapDay>()
        val weekdayCompletions = mutableMapOf<DayOfWeek, Int>()
        DayOfWeek.values().forEach { weekdayCompletions[it] = 0 }

        var totalCompletions = 0
        var totalEligibleDays = 0

        var curr = startDate
        while (!curr.isAfter(upToDate)) {
            val dateStr = DateUtils.toDateString(curr)
            val entry = entryMap[dateStr]
            val valNum = entry?.value ?: 0f

            val target = if (habit.isCountType) habit.target.coerceAtLeast(1f) else 1f
            val fraction = (valNum / target).coerceIn(0f, 1f)
            val isCompleted = valNum >= target

            val isDue = habit.isFrequencyDay(curr.dayOfWeek.value)

            if (isDue) {
                totalEligibleDays++
                if (isCompleted) {
                    totalCompletions++
                    val currentCount = weekdayCompletions[curr.dayOfWeek] ?: 0
                    weekdayCompletions[curr.dayOfWeek] = currentCount + 1
                }
                // Update Loop-style strength EMA
                currentEma = currentEma * DECAY_FACTOR + fraction * (1f - DECAY_FACTOR)
            } else {
                // If not due on this day, keep strength decaying very gently
                currentEma *= 0.99f
            }

            scorePoints.add(
                HabitScorePoint(
                    date = curr,
                    score = currentEma.coerceIn(0f, 1f),
                    value = valNum,
                    isCompleted = isCompleted
                )
            )

            heatmapDays.add(
                HeatmapDay(
                    date = curr,
                    value = valNum,
                    target = target,
                    completionFraction = fraction
                )
            )

            curr = curr.plusDays(1)
        }

        val streakInfo = calculateStreaks(habit, entryMap, upToDate)
        val completionRate = if (totalEligibleDays > 0) {
            (totalCompletions.toFloat() / totalEligibleDays * 100f).coerceIn(0f, 100f)
        } else 0f

        return HabitAnalytics(
            currentScore = (currentEma * 100f).coerceIn(0f, 100f),
            currentStreak = streakInfo.currentStreak,
            bestStreak = streakInfo.bestStreak,
            topStreaks = streakInfo.topStreaks,
            totalCompletions = totalCompletions,
            totalEligibleDays = totalEligibleDays,
            completionRate = completionRate,
            weekdayCounts = weekdayCompletions,
            historyScores = scorePoints,
            yearHeatmap = heatmapDays
        )
    }

    /**
     * Calculates streak information (current streak, longest streak, and top streaks list)
     */
    fun calculateStreaks(
        habit: Habit,
        entryMap: Map<String, HabitEntry>,
        today: LocalDate = DateUtils.today()
    ): StreakInfo {
        val target = if (habit.isCountType) habit.target.coerceAtLeast(1f) else 1f

        // Check completion on given day
        fun isDayDone(date: LocalDate): Boolean {
            val entry = entryMap[DateUtils.toDateString(date)] ?: return false
            return entry.value >= target
        }

        // 1. Current streak calculation:
        // If today is completed, streak includes today and goes backward.
        // If today is not completed yet, streak might be active starting from yesterday.
        var currentStreak = 0
        var checkDate = today
        if (!isDayDone(checkDate)) {
            checkDate = today.minusDays(1)
        }

        while (true) {
            if (habit.isFrequencyDay(checkDate.dayOfWeek.value)) {
                if (isDayDone(checkDate)) {
                    currentStreak++
                } else {
                    break
                }
            }
            checkDate = checkDate.minusDays(1)
            // Stop if searching too far back (e.g. 5 years)
            if (checkDate.isBefore(today.minusYears(5))) break
        }

        // 2. Best streak & top streaks calculation over historical entries
        val sortedDates = entryMap.keys
            .mapNotNull {
                try {
                    LocalDate.parse(it, DateUtils.ISO_DATE)
                } catch (e: Exception) {
                    null
                }
            }
            .filter { isDayDone(it) }
            .sorted()

        val allIntervals = mutableListOf<StreakInterval>()
        if (sortedDates.isNotEmpty()) {
            var streakStart = sortedDates.first()
            var prevDate = sortedDates.first()
            var currentLen = 1

            for (i in 1 until sortedDates.size) {
                val date = sortedDates[i]
                if (date == prevDate.plusDays(1)) {
                    currentLen++
                    prevDate = date
                } else if (date != prevDate) {
                    // streak broke
                    allIntervals.add(StreakInterval(currentLen, streakStart, prevDate))
                    streakStart = date
                    prevDate = date
                    currentLen = 1
                }
            }
            allIntervals.add(StreakInterval(currentLen, streakStart, prevDate))
        }

        // Sort top streaks descending by length
        val sortedTop = allIntervals.sortedByDescending { it.length }
        val bestStreak = sortedTop.firstOrNull()?.length ?: currentStreak

        return StreakInfo(
            currentStreak = currentStreak,
            bestStreak = maxOf(currentStreak, bestStreak),
            topStreaks = sortedTop.take(5)
        )
    }

    /**
     * Groups entries into period summaries for bar charts (week, month, year)
     */
    fun aggregatePeriodHistory(
        entries: List<HabitEntry>,
        habit: Habit,
        periodType: String // "WEEK", "MONTH", "YEAR"
    ): List<PeriodSummary> {
        val target = if (habit.isCountType) habit.target.coerceAtLeast(1f) else 1f
        val entryMap = entries.associateBy { it.dateString }
        val today = DateUtils.today()
        val result = mutableListOf<PeriodSummary>()

        when (periodType.uppercase()) {
            "WEEK" -> {
                // Last 12 weeks
                for (w in 11 downTo 0) {
                    val weekStart = today.minusWeeks(w.toLong()).with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    val weekEnd = weekStart.plusDays(6)
                    var completedCount = 0
                    var sum = 0f
                    var eligible = 0
                    var d = weekStart
                    while (!d.isAfter(weekEnd) && !d.isAfter(today)) {
                        if (habit.isFrequencyDay(d.dayOfWeek.value)) {
                            eligible++
                            val entry = entryMap[DateUtils.toDateString(d)]
                            val v = entry?.value ?: 0f
                            sum += v
                            if (v >= target) completedCount++
                        }
                        d = d.plusDays(1)
                    }
                    val label = "${weekStart.format(DateTimeFormatter.ofPattern("MMM d"))}"
                    val rate = if (eligible > 0) completedCount.toFloat() / eligible else 0f
                    result.add(PeriodSummary(label, completedCount, eligible.toFloat(), sum, rate))
                }
            }
            "MONTH" -> {
                // Last 6 months
                for (m in 5 downTo 0) {
                    val ym = java.time.YearMonth.from(today.minusMonths(m.toLong()))
                    val monthStart = ym.atDay(1)
                    val monthEnd = ym.atEndOfMonth()
                    var completedCount = 0
                    var sum = 0f
                    var eligible = 0
                    var d = monthStart
                    while (!d.isAfter(monthEnd) && !d.isAfter(today)) {
                        if (habit.isFrequencyDay(d.dayOfWeek.value)) {
                            eligible++
                            val entry = entryMap[DateUtils.toDateString(d)]
                            val v = entry?.value ?: 0f
                            sum += v
                            if (v >= target) completedCount++
                        }
                        d = d.plusDays(1)
                    }
                    val label = ym.format(DateTimeFormatter.ofPattern("MMM"))
                    val rate = if (eligible > 0) completedCount.toFloat() / eligible else 0f
                    result.add(PeriodSummary(label, completedCount, eligible.toFloat(), sum, rate))
                }
            }
            else -> {
                // Year by quarters or last 4 years
                for (y in 3 downTo 0) {
                    val yr = today.year - y
                    var completedCount = 0
                    var sum = 0f
                    var eligible = 0
                    var d = LocalDate.of(yr, 1, 1)
                    val endOfYear = LocalDate.of(yr, 12, 31)
                    while (!d.isAfter(endOfYear) && !d.isAfter(today)) {
                        if (habit.isFrequencyDay(d.dayOfWeek.value)) {
                            eligible++
                            val entry = entryMap[DateUtils.toDateString(d)]
                            val v = entry?.value ?: 0f
                            sum += v
                            if (v >= target) completedCount++
                        }
                        d = d.plusDays(1)
                    }
                    val label = "$yr"
                    val rate = if (eligible > 0) completedCount.toFloat() / eligible else 0f
                    result.add(PeriodSummary(label, completedCount, eligible.toFloat(), sum, rate))
                }
            }
        }
        return result
    }
}
