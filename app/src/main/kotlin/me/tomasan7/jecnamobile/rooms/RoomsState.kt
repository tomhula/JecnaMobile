package me.tomasan7.jecnamobile.rooms

import android.icu.text.Collator
import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import io.github.tomhula.jecnaapi.data.room.RoomsPage
import me.tomasan7.jecnamobile.util.removeAccent
import java.util.Locale

@Immutable
data class RoomsState(
    val loading: Boolean = false,
    val classroomsPage: RoomsPage? = null,
    val filterFieldValue: String = "",
    val snackBarMessageEvent: StateEventWithContent<String> = consumed()
)
{
    val RoomReferencesSorted = classroomsPage?.roomReferences
        ?.sortedWith(compareBy(Collator.getInstance(Locale("cs"))) { it.name })

    val RoomReferencesSortedFiltered = RoomReferencesSorted
        ?.filter { it.name.removeAccent().contains(filterFieldValue.removeAccent(), ignoreCase = true) || it.roomCode.contains(filterFieldValue, ignoreCase = true) }
}

