package me.tomasan7.jecnamobile.grades

import android.icu.text.Collator
import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import me.tomasan7.jecnaapi.data.grade.GradesPage
import me.tomasan7.jecnaapi.util.SchoolYear
import me.tomasan7.jecnaapi.util.SchoolYearHalf
import java.time.Instant
import java.util.Locale

@Immutable
data class GradesState(
    val loading: Boolean = false,
    val gradesPage: GradesPage? = null,
    val lastUpdateTimestamp: Instant? = null,
    val isCache: Boolean = false,
    val selectedSchoolYear: SchoolYear = gradesPage?.selectedSchoolYear ?: SchoolYear.current(),
    val selectedSchoolYearHalf: SchoolYearHalf = gradesPage?.selectedSchoolYearHalf ?: SchoolYearHalf.current(),
    val snackBarMessageEvent: StateEventWithContent<String> = consumed()
)
{
    /**
     * [gradesPage]'s `subjectNames` sorted according to Czech alphabet. Is `null` when [gradesPage] is `null`.
     */
    val subjectsSorted =
        gradesPage?.subjects?.sortedWith(compareBy(Collator.getInstance(Locale("cs"))) { it.name.full })
}
