package me.tomasan7.jecnamobile.widgets.shared

import io.github.tomhula.jecnaapi.data.timetable.TimetablePage
import kotlinx.serialization.Serializable
import me.tomasan7.jecnamobile.timetable.SubstitutionData

@Serializable
data class SharedTimetableWidgetState(
    val timetablePage: TimetablePage? = null,
    val substitutions: SubstitutionData? = null,
    val lastUpdated: Long = 0L,
    val isLoading: Boolean = false,
    val isManualRefresh: Boolean = false,
    val error: String? = null
)
