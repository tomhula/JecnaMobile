package me.tomasan7.jecnamobile.absence

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                        val daysSorted = uiState.absencesPage.days.sortedDescending()

                        if (daysSorted.isEmpty())
                            NoAbsencesMessage(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(500.dp)
                            )
                        else
                            daysSorted.forEach { day ->
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
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AbsenceInfoRow(
                label = stringResource(R.string.absences_hours_absent),
                value = absenceInfo.hoursAbsent.toString()
            )

            AbsenceInfoRow(
                label = stringResource(R.string.absences_unexcused_hours),
                value = absenceInfo.unexcusedHours.toString(),
                highlighted = absenceInfo.unexcusedHours > 0
            )

            AbsenceInfoRow(
                label = stringResource(R.string.absences_late_entries),
                value = absenceInfo.lateEntryCount.toString(),
                highlighted = absenceInfo.lateEntryCount > 0
            )
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
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (highlighted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
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
