package me.tomasan7.jecnamobile.attendances

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.github.tomhula.jecnaapi.data.attendance.AttendancesPage
import io.github.tomhula.jecnaapi.util.SchoolYear
import kotlinx.datetime.*
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.SubScreenCacheViewModel
import me.tomasan7.jecnamobile.util.CachedData
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

@HiltViewModel
class AttendancesViewModel @Inject constructor(
    @ApplicationContext
    appContext: Context,
    private val repository: CacheAttendancesRepository
) : SubScreenCacheViewModel<AttendancesPage>(appContext)
{
    override val parseErrorMessage = appContext.getString(R.string.error_unsupported_attendances)
    override val loadErrorMessage = appContext.getString(R.string.attendances_load_error)
    
    var uiState by mutableStateOf(AttendancesState())
        private set

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

    override fun setCacheDataUiState(data: CachedData<AttendancesPage>) = changeUiState(
        attendancesPage = data.data,
        lastUpdateTimestamp = data.timestamp,
        isCache = true
    )

    override fun getCache(): CachedData<AttendancesPage>?
    {
        val cache = repository.getCachedAttendances() ?: return null
        if (cache.data.selectedSchoolYear != uiState.selectedSchoolYear || cache.data.selectedMonth != uiState.selectedMonth)
            return null

        return cache
    }
    override fun isCacheAvailable() = repository.isCacheAvailable()
    override fun getLastUpdateTimestamp() = uiState.lastUpdateTimestamp
    override fun isCurrentlyShowingCache() = uiState.isCache

    override suspend fun fetchRealData() = repository.getRealAttendances(uiState.selectedSchoolYear, uiState.selectedMonth)

    override fun showSnackBarMessage(message: String) = changeUiState(snackBarMessageEvent = triggered(message))

    override fun setLoadingUiState(loading: Boolean) = changeUiState(loading)

    override fun setDataUiState(data: AttendancesPage) = changeUiState(
        attendancesPage = data,
        lastUpdateTimestamp = Clock.System.now(),
        isCache = false
    )

    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())

    private fun changeUiState(
        loading: Boolean = uiState.loading,
        attendancesPage: AttendancesPage? = uiState.attendancesPage,
        lastUpdateTimestamp: Instant? = uiState.lastUpdateTimestamp,
        isCache: Boolean = uiState.isCache,
        selectedSchoolYear: SchoolYear = uiState.selectedSchoolYear,
        selectedMonth: Month = uiState.selectedMonth,
        snackBarMessageEvent: StateEventWithContent<String> = uiState.snackBarMessageEvent
    )
    {
        uiState = uiState.copy(
            loading = loading,
            attendancesPage = attendancesPage,
            lastUpdateTimestamp = lastUpdateTimestamp,
            isCache = isCache,
            selectedSchoolYear = selectedSchoolYear,
            selectedMonth = selectedMonth,
            snackBarMessageEvent = snackBarMessageEvent
        )
    }
}
