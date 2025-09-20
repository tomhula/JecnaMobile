package me.tomasan7.jecnamobile.attendances

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import de.palm.composestateevents.EventEffect
import me.tomasan7.jecnaapi.data.attendance.Attendance
import me.tomasan7.jecnaapi.data.attendance.AttendanceType
import me.tomasan7.jecnaapi.util.SchoolYear
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.mainscreen.NavDrawerController
import me.tomasan7.jecnamobile.mainscreen.SubScreenDestination
import me.tomasan7.jecnamobile.mainscreen.SubScreensNavGraph
import me.tomasan7.jecnamobile.ui.component.*
import me.tomasan7.jecnamobile.ui.theme.jm_late_attendance
import me.tomasan7.jecnamobile.util.PullToRefreshHandler
import me.tomasan7.jecnamobile.util.getWeekDayName
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.format.DateTimeFormatter


@OptIn(ExperimentalMaterial3Api::class)
@Destination<SubScreensNavGraph>
@Composable
fun AttendancesSubScreen(
    navDrawerController: NavDrawerController,
    viewModel: AttendancesViewModel = hiltViewModel()
)
{
    DisposableEffect(Unit) {
        viewModel.enteredComposition()
        onDispose {
            viewModel.leftComposition()
        }
    }

    val uiState = viewModel.uiState
    val pullToRefreshState = rememberPullToRefreshState()
    val snackbarHostState = remember { SnackbarHostState() }

    PullToRefreshHandler(
        state = pullToRefreshState,
        shown = uiState.loading,
        onRefresh = { viewModel.reload() }
    )

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
            modifier = Modifier
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
                .padding(paddingValues)
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

            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
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
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            //if the entry time is after 7:25 as recommended by the Headmaster, it's marked as late.
            val cutoff = LocalTime.of(7,25)
            attendanceRow.second.forEach { attendance ->

                val entryTime: LocalTime? = attendance.time
                val isLate = entryTime?.isAfter(cutoff) == true

                if (attendance.type == AttendanceType.ENTER){
                    AttendanceChip(attendance, late = isLate)
                } else {
                    AttendanceChip(attendance)
                }
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

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("d.M.")

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
