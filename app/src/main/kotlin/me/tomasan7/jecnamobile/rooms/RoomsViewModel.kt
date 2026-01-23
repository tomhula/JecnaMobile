package me.tomasan7.jecnamobile.rooms

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.github.tomhula.jecnaapi.data.room.RoomsPage
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.SubScreenViewModel
import javax.inject.Inject

@HiltViewModel
class RoomsViewModel @Inject constructor(
    @ApplicationContext
    appContext: Context,
    private val repository: RoomsRepository
) : SubScreenViewModel<RoomsPage>(appContext)
{
    var uiState by mutableStateOf(RoomsState())
        private set

    fun onFilterFieldValueChange(value: String) = changeUiState(filterFieldValue = value)

    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())

    private fun changeUiState(
        loading: Boolean = uiState.loading,
        roomsPage: RoomsPage? = uiState.roomsPage,
        filterFieldValue: String = uiState.filterFieldValue,
        snackBarMessageEvent: StateEventWithContent<String> = uiState.snackBarMessageEvent,
    )
    {
        uiState = uiState.copy(
            loading = loading,
            roomsPage = roomsPage,
            filterFieldValue = filterFieldValue,
            snackBarMessageEvent = snackBarMessageEvent
        )
    }

    override val parseErrorMessage = appContext.getString(R.string.error_unsupported_rooms)
    override val loadErrorMessage = appContext.getString(R.string.rooms_load_error)

    override fun showSnackBarMessage(message: String) = changeUiState(snackBarMessageEvent = triggered(message))
    override fun setLoadingUiState(loading: Boolean) = changeUiState(loading = loading)
    override fun setDataUiState(data: RoomsPage) = changeUiState(roomsPage = data)
    override suspend fun fetchRealData(): RoomsPage = repository.getRoomsPage()
}
