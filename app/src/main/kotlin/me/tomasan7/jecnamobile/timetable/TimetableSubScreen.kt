package me.tomasan7.jecnamobile.timetable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import de.palm.composestateevents.EventEffect
import io.github.tomhula.jecnaapi.data.timetable.TimetablePage
import io.github.tomhula.jecnaapi.util.SchoolYear
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.destinations.TeacherScreenDestination
import me.tomasan7.jecnamobile.mainscreen.NavDrawerController
import me.tomasan7.jecnamobile.mainscreen.SubScreenDestination
import me.tomasan7.jecnamobile.mainscreen.SubScreensNavGraph
import me.tomasan7.jecnamobile.ui.component.OfflineDataIndicator
import me.tomasan7.jecnamobile.ui.component.OutlinedDropDownSelector
import me.tomasan7.jecnamobile.ui.component.SchoolYearSelector
import me.tomasan7.jecnamobile.ui.component.SubScreenTopAppBar
import me.tomasan7.jecnamobile.ui.component.Timetable

@OptIn(ExperimentalMaterial3Api::class)
@Destination<SubScreensNavGraph>(start = true)
@Composable
fun TimetableSubScreen(
    navDrawerController: NavDrawerController,
    navigator: DestinationsNavigator,
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

                if (uiState.timetablePage != null)
                    Timetable(
                        modifier = Modifier.fillMaxSize(),
                        timetable = uiState.timetablePage.timetable,
                        hideClass = true,
                        onTeacherClick = { navigator.navigate(TeacherScreenDestination(it)) }
                    )
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
