package me.tomasan7.jecnamobile.rooms

import android.content.Context
import android.content.IntentFilter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.WebJecnaClient
import io.github.tomhula.jecnaapi.data.room.RoomsPage
import io.github.tomhula.jecnaapi.parser.ParseException
import io.ktor.util.network.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.tomasan7.jecnamobile.JecnaMobileApplication
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.util.createBroadcastReceiver
import javax.inject.Inject

@HiltViewModel
class RoomsViewModel @Inject constructor(
    @ApplicationContext
    private val appContext: Context,
    jecnaClient: JecnaClient,
    private val repository: RoomsRepository
) : ViewModel()
{
    var uiState by mutableStateOf(RoomsState())
        private set

    private var loadClassroomsJob: Job? = null

    private val loginBroadcastReceiver = createBroadcastReceiver { _, intent ->
        val first = intent.getBooleanExtra(JecnaMobileApplication.SUCCESSFUL_LOGIN_FIRST_EXTRA, false)

        if (loadClassroomsJob == null || loadClassroomsJob!!.isCompleted)
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

    fun enteredComposition()
    {
        appContext.registerReceiver(
            loginBroadcastReceiver,
            IntentFilter(JecnaMobileApplication.SUCCESSFUL_LOGIN_ACTION),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    fun leftComposition()
    {
        loadClassroomsJob?.cancel()
        appContext.unregisterReceiver(loginBroadcastReceiver)
    }

    fun onFilterFieldValueChange(value: String) = changeUiState(filterFieldValue = value)

    private fun loadReal()
    {
        loadClassroomsJob?.cancel()

        changeUiState(loading = true)

        loadClassroomsJob = viewModelScope.launch {
            try
            {
                changeUiState(classroomsPage = repository.getRoomsPage())
            }
            catch (e: UnresolvedAddressException)
            {
                changeUiState(snackBarMessageEvent = triggered(getOfflineMessage()))
            }
            catch (e: ParseException)
            {
                changeUiState(
                    snackBarMessageEvent = triggered(appContext.getString(R.string.error_unsupported_classrooms))
                )
            }
            catch (e: CancellationException)
            {
                throw e
            }
            catch (e: Exception)
            {
                changeUiState(snackBarMessageEvent = triggered(appContext.getString(R.string.classrooms_load_error)))
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
        classroomsPage: RoomsPage? = uiState.roomsPage,
        filterFieldValue: String = uiState.filterFieldValue,
        snackBarMessageEvent: StateEventWithContent<String> = uiState.snackBarMessageEvent,
    )
    {
        uiState = uiState.copy(
            loading = loading,
            roomsPage = classroomsPage,
            filterFieldValue = filterFieldValue,
            snackBarMessageEvent = snackBarMessageEvent
        )
    }
}

