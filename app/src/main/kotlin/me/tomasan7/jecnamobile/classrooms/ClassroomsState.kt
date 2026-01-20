package me.tomasan7.jecnamobile.classrooms

import android.icu.text.Collator
import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import io.github.tomhula.jecnaapi.data.classroom.ClassroomPage
import me.tomasan7.jecnamobile.util.removeAccent
import java.util.Locale

@Immutable
data class ClassroomsState(
    val loading: Boolean = false,
    val classroomsPage: ClassroomPage? = null,
    val filterFieldValue: String = "",
    val snackBarMessageEvent: StateEventWithContent<String> = consumed()
)
{
    val classroomReferencesSorted = classroomsPage?.classroomRefs
        ?.sortedWith(compareBy(Collator.getInstance(Locale("cs"))) { it.name })

    val classroomReferencesSortedFiltered = classroomReferencesSorted
        ?.filter { it.name.removeAccent().contains(filterFieldValue.removeAccent(), ignoreCase = true) || it.roomCode.contains(filterFieldValue, ignoreCase = true) }
}

