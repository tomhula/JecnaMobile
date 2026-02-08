package me.tomasan7.jecnamobile.timetable

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import de.palm.composestateevents.EventEffect
import io.github.tomhula.jecnaapi.data.room.RoomReference
import io.github.tomhula.jecnaapi.data.schoolStaff.TeacherReference
import io.github.tomhula.jecnaapi.data.timetable.TimetablePage
import io.github.tomhula.jecnaapi.util.SchoolYear
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.mainscreen.NavDrawerController
import me.tomasan7.jecnamobile.mainscreen.SubScreenDestination
import me.tomasan7.jecnamobile.ui.component.*

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.core.net.toUri
import me.tomasan7.jecnamobile.mainscreen.SidebarLink
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableSubScreen(
    navDrawerController: NavDrawerController,
    onTeacherClick: (TeacherReference) -> Unit,
    onRoomClick: (RoomReference) -> Unit,
    viewModel: TimetableViewModel = hiltViewModel()
)
{
    DisposableEffect(Unit) {
        viewModel.enteredComposition()
        onDispose {
            viewModel.leftComposition()
        }
    }

    val uiState = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current;

    EventEffect(
        event = uiState.snackBarMessageEvent,
        onConsumed = viewModel::onSnackBarMessageEventConsumed
    ) {
        snackbarHostState.showSnackbar(it)
    }

    Scaffold(
        topBar = {
            SubScreenTopAppBar(R.string.sidebar_timetable, navDrawerController) {
                OfflineDataIndicator(
                    modifier = Modifier.padding(end = 16.dp),
                    underlyingIcon = SubScreenDestination.Timetable.iconSelected,
                    lastUpdateTimestamp = uiState.lastUpdateTimestamp,
                    visible = uiState.isCache
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.loading,
            onRefresh = { viewModel.reload() },
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                PeriodSelectors(
                    modifier = Modifier.fillMaxWidth(),
                    timetablePeriodOptions = uiState.timetablePage?.periodOptions ?: emptyList(),
                    selectedSchoolYear = uiState.selectedSchoolYear,
                    selectedTimetablePeriod = uiState.selectedPeriod,
                    onChangeSchoolYear = { viewModel.selectSchoolYear(it) },
                    onChangeTimetablePeriod = { viewModel.selectTimetablePeriod(it) }
                )
                
                if (uiState.timetablePage != null) {
                    val substitutionsMap: Map<DayOfWeek, List<String?>> = remember(uiState.substitutions) {
                        runCatching {
                            uiState.substitutions
                                ?.data
                                ?.associate { item ->
                                    val date = LocalDate.parse(item.date)
                                    date.dayOfWeek to item.changes.map { it?.text }
                                }
                                ?: emptyMap()
                        }.getOrDefault(emptyMap())
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (uiState.substitutions == null) {
                            val sidebarLink = SidebarLink.SubstitutionTimetable
                            
                            Text(
                                text = stringResource(R.string.substitution_load_error),
                                modifier = Modifier.clickable(onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW)
                                    intent.data = sidebarLink.link.toUri()
                                    context.startActivity(intent)
                                })
                            )
                        } else {
                                val intervalText = if (uiState.substitutions.currentUpdateSchedule < 60) {
                                    pluralStringResource(
                                        id = R.plurals.substitution_update_interval_minutes,
                                        count = uiState.substitutions.currentUpdateSchedule,
                                        uiState.substitutions.currentUpdateSchedule
                                    )
                                } else {
                                    val hours = uiState.substitutions.currentUpdateSchedule / 60
                                    pluralStringResource(
                                        id = R.plurals.subtitution_update_interval_hours,
                                        count = uiState.substitutions.currentUpdateSchedule,
                                        hours 
                                    )
                                }
                                Text(
                                    text = stringResource(
                                        R.string.subtitution_info,
                                        uiState.substitutions.lastUpdated,
                                        intervalText
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )

                                val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
                                val today = now.date
                                
                                val (targetDate, labelRes) = remember(today, now.hour, now.minute, uiState.timetablePage) {
                                    val currentTime = now.time
                                    val timetable = uiState.timetablePage.timetable
                                    val todayLessons = timetable.getLessonSpotsForDay(today.dayOfWeek)
                                    
                                    val lastLessonEndTime = todayLessons
                                        ?.mapIndexedNotNull { index, spot -> if (spot.isNotEmpty()) index else null }
                                        ?.lastOrNull()
                                        ?.let { lastIndex -> timetable.lessonPeriods.getOrNull(lastIndex)?.to }

                                    val dayFinished = lastLessonEndTime?.let { currentTime >= it } ?: (now.hour >= 16)

                                    when {
                                        now.dayOfWeek == DayOfWeek.SATURDAY -> {
                                            today.plus(DatePeriod(days = 2)) to R.string.substitution_day_info_monday
                                        }
                                        now.dayOfWeek == DayOfWeek.SUNDAY -> {
                                            today.plus(DatePeriod(days = 1)) to R.string.substitution_day_info_monday
                                        }
                                        dayFinished -> {
                                            if (now.dayOfWeek == DayOfWeek.FRIDAY) {
                                                today.plus(DatePeriod(days = 3)) to R.string.substitution_day_info_monday
                                            } else {
                                                today.plus(DatePeriod(days = 1)) to R.string.substitution_day_info_tomorrow
                                            }
                                        }
                                        else -> today to R.string.substitution_day_info
                                    }
                                }

                                val targetInfo = uiState.substitutions.data.find { it.date == targetDate.toString() }
                                if (targetInfo != null && targetInfo.takesPlace.isNotBlank()) {
                                    TakesPlaceInfo(stringResource(labelRes), targetInfo.takesPlace)
                                }
                        }

                        Timetable(
                            modifier = Modifier.fillMaxSize(),
                            timetable = uiState.timetablePage.timetable,
                            substitutions = substitutionsMap,
                            hideClass = true,
                            onRoomClick = onRoomClick,
                            onTeacherClick = onTeacherClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodSelectors(
    timetablePeriodOptions: List<TimetablePage.PeriodOption>,
    selectedSchoolYear: SchoolYear,
    selectedTimetablePeriod: TimetablePage.PeriodOption?,
    onChangeSchoolYear: (SchoolYear) -> Unit,
    onChangeTimetablePeriod: (TimetablePage.PeriodOption) -> Unit,
    modifier: Modifier = Modifier
)
{
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SchoolYearSelector(
            modifier = Modifier.width(160.dp),
            showYearAhead = true,
            selectedSchoolYear = selectedSchoolYear,
            onChange = onChangeSchoolYear
        )
        TimetablePeriodSelector(
            modifier = Modifier.width(160.dp),
            periodOptions = timetablePeriodOptions,
            selectedOption = selectedTimetablePeriod,
            onChange = onChangeTimetablePeriod
        )
    }
}

@Composable
private fun TimetablePeriodSelector(
    periodOptions: List<TimetablePage.PeriodOption>,
    modifier: Modifier = Modifier,
    selectedOption: TimetablePage.PeriodOption?,
    onChange: (TimetablePage.PeriodOption) -> Unit
)
{
    OutlinedDropDownSelector(
        modifier = modifier,
        label = stringResource(R.string.timetable_period),
        options = periodOptions,
        optionStringMap = { it?.toString() ?: "" },
        selectedValue = selectedOption,
        onChange = onChange
    )
}
