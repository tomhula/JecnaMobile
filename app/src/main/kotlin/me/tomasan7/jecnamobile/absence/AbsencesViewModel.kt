package me.tomasan7.jecnamobile.absence

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
import io.github.tomhula.jecnaapi.data.absence.AbsencesPage
import io.github.tomhula.jecnaapi.util.SchoolYear
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import me.tomasan7.jecnamobile.JecnaMobileApplication
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.util.createBroadcastReceiver
import me.tomasan7.jecnamobile.util.now
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant


@HiltViewModel
class AbsencesViewModel @Inject constructor(
    @ApplicationContext
    private val appContext: Context,
    jecnaClient: JecnaClient,
    private val repository: CacheAbsencesRepository
) : ViewModel() {
    var uiState by mutableStateOf(AbsencesState())
        private set

    private var loadAbsencesJob: Job? = null

    private val loginBroadcastReceiver = createBroadcastReceiver { _, intent ->
        val first =
            intent.getBooleanExtra(JecnaMobileApplication.SUCCESSFUL_LOGIN_FIRST_EXTRA, false)
        if (loadAbsencesJob == null || loadAbsencesJob!!.isCompleted) {
            if (!first)
                changeUiState(snackBarMessageEvent = triggered(appContext.getString(R.string.back_online)))
            loadReal()
        }
    }
    private fun changeUiState(
        loading: Boolean = uiState.loading,
        absencesPage: AbsencesPage? = uiState.absencesPage,
        lastUpdateTimestamp: Instant? = uiState.lastUpdateTimestamp,
        isCache: Boolean = uiState.isCache,
        selectedSchoolYear: SchoolYear = uiState.selectedSchoolYear,
        snackBarMessageEvent: StateEventWithContent<String> = uiState.snackBarMessageEvent
    )
    {
        uiState = uiState.copy(
            loading = loading,
            absencesPage = absencesPage,
            lastUpdateTimestamp = lastUpdateTimestamp,
            isCache = isCache,
            selectedSchoolYear = selectedSchoolYear,
            snackBarMessageEvent = snackBarMessageEvent
        )
    }


    private fun loadReal()
    {
        loadAbsencesJob?.cancel()

        changeUiState(loading = true)

        loadAbsencesJob = viewModelScope.launch {
            try
            {
                val realAbsences = if (isSelectedPeriodCurrent())
                    repository.getRealAbsences()
                else
                    repository.getRealAbsences(uiState.selectedSchoolYear)
                
                changeUiState(
                    absencesPage = realAbsences,
                    lastUpdateTimestamp = Clock.System.now(),
                    isCache = false
                )
            }
            catch (e: UnresolvedAddressException)
            {
                if (uiState.lastUpdateTimestamp != null && uiState.isCache)
                    changeUiState(snackBarMessageEvent = triggered(getOfflineMessage()!!))
                else
                    changeUiState(snackBarMessageEvent =
                    triggered(appContext.getString(R.string.no_internet_connection)))
            }
            catch (e: CancellationException)
            {
                throw e
            }
            catch (e: Exception)
            {
                changeUiState(snackBarMessageEvent = triggered(appContext.getString(R.string.absences_load_error)))
                e.printStackTrace()
            }
            finally
            {
                changeUiState(loading = false)
            }
        }
    }

    init
    {
        loadCache()
        if (jecnaClient.lastSuccessfulLoginTime != null)
            loadReal()
    }

    private fun loadCache()
    {
        if (!repository.isCacheAvailable())
            return

        viewModelScope.launch {
            val cachedAbsences = repository.getCachedAbsences() ?: return@launch

            changeUiState(
                absencesPage = cachedAbsences.data,
                lastUpdateTimestamp = cachedAbsences.timestamp,
                isCache = true
            )
        }
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
        loadAbsencesJob?.cancel()
        appContext.unregisterReceiver(loginBroadcastReceiver)
    }

    fun selectSchoolYear(schoolYear: SchoolYear)
    {
        changeUiState(selectedSchoolYear = schoolYear)
        loadReal()
    }

    private fun getOfflineMessage(): String?
    {
        val cacheTimestamp = uiState.lastUpdateTimestamp ?: return null
        val localDateTime = cacheTimestamp.toLocalDateTime(TimeZone.currentSystemDefault())
        val localDate = localDateTime.date

        val today = LocalDate.now()
        
        return if (localDate == today)
        {
            val timeStr = localDateTime.time.format(OFFLINE_MESSAGE_TIME_FORMATTER)
            appContext.getString(R.string.showing_offline_data_time, timeStr)
        }
        else
        {
            val dateStr = localDate.format(OFFLINE_MESSAGE_DATE_FORMATTER)
            appContext.getString(R.string.showing_offline_data_date, dateStr)
        }
    }

    fun reload() = if (!uiState.loading) loadReal() else Unit
    private fun isSelectedPeriodCurrent() =
        uiState.selectedSchoolYear == SchoolYear.current()

    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())
    companion object
    {
        val OFFLINE_MESSAGE_TIME_FORMATTER = LocalTime.Format { 
            hour(padding = Padding.ZERO)
            char(':')
            minute(padding = Padding.ZERO)
        } 
        val OFFLINE_MESSAGE_DATE_FORMATTER = LocalDate.Format { 
            day(padding = Padding.NONE)
            chars(". ")
            monthNumber(padding = Padding.NONE)
        }
    }
}
