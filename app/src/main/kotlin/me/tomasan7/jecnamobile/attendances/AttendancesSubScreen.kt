package me.tomasan7.jecnamobile.attendances

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import de.palm.composestateevents.EventEffect
import io.github.tomhula.jecnaapi.data.attendance.Attendance
import io.github.tomhula.jecnaapi.data.attendance.AttendanceType
import io.github.tomhula.jecnaapi.util.SchoolYear
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.mainscreen.NavDrawerController
import me.tomasan7.jecnamobile.mainscreen.SubScreenDestination
import me.tomasan7.jecnamobile.ui.component.*
import me.tomasan7.jecnamobile.ui.theme.jm_late_attendance
import me.tomasan7.jecnamobile.util.getWeekDayName
import me.tomasan7.jecnamobile.SubScreenViewModelHook


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendancesSubScreen(
    navDrawerController: NavDrawerController,
    viewModel: AttendancesViewModel = hiltViewModel()
)
{
    SubScreenViewModelHook(viewModel)

    val uiState = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }

    EventEffect(
        event = uiState.snackBarMessageEvent,
        onConsumed = viewModel::onSnackBarMessageEventConsumed
    ) {
        snackbarHostState.showSnackbar(it)
    }

    Scaffold(
        topBar = {
            SubScreenTopAppBar(R.string.sidebar_attendances, navDrawerController) {
                OfflineDataIndicator(
                    modifier = Modifier.padding(end = 16.dp),
                    underlyingIcon = SubScreenDestination.Attendances.iconSelected,
                    lastUpdateTimestamp = uiState.lastUpdateTimestamp,
                    visible = uiState.isCache
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues)
        ) {
            PullToRefreshBox(
                isRefreshing = uiState.loading,
                onRefresh = viewModel::reload,
                modifier = Modifier.align(Alignment.TopCenter)
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
                        selectedSchoolYear = uiState.selectedSchoolYear,
                        selectedMonth = uiState.selectedMonth,
                        onChangeSchoolYear = { viewModel.selectSchoolYear(it) },
                        onChangeMonth = { viewModel.selectMonth(it) }
                    )

                    if (uiState.attendancesPage != null)
                    {
                        if (uiState.daysSorted!!.isEmpty())
                            NoAttendancesMessage(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(500.dp)
                            )
                        else
                            uiState.daysSorted.forEach { day ->
                                val attendance = uiState.attendancesPage[day]
                                key(attendance) {
                                    AttendancesDay(day to attendance)
                                }
                            }
                    }

                    /* 0 because the space is already created by the Column, because of Arrangement.spacedBy() */
                    VerticalSpacer(0.dp)
                }
            }
        }
    }
}

@Composable
private fun PeriodSelectors(
    selectedSchoolYear: SchoolYear,
    selectedMonth: Month,
    onChangeSchoolYear: (SchoolYear) -> Unit,
    onChangeMonth: (Month) -> Unit,
    modifier: Modifier = Modifier
)
{
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SchoolYearSelector(
            modifier = Modifier.width(160.dp),
            selectedSchoolYear = selectedSchoolYear,
            onChange = onChangeSchoolYear
        )
        MonthSelector(
            modifier = Modifier.width(160.dp),
            selectedMonth = selectedMonth,
            onChange = onChangeMonth
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AttendancesDay(
    attendanceRow: Pair<LocalDate, List<Attendance>>,
    modifier: Modifier = Modifier
)
{
    val dayName = getWeekDayName(attendanceRow.first.dayOfWeek)
    val dayDate = attendanceRow.first.format(DATE_FORMATTER)

    Card(
        title = {
            Text(
                text = "$dayName $dayDate",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
        },
        modifier = modifier
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalSpacing = 7.dp,
            verticalSpacing = 7.dp
        ) {
            attendanceRow.second.forEach { attendance ->
                AttendanceChip(attendance)
            }
        }
    }
}

@Composable
private fun AttendanceChip(attendance: Attendance, late: Boolean = false)
{
    Surface(
        tonalElevation = 10.dp,
        border = if (late) BorderStroke(1.dp, jm_late_attendance) else null,
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = "${getAttendanceTypeName(attendance.type)} ${attendance.time.format(TIME_FORMATTER)}",
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Composable
private fun NoAttendancesMessage(
    modifier: Modifier = Modifier
)
{
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.attendances_no_attendances),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun getAttendanceTypeName(type: AttendanceType) = when (type)
{
    AttendanceType.ENTER -> stringResource(R.string.attendances_attendance_type_enter)
    AttendanceType.EXIT  -> stringResource(R.string.attendances_attendance_type_exit)
}

private val TIME_FORMATTER = LocalTime.Format {
    hour(padding = Padding.ZERO)
    char(':')
    minute(padding = Padding.ZERO)
}
private val DATE_FORMATTER = LocalDate.Format {
    day(padding = Padding.NONE)
    char('.')
    monthNumber(padding = Padding.NONE)
}
