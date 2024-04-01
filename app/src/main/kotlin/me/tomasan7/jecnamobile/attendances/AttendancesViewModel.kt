package me.tomasan7.jecnamobile.attendances

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
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.tomasan7.jecnaapi.JecnaClient
import me.tomasan7.jecnaapi.data.attendance.AttendancesPage
import me.tomasan7.jecnaapi.util.SchoolYear
import me.tomasan7.jecnamobile.JecnaMobileApplication
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.util.createBroadcastReceiver
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class AttendancesViewModel @Inject constructor(
    @ApplicationContext
    private val appContext: Context,
    jecnaClient: JecnaClient,
    private val repository: CacheAttendancesRepository
) : ViewModel()
{
    var uiState by mutableStateOf(AttendancesState())
        private set

    private var loadAttendancesJob: Job? = null

    private val loginBroadcastReceiver = createBroadcastReceiver { _, intent ->
        val first = intent.getBooleanExtra(JecnaMobileApplication.SUCCESSFUL_LOGIN_FIRST_EXTRA, false)

        if (loadAttendancesJob == null || loadAttendancesJob!!.isCompleted)
        {
            if (!first)
                changeUiState(snackBarMessageEvent = triggered(appContext.getString(R.string.back_online)))
            loadReal()
        }
    }

    init
    {
        loadCache()
        if (jecnaClient.lastSuccessfulLoginTime != null)
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
        loadAttendancesJob?.cancel()
        appContext.unregisterReceiver(loginBroadcastReceiver)
    }

    fun selectSchoolYear(schoolYear: SchoolYear)
    {
        changeUiState(selectedSchoolYear = schoolYear)
        loadReal()
    }

    fun selectMonth(month: Month)
    {
        changeUiState(selectedMonth = month)
        loadReal()
    }

    private fun loadCache()
    {
        if (!repository.isCacheAvailable())
            return

        viewModelScope.launch {
            val cachedGrades = repository.getCachedAttendances() ?: return@launch

            changeUiState(
                attendancePage = cachedGrades.data,
                lastUpdateTimestamp = cachedGrades.timestamp,
                isCache = true
            )
        }
    }

    private fun loadReal()
    {
        loadAttendancesJob?.cancel()

        changeUiState(loading = true)

        loadAttendancesJob = viewModelScope.launch {
            try
            {
                val realGrades = if (isSelectedPeriodCurrent())
                    repository.getRealAttendances()
                else
                    repository.getRealAttendances(uiState.selectedSchoolYear, uiState.selectedMonth)

                changeUiState(
                    attendancePage = realGrades,
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
                changeUiState(snackBarMessageEvent = triggered(appContext.getString(R.string.attendances_load_error)))
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

    fun reload() = if (!uiState.loading) loadReal() else Unit

    private fun isSelectedPeriodCurrent() =
        uiState.selectedSchoolYear == SchoolYear.current() && uiState.selectedMonth == LocalDate.now().month

    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())

    private fun changeUiState(
        loading: Boolean = uiState.loading,
        attendancePage: AttendancesPage? = uiState.attendancesPage,
        lastUpdateTimestamp: Instant? = uiState.lastUpdateTimestamp,
        isCache: Boolean = uiState.isCache,
        selectedSchoolYear: SchoolYear = uiState.selectedSchoolYear,
        selectedMonth: Month = uiState.selectedMonth,
        snackBarMessageEvent: StateEventWithContent<String> = uiState.snackBarMessageEvent
    )
    {
        uiState = uiState.copy(
            loading = loading,
            attendancesPage = attendancePage,
            lastUpdateTimestamp = lastUpdateTimestamp,
            isCache = isCache,
            selectedSchoolYear = selectedSchoolYear,
            selectedMonth = selectedMonth,
            snackBarMessageEvent = snackBarMessageEvent
        )
    }

    companion object
    {
        val OFFLINE_MESSAGE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
        val OFFLINE_MESSAGE_DATE_FORMATTER = DateTimeFormatter.ofPattern("d. M.")
    }
}
