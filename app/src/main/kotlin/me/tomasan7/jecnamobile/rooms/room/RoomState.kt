package me.tomasan7.jecnamobile.rooms.room

import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import io.github.tomhula.jecnaapi.data.room.Room

@Immutable
data class RoomState(
    val loading: Boolean = false,
    val room: Room? = null,
    val snackBarMessageEvent: StateEventWithContent<String> = consumed()
)

