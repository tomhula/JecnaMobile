package me.tomasan7.jecnamobile.timetable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import de.palm.composestateevents.EventEffect
import io.github.stevekk11.dtos.LabeledTeacherAbsences
import io.github.tomhula.jecnaapi.data.room.RoomReference
import io.github.tomhula.jecnaapi.data.schoolStaff.TeacherReference
import io.github.tomhula.jecnaapi.data.timetable.TimetablePage
import io.github.tomhula.jecnaapi.util.SchoolYear
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.mainscreen.NavDrawerController
import me.tomasan7.jecnamobile.mainscreen.SubScreenDestination
import me.tomasan7.jecnamobile.ui.component.*

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
                
                val (mergedTimetable, lessonColors) = viewModel.processedTimetableData ?: (null to emptyMap())
                if (mergedTimetable != null) {
                    Timetable(
                        modifier = Modifier.fillMaxSize(),
                        timetable = mergedTimetable,
                        lessonColors = lessonColors,
                        hideClass = true,
                        onRoomClick = onRoomClick,
                        onTeacherClick = onTeacherClick
                    )
                }
                InfoRow("Suplování aktuální pro", uiState.substitutionStatus?.lastUpdated ?: "Neznámo")
                InfoRow("Četnost aktualizace", uiState.substitutionStatus?.currentUpdateSchedule.toString() + " minut")
                TeacherAbsencesSection(uiState.teacherAbsences)
            }
        }
    }
}
@Composable
private fun TeacherAbsencesSection(
    teacherAbsences: List<LabeledTeacherAbsences>?,
    modifier: Modifier = Modifier
) {
    if (teacherAbsences.isNullOrEmpty()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Absence učitelů",
            style = MaterialTheme.typography.titleMedium
        )
        teacherAbsences.forEach { labeledAbsence ->
            ExpandableCard(
                modifier = Modifier.fillMaxWidth(),
                title = { Text(labeledAbsence.date) }
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    labeledAbsence.absences.forEach { absence ->
                        Column {
                            val type = when (absence.type)
                            {
                                "single" -> "hodina"
                                "wholeDay" -> "celý den"
                                "range" -> "rozsah"
                                "invalid" -> "neznámý typ"
                                else -> absence.type
                            }

                            Text(
                                text = "${absence.teacher ?: absence.teacherCode ?: "Neznámý učitel"}: $type",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (absence.hours != null) {
                                Text(
                                    text = "${absence.hours!!.from}. - ${absence.hours!!.to}. hodina",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!absence.message.isNullOrBlank()) {
                                Text(
                                    text = absence.message!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
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
