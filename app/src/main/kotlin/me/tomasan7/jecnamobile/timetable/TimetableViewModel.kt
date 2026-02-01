package me.tomasan7.jecnamobile.timetable

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.github.tomhula.jecnaapi.data.timetable.TimetablePage
import io.github.tomhula.jecnaapi.util.SchoolYear
import me.tomasan7.jecnamobile.LoginStateProvider
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.SubScreenCacheViewModel
import me.tomasan7.jecnamobile.caching.CacheRepository
import me.tomasan7.jecnamobile.caching.SchoolYearPeriodParams
import me.tomasan7.jecnamobile.util.CachedDataNew
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

@HiltViewModel
class TimetableViewModel @Inject constructor(
    @ApplicationContext
    appContext: Context,
    loginStateProvider: LoginStateProvider,
    repository: CacheRepository<TimetablePage, SchoolYearPeriodParams>
) : SubScreenCacheViewModel<TimetablePage, SchoolYearPeriodParams>(appContext, loginStateProvider, repository)
{
    override val parseErrorMessage = appContext.getString(R.string.error_unsupported_timetable)
    override val loadErrorMessage = appContext.getString(R.string.timetable_load_error)
    
    var uiState by mutableStateOf(TimetableState())
        private set
    
    fun selectSchoolYear(schoolYear: SchoolYear)
    {
        changeUiState(selectedSchoolYear = schoolYear)
        loadReal()
    }
    
    fun getTeacherAbsences(){
       
    }

    fun selectTimetablePeriod(timetablePeriod: TimetablePage.PeriodOption)
    {
        changeUiState(selectedPeriod = timetablePeriod)
        loadReal()
    }
    
    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())

    override fun setDataUiState(data: TimetablePage) = changeUiState(
        timetablePage = data,
        lastUpdateTimestamp = Clock.System.now(),
        selectedSchoolYear = data.selectedSchoolYear,
        selectedPeriod = data.periodOptions.find { it.selected },
        isCache = false
    )

    override fun setCacheDataUiState(data: CachedDataNew<TimetablePage, SchoolYearPeriodParams>) = changeUiState(
        timetablePage = data.data,
        lastUpdateTimestamp = data.timestamp,
        selectedSchoolYear = data.data.selectedSchoolYear,
        selectedPeriod = data.data.periodOptions.find { it.selected },
        isCache = true
    )
    
    override fun getParams() = SchoolYearPeriodParams(uiState.selectedSchoolYear, uiState.selectedPeriod?.id ?: SchoolYearPeriodParams.CURRENT_PERIOD_ID)
    
    override fun getLastUpdateTimestamp() = uiState.lastUpdateTimestamp
    override fun isCurrentlyShowingCache() = uiState.isCache
    override fun showSnackBarMessage(message: String) = changeUiState(snackBarMessageEvent = triggered(message))
    override fun setLoadingUiState(loading: Boolean) = changeUiState(loading = loading)

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
}
