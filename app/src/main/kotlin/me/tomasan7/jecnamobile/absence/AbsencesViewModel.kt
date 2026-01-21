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
import io.github.tomhula.jecnaapi.WebJecnaClient
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
import me.tomasan7.jecnamobile.CacheRepository
import me.tomasan7.jecnamobile.JecnaMobileApplication
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.SubScreenCacheViewModel
import me.tomasan7.jecnamobile.util.CachedDataNew
import me.tomasan7.jecnamobile.util.createBroadcastReceiver
import me.tomasan7.jecnamobile.util.now
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant


@HiltViewModel
class AbsencesViewModel @Inject constructor(
    @ApplicationContext
    appContext: Context,
    repository: CacheRepository<AbsencesPage, SchoolYear>
) : SubScreenCacheViewModel<AbsencesPage, SchoolYear>(appContext, repository) 
{
    override val parseErrorMessage = appContext.getString(R.string.error_unsupported_absences)
    override val loadErrorMessage = appContext.getString(R.string.absences_load_error)
    
    var uiState by mutableStateOf(AbsencesState())
        private set
    
    fun selectSchoolYear(schoolYear: SchoolYear)
    {
        changeUiState(selectedSchoolYear = schoolYear)
        loadReal()
    }


    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())

    override fun setCacheDataUiState(data: CachedDataNew<AbsencesPage, SchoolYear>) = changeUiState(
        absencesPage = data.data,
        lastUpdateTimestamp = data.timestamp,
        isCache = true
    )

    override fun setDataUiState(data: AbsencesPage) = changeUiState(
        absencesPage = data,
        lastUpdateTimestamp = Clock.System.now(),
        isCache = false
    )

    override fun getLastUpdateTimestamp() = uiState.lastUpdateTimestamp

    override fun isCurrentlyShowingCache() = uiState.isCache
    override fun getParams() = uiState.selectedSchoolYear
    override fun showSnackBarMessage(message: String) = changeUiState(snackBarMessageEvent = triggered(message))
    override fun setLoadingUiState(loading: Boolean) = changeUiState(loading = loading)

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
}
