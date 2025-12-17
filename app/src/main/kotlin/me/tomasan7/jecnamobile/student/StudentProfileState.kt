package me.tomasan7.jecnamobile.student

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import io.github.tomhula.jecnaapi.data.student.Student


data class StudentProfileState(
    val loading: Boolean = false,
    val student: Student? = null,
    val snackBarMessageEvent: StateEventWithContent<String> = consumed()
)

