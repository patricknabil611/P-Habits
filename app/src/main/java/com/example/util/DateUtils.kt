package com.example.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object DateUtils {
    val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    val MONTH_YEAR_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
    val SHORT_MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)
    val DAY_MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    val FULL_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US)

    fun today(): LocalDate = LocalDate.now()

    fun todayString(): String = today().format(ISO_DATE)

    fun toDateString(date: LocalDate): String = date.format(ISO_DATE)

    fun parseDate(dateString: String): LocalDate {
        return try {
            LocalDate.parse(dateString, ISO_DATE)
        } catch (e: Exception) {
            LocalDate.now()
        }
    }

    fun parseFirstDayOfWeek(firstDaySetting: String): DayOfWeek {
        return when (firstDaySetting.uppercase()) {
            "SUNDAY" -> DayOfWeek.SUNDAY
            "SATURDAY" -> DayOfWeek.SATURDAY
            else -> DayOfWeek.MONDAY
        }
    }

    /**
     * Returns the 7 days of the week containing [anchorDate] based on [firstDayOfWeek].
     */
    fun getWeekDays(anchorDate: LocalDate, firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY): List<LocalDate> {
        val startOfWeek = if (anchorDate.dayOfWeek == firstDayOfWeek) {
            anchorDate
        } else {
            anchorDate.with(TemporalAdjusters.previous(firstDayOfWeek))
        }
        return (0..6).map { startOfWeek.plusDays(it.toLong()) }
    }

    /**
     * Returns all days for a calendar month grid (including padding days from prev/next months
     * so that the grid starts on firstDayOfWeek and has complete 7-day rows).
     */
    fun getMonthCalendarDays(yearMonth: YearMonth, firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY): List<LocalDate> {
        val firstOfMonth = yearMonth.atDay(1)
        val lastOfMonth = yearMonth.atEndOfMonth()

        val startDay = if (firstOfMonth.dayOfWeek == firstDayOfWeek) {
            firstOfMonth
        } else {
            firstOfMonth.with(TemporalAdjusters.previous(firstDayOfWeek))
        }

        val lastDayOfWeek = firstDayOfWeek.minus(1)
        val endDay = if (lastOfMonth.dayOfWeek == lastDayOfWeek) {
            lastOfMonth
        } else {
            lastOfMonth.with(TemporalAdjusters.next(lastDayOfWeek))
        }

        val days = mutableListOf<LocalDate>()
        var curr = startDay
        while (!curr.isAfter(endDay)) {
            days.add(curr)
            curr = curr.plusDays(1)
        }
        return days
    }

    /**
     * Formats a local date using standard or custom format
     */
    fun formatDate(date: LocalDate, pattern: String = "MMM d"): String {
        return try {
            date.format(DateTimeFormatter.ofPattern(pattern, Locale.US))
        } catch (e: Exception) {
            date.format(DAY_MONTH_FORMAT)
        }
    }

    /**
     * Generates dates for past N days up to today
     */
    fun getPastDays(daysCount: Int): List<LocalDate> {
        val today = today()
        return (daysCount - 1 downTo 0).map { today.minusDays(it.toLong()) }
    }
}
