package me.tomasan7.jecnamobile.grades

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
import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.WebJecnaClient
import io.github.tomhula.jecnaapi.data.grade.GradesPage
import io.github.tomhula.jecnaapi.data.grade.Subject
import io.github.tomhula.jecnaapi.data.notification.Notification
import io.github.tomhula.jecnaapi.data.notification.NotificationReference
import io.github.tomhula.jecnaapi.util.SchoolYear
import io.github.tomhula.jecnaapi.util.SchoolYearHalf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import me.tomasan7.jecnamobile.JecnaMobileApplication
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.settings.Settings
import me.tomasan7.jecnamobile.util.createBroadcastReceiver
import me.tomasan7.jecnamobile.util.settingsDataStore
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import me.tomasan7.jecnamobile.CacheRepository
import me.tomasan7.jecnamobile.SchoolYearHalfParams
import me.tomasan7.jecnamobile.SubScreenCacheViewModel
import me.tomasan7.jecnamobile.util.CachedDataNew
import me.tomasan7.jecnamobile.util.now
import javax.inject.Inject
import kotlin.time.Clock

@HiltViewModel
class GradesViewModel @Inject constructor(
    @ApplicationContext
    appContext: Context,
    repository: CacheRepository<GradesPage, SchoolYearHalfParams>,
    private val jecnaClient: JecnaClient
) : SubScreenCacheViewModel<GradesPage, SchoolYearHalfParams>(appContext, repository)
{
    override val parseErrorMessage = appContext.getString(R.string.error_unsupported_grades)
    override val loadErrorMessage = appContext.getString(R.string.grade_load_error)
    
    var uiState by mutableStateOf(GradesState())
        private set
    
    private val settingsDataStore = appContext.settingsDataStore

    override fun setCacheDataUiState(data: CachedDataNew<GradesPage, SchoolYearHalfParams>) = changeUiState(
        gradesPage = data.data,
        lastUpdateTimestamp = data.timestamp,
        isCache = true
    )

    override fun setDataUiState(data: GradesPage) = changeUiState(
        gradesPage = data,
        lastUpdateTimestamp = Clock.System.now(),
        isCache = false
    )

    override fun getLastUpdateTimestamp() = uiState.lastUpdateTimestamp
    override fun isCurrentlyShowingCache() = uiState.isCache
    override fun getParams() = SchoolYearHalfParams(uiState.selectedSchoolYear, uiState.selectedSchoolYearHalf)
    override fun showSnackBarMessage(message: String) = changeUiState(snackBarMessageEvent = triggered(message))
    override fun setLoadingUiState(loading: Boolean) = changeUiState(loading = loading)
    
    fun setViewMode(gradesViewMode: Settings.GradesViewMode)
    {
        viewModelScope.launch {
            settingsDataStore.updateData {
                it.copy(gradesViewMode = gradesViewMode)
            }
        }
    }

    fun selectSchoolYearHalf(schoolYearHalf: SchoolYearHalf)
    {
        changeUiState(selectedSchoolYearHalf = schoolYearHalf)
        loadReal()
    }

    fun selectSchoolYear(schoolYear: SchoolYear)
    {
        changeUiState(selectedSchoolYear = schoolYear)
        loadReal()
    }

    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())

    fun addPredictedGrade(subject: Subject, predictedGrade: PredictedGrade)
    {
        val currentPredictions = uiState.predictedGrades.toMutableMap()
        val subjectPredictions = currentPredictions[subject]?.toMutableList() ?: mutableListOf()
        subjectPredictions.add(predictedGrade)
        currentPredictions[subject] = subjectPredictions
        changeUiState(predictedGrades = currentPredictions)
    }

    fun removePredictedGrade(subject: Subject, predictedGrade: PredictedGrade)
    {
        val currentPredictions = uiState.predictedGrades.toMutableMap()
        val predictedGrades = currentPredictions[subject]?.toMutableList() ?: return
        predictedGrades.remove(predictedGrade)
        currentPredictions[subject] = predictedGrades
        changeUiState(predictedGrades = currentPredictions)
    }

    fun setShowPredictions(show: Boolean)
    {
        if (!show)
            changeUiState(predictedGrades = emptyMap(), showPredictions = false)
        else
            changeUiState(showPredictions = true)
    }
    
    fun onBehaviourNotificationClick(notificationReference: NotificationReference)
    {
        changeUiState(loadingNotification = notificationReference)
        viewModelScope.launch {
            val notification = jecnaClient.getNotification(notificationReference)
            changeUiState(loadingNotification = null, dialogNotification = notification)
        }
    }
    
    fun onNotificationDialogDismiss()
    {
        changeUiState(dialogNotification = null)
    }

    private fun changeUiState(
        loading: Boolean = uiState.loading,
        gradesPage: GradesPage? = uiState.gradesPage,
        lastUpdateTimestamp: Instant? = uiState.lastUpdateTimestamp,
        selectedSchoolYear: SchoolYear = uiState.selectedSchoolYear,
        isCache: Boolean = uiState.isCache,
        selectedSchoolYearHalf: SchoolYearHalf = uiState.selectedSchoolYearHalf,
        snackBarMessageEvent: StateEventWithContent<String> = uiState.snackBarMessageEvent,
        predictedGrades: Map<Subject, List<PredictedGrade>> = uiState.predictedGrades,
        showPredictions: Boolean = uiState.showPredictions,
        loadingNotification: NotificationReference? = uiState.loadingNotification,
        dialogNotification: Notification? = uiState.dialogNotification
    )
    {
        uiState = uiState.copy(
            loading = loading,
            gradesPage = gradesPage,
            lastUpdateTimestamp = lastUpdateTimestamp,
            isCache = isCache,
            selectedSchoolYear = selectedSchoolYear,
            selectedSchoolYearHalf = selectedSchoolYearHalf,
            snackBarMessageEvent = snackBarMessageEvent,
            predictedGrades = predictedGrades,
            showPredictions = showPredictions,
            loadingNotification = loadingNotification,
            dialogNotification = dialogNotification
        )
    }
}
