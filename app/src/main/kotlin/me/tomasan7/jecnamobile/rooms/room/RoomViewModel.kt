package me.tomasan7.jecnamobile.rooms.room

import android.content.Context
import android.content.IntentFilter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.WebJecnaClient
import io.github.tomhula.jecnaapi.data.room.RoomReference
import io.github.tomhula.jecnaapi.data.room.RoomsPage
import io.github.tomhula.jecnaapi.parser.ParseException
import io.ktor.util.network.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.tomasan7.jecnamobile.JecnaMobileApplication
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.SubScreenViewModel
import me.tomasan7.jecnamobile.rooms.RoomsRepository
import me.tomasan7.jecnamobile.util.createBroadcastReceiver
import javax.inject.Inject

@HiltViewModel
class RoomViewModel @Inject constructor(
    @ApplicationContext
    private val appContext: Context,
    jecnaClient: JecnaClient,
    private val repository: RoomsRepository
) : SubScreenViewModel<RoomsPage>(appContext)
{
    var uiState by mutableStateOf(RoomState())
        private set

    private var loadClassroomJob: Job? = null
    private var RoomReference: RoomReference? = null

    private val loginBroadcastReceiver = createBroadcastReceiver { _, intent ->
        val first = intent.getBooleanExtra(JecnaMobileApplication.SUCCESSFUL_LOGIN_FIRST_EXTRA, false)

        if (loadClassroomJob == null || loadClassroomJob!!.isCompleted)
        {
            if (!first)
                changeUiState(snackBarMessageEvent = triggered(appContext.getString(R.string.back_online)))
            loadReal()
        }
    }

    init
    {
        if ((jecnaClient as WebJecnaClient).lastSuccessfulLoginTime != null)
            loadReal()
    }

    fun enteredComposition(RoomReference: RoomReference)
    {
        this.RoomReference = RoomReference
        loadReal()

        appContext.registerReceiver(
            loginBroadcastReceiver,
            IntentFilter(JecnaMobileApplication.SUCCESSFUL_LOGIN_ACTION),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    fun leftComposition()
    {
        loadClassroomJob?.cancel()
        appContext.unregisterReceiver(loginBroadcastReceiver)
    }

    private fun loadReal()
    {
        if (RoomReference == null) return

        loadClassroomJob?.cancel()

        changeUiState(loading = true)

        loadClassroomJob = viewModelScope.launch {
            try
            {
                changeUiState(room = repository.getRoom(RoomReference!!))
            }
            catch (e: UnresolvedAddressException)
            {
                changeUiState(snackBarMessageEvent = triggered(getOfflineMessage()))
            }
            catch (e: ParseException)
            {
                changeUiState(
                    snackBarMessageEvent = triggered(appContext.getString(R.string.error_unsupported_room))
                )
            }
            catch (e: CancellationException)
            {
                throw e
            }
            catch (e: Exception)
            {
                changeUiState(snackBarMessageEvent = triggered(appContext.getString(R.string.room_load_error)))
                e.printStackTrace()
            }
            finally
            {
                changeUiState(loading = false)
            }
        }
    }

    private fun getOfflineMessage() = appContext.getString(R.string.no_internet_connection)

    fun reload() = if (!uiState.loading) loadReal() else Unit

    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())

    private fun changeUiState(
        loading: Boolean = uiState.loading,
        room: io.github.tomhula.jecnaapi.data.room.Room? = uiState.room,
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

