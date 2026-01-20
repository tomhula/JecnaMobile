package me.tomasan7.jecnamobile.classrooms.classroom

import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import io.github.tomhula.jecnaapi.data.classroom.Room

@Immutable
data class ClassroomState(
    val loading: Boolean = false,
    val room: Room? = null,
    val snackBarMessageEvent: StateEventWithContent<String> = consumed()
)

