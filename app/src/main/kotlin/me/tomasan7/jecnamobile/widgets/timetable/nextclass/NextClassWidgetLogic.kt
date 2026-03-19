package me.tomasan7.jecnamobile.widgets.timetable.nextclass

import android.content.Context
import io.github.tomhula.jecnaapi.data.timetable.Lesson
import io.github.tomhula.jecnaapi.data.timetable.LessonPeriod
import io.github.tomhula.jecnaapi.data.timetable.Timetable
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import me.tomasan7.jecnamobile.util.getWeekDayNameKey
import kotlin.math.max

data class NextClassDisplayInfo(
    val title: String,
    val targetDate: LocalDate,
    val currentLessons: List<Lesson>,
    val currentPeriod: LessonPeriod?,
    val currentLessonIndex: Int,
    val nextLessons: List<Lesson>,
    val nextPeriod: LessonPeriod?,
    val nextLessonIndex: Int,
    val isBreak: Boolean,
    val isSchoolOut: Boolean,
    val minutesToNextState: Long?
)

fun getNextClassDisplayInfo(
    context: Context,
    timetable: Timetable,
    today: LocalDateTime
): NextClassDisplayInfo {
    val currentDay = today.dayOfWeek
    val currentTime = today.time

    if (currentDay == DayOfWeek.SATURDAY || currentDay == DayOfWeek.SUNDAY) {
        val mondayDate = today.date.plus(if (currentDay == DayOfWeek.SATURDAY) 2 else 1, DateTimeUnit.DAY)
        val title = context.getString(getWeekDayNameKey(DayOfWeek.MONDAY))
        return NextClassDisplayInfo(
            title = title,
            targetDate = mondayDate,
            currentLessons = emptyList(),
            currentPeriod = null,
            currentLessonIndex = -1,
            nextLessons = emptyList(),
            nextPeriod = null,
            nextLessonIndex = -1,
            isBreak = false,
            isSchoolOut = true,
            minutesToNextState = null
        )
    }

    val todaySpots = timetable.getLessonSpotsForDay(currentDay)
    if (todaySpots == null || todaySpots.isEmpty()) {
        val tomorrowDate = today.date.plus(1, DateTimeUnit.DAY)
        val tomorrowDay = tomorrowDate.dayOfWeek
        val title = context.getString(getWeekDayNameKey(tomorrowDay))
        return NextClassDisplayInfo(
            title = title,
            targetDate = tomorrowDate,
            currentLessons = emptyList(),
            currentPeriod = null,
            currentLessonIndex = -1,
            nextLessons = emptyList(),
            nextPeriod = null,
            nextLessonIndex = -1,
            isBreak = false,
            isSchoolOut = true,
            minutesToNextState = null
        )
    }

    val periods = timetable.lessonPeriods
    var currentLessons: List<Lesson> = emptyList()
    var currentPeriod: LessonPeriod? = null
    var currentLessonIndex = -1
    var nextLessons: List<Lesson> = emptyList()
    var nextPeriod: LessonPeriod? = null
    var nextLessonIndex = -1
    var isBreak = false
    var minutesToNextState: Long? = null

    for (i in todaySpots.indices) {
        val spot = todaySpots[i].toList()
        if (spot.isEmpty()) continue

        val period = periods.getOrNull(i) ?: continue

        val lessonStart = period.from
        val lessonEnd = period.to

        if (currentTime in lessonStart..<lessonEnd) {
            currentLessons = spot
            currentPeriod = period
            currentLessonIndex = i
            minutesToNextState = getMinutesBetween(currentTime, lessonEnd)

            val nextIndex = i + 1
            if (nextIndex < todaySpots.size) {
                val nextSpot = todaySpots[nextIndex].toList()
                if (nextSpot.isNotEmpty()) {
                    nextLessons = nextSpot
                    nextPeriod = periods.getOrNull(nextIndex)
                    nextLessonIndex = nextIndex
                }
            }
            break
        } else if (currentTime < lessonStart) {
            nextLessons = spot
            nextPeriod = period
            nextLessonIndex = i

            if (i > 0) {
                val prevPeriod = periods.getOrNull(i - 1)
                if (prevPeriod != null && currentTime >= prevPeriod.to) {
                    isBreak = true
                }
            } else {
                isBreak = true
            }

            minutesToNextState = getMinutesBetween(currentTime, lessonStart)
            break
        }
    }

    if (currentLessons.isEmpty() && nextLessons.isEmpty()) {
        val isFriday = currentDay == DayOfWeek.FRIDAY
        val nextDate = if (isFriday) today.date.plus(3, DateTimeUnit.DAY) else today.date.plus(1, DateTimeUnit.DAY)
        val nextDay = nextDate.dayOfWeek
        val title = context.getString(getWeekDayNameKey(nextDay))
        return NextClassDisplayInfo(
            title = title,
            targetDate = nextDate,
            currentLessons = emptyList(),
            currentPeriod = null,
            currentLessonIndex = -1,
            nextLessons = emptyList(),
            nextPeriod = null,
            nextLessonIndex = -1,
            isBreak = false,
            isSchoolOut = true,
            minutesToNextState = null
        )
    }

    val title = context.getString(getWeekDayNameKey(currentDay))

    return NextClassDisplayInfo(
        title = title,
        targetDate = today.date,
        currentLessons = currentLessons,
        currentPeriod = currentPeriod,
        currentLessonIndex = currentLessonIndex,
        nextLessons = nextLessons,
        nextPeriod = nextPeriod,
        nextLessonIndex = nextLessonIndex,
        isBreak = isBreak,
        isSchoolOut = false,
        minutesToNextState = minutesToNextState
    )
}

private fun getMinutesBetween(start: LocalTime, end: LocalTime): Long {
    val startMin = start.hour * 60 + start.minute
    val endMin = end.hour * 60 + end.minute
    return max(0L, (endMin - startMin).toLong())
}
