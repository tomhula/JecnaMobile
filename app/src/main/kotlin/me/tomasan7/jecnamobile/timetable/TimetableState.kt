package me.tomasan7.jecnamobile.timetable

import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import io.github.tomhula.jecnaapi.data.timetable.TimetablePage
import io.github.tomhula.jecnaapi.util.SchoolYear
import kotlin.time.Instant

@Immutable
data class TimetableState(
    val loading: Boolean = false,
    val timetablePage: TimetablePage? = null,
    val substitutions: SubstitutionData? = null,
    val lastUpdateTimestamp: Instant? = null,
    val isCache: Boolean = false,
    val selectedSchoolYear: SchoolYear = timetablePage?.selectedSchoolYear ?: SchoolYear.current(),
    val selectedPeriod: TimetablePage.PeriodOption? = timetablePage?.periodOptions?.find { it.selected },
    val snackBarMessageEvent: StateEventWithContent<String> = consumed()
)
