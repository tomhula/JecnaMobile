package me.tomasan7.jecnamobile.widgets.timetable

import android.content.Context
import androidx.annotation.StringRes
import io.github.tomhula.jecnaapi.data.timetable.Lesson
import io.github.tomhula.jecnaapi.data.timetable.Timetable
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.util.getWeekDayNameKey

data class WidgetDisplayInfo(
    val targetDate: LocalDate,
    val targetDay: DayOfWeek,
    val title: String,
    val visibleSpots: List<List<Lesson>>
)

fun getWidgetDisplayInfo(
    context: Context,
    timetable: Timetable,
    today: LocalDateTime
): WidgetDisplayInfo {
    val currentDay = today.dayOfWeek

    val isAfterSchool = when (currentDay) {
        DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> true
        else -> {
            val todaySpots = timetable.getLessonSpotsForDay(currentDay).orEmpty()
            val lastLessonIndex = todaySpots.indexOfLast { it.toList().isNotEmpty() }

            if (lastLessonIndex != -1) {
                val lastPeriod = timetable.lessonPeriods.getOrNull(lastLessonIndex)
                lastPeriod != null && today.time > lastPeriod.to
            } else {
                today.hour >= 15
            }
        }
    }

    val daysToAdd = when {
        currentDay == DayOfWeek.SATURDAY -> 2
        currentDay == DayOfWeek.SUNDAY -> 1
        currentDay == DayOfWeek.FRIDAY && isAfterSchool -> 3
        isAfterSchool -> 1
        else -> 0
    }

    val targetDate = today.date.plus(DatePeriod(days = daysToAdd))
    val targetDay = targetDate.dayOfWeek

    val targetDayName = context.getString(getWeekDayNameKey(targetDay))

    val title = when {
        currentDay in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) ||
                (currentDay == DayOfWeek.FRIDAY && isAfterSchool) -> {
            context.getString(R.string.widget_timetable_on_monday)
        }
        isAfterSchool -> {
            context.getString(R.string.widget_timetable_tomorrow, targetDayName)
        }
        else -> {
            context.getString(R.string.widget_timetable_today, targetDayName)
        }
    }

    val visibleSpots = getVisibleLessonSpots(timetable, targetDay)

    return WidgetDisplayInfo(targetDate, targetDay, title, visibleSpots)
}

private fun getVisibleLessonSpots(timetable: Timetable, targetDay: DayOfWeek): List<List<Lesson>> {
    val allSpots: List<Iterable<Lesson>> = timetable.getLessonSpotsForDay(targetDay) ?: emptyList()
    val lastLessonIndex = allSpots.indexOfLast { it.toList().isNotEmpty() }
    return if (lastLessonIndex != -1) allSpots.take(lastLessonIndex + 1).map { it.toList() } else emptyList()
}
