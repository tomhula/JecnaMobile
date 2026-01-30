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
import io.github.stevekk11.dtos.DailySchedule
import io.github.stevekk11.dtos.SubstitutionStatus
import io.github.tomhula.jecnaapi.data.timetable.TimetablePage
import io.github.tomhula.jecnaapi.util.SchoolYear
import me.tomasan7.jecnamobile.LoginStateProvider
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.SubScreenCacheViewModel
import me.tomasan7.jecnamobile.caching.CacheRepository
import me.tomasan7.jecnamobile.caching.SchoolYearPeriodParams
import me.tomasan7.jecnamobile.util.CachedDataNew
import me.tomasan7.jecnamobile.student.StudentProfileRepository
import me.tomasan7.jecnamobile.teachers.TeachersRepository
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.tomasan7.jecnamobile.substitution.SubstitutionRepository
import androidx.compose.runtime.derivedStateOf
import io.github.stevekk11.dtos.LabeledTeacherAbsences
import kotlinx.coroutines.async
import mergeTimetableWithSubstitutions

@HiltViewModel
class TimetableViewModel @Inject constructor(
    @ApplicationContext
    appContext: Context,
    loginStateProvider: LoginStateProvider,
    repository: CacheRepository<TimetablePage, SchoolYearPeriodParams>,
    private val substitutionRepository: SubstitutionRepository,
    private val studentProfileRepository: StudentProfileRepository,
    private val teachersRepository: TeachersRepository
) : SubScreenCacheViewModel<TimetablePage, SchoolYearPeriodParams>(appContext, loginStateProvider, repository)
{
    override val parseErrorMessage = appContext.getString(R.string.error_unsupported_timetable)
    override val loadErrorMessage = appContext.getString(R.string.timetable_load_error)

    var uiState by mutableStateOf(TimetableState())
        private set

    val processedTimetableData by derivedStateOf {
        val baseTimetable = uiState.timetablePage?.timetable ?: return@derivedStateOf null
        val dailySubs = uiState.substitutions

        // Calls the utility function using reflection to bypass private constructor
        mergeTimetableWithSubstitutions(baseTimetable, dailySubs)
    }

    fun selectSchoolYear(schoolYear: SchoolYear)
    {
        changeUiState(selectedSchoolYear = schoolYear)
        loadReal()
    }

    fun selectTimetablePeriod(timetablePeriod: TimetablePage.PeriodOption)
    {
        changeUiState(selectedPeriod = timetablePeriod)
        loadReal()
    }

    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())

    override fun setDataUiState(data: TimetablePage) {
        viewModelScope.launch {
            try {
                /* 1. Start fetching background data in parallel while we have the TimetablePage */
                val studentDeferred = async { studentProfileRepository.getCurrentStudent() }
                val teachersPageDeferred = async { teachersRepository.getTeachersPage() }
                /* Wait for student to set the class symbol for substitutions */
                val student = studentDeferred.await()
                substitutionRepository.setClassSymbol(student.className ?: "")
                /* 2. Fetch all substitution-related data in parallel */
                val substitutionsDeferred = async { substitutionRepository.getDailySubstitutions() }
                val statusDeferred = async { substitutionRepository.getSubstitutionsStatus() }
                val absencesDeferred = async { substitutionRepository.getTeacherAbsences() }
                val substitutions = substitutionsDeferred.await()
                val status = statusDeferred.await()
                val absences = absencesDeferred.await()
                val teachersPage = teachersPageDeferred.await()
                val teacherNameMap = teachersPage.teachersReferences.associate { it.tag to it.fullName }
                
                changeUiState(
                    timetablePage = data,
                    lastUpdateTimestamp = Clock.System.now(),
                    selectedSchoolYear = data.selectedSchoolYear,
                    selectedPeriod = data.periodOptions.find { it.selected },
                    isCache = false,
                    substitutions = substitutions,
                    substitutionStatus = status,
                    teacherNameMap = teacherNameMap,
                    teacherAbsences = absences
                )
            } catch (e: Exception) {
                changeUiState(
                    timetablePage = data,
                    lastUpdateTimestamp = Clock.System.now(),
                    selectedSchoolYear = data.selectedSchoolYear,
                    selectedPeriod = data.periodOptions.find { it.selected },
                    isCache = false
                )
                showSnackBarMessage("Failed to load substitutions: ${e.message}")
            } finally {
                /* Ensure the refresh indicator stops spinning */
                setLoadingUiState(false)
            }
        }
    }

    override fun setCacheDataUiState(data: CachedDataNew<TimetablePage, SchoolYearPeriodParams>) = changeUiState(
        timetablePage = data.data,
        lastUpdateTimestamp = data.timestamp,
        selectedSchoolYear = data.data.selectedSchoolYear,
        selectedPeriod = data.data.periodOptions.find { it.selected },
        isCache = true
    )

    override fun getParams() = SchoolYearPeriodParams(
        uiState.selectedSchoolYear,
        uiState.selectedPeriod?.id ?: SchoolYearPeriodParams.CURRENT_PERIOD_ID
    )

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
        substitutions: List<DailySchedule>? = uiState.substitutions,
        substitutionStatus: SubstitutionStatus? = uiState.substitutionStatus,
        teacherNameMap: Map<String, String> = uiState.teacherNameMap,
        snackBarMessageEvent: StateEventWithContent<String> = uiState.snackBarMessageEvent,
        teacherAbsences: List<LabeledTeacherAbsences>? = uiState.teacherAbsences
    )
    {
        uiState = uiState.copy(
            loading = loading,
            timetablePage = timetablePage,
            lastUpdateTimestamp = lastUpdateTimestamp,
            isCache = isCache,
            selectedSchoolYear = selectedSchoolYear,
            selectedPeriod = selectedPeriod,
            substitutions = substitutions,
            substitutionStatus = substitutionStatus,
            teacherNameMap = teacherNameMap,
            snackBarMessageEvent = snackBarMessageEvent,
            teacherAbsences = teacherAbsences
        )
    }
}
