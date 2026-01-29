package me.tomasan7.jecnamobile.timetable

import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import io.github.stevekk11.dtos.DailySchedule
import io.github.stevekk11.dtos.SubstitutionStatus
import io.github.tomhula.jecnaapi.data.timetable.TimetablePage
import io.github.tomhula.jecnaapi.util.SchoolYear
import kotlin.time.Instant

@Immutable
data class TimetableState(
    val loading: Boolean = false,
    val timetablePage: TimetablePage? = null,
    val lastUpdateTimestamp: Instant? = null,
    val isCache: Boolean = false,
    val selectedSchoolYear: SchoolYear = timetablePage?.selectedSchoolYear ?: SchoolYear.current(),
    val selectedPeriod: TimetablePage.PeriodOption? = timetablePage?.periodOptions?.find { it.selected },
    val substitutions: List<DailySchedule> = emptyList(),
    val substitutionStatus: SubstitutionStatus? = null,
    val teacherNameMap: Map<String, String> = emptyMap(),
    val snackBarMessageEvent: StateEventWithContent<String> = consumed()
)
