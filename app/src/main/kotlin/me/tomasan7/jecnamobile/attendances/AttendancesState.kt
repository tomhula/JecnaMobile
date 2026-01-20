package me.tomasan7.jecnamobile.attendances

import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import io.github.tomhula.jecnaapi.data.attendance.AttendancesPage
import io.github.tomhula.jecnaapi.util.SchoolYear
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import me.tomasan7.jecnamobile.util.now
import kotlin.time.Instant

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
