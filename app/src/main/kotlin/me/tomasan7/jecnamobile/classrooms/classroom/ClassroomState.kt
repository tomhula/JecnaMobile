package me.tomasan7.jecnamobile.classrooms.classroom

import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import io.github.tomhula.jecnaapi.data.classroom.Classroom

@Immutable
data class ClassroomState(
    val loading: Boolean = false,
    val classroom: Classroom? = null,
    val snackBarMessageEvent: StateEventWithContent<String> = consumed()
)

