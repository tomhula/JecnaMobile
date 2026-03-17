package me.tomasan7.jecnamobile.timetable

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import de.palm.composestateevents.EventEffect
import io.github.tomhula.jecnaapi.data.room.RoomReference
import io.github.tomhula.jecnaapi.data.schoolStaff.TeacherReference
import io.github.tomhula.jecnaapi.data.timetable.TimetablePage
import io.github.tomhula.jecnaapi.util.SchoolYear
import kotlinx.datetime.*
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.mainscreen.NavDrawerHandleImpl
import me.tomasan7.jecnamobile.mainscreen.SidebarLink
import me.tomasan7.jecnamobile.navigation.SidebarDestination
import me.tomasan7.jecnamobile.ui.component.*
import me.tomasan7.jecnamobile.SubScreenViewModelHook
import me.tomasan7.jecnamobile.mainscreen.LocalNavDrawerHandle
import me.tomasan7.jecnamobile.util.settingsAsState
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableSubScreen(
    onTeacherClick: (TeacherReference) -> Unit,
    onRoomClick: (RoomReference) -> Unit,
    viewModel: TimetableViewModel = hiltViewModel()
)
{
    SubScreenViewModelHook(viewModel)

    val uiState = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current;
    val showReportDialog = remember { mutableStateOf(false) }
    val isReporting = remember { mutableStateOf(false) }
    val settings by settingsAsState()
    val showSubstitution = remember(
        uiState.selectedSchoolYear,
        uiState.selectedPeriod,
        settings.substitutionTimetableEnabled
    ) {
        if (!settings.substitutionTimetableEnabled) return@remember false

        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date

        val schoolYear = uiState.selectedSchoolYear
        val period = uiState.selectedPeriod ?: return@remember false

        val periodStart = period.from
        val periodEnd = period.to

        val inSchoolYear = today in schoolYear
        val inPeriod = today >= periodStart &&
                (periodEnd == null || today <= periodEnd)

        val isShowingCurrentTimetable = inSchoolYear && inPeriod
        
        isShowingCurrentTimetable
    }

    EventEffect(
        event = uiState.snackBarMessageEvent,
        onConsumed = viewModel::onSnackBarMessageEventConsumed
    ) {
        snackbarHostState.showSnackbar(it)
    }

    if (showReportDialog.value) {
        ReportDialog(
            isLoading = isReporting.value,
            onDismissRequest = { if (!isReporting.value) showReportDialog.value = false },
            onReport = { content, location ->
                isReporting.value = true
                viewModel.reportError(content, location) {
                    isReporting.value = false
                    showReportDialog.value = false
                }
            }
        )
    }

    Scaffold(
        topBar = {
            SubScreenTopAppBar(R.string.sidebar_timetable, LocalNavDrawerHandle.current) {
                if (showSubstitution) {
                    IconButton(onClick = { showReportDialog.value = true }) {
                        Icon(Icons.Filled.Report, contentDescription = stringResource(R.string.report_button_description))
                    }
                }
                OfflineDataIndicator(
                    modifier = Modifier.padding(end = 16.dp),
                    underlyingIcon = SidebarDestination.Timetable.iconSelected,
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
                ExpandableSection(title = stringResource(R.string.timetable_period)) {
                    PeriodSelectors(
                        modifier = Modifier.fillMaxWidth(),
                        timetablePeriodOptions = uiState.timetablePage?.periodOptions ?: emptyList(),
                        selectedSchoolYear = uiState.selectedSchoolYear,
                        selectedTimetablePeriod = uiState.selectedPeriod,
                        onChangeSchoolYear = { viewModel.selectSchoolYear(it) },
                        onChangeTimetablePeriod = { viewModel.selectTimetablePeriod(it) }
                    )
                }
                
                if (uiState.timetablePage != null) {

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (showSubstitution) {
                            if (uiState.substitutions == null) {
                                val sidebarLink = SidebarLink.SubstitutionTimetable

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val intent = Intent(Intent.ACTION_VIEW)
                                            intent.data = sidebarLink.link.toUri()
                                            context.startActivity(intent)
                                        },
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(6.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.error
                                        ) {}
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.substitution_load_error),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
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

                                val targetInfo = remember(targetDate, uiState.substitutions.data) {
                                    uiState.substitutions.data.find { it.date == targetDate.toString() }
                                }
                                val takesPlaceText = targetInfo?.takesPlace?.takeIf { it.isNotBlank() }

                                val contentPadding = ExpandableSectionPadding.Custom(
                                    start = 30.dp,
                                    top = 0.dp,
                                    end = 8.dp,
                                    bottom = if (takesPlaceText != null) 8.dp else 0.dp
                                )
                                ExpandableSection(
                                    title = stringResource(R.string.sidebar_link_substitution_timetable),
                                    modifier = Modifier.fillMaxWidth(),
                                    icon = {
                                        Surface(
                                            modifier = Modifier.size(6.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary
                                        ) {}
                                    },
                                    trailingHeaderContent = {
                                        Spacer(Modifier.weight(1f))
                                        Text(
                                            text = "${uiState.substitutions.lastUpdated} ($intervalText)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    },
                                    expandable = takesPlaceText != null,
                                    color = if (takesPlaceText != null) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.secondaryContainer.copy(
                                        alpha = 0.4f
                                    ),
                                    titleColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    contentPadding = contentPadding
                                ) {
                                    if (takesPlaceText != null)
                                    {
                                        Text(
                                            text = stringResource(labelRes),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                        Text(
                                            text = takesPlaceText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        Timetable(
                            modifier = Modifier.fillMaxSize(),
                            timetable = uiState.timetablePage.timetable,
                            substitutions = if (showSubstitution) uiState.substitutions else null,
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
    SelectionRow(modifier = modifier) {
        SchoolYearSelector(
            modifier = Modifier.weight(1f),
            showYearAhead = true,
            selectedSchoolYear = selectedSchoolYear,
            onChange = onChangeSchoolYear
        )
        TimetablePeriodSelector(
            modifier = Modifier.weight(1f),
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
