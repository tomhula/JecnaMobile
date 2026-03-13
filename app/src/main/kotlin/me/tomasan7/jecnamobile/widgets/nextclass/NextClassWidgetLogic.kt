package me.tomasan7.jecnamobile.widgets.nextclass

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.tomhula.jecnaapi.data.timetable.Lesson
import io.github.tomhula.jecnaapi.data.timetable.LessonPeriod
import io.github.tomhula.jecnaapi.data.timetable.Timetable
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import me.tomasan7.jecnamobile.timetable.ChangeEntry
import me.tomasan7.jecnamobile.timetable.DailySchedule
import kotlin.time.Clock
import kotlin.math.max

data class NextClassDisplayInfo(
    val currentLessons: List<LessonWithPeriodAndChange>,
    val nextLessons: List<LessonWithPeriodAndChange>,
    val isBreak: Boolean,
    val isSchoolOut: Boolean,
    val targetDate: LocalDate?,
    val minutesToNextState: Long?
)

data class LessonWithPeriodAndChange(
    val index: Int,
    val lesson: Lesson,
    val period: LessonPeriod,
    val change: ChangeEntry?
)

fun scheduleNextTick(context: Context, nextTickTime: LocalTime?) {
    if (nextTickTime == null) return

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, NextClassTickReceiver::class.java).apply {
        action = NextClassTickReceiver.ACTION_TICK
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    
    var targetDateTime = LocalDateTime(now.year, now.month, now.dayOfMonth, nextTickTime.hour, nextTickTime.minute, nextTickTime.second)
    
    if (targetDateTime < now) {
        targetDateTime = LocalDateTime(
            now.date.plus(1, DateTimeUnit.DAY), 
            nextTickTime
        )
    }

    val triggerAtMillis = targetDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    } catch (e: SecurityException) {
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }
}

private fun getMinutesBetween(start: LocalTime, end: LocalTime): Long {
    val startMin = start.hour * 60 + start.minute
    val endMin = end.hour * 60 + end.minute
    return max(0L, (endMin - startMin).toLong())
}

fun calculateNextClassInfo(
    timetable: Timetable,
    today: LocalDateTime,
    dailySchedule: DailySchedule?
): Pair<NextClassDisplayInfo, LocalTime?> {
    val currentDay = today.dayOfWeek
    val currentTime = today.time

    // Handle weekends
    if (currentDay == DayOfWeek.SATURDAY || currentDay == DayOfWeek.SUNDAY) {
        val mondayDate = today.date.plus(if (currentDay == DayOfWeek.SATURDAY) 2 else 1, DateTimeUnit.DAY)
        return getFirstLessonOfDay(timetable, DayOfWeek.MONDAY, null, mondayDate) to null
    }

    val todaySpots = timetable.getLessonSpotsForDay(currentDay)
    
    if (todaySpots == null || todaySpots.isEmpty()) {
        return NextClassDisplayInfo(emptyList(), emptyList(), false, true, null, null) to null
    }

    val periods = timetable.lessonPeriods
    var currentLessons = mutableListOf<LessonWithPeriodAndChange>()
    var nextLessons = mutableListOf<LessonWithPeriodAndChange>()
    var isBreak = false
    var nextTickTime: LocalTime? = null
    var minutesToNextState: Long? = null

    for (i in todaySpots.indices) {
        val spot = todaySpots[i].toList()
        if (spot.isEmpty()) continue
        
        val period = periods.getOrNull(i) ?: continue
        val lessonList = spot
        val change = dailySchedule?.changes?.getOrNull(i)

        val lessonStart = period.from
        val lessonEnd = period.to

        if (currentTime >= lessonStart && currentTime < lessonEnd) {
            // We are currently in this lesson (add all lessons in this period)
            lessonList.forEach { lesson ->
                currentLessons.add(LessonWithPeriodAndChange(i, lesson, period, change))
            }
            nextTickTime = lessonEnd // Next tick when lesson ends
            minutesToNextState = getMinutesBetween(currentTime, lessonEnd)
            break
        } else if (currentTime < lessonStart) {
            // This is the next upcoming lesson (add all lessons in this period)
            lessonList.forEach { lesson ->
                nextLessons.add(LessonWithPeriodAndChange(i, lesson, period, change))
            }
            
            // Check if we are currently in a break
            if (i > 0) {
                 val prevPeriod = periods.getOrNull(i - 1)
                 if (prevPeriod != null && currentTime >= prevPeriod.to) {
                     isBreak = true
                 }
            } else {
                 isBreak = true // Before school starts
            }
            
            if (nextTickTime == null) {
                nextTickTime = lessonStart // Next tick when lesson starts
                minutesToNextState = getMinutesBetween(currentTime, lessonStart)
            }
            break
        }
    }

    // Find next lesson if we only found current
    if (currentLessons.isNotEmpty() && nextLessons.isEmpty()) {
        for (i in (currentLessons.first().index + 1) until todaySpots.size) {
            val spot = todaySpots[i].toList()
            if (spot.isNotEmpty()) {
                val period = periods.getOrNull(i) ?: continue
                val lessonList = spot
                val change = dailySchedule?.changes?.getOrNull(i)
                lessonList.forEach { lesson ->
                     nextLessons.add(LessonWithPeriodAndChange(i, lesson, period, change))
                }
                break
            }
        }
    }

    if (currentLessons.isEmpty() && nextLessons.isEmpty()) {
        // School's out for today
        if (currentDay == DayOfWeek.FRIDAY) {
            val mondayDate = today.date.plus(3, DateTimeUnit.DAY)
            return getFirstLessonOfDay(timetable, DayOfWeek.MONDAY, null, mondayDate) to null
        } else {
            val tomorrowDate = today.date.plus(1, DateTimeUnit.DAY)
            return getFirstLessonOfDay(timetable, currentDay.plus(1), null, tomorrowDate) to null
        }
    }

    val info = NextClassDisplayInfo(
        currentLessons = currentLessons,
        nextLessons = nextLessons,
        isBreak = isBreak,
        isSchoolOut = false,
        targetDate = today.date,
        minutesToNextState = minutesToNextState
    )
    
    return info to nextTickTime
}

private fun getFirstLessonOfDay(
    timetable: Timetable, 
    day: DayOfWeek, 
    dailySchedule: DailySchedule?,
    targetDate: LocalDate
): NextClassDisplayInfo {
    val spots = timetable.getLessonSpotsForDay(day)
    if (spots == null || spots.isEmpty()) {
         return NextClassDisplayInfo(emptyList(), emptyList(), false, true, targetDate, null)
    }
    
    for (i in spots.indices) {
        val spot = spots[i].toList()
        if (spot.isNotEmpty()) {
            val period = timetable.lessonPeriods.getOrNull(i) ?: continue
            val lessonList = spot
            val change = dailySchedule?.changes?.getOrNull(i)
            
            val nextLessons = mutableListOf<LessonWithPeriodAndChange>()
            lessonList.forEach { lesson ->
                 nextLessons.add(LessonWithPeriodAndChange(i, lesson, period, change))
            }
            return NextClassDisplayInfo(emptyList(), nextLessons, false, true, targetDate, null)
        }
    }
    
    return NextClassDisplayInfo(emptyList(), emptyList(), false, true, targetDate, null)
}

// Helper to add days to DayOfWeek
private fun DayOfWeek.plus(days: Int): DayOfWeek {
    val currentOrdinal = this.ordinal
    val newOrdinal = (currentOrdinal + days) % 7
    return DayOfWeek.values()[newOrdinal]
}
