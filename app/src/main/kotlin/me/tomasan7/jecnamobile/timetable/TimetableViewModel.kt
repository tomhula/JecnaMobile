package me.tomasan7.jecnamobile.timetable

import android.content.Context
import android.content.IntentFilter
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.data.timetable.TimetablePage
import io.github.tomhula.jecnaapi.util.SchoolYear
import me.tomasan7.jecnamobile.JecnaMobileApplication
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.util.createBroadcastReceiver
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class TimetableViewModel @Inject constructor(
    @ApplicationContext
    private val appContext: Context,
    jecnaClient: JecnaClient,
    private val repository: CacheTimetableRepository
) : ViewModel()
{
    var uiState by mutableStateOf(TimetableState())
        private set

    private var loadTimetableJob: Job? = null

    private val loginBroadcastReceiver = createBroadcastReceiver { _, intent ->
        val first = intent.getBooleanExtra(JecnaMobileApplication.SUCCESSFUL_LOGIN_FIRST_EXTRA, false)

        if (loadTimetableJob == null || loadTimetableJob!!.isCompleted)
        {
            if (!first)
                changeUiState(snackBarMessageEvent = triggered(appContext.getString(R.string.back_online)))
            loadReal(true)
        }
    }

    init
    {
        loadCache()
        if (jecnaClient.lastSuccessfulLoginTime != null)
            loadReal(true)
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
        loadTimetableJob?.cancel()
        appContext.unregisterReceiver(loginBroadcastReceiver)
    }

    fun selectSchoolYear(schoolYear: SchoolYear)
    {
        changeUiState(selectedSchoolYear = schoolYear)
        loadReal(false)
    }

    fun selectTimetablePeriod(timetablePeriod: TimetablePage.PeriodOption)
    {
        changeUiState(selectedPeriod = timetablePeriod)
        loadReal(false)
    }

    private fun loadCache()
    {
        if (!repository.isCacheAvailable())
            return

        viewModelScope.launch {
            val cachedGrades = repository.getCachedTimetable() ?: return@launch

            changeUiState(
                timetablePage = cachedGrades.data,
                selectedSchoolYear = cachedGrades.data.selectedSchoolYear,
                selectedPeriod = cachedGrades.data.periodOptions.find { it.selected },
                lastUpdateTimestamp = cachedGrades.timestamp,
                isCache = true
            )
        }
    }

    /**
     * @param current If true, the newset timetable will be loaded. If false, the selected one will be loaded.
     */
    private fun loadReal(current: Boolean)
    {
        loadTimetableJob?.cancel()

        changeUiState(loading = true)

        loadTimetableJob = viewModelScope.launch {
            try
            {
                /* Load the current timetable if there is no cache or
                the load was initiated automatically (upon open internet reconnection) */
                val realTimetable = if (uiState.selectedPeriod == null || current)
                    repository.getRealTimetable()
                else
                    repository.getRealTimetable(uiState.selectedSchoolYear, uiState.selectedPeriod!!)

                changeUiState(
                    timetablePage = realTimetable,
                    selectedSchoolYear = realTimetable.selectedSchoolYear,
                    selectedPeriod = realTimetable.periodOptions.find { it.selected },
                    lastUpdateTimestamp = Instant.now(),
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
                changeUiState(snackBarMessageEvent = triggered(appContext.getString(R.string.timetable_load_error)))
                e.printStackTrace()
            }
            finally
            {
                changeUiState(loading = false)
            }
        }
    }

    private fun getOfflineMessage(): String?
    {
        val cacheTimestamp = uiState.lastUpdateTimestamp ?: return null
        val localDateTime = LocalDateTime.ofInstant(cacheTimestamp, ZoneId.systemDefault())
        val localDate = localDateTime.toLocalDate()

        return if (localDate == LocalDate.now())
        {
            val time = localDateTime.toLocalTime()
            val timeStr = time.format(OFFLINE_MESSAGE_TIME_FORMATTER)
            appContext.getString(R.string.showing_offline_data_time, timeStr)
        }
        else
        {
            val dateStr = localDateTime.format(OFFLINE_MESSAGE_DATE_FORMATTER)
            appContext.getString(R.string.showing_offline_data_date, dateStr)
        }
    }

    fun reload() = if (!uiState.loading) loadReal(false) else Unit

    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())

    private fun changeUiState(
        loading: Boolean = uiState.loading,
        timetablePage: TimetablePage? = uiState.timetablePage,
        lastUpdateTimestamp: Instant? = uiState.lastUpdateTimestamp,
        isCache: Boolean = uiState.isCache,
        selectedSchoolYear: SchoolYear = uiState.selectedSchoolYear,
        selectedPeriod: TimetablePage.PeriodOption? = uiState.selectedPeriod,
        snackBarMessageEvent: StateEventWithContent<String> = uiState.snackBarMessageEvent
    )
    {
        uiState = uiState.copy(
            loading = loading,
            timetablePage = timetablePage,
            lastUpdateTimestamp = lastUpdateTimestamp,
            isCache = isCache,
            selectedSchoolYear = selectedSchoolYear,
            selectedPeriod = selectedPeriod,
            snackBarMessageEvent = snackBarMessageEvent
        )
    }

    companion object
    {
        private val OFFLINE_MESSAGE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
        private val OFFLINE_MESSAGE_DATE_FORMATTER = DateTimeFormatter.ofPattern("d. M.")
    }
}
