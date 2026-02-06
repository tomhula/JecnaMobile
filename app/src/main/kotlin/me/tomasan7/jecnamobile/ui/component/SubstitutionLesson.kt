package me.tomasan7.jecnamobile.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.tomhula.jecnaapi.data.room.RoomReference
import io.github.tomhula.jecnaapi.data.schoolStaff.TeacherReference
import io.github.tomhula.jecnaapi.data.timetable.LessonSpot
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.ui.ElevationLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubstitutionLesson(
    text: String,
    modifier: Modifier = Modifier,
    onTeacherClick: (TeacherReference) -> Unit,
    onRoomClick: (RoomReference) -> Unit,
    lessonSpot: LessonSpot
)
{
    val dialogState = rememberObjectDialogState<String>()
    val shape = RoundedCornerShape(5.dp)

    Surface(
        modifier = modifier,
        tonalElevation = ElevationLevel.level2,
        shadowElevation = ElevationLevel.level1,
        shape = shape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        onClick = { dialogState.show(text) }
    ) {
        Box(Modifier.padding(4.dp), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 4
            )
        }
    }

    ObjectDialog(
        state = dialogState,
        onDismissRequest = { dialogState.hide() },
        content = { fullText ->
            DialogContainer(
                title = {
                    Box(Modifier.padding(8.dp)) {
                        Text("Suplování", fontWeight = FontWeight.Bold)
                    }
                },
                buttons = {
                    TextButton(onClick = { dialogState.hide() }) {
                        Text("Zavřít")
                    }
                }
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    DialogRow(
                        label = "Text mimořádné změny",
                        value = fullText,
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val roomLabel = stringResource(R.string.timetable_dialog_room)
                    lessonSpot.forEach { lesson ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                Text(
                                    text = lesson.subjectName.full,
                                    textAlign = TextAlign.Center,
                                )
                            }
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
            }
        }
    )
}
