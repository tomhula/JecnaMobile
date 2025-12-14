package me.tomasan7.jecnamobile.absence

import androidx.annotation.PluralsRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import de.palm.composestateevents.EventEffect
import io.github.tomhula.jecnaapi.data.absence.AbsenceInfo
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.mainscreen.NavDrawerController
import me.tomasan7.jecnamobile.mainscreen.SubScreenDestination
import me.tomasan7.jecnamobile.mainscreen.SubScreensNavGraph
import me.tomasan7.jecnamobile.ui.ElevationLevel
import me.tomasan7.jecnamobile.ui.component.*
import me.tomasan7.jecnamobile.util.getWeekDayName
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@OptIn(ExperimentalMaterial3Api::class)
@Destination<SubScreensNavGraph>
@Composable
fun AbsencesSubScreen(
    navDrawerController: NavDrawerController,
    viewModel: AbsencesViewModel = hiltViewModel()
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

    EventEffect(
        event = uiState.snackBarMessageEvent,
        onConsumed = viewModel::onSnackBarMessageEventConsumed
    ) {
        snackbarHostState.showSnackbar(it)
    }

    Scaffold(
        topBar = {
            SubScreenTopAppBar(R.string.sidebar_absences, navDrawerController) {
                OfflineDataIndicator(
                    modifier = Modifier.padding(end = 16.dp),
                    underlyingIcon = SubScreenDestination.Absences.iconSelected,
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
                    SchoolYearSelector(
                        modifier = Modifier.align(Alignment.CenterHorizontally).width(160.dp),
                        selectedSchoolYear = uiState.selectedSchoolYear,
                        onChange = { viewModel.selectSchoolYear(it) }
                    )

                    if (uiState.absencesPage != null)
                    {
                        uiState.summary?.let { summary ->
                            if (uiState.daysSorted.isNotEmpty()) {
                                AbsencesTotalSummary(
                                    totalHoursAbsent = summary.totalHoursAbsent,
                                    totalExcusedHours = summary.totalExcusedHours,
                                    totalUnexcusedHours = summary.totalUnexcusedHours,
                                    totalLateEntries = summary.totalLateEntries
                                )
                            }
                        }

                        if (uiState.daysSorted.isEmpty())
                            NoAbsencesMessage(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(500.dp)
                            )
                        else
                            uiState.daysSorted.forEach { day ->
                                val absenceInfo = uiState.absencesPage[day]
                                key(day) {
                                    AbsenceDay(day to absenceInfo)
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
private fun AbsencesTotalSummary(
    totalHoursAbsent: Int,
    totalExcusedHours: Int,
    totalUnexcusedHours: Int,
    totalLateEntries: Int,
    modifier: Modifier = Modifier
)
{
    Card(
        title = {
            Text(
                text = stringResource(R.string.absences_total_summary),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
        },
        modifier  = modifier,
        tonalElevation = ElevationLevel.level4,
        shadowElevation = ElevationLevel.level4
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AbsenceInfoRow(
                label = stringResource(R.string.absences_total_hours_absent),
                value = totalHoursAbsent.toString(),
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.primary
            )

            AbsenceInfoRow(
                label = stringResource(R.string.absences_total_excused_hours),
                value = totalExcusedHours.toString()
            )

            AbsenceInfoRow(
                label = stringResource(R.string.absences_total_unexcused_hours),
                value = totalUnexcusedHours.toString(),
                highlighted = totalUnexcusedHours > 0
            )

            AbsenceInfoRow(
                label = stringResource(R.string.absences_total_late_entries),
                value = totalLateEntries.toString(),
            )
        }
    }
}

@Composable
private fun AbsenceDay(
    absenceRow: Pair<LocalDate, AbsenceInfo?>,
    modifier: Modifier = Modifier
)
{
    val (day, absenceInfo) = absenceRow

    if (absenceInfo == null) return

    val dayName = getWeekDayName(day.dayOfWeek)
    val dayDate = day.format(DATE_FORMATTER)

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
            val excusedHours = absenceInfo.hoursAbsent - absenceInfo.unexcusedHours
            
            if (excusedHours != 0)
                AbsenceChip(R.plurals.absences_hours_excused, excusedHours)
            
            if (absenceInfo.unexcusedHours != 0)
                AbsenceChip(R.plurals.absences_unexcused_hours, absenceInfo.unexcusedHours, warning = true)

            if (absenceInfo.lateEntryCount != 0)
                AbsenceChip(R.plurals.absences_late_entries, absenceInfo.lateEntryCount)
        }
    }
}

@Composable
private fun AbsenceInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
)
{
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val textColor = if (highlighted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
    }
}

@Composable
private fun AbsenceChip(@PluralsRes string: Int, number: Int, warning: Boolean = false)
{
    Surface(
        color = if (warning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
        tonalElevation = 10.dp,
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = pluralStringResource(string, number, number),
            modifier = Modifier.padding(10.dp),
        )
    }
}

@Composable
private fun NoAbsencesMessage(
    modifier: Modifier = Modifier
)
{
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.absences_no_absences),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("d.M.")
