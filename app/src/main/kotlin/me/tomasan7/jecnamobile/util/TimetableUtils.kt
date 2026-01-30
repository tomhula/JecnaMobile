import androidx.compose.ui.graphics.Color
import io.github.tomhula.jecnaapi.data.timetable.Lesson
import io.github.tomhula.jecnaapi.data.timetable.LessonSpot
import io.github.tomhula.jecnaapi.data.timetable.Timetable
import io.github.stevekk11.dtos.DailySchedule
import kotlinx.datetime.toKotlinDayOfWeek
import me.tomasan7.jecnamobile.ui.component.getSubstitutionColor
import java.time.LocalDate
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

/**
 * Merges substitutions into the timetable.
 * Returns a Pair containing the updated Timetable and a Map of Lessons to their specific status Colors.
 */
fun mergeTimetableWithSubstitutions(
    baseTimetable: Timetable,
    dailySchedules: List<DailySchedule>?
): Pair<Timetable, Map<Lesson, Color>> {
    val lessonColors = mutableMapOf<Lesson, Color>()
    
    val subsByDay = dailySchedules?.associate { schedule ->
        val date = LocalDate.parse(schedule.date)

        date.dayOfWeek.toKotlinDayOfWeek() to schedule.classSubs.values.flatten()
    }

    val updatedDaysMap = baseTimetable.days.associateWith { day ->
        val daySubs = subsByDay?.get(day) ?: return@associateWith baseTimetable.getLessonSpotsForDay(day)!!

        var currentHour = 1
        baseTimetable.getLessonSpotsForDay(day)!!.map { spot ->
            val hourRange = currentHour until (currentHour + spot.periodSpan)
            val relevantSubs = daySubs.filter { it.hour in hourRange }

            val newSpot = if (relevantSubs.isEmpty()) {
                spot
            } else {
                // Transform lessons within the spot
                val newLessons = spot.map { lesson ->
                    val sub = relevantSubs.find { it.group == lesson.group || it.group == null }
                    if (sub != null) {
                        val updatedLesson = lesson.copy(
                            subjectName = sub.subject?.let { lesson.subjectName.copy(full = it, short = it) } ?: lesson.subjectName,
                            teacherName = sub.substitutingTeacher?.let { lesson.teacherName?.copy(full = it, short = it) } ?: lesson.teacherName,
                            classroom = sub.room ?: lesson.classroom,
                            clazz = sub.originalText ?: lesson.clazz
                        )
                        lessonColors[updatedLesson] = getSubstitutionColor(sub)
                        updatedLesson
                    } else {
                        lesson
                    }
                }
                LessonSpot(newLessons, spot.periodSpan)
            }

            currentHour += spot.periodSpan
            newSpot
        }
    }
    val constructor = Timetable::class.primaryConstructor?.apply {
        isAccessible = true
    } ?: throw IllegalStateException("Could not find Timetable constructor")
    
    val mergedTimetable = constructor.call(baseTimetable.lessonPeriods, updatedDaysMap)
    return mergedTimetable to lessonColors
}
