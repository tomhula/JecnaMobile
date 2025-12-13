package me.tomasan7.jecnamobile.grades

import android.icu.text.Collator
import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import io.github.tomhula.jecnaapi.data.grade.GradesPage
import io.github.tomhula.jecnaapi.data.grade.Subject
import io.github.tomhula.jecnaapi.data.notification.Notification
import io.github.tomhula.jecnaapi.data.notification.NotificationReference
import io.github.tomhula.jecnaapi.util.SchoolYear
import io.github.tomhula.jecnaapi.util.SchoolYearHalf
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
    val snackBarMessageEvent: StateEventWithContent<String> = consumed(),
    val predictedGrades: Map<Subject, List<PredictedGrade>> = emptyMap(),
    val showPredictions: Boolean = false,
    /** `null` if no notification is loading, or the [NotificationReference] that was clicked if it is loading. */
    val loadingNotification: NotificationReference? = null,
    val dialogNotification: Notification? = null,
)
{
    /**
     * [gradesPage]'s `subjectNames` sorted according to Czech alphabet. Is `null` when [gradesPage] is `null`.
     */
    val subjectsSorted =
        gradesPage?.subjects?.sortedWith(compareBy(Collator.getInstance(Locale("cs"))) { it.name.full })
}
