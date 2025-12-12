package me.tomasan7.jecnamobile.absence

import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import io.github.tomhula.jecnaapi.data.absence.AbsencesPage
import io.github.tomhula.jecnaapi.util.SchoolYear
import java.time.Instant

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
    val snackBarMessageEvent: StateEventWithContent<String> = consumed()
)


