package me.tomasan7.jecnamobile.substitutions

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.SubScreenViewModel
import me.tomasan7.jecnamobile.timetable.SubstitutionAllData
import me.tomasan7.jecnamobile.timetable.TimetableRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlin.time.Clock

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import cz.jzitnik.jecna_supl_client.ReportLocation

@HiltViewModel
class SubstitutionViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: TimetableRepository
) : SubScreenViewModel<SubstitutionAllData?>(context)
{
    override val parseErrorMessage = context.getString(R.string.substitution_all_load_error)
    override val loadErrorMessage = context.getString(R.string.substitution_all_load_error)

    var uiState by mutableStateOf(SubstitutionState())
        private set

    override suspend fun fetchRealData() = repository.getAllSubstitutions()

    override fun setDataUiState(data: SubstitutionAllData?)
    {
        if (data != null)
        {
            uiState = uiState.copy(
                data = data,
                lastUpdateTimestamp = Clock.System.now(),
                selectedDate = uiState.selectedDate
                    ?: data.schedule.keys.minOrNull()?.let {
                        SubstitutionDay(it, LocalDate.parse(it), data.schedule[it]?.info?.inWork ?: false)
                    }
            )
        }
        else
        {
            showSnackBarMessage(appContext.getString(R.string.substitution_all_load_error))
        }
    }

    override fun setLoadingUiState(loading: Boolean)
    {
        uiState = uiState.copy(loading = loading)
    }

    override fun showSnackBarMessage(message: String)
    {
        uiState = uiState.copy(snackBarMessageEvent = triggered(message))
    }

    fun selectDate(date: SubstitutionDay)
    {
        uiState = uiState.copy(selectedDate = date)
    }

    fun onSnackBarMessageEventConsumed()
    {
        uiState = uiState.copy(snackBarMessageEvent = consumed())
    }

    fun reportError(content: String, location: ReportLocation, onFinished: () -> Unit)
    {
        viewModelScope.launch {
            repository.reportSubstitutionError(content, location)
                .onSuccess {
                    showSnackBarMessage(appContext.getString(R.string.report_success))
                }
                .onFailure {
                    showSnackBarMessage(appContext.getString(R.string.report_error))
                }
            onFinished()
        }
    }
}
