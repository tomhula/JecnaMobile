package me.tomasan7.jecnamobile.substitutions

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import me.tomasan7.jecnamobile.timetable.SubstitutionAllData
import java.time.LocalDate
import kotlin.time.Instant

data class SubstitutionState(
    val loading: Boolean = false,
    val data: SubstitutionAllData? = null,
    val lastUpdateTimestamp: Instant? = null,
    val selectedDate: SubstitutionDay? = null,
    val snackBarMessageEvent: StateEventWithContent<String> = consumed()
)

data class SubstitutionDay(
    val key: String,
    val date: LocalDate,
    val inWork: Boolean
)
