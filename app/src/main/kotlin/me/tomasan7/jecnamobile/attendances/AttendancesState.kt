package me.tomasan7.jecnamobile.attendances

import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import me.tomasan7.jecnaapi.data.attendance.AttendancesPage
import me.tomasan7.jecnaapi.util.SchoolYear
import java.time.Instant
import java.time.LocalDate
import java.time.Month

@Immutable
data class AttendancesState(
    val loading: Boolean = false,
    val attendancesPage: AttendancesPage? = null,
    val lastUpdateTimestamp: Instant? = null,
    val isCache: Boolean = false,
    val selectedSchoolYear: SchoolYear = attendancesPage?.selectedSchoolYear ?: SchoolYear.current(),
    val selectedMonth: Month = attendancesPage?.selectedMonth ?: LocalDate.now().month,
    val snackBarMessageEvent: StateEventWithContent<String> = consumed()
)
{
    val daysSorted = attendancesPage?.days?.sortedDescending()
}
