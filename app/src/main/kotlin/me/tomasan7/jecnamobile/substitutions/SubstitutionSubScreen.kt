package me.tomasan7.jecnamobile.substitutions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import de.palm.composestateevents.EventEffect
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.mainscreen.NavDrawerController
import me.tomasan7.jecnamobile.ui.component.*

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp

import io.github.tomhula.jecnaapi.data.schoolStaff.TeacherReference

import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.graphics.toColorInt
import me.tomasan7.jecnamobile.timetable.AbsenceEntry
import me.tomasan7.jecnamobile.timetable.ChangeEntry
import me.tomasan7.jecnamobile.timetable.TakesPlaceInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubstitutionSubScreen(
    navDrawerController: NavDrawerController,
    onTeacherClick: (TeacherReference) -> Unit,
    viewModel: SubstitutionViewModel = hiltViewModel()
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
    val showReportDialog = remember { mutableStateOf(false) }
    val isReporting = remember { mutableStateOf(false) }

    EventEffect(
        event = uiState.snackBarMessageEvent,
        onConsumed = viewModel::onSnackBarMessageEventConsumed
    ) {
        snackbarHostState.showSnackbar(it)
    }

    if (showReportDialog.value)
    {
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
            SubScreenTopAppBar(R.string.sidebar_link_substitution_timetable, navDrawerController) {
                IconButton(onClick = { showReportDialog.value = true }) {
                    Icon(Icons.Filled.Report, contentDescription = stringResource(R.string.report_button_description))
                }
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.data != null)
                {
                    val intervalText = if (uiState.data.currentUpdateSchedule < 60)
                    {
                        pluralStringResource(
                            id = R.plurals.substitution_update_interval_minutes,
                            count = uiState.data.currentUpdateSchedule,
                            uiState.data.currentUpdateSchedule
                        )
                    }
                    else
                    {
                        val hours = uiState.data.currentUpdateSchedule / 60
                        pluralStringResource(
                            id = R.plurals.subtitution_update_interval_hours,
                            count = uiState.data.currentUpdateSchedule,
                            hours
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.subtitution_info,
                            uiState.data.lastUpdated,
                            intervalText
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )

                    val dates = remember(uiState.data) {
                        uiState.data.schedule.keys.sorted().map {
                            SubstitutionDay(it, LocalDate.parse(it), uiState.data.schedule[it]?.info?.inWork ?: false)
                        }
                    }

                    OutlinedDropDownSelector(
                        modifier = Modifier.fillMaxWidth(),
                        textFieldModifier = Modifier.fillMaxWidth(),
                        label = stringResource(R.string.substitution_all_date_label),
                        options = dates,
                        optionStringMap = {
                            it?.let {
                                if (it.inWork)
                                {
                                    it.date.format(DATE_FORMATTER) + " (příprava)"
                                }
                                else
                                {
                                    it.date.format(DATE_FORMATTER)
                                }
                            } ?: ""
                        },
                        selectedValue = uiState.selectedDate,
                        onChange = { viewModel.selectDate(it) }
                    )

                    val selectedDayData = uiState.data.schedule[uiState.selectedDate?.key]
                    if (selectedDayData != null)
                    {
                        TakesPlaceInfo(
                            label = stringResource(R.string.substitution_day_info),
                            text = selectedDayData.takesPlace.ifBlank {
                                stringResource(R.string.substitution_no_actions_take_place)
                            },
                            expandable = false
                        )

                        Text(
                            text = stringResource(R.string.substitution_all_changes_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        SubstitutionsTable(selectedDayData.changes)

                        Text(
                            text = stringResource(R.string.substitution_all_absences_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (selectedDayData.absence.isNotEmpty())
                        {
                            selectedDayData.absence.forEach { entry ->
                                TeacherAbsenceItem(entry, onTeacherClick)
                            }
                        }
                        else
                        {
                            Text(
                                text = stringResource(R.string.substitution_no_teacher_absences),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherAbsenceItem(
    entry: AbsenceEntry,
    onTeacherClick: (TeacherReference) -> Unit
)
{
    val teacherName = when (entry)
    {
        is AbsenceEntry.WholeDay -> entry.teacher
        is AbsenceEntry.Single -> entry.teacher
        is AbsenceEntry.Range -> entry.teacher
        is AbsenceEntry.Exkurze -> entry.teacher
        is AbsenceEntry.Zastoupen -> entry.teacher
        is AbsenceEntry.Invalid -> null
    }

    val teacherCode = when (entry)
    {
        is AbsenceEntry.WholeDay -> entry.teacherCode
        is AbsenceEntry.Single -> entry.teacherCode
        is AbsenceEntry.Range -> entry.teacherCode
        is AbsenceEntry.Exkurze -> entry.teacherCode
        is AbsenceEntry.Zastoupen -> entry.teacherCode
        is AbsenceEntry.Invalid -> null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = {
            if (teacherName != null && teacherCode != null)
            {
                onTeacherClick(TeacherReference(teacherName, teacherCode))
            }
        },
        enabled = teacherName != null && teacherCode != null
    ) {
        Column(Modifier.padding(8.dp)) {
            val typeText = when (entry)
            {
                is AbsenceEntry.WholeDay -> stringResource(R.string.substitution_all_absence_whole_day)
                is AbsenceEntry.Single -> stringResource(R.string.substitution_all_absence_single, entry.hours)
                is AbsenceEntry.Range -> stringResource(
                    R.string.substitution_all_absence_range,
                    entry.hours.from,
                    entry.hours.to
                )

                is AbsenceEntry.Exkurze -> stringResource(R.string.substitution_all_absence_exkurze)
                is AbsenceEntry.Zastoupen -> stringResource(
                    R.string.substitution_all_absence_zastoupen,
                    entry.zastupuje.teacher ?: stringResource(R.string.substitution_all_teacher_unknown)
                )

                is AbsenceEntry.Invalid -> stringResource(R.string.substitution_all_absence_invalid, entry.original)
            }

            Text(
                text = teacherName ?: stringResource(R.string.substitution_all_teacher_unknown),
                fontWeight = FontWeight.Bold
            )
            Text(text = typeText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SubstitutionsTable(changes: Map<String, List<ChangeEntry?>>)
{
    val classRegex = remember { Regex("([A-Z]+)(\\d+)([a-z]*)") }
    val sortedClasses = remember(changes) {
        changes.keys.sortedWith { c1, c2 ->
            val m1 = classRegex.matchEntire(c1)
            val m2 = classRegex.matchEntire(c2)

            if (m1 != null && m2 != null)
            {
                val (p1, n1, s1) = m1.destructured
                val (p2, n2, s2) = m2.destructured

                val numCompare = n1.toInt().compareTo(n2.toInt())
                if (numCompare != 0) return@sortedWith numCompare

                val preCompare = p1.compareTo(p2)
                if (preCompare != 0) return@sortedWith preCompare

                s1.compareTo(s2)
            }
            else
            {
                c1.compareTo(c2)
            }
        }
    }
    val maxHours = changes.values.maxOfOrNull { it.size } ?: 0
    val horizontalScrollState = rememberScrollState()
    val cellSize = 80.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
            Row(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
                TableCell(text = "", isHeader = true, size = cellSize)
                for (hour in 1..maxHours)
                {
                    TableCell(text = hour.toString(), isHeader = true, size = cellSize)
                }
            }

            sortedClasses.forEach { className ->
                val classChanges = changes[className] ?: emptyList()
                Row {
                    TableCell(text = className, isHeader = true, size = cellSize)
                    for (hourIndex in 0 until maxHours)
                    {
                        val change = classChanges.getOrNull(hourIndex)
                        val bgColor = change?.backgroundColor?.let { hex ->
                            runCatching { Color(hex.toColorInt()) }.getOrNull()
                        }
                        val fgColor = change?.foregroundColor?.let { hex ->
                            runCatching { Color(hex.toColorInt()) }.getOrNull()
                        }
                        TableCell(
                            text = change?.text ?: "",
                            size = cellSize,
                            backgroundColor = bgColor,
                            foregroundColor = fgColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    size: Dp,
    isHeader: Boolean = false,
    backgroundColor: Color? = null,
    foregroundColor: Color? = null
)
{
    Box(
        modifier = Modifier
            .size(size)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            .then(if (backgroundColor != null) Modifier.background(backgroundColor) else Modifier)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = if (isHeader) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodySmall,
            color = foregroundColor ?: Color.Unspecified,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("d.M.yyyy")
