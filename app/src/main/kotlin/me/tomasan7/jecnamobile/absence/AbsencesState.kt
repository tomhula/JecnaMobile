package me.tomasan7.jecnamobile.absence

import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import io.github.tomhula.jecnaapi.data.absence.AbsencesPage
import io.github.tomhula.jecnaapi.util.SchoolYear
import kotlin.time.Instant

/**
 * State for the Absences screen.
 */
@Immutable
data class AbsencesState (
    val loading: Boolean = false,
    val absencesPage: AbsencesPage? = null,
    val lastUpdateTimestamp: Instant? = null,
    val isCache: Boolean = false,
    val selectedSchoolYear: SchoolYear = absencesPage?.selectedSchoolYear ?: SchoolYear.current(),
    val snackBarMessageEvent: StateEventWithContent<String> = consumed(),
)
{
    val daysSorted = absencesPage?.days?.sortedDescending() ?: emptyList()
    val summary = absencesPage?.let { page ->
        var totalHoursAbsent = 0
        var totalUnexcusedHours = 0
        var totalLateEntries = 0

        page.days.forEach { day ->
            page[day]?.let { info ->
                totalHoursAbsent += info.hoursAbsent
                totalUnexcusedHours += info.unexcusedHours
                totalLateEntries += info.lateEntryCount
            }
        }

        AbsencesSummary(
            totalHoursAbsent = totalHoursAbsent,
            totalUnexcusedHours = totalUnexcusedHours,
            totalLateEntries = totalLateEntries
        )
    }
}


@Immutable
data class AbsencesSummary(
    val totalHoursAbsent: Int,
    val totalUnexcusedHours: Int,
    val totalLateEntries: Int
) {
    val totalExcusedHours: Int 
        get() = totalHoursAbsent - totalUnexcusedHours
}
