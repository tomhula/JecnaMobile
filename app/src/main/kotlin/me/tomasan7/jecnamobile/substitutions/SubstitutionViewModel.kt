package me.tomasan7.jecnamobile.substitutions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.launch
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.timetable.TimetableRepository
import javax.inject.Inject
import kotlin.time.Clock

@HiltViewModel
class SubstitutionViewModel @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val repository: TimetableRepository
) : ViewModel() {

    var uiState by mutableStateOf(SubstitutionState())
        private set

    init {
        load()
    }

    fun load() {
        uiState = uiState.copy(loading = true)
        viewModelScope.launch {
            val result = repository.getAllSubstitutions()
            uiState = if (result != null) {
                uiState.copy(
                    loading = false,
                    data = result,
                    lastUpdateTimestamp = Clock.System.now(),
                    selectedDate = uiState.selectedDate ?: result.schedule.keys.minOrNull()
                )
            } else {
                uiState.copy(
                    loading = false,
                    snackBarMessageEvent = triggered(context.getString(R.string.substitution_all_load_error))
                )
            }
        }
    }

    fun selectDate(date: String) {
        uiState = uiState.copy(selectedDate = date)
    }

    fun onSnackBarMessageEventConsumed() {
        uiState = uiState.copy(snackBarMessageEvent = consumed())
    }
}
