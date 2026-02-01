package me.tomasan7.jecnamobile.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import io.github.stevekk11.dtos.SubstitutedLesson
import io.github.tomhula.jecnaapi.data.room.RoomReference
import io.github.tomhula.jecnaapi.data.schoolStaff.TeacherReference
import io.github.tomhula.jecnaapi.data.timetable.Lesson
import io.github.tomhula.jecnaapi.data.timetable.LessonPeriod
import io.github.tomhula.jecnaapi.data.timetable.LessonSpot
import io.github.tomhula.jecnaapi.data.timetable.Timetable
import kotlinx.datetime.DayOfWeek
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.ui.ElevationLevel
import me.tomasan7.jecnamobile.ui.theme.dropped
import me.tomasan7.jecnamobile.ui.theme.joined
import me.tomasan7.jecnamobile.ui.theme.room_change
import me.tomasan7.jecnamobile.ui.theme.shifted
import me.tomasan7.jecnamobile.ui.theme.separated
import me.tomasan7.jecnamobile.ui.theme.substitution
import me.tomasan7.jecnamobile.util.getWeekDayName
import me.tomasan7.jecnamobile.util.manipulate
import kotlin.time.Clock

@Composable
fun Timetable(
    timetable: Timetable,
    modifier: Modifier = Modifier,
    lessonColors: Map<Lesson, Color> = emptyMap(),
    substitutions: Map<String, SubstitutedLesson> = emptyMap(),
    hideClass: Boolean = false,
    onTeacherClick: (TeacherReference) -> Unit = {},
    onRoomClick: (RoomReference) -> Unit = { }
)
{
    val mostLessonsInLessonSpotInEachDay = remember(timetable) {
        timetable.run {
            val result = mutableMapOf<DayOfWeek, Int>()

            for (day in days)
            {
                var dayResult = 0

                for (lessonSpot in getLessonSpotsForDay(day)!!)
                    if (lessonSpot.size > dayResult)
                        dayResult = lessonSpot.size

                result[day] = dayResult
            }

            result
        }
    }

    val dialogState = rememberObjectDialogState<Lesson>()

    Box(modifier = modifier) {
        val breakWidth = 5.dp
        Column(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(breakWidth)
        ) {
            Row {
                timetable.lessonPeriods.forEachIndexed { i, lessonPeriod ->
                    if (i == 0)
                        HorizontalSpacer(30.dp)

                    HorizontalSpacer(breakWidth)

                    TimetableLessonPeriod(
                        modifier = Modifier.size(width = 100.dp, height = 50.dp),
                        lessonPeriod = lessonPeriod,
                        hourIndex = i + 1
                    )
                }
            }

            timetable.daysSorted.forEach { day ->
                
                val rowModifier = if (mostLessonsInLessonSpotInEachDay[day]!! <= 2)
                    Modifier.height(100.dp)
                else
                    Modifier.height(IntrinsicSize.Min)
                Row(rowModifier) {
                    DayLabel(
                        getWeekDayName(day).substring(0, 2), Modifier
                            .width(30.dp)
                            .fillMaxHeight()
                    )
                    HorizontalSpacer(breakWidth)
                    timetable.getLessonSpotsForDay(day)!!.forEachIndexed { hourIndex, lessonSpot ->
                        LessonSpot(
                            lessonSpot = lessonSpot,
                            hourIndex = hourIndex + 1, // Hours are 1-indexed
                            lessonColors = lessonColors,
                            substitutions = substitutions,
                            onLessonClick = { dialogState.show(it) },
                            current = timetable.getLessonSpot(Clock.System.now()) === lessonSpot,
                            next = timetable.getNextLessonSpot(Clock.System.now(), takeEmpty = true) === lessonSpot,
                            hideClass = hideClass,
                            breakWidth = breakWidth
                        )
                        HorizontalSpacer(breakWidth)
                    }
                }
            }
        }
        ObjectDialog(
            state = dialogState,
            onDismissRequest = { dialogState.hide() },
            content = { lesson ->
                LessonDialogContent(
                    lesson = lesson,
                    onCloseClick = { dialogState.hide() },
                    onTeacherClick = { dialogState.hide(); onTeacherClick(it) },
                    onRoomClick = { dialogState.hide(); onRoomClick(it) }
                )
            }
        )
    }
}

@Composable
private fun TimetableLessonPeriod(
    lessonPeriod: LessonPeriod,
    hourIndex: Int,
    modifier: Modifier = Modifier
)
{
    Surface(
        modifier = modifier,
        shadowElevation = ElevationLevel.level1,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp).manipulate(1.5f),
        shape = RoundedCornerShape(5.dp),
    ) {
        Box(Modifier.padding(4.dp)) {
            Text(
                modifier = Modifier.align(Alignment.TopCenter),
                text = hourIndex.toString(),
                fontWeight = FontWeight.Bold
            )
            Text(
                modifier = Modifier.align(Alignment.BottomCenter),
                text = lessonPeriod.toString(),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun LessonSpot(
    lessonSpot: LessonSpot,
    hourIndex: Int,
    lessonColors: Map<Lesson, Color>?,
    substitutions: Map<String, SubstitutedLesson> = emptyMap(),
    onLessonClick: (Lesson) -> Unit = {},
    current: Boolean = false,
    next: Boolean = false,
    hideClass: Boolean = false,
    breakWidth: Dp = 0.dp
)
{
    val totalWidth = lessonSpot.periodSpan * 100.dp + breakWidth * (lessonSpot.periodSpan - 1)
    var lessonSpotModifier = Modifier.width(totalWidth)

    if (lessonSpot.size <= 2)
        lessonSpotModifier = lessonSpotModifier.fillMaxHeight()

    Column(lessonSpotModifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        lessonSpot.forEach { lesson ->
            /* If there is < 2 lessons, they are stretched to  */
            var lessonModifier = Modifier.fillMaxWidth()
            lessonModifier = if (lessonSpot.size <= 2)
                lessonModifier.weight(1f)
            else
                lessonModifier.height(50.dp)

            // Find substitution for this lesson
            val substitutedLesson = findSubstitutionForLesson(lesson, hourIndex, substitutions)
            val substitutionColor = substitutedLesson?.let { getSubstitutionColor(it) }
            
            // Extract originalText from SubstitutedLesson
            val originalText = substitutedLesson?.originalText
            
            Lesson(
                modifier = lessonModifier,
                onClick = { onLessonClick(lesson) },
                lesson = lesson,
                current = current,
                next = next,
                substitutionColor = substitutionColor,
                originalText = originalText,
                hideClass = hideClass
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Lesson(
    lesson: Lesson,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    current: Boolean = false,
    next: Boolean = false,
    substitutionColor: Color? = null,
    originalText: String? = null,
    hideClass: Boolean = false
) {
    val shape = RoundedCornerShape(5.dp)
    
    val borderModifier = when {
        next -> modifier.border(1.dp, MaterialTheme.colorScheme.inverseSurface, shape)
        substitutionColor != null -> modifier.border(2.dp, substitutionColor, shape)
        else -> modifier
    }

    Surface(
        modifier = borderModifier,
        tonalElevation = ElevationLevel.level2,
        shadowElevation = ElevationLevel.level1,
        shape = shape,
        color = if (current) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        Box(Modifier.padding(4.dp)) {
            if (lesson.subjectName.short != null)
                Text(
                    text = lesson.subjectName.short!!,
                    modifier = Modifier.align(Alignment.Center),
                    fontWeight = FontWeight.Bold
                )

            /* Bottom-Left corner:
            If it's a substitution, we show the 'originalText' in Red.
            If originalText is provided, use that; otherwise fall back to lesson.clazz
            If no substitution, show the standard class name if hideClass is false.
            */
            if (substitutionColor != null) {
                val textToShow = originalText ?: lesson.clazz
                if (textToShow != null) {
                    Text(
                        text = textToShow,
                        modifier = Modifier.align(Alignment.BottomStart),
                        color = Color.Red,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (!hideClass && lesson.clazz != null) {
                Text(
                    text = lesson.clazz!!,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }

            /* Teacher (Top-Left) */
            if (lesson.teacherName?.short != null)
                Text(
                    text = lesson.teacherName!!.short!!,
                    modifier = Modifier.align(Alignment.TopStart)
                )

            /* Classroom (Top-Right) */
            if (lesson.classroom != null)
                Text(
                    text = lesson.classroom!!,
                    modifier = Modifier.align(Alignment.TopEnd)
                )

            /* Group (Bottom-Right) */
            if (lesson.group != null)
                Text(
                    text = lesson.group!!,
                    modifier = Modifier.align(Alignment.BottomEnd),
                    fontSize = 10.sp
                )
        }
    }
}

@Composable
private fun DayLabel(
    day: String,
    modifier: Modifier = Modifier
)
{
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp).manipulate(1.5f),
        shadowElevation = ElevationLevel.level1,
        shape = RoundedCornerShape(5.dp)
    ) {
        Box(Modifier.padding(4.dp), contentAlignment = Alignment.Center) {
            Text(text = day, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LessonDialogContent(
    lesson: Lesson,
    onCloseClick: () -> Unit = {},
    onTeacherClick: (TeacherReference) -> Unit,
    onRoomClick: (RoomReference) -> Unit
)
{
    val roomLabel = stringResource(R.string.timetable_dialog_room)
    DialogContainer(
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    text = lesson.subjectName.full,
                    textAlign = TextAlign.Center,
                )
            }
        },
        buttons = {
            TextButton(onClick = onCloseClick) {
                Text(stringResource(R.string.close))
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val teacher = lesson.teacherName
            if (teacher != null && teacher.short == null)
                DialogRow(
                    label = stringResource(R.string.timetable_dialog_teacher),
                    value = teacher.full
                )
            else if (teacher?.short != null)
                DialogRow(
                    label = stringResource(R.string.timetable_dialog_teacher),
                    value = teacher.full,
                    onClick = { onTeacherClick(TeacherReference(teacher.full, teacher.short!!)) }
                )
            if (lesson.classroom != null)
                DialogRow(
                    label = stringResource(id = R.string.timetable_dialog_room),
                    value = lesson.classroom!!,
                    onClick = {
                        onRoomClick(
                            RoomReference(
                                name = roomLabel + " " + lesson.classroom!!,
                                roomCode = lesson.classroom!!
                            )
                        )
                    }
                )
            if (lesson.group != null)
                DialogRow(stringResource(R.string.timetable_dialog_group), lesson.group!!)
        }
    }
}
fun getSubstitutionColor(sub: SubstitutedLesson): Color
{
    return when {
        sub.isDropped -> dropped
        sub.isShifted -> shifted
        sub.isJoined -> joined
        sub.isSeparated -> separated
        sub.roomChanged -> room_change
        else -> substitution
    }
}

/**
 * Finds the substitution for a given lesson by matching key attributes.
 * 
 * Matches substitutions from DailySchedule.classSubs map based on:
 * - Hour/period number (map key is Int, the hour)
 * - Group (for split lessons)
 * - Subject or teacher (for additional validation)
 */
private fun findSubstitutionForLesson(
    lesson: Lesson,
    hourIndex: Int,
    substitutions: Map<String, SubstitutedLesson>
): SubstitutedLesson? {
    // The map from processSubstitutions uses "date_hour" as key
    // We need to find any substitution that matches this hour
    return substitutions.values.firstOrNull { sub ->
        // Match by hour (required)
        val hourMatches = sub.hour == hourIndex
        
        // Match by group if lesson is split
        val groupMatches = if (lesson.group != null) {
            // If lesson has a group, substitution must match that group
            lesson.group == sub.group
        } else {
            // If lesson has no group, accept substitutions with no group or any group
            true
        }
        
        // Match by subject as additional validation (if available)
        val subjectMatches = if (sub.subject != null && lesson.subjectName.short != null) {
            lesson.subjectName.short.equals(sub.subject, ignoreCase = true)
        } else {
            true // If no subject in substitution or lesson, don't filter by it
        }
        
        // Match by teacher (if available)
        val teacherMatches = if (sub.missingTeacher != null && lesson.teacherName?.short != null) {
            lesson.teacherName.short.equals(sub.missingTeacher, ignoreCase = true)
        } else {
            true // If no teacher info, don't filter by it
        }
        
        // A substitution matches if hour matches and group is compatible
        // Subject and teacher are used as additional filters when available
        hourMatches && groupMatches && (subjectMatches || teacherMatches)
    }
}
