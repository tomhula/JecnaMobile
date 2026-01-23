package me.tomasan7.jecnamobile.rooms.room

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.github.tomhula.jecnaapi.data.room.Room
import io.github.tomhula.jecnaapi.data.room.RoomReference
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.SubScreenViewModel
import me.tomasan7.jecnamobile.rooms.RoomsRepository
import javax.inject.Inject

@HiltViewModel
class RoomViewModel @Inject constructor(
    @ApplicationContext
    appContext: Context,
    private val repository: RoomsRepository
) : SubScreenViewModel<Room>(appContext)
{
    override val parseErrorMessage = appContext.getString(R.string.error_unsupported_room)
    override val loadErrorMessage = appContext.getString(R.string.room_load_error)

    private lateinit var roomReference: RoomReference

    var uiState by mutableStateOf(RoomState())
        private set

    /** Must be called before [enteredComposition]. */
    fun setRoomReference(roomReference: RoomReference)
    {
        this.roomReference = roomReference
    }

    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())

    override fun showSnackBarMessage(message: String) = changeUiState(snackBarMessageEvent = triggered(message))
    override fun setLoadingUiState(loading: Boolean) = changeUiState(loading = loading)
    override fun setDataUiState(data: Room) = changeUiState(room = data)
    override suspend fun fetchRealData(): Room = repository.getRoom(roomReference)

    private fun changeUiState(
        loading: Boolean = uiState.loading,
        room: Room? = uiState.room,
        snackBarMessageEvent: StateEventWithContent<String> = uiState.snackBarMessageEvent,
    )
    {
        uiState = uiState.copy(
            loading = loading,
            room = room,
            snackBarMessageEvent = snackBarMessageEvent
        )
    }
}
