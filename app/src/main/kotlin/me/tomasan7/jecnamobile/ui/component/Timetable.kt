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
import me.tomasan7.jecnamobile.ui.theme.separated
import me.tomasan7.jecnamobile.ui.theme.shifted
import me.tomasan7.jecnamobile.ui.theme.substitution
import me.tomasan7.jecnamobile.util.getWeekDayName
import me.tomasan7.jecnamobile.util.manipulate
import kotlin.time.Clock

@Composable
fun Timetable(
    timetable: Timetable,
    modifier: Modifier = Modifier,
    hideClass: Boolean = false,
    substitutions: List<SubstitutedLesson> = emptyList(),
    teacherNameMap: Map<String, String> = emptyMap(),
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

    val substitutionMap = remember(substitutions, timetable) {
        val map = mutableMapOf<Lesson, SubstitutedLesson>()
        substitutions.forEach { sub ->
            val lessonPeriodIndex = sub.hour - 1
            if (lessonPeriodIndex in timetable.lessonPeriods.indices) {
                timetable.daysSorted.forEach { day ->
                    val lessonSpots = timetable.getLessonSpotsForDay(day)!!
                    lessonSpots.forEach { lessonSpot ->
                        lessonSpot.forEach { lesson ->
                            if (lesson.subjectName.full == sub.subject && lessonSpot.periodSpan == lessonPeriodIndex) {
                                map[lesson] = sub
                            }
                        }
                    }
                }
            }
        }
        map
    }

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
                    timetable.getLessonSpotsForDay(day)!!.forEach { lessonSpot ->
                        LessonSpot(
                            lessonSpot = lessonSpot,
                            onLessonClick = { dialogState.show(it) },
                            current = timetable.getLessonSpot(Clock.System.now()) === lessonSpot,
                            next = timetable.getNextLessonSpot(Clock.System.now(), takeEmpty = true) === lessonSpot,
                            hideClass = hideClass,
                            breakWidth = breakWidth,
                            getSubstitutedLesson = { lesson -> substitutionMap[lesson] }
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
                    onRoomClick = { dialogState.hide(); onRoomClick(it) },
                    substitutedLesson = substitutionMap[lesson],
                    teacherNameMap = teacherNameMap
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
    onLessonClick: (Lesson) -> Unit = {},
    current: Boolean = false,
    next: Boolean = false,
    hideClass: Boolean = false,
    breakWidth: Dp = 0.dp,
    getSubstitutedLesson: (Lesson) -> SubstitutedLesson? = { null }
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

            Lesson(
                modifier = lessonModifier,
                onClick = { onLessonClick(lesson) },
                lesson = lesson,
                current = current,
                next = next,
                hideClass = hideClass,
                substitutedLesson = getSubstitutedLesson(lesson)
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
    hideClass: Boolean = false,
    substitutedLesson: SubstitutedLesson? = null
)
{
    val shape = RoundedCornerShape(5.dp)
    val baseColor = if (current) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surface
    val substitutionColor = when {
        substitutedLesson?.isDropped == true -> dropped
        substitutedLesson?.isJoined == true -> joined
        substitutedLesson?.isSeparated == true -> separated
        substitutedLesson?.roomChanged == true -> room_change
        substitutedLesson?.isShifted == true -> shifted
        substitutedLesson != null -> substitution
        else -> null
    }
    val backgroundColor = substitutionColor?.copy(alpha = 0.3f) ?: baseColor
    Surface(
        modifier = if (next) modifier.border(1.dp, MaterialTheme.colorScheme.inverseSurface, shape) else modifier,
        tonalElevation = ElevationLevel.level2,
        shadowElevation = ElevationLevel.level1,
        shape = shape,
        color = backgroundColor,
        onClick = onClick
    ) {
        Box(Modifier.padding(4.dp)) {
            if (lesson.subjectName.short != null)
                Text(lesson.subjectName.short!!, Modifier.align(Alignment.Center), fontWeight = FontWeight.Bold)
            if (!hideClass && lesson.clazz != null)
                Text(lesson.clazz!!, Modifier.align(Alignment.BottomStart))
            if (lesson.teacherName?.short != null)
                Text(lesson.teacherName!!.short!!, Modifier.align(Alignment.TopStart))
            if (lesson.classroom != null)
                Text(lesson.classroom!!, Modifier.align(Alignment.TopEnd))
            if (lesson.group != null)
                Text(lesson.group!!, Modifier.align(Alignment.BottomEnd), fontSize = 10.sp)
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
    onRoomClick: (RoomReference) -> Unit,
    substitutedLesson: SubstitutedLesson? = null,
    teacherNameMap: Map<String, String> = emptyMap()
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
            if (substitutedLesson != null) {
                val substitutionTeacher = substitutedLesson.substitutingTeacher
                val substitutionRoom = substitutedLesson.room
                val substitutionGroup = substitutedLesson.group

                if (substitutionTeacher != null) {
                    val fullName = teacherNameMap[substitutionTeacher] ?: substitutionTeacher
                    DialogRow(
                        label = stringResource(R.string.timetable_dialog_sub_teacher),
                        value = fullName,
                        onClick = { onTeacherClick(TeacherReference(fullName, substitutionTeacher)) }
                    )
                }
                if (substitutionRoom != null) {
                    DialogRow(
                        label = stringResource(R.string.timetable_dialog_sub_room),
                        value = substitutionRoom,
                        onClick = {
                            onRoomClick(
                                RoomReference(
                                    name = roomLabel + " " + substitutionRoom,
                                    roomCode = substitutionRoom
                                )
                            )
                        }
                    )
                }
                if (substitutionGroup != null) {
                    DialogRow(
                        label = stringResource(R.string.timetable_dialog_sub_group),
                        value = substitutionGroup
                    )
                }
            }
        }
    }
}
