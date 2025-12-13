package me.tomasan7.jecnamobile.grades

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import de.palm.composestateevents.EventEffect
import io.github.tomhula.jecnaapi.data.grade.Behaviour
import io.github.tomhula.jecnaapi.data.grade.FinalGrade
import io.github.tomhula.jecnaapi.data.grade.Grade
import io.github.tomhula.jecnaapi.data.grade.Subject
import io.github.tomhula.jecnaapi.data.notification.NotificationReference
import io.github.tomhula.jecnaapi.data.schoolStaff.TeacherReference
import io.github.tomhula.jecnaapi.util.SchoolYear
import io.github.tomhula.jecnaapi.util.SchoolYearHalf
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.destinations.TeacherScreenDestination
import me.tomasan7.jecnamobile.mainscreen.NavDrawerController
import me.tomasan7.jecnamobile.mainscreen.SubScreenDestination
import me.tomasan7.jecnamobile.mainscreen.SubScreensNavGraph
import me.tomasan7.jecnamobile.settings.Settings
import me.tomasan7.jecnamobile.ui.ElevationLevel
import me.tomasan7.jecnamobile.ui.component.DialogRow
import me.tomasan7.jecnamobile.ui.component.HorizontalSpacer
import me.tomasan7.jecnamobile.ui.component.ObjectDialog
import me.tomasan7.jecnamobile.ui.component.OfflineDataIndicator
import me.tomasan7.jecnamobile.ui.component.SchoolYearHalfSelector
import me.tomasan7.jecnamobile.ui.component.SchoolYearSelector
import me.tomasan7.jecnamobile.ui.component.SubScreenTopAppBar
import me.tomasan7.jecnamobile.ui.component.VerticalSpacer
import me.tomasan7.jecnamobile.ui.component.dashedBorder
import me.tomasan7.jecnamobile.ui.component.rememberObjectDialogState
import me.tomasan7.jecnamobile.ui.theme.grade_absence_warning
import me.tomasan7.jecnamobile.ui.theme.grade_excused
import me.tomasan7.jecnamobile.ui.theme.grade_grades_warning
import me.tomasan7.jecnamobile.ui.theme.jm_label
import me.tomasan7.jecnamobile.util.calculateAverageWithPredictions
import me.tomasan7.jecnamobile.util.getGradeColor
import me.tomasan7.jecnamobile.util.rememberMutableStateOf
import me.tomasan7.jecnamobile.util.settingsAsState
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Destination<SubScreensNavGraph>
@Composable
fun GradesSubScreen(
    navDrawerController: NavDrawerController,
    navigator: DestinationsNavigator,
    viewModel: GradesViewModel = hiltViewModel()
)
{
    DisposableEffect(Unit) {
        viewModel.enteredComposition()
        onDispose {
            viewModel.leftComposition()
        }
    }

    val uiState = viewModel.uiState
    val objectDialogState = rememberObjectDialogState<Grade>()
    val predictionDialogState = rememberObjectDialogState<Subject>()
    val snackbarHostState = remember { SnackbarHostState() }
    val settings by settingsAsState()

    EventEffect(
        event = uiState.snackBarMessageEvent,
        onConsumed = viewModel::onSnackBarMessageEventConsumed
    ) {
        snackbarHostState.showSnackbar(it)
    }

    Scaffold(
        topBar = {
            SubScreenTopAppBar(R.string.sidebar_grades, navDrawerController) {
                OfflineDataIndicator(
                    underlyingIcon = SubScreenDestination.Grades.iconSelected,
                    lastUpdateTimestamp = uiState.lastUpdateTimestamp,
                    visible = uiState.isCache
                )
                PredictionToggleButton(
                    enabled = uiState.showPredictions,
                    onClick = { viewModel.setShowPredictions(!uiState.showPredictions) }
                )
                ViewModeButton(
                    viewMode = settings.gradesViewMode,
                    onViewModeChange = viewModel::setViewMode
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
                    selectedSchoolYear = uiState.selectedSchoolYear,
                    selectedSchoolYearHalf = uiState.selectedSchoolYearHalf,
                    onChangeSchoolYear = { viewModel.selectSchoolYear(it) },
                    onChangeSchoolYearHalf = { viewModel.selectSchoolYearHalf(it) }
                )

                if (uiState.gradesPage != null)
                {
                    uiState.subjectsSorted!!.forEach { subject ->
                        key(subject) {
                            val predictedGrades = uiState.predictedGrades[subject] ?: emptyList()
                            when (settings.gradesViewMode)
                            {
                                Settings.GradesViewMode.LIST -> ListSubject(
                                    subject = subject,
                                    predictedGrades = predictedGrades,
                                    showAddPredictionButton = uiState.showPredictions,
                                    onGradeClick = { objectDialogState.show(it) },
                                    onAddPredictionClick = { predictionDialogState.show(subject) },
                                    onPredictedGradeClick = { viewModel.removePredictedGrade(subject, it) }
                                )

                                Settings.GradesViewMode.GRID -> Subject(
                                    subject = subject,
                                    predictedGrades = predictedGrades,
                                    showAddPredictionButton = uiState.showPredictions,
                                    onGradeClick = { objectDialogState.show(it) },
                                    onAddPredictionClick = { predictionDialogState.show(subject) },
                                    onPredictedGradeClick = { viewModel.removePredictedGrade(subject, it) }
                                )
                            }
                        }
                    }

                    Behaviour(uiState.gradesPage.behaviour)
                }
            }

            ObjectDialog(
                state = objectDialogState,
                onDismissRequest = { objectDialogState.hide() },
                content = { grade ->
                    GradeDialogContent(
                        grade = grade,
                        onTeacherClick = { navigator.navigate(TeacherScreenDestination(it)) },
                        onCloseClick = { objectDialogState.hide() }
                    )
                }
            )

            ObjectDialog(
                state = predictionDialogState,
                onDismissRequest = { predictionDialogState.hide() },
                content = { subject ->
                    AddPredictionDialog(
                        subject = subject,
                        onConfirm = { grade ->
                            viewModel.addPredictedGrade(subject, grade)
                            predictionDialogState.hide()
                        },
                        onCancel = { predictionDialogState.hide() }
                    )
                }
            )
        }
    }
}

@Composable
private fun ViewModeButton(
    viewMode: Settings.GradesViewMode,
    onViewModeChange: (Settings.GradesViewMode) -> Unit,
)
{
    val icon = remember(viewMode) {
        when (viewMode)
        {
            Settings.GradesViewMode.LIST -> Icons.Filled.ViewModule
            Settings.GradesViewMode.GRID -> Icons.AutoMirrored.Filled.ViewList
        }
    }

    val nextViewMode = remember(viewMode) {
        when (viewMode)
        {
            Settings.GradesViewMode.LIST -> Settings.GradesViewMode.GRID
            Settings.GradesViewMode.GRID -> Settings.GradesViewMode.LIST
        }
    }

    IconButton(onClick = { onViewModeChange(nextViewMode) }) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            //tint = MaterialTheme.colorScheme.surfaceColorAtElevation(100.dp)
        )
    }
}

@Composable
private fun PeriodSelectors(
    selectedSchoolYear: SchoolYear,
    selectedSchoolYearHalf: SchoolYearHalf,
    onChangeSchoolYear: (SchoolYear) -> Unit,
    onChangeSchoolYearHalf: (SchoolYearHalf) -> Unit,
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
        SchoolYearHalfSelector(
            modifier = Modifier.width(160.dp),
            selectedSchoolYearHalf = selectedSchoolYearHalf,
            onChange = onChangeSchoolYearHalf
        )
    }
}

@Composable
private fun Container(
    title: @Composable () -> Unit = {},
    rightColumnVisible: Boolean = true,
    rightColumnContent: @Composable ColumnScope.() -> Unit = {},
    onRightColumnClick: (() -> Unit)? = null,
    modifier: Modifier,
    content: @Composable () -> Unit = {}
)
{
    Surface(
        modifier = modifier,
        tonalElevation = ElevationLevel.level1,
        shadowElevation = ElevationLevel.level1,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(20.dp)
        ) {
            Column(Modifier.weight(1f, true)) {
                title()

                Spacer(Modifier.height(15.dp))

                content()
            }

            if (rightColumnVisible)
            {
                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 10.dp),
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(100.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clickable(
                            enabled = onRightColumnClick != null,
                            onClick = { onRightColumnClick?.invoke() },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    content = rightColumnContent
                )
            }
        }
    }
}

@Composable
private fun Container(
    title: String,
    rightColumnVisible: Boolean = true,
    rightColumnContent: @Composable ColumnScope.() -> Unit = {},
    onRightColumnClick: (() -> Unit)? = null,
    modifier: Modifier,
    content: @Composable () -> Unit = {}
) = Container(
    title = {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
    },
    rightColumnVisible = rightColumnVisible,
    rightColumnContent = rightColumnContent,
    onRightColumnClick = onRightColumnClick,
    modifier = modifier,
    content = content
)

@Composable
private fun Subject(
    subject: Subject,
    predictedGrades: List<PredictedGrade> = emptyList(),
    showAddPredictionButton: Boolean = true,
    onGradeClick: (Grade) -> Unit = {},
    onAddPredictionClick: () -> Unit = {},
    onPredictedGradeClick: (PredictedGrade) -> Unit = {}
)
{
    val average = remember(subject, predictedGrades) {
        if (predictedGrades.isNotEmpty())
            subject.grades.calculateAverageWithPredictions(predictedGrades)
        else
            subject.grades.average()
    }
    var showAverage by rememberMutableStateOf(false)

    Container(
        modifier = Modifier.fillMaxWidth(),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(subject.name.full, style = MaterialTheme.typography.titleMedium)
                    if (subject.grades.count != 0)
                        Text(
                            text = pluralStringResource(R.plurals.grades_count, subject.grades.count, subject.grades.count),
                            color = jm_label,
                            style = MaterialTheme.typography.labelSmall
                        )
                }
                if (showAddPredictionButton)
                    IconButton(
                        onClick = onAddPredictionClick,
                        modifier = Modifier
                            .size(Constants.gradeWidth)
                            .align(Alignment.CenterVertically)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.grade_predictor_add_prediction),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
            }
        },
        rightColumnVisible = subject.finalGrade != null || average != null,
        rightColumnContent = {
            if (subject.finalGrade == null)
                GradeAverage(average!!, isPrediction = predictedGrades.isNotEmpty())
            else
                if (showAverage)
                    GradeAverage(average!!, isPrediction = predictedGrades.isNotEmpty())
                else
                    FinalGrade(finalGrade = subject.finalGrade!!)

        },
        onRightColumnClick = {
            // Excused subject won't have any grades. This will throw NullPointerException if switched to showAwerage
            if (subject.finalGrade !== FinalGrade.Excused) {
                showAverage = !showAverage
            }
        }
    ) {
        if (subject.grades.isEmpty() && predictedGrades.isEmpty())
            Text(
                text = stringResource(R.string.no_grades),
                style = MaterialTheme.typography.bodyMedium
            )
        else
            Column {
                subject.grades.subjectParts.forEach { subjectPart ->
                    SubjectPart(
                        subjectPart = subjectPart,
                        grades = subject.grades[subjectPart]!!,
                        predictedGrades = if (subjectPart == null) predictedGrades else emptyList(),
                        onPredictedGradeClick = onPredictedGradeClick,
                        onGradeClick = onGradeClick,
                    )
                }
                if (null !in subject.grades.subjectParts && predictedGrades.isNotEmpty())
                    SubjectPart(
                        subjectPart = stringResource(R.string.grade_predictor_predicted_label),
                        grades = emptyList(),
                        onPredictedGradeClick = onPredictedGradeClick,
                        predictedGrades = predictedGrades
                    )
            }
    }
}

@Composable
private fun ListSubject(
    subject: Subject,
    predictedGrades: List<PredictedGrade> = emptyList(),
    showAddPredictionButton: Boolean = true,
    onGradeClick: (Grade) -> Unit = {},
    onAddPredictionClick: () -> Unit = {},
    onPredictedGradeClick: (PredictedGrade) -> Unit = {}
)
{
    val average = remember(subject, predictedGrades) {
        if (predictedGrades.isNotEmpty())
            subject.grades.calculateAverageWithPredictions(predictedGrades)
        else
            subject.grades.average()
    }
    var showAverage by rememberMutableStateOf(false)
    var showGrades by rememberMutableStateOf(false)

    Container(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showGrades = !showGrades },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(subject.name.full, style = MaterialTheme.typography.titleMedium)
                        if (subject.grades.count != 0)
                            Text(
                                text = pluralStringResource(
                                    R.plurals.grades_count, subject.grades.count, subject.grades.count
                                ),
                                color = jm_label,
                                style = MaterialTheme.typography.labelSmall
                            )
                    }

                    if (showAddPredictionButton)
                        IconButton(
                            onClick = onAddPredictionClick,
                            modifier = Modifier
                                .size(Constants.gradeWidth)
                                .padding(end = 20.dp)
                                .align(Alignment.CenterVertically)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.grade_predictor_add_prediction),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                }

                if (subject.finalGrade != null || average != null)
                    if (subject.finalGrade == null)
                        GradeAverage(
                            value = average!!,
                            onClick = { showAverage = !showAverage },
                            isPrediction = predictedGrades.isNotEmpty()
                        )
                    else
                        if (showAverage)
                            GradeAverage(
                                value = average!!,
                                onClick = { showAverage = !showAverage },
                                isPrediction = predictedGrades.isNotEmpty()
                            )
                        else
                            FinalGrade(finalGrade = subject.finalGrade!!, onClick = { showAverage = !showAverage })
            }
        },
        rightColumnVisible = false,
        rightColumnContent = {},
    ) {
        if (subject.grades.isEmpty() && predictedGrades.isEmpty())
            Text(
                text = stringResource(R.string.no_grades),
                style = MaterialTheme.typography.bodyMedium
            )
        else
        {
            if (showGrades)
                Column {
                    subject.grades.subjectParts.forEach { subjectPart ->
                        ListSubjectPart(
                            subjectPart = subjectPart,
                            grades = subject.grades[subjectPart]!!,
                            predictedGrades = if (subjectPart == null) predictedGrades else emptyList(),
                            onPredictedGradeClick = onPredictedGradeClick,
                            onGradeClick = onGradeClick,
                        )
                    }
                    if (null !in subject.grades.subjectParts && predictedGrades.isNotEmpty())
                        ListSubjectPart(
                            subjectPart = stringResource(R.string.grade_predictor_predicted_label),
                            grades = emptyList(),
                            onPredictedGradeClick = onPredictedGradeClick,
                            predictedGrades = predictedGrades
                        )
                }
        }
    }
}

@Composable
private fun PredictionToggleButton(
    enabled: Boolean,
    onClick: () -> Unit,
)
{
    val tint = if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant

    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.Lightbulb,
            contentDescription = null,
            tint = tint
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectPart(
    subjectPart: String? = null,
    grades: List<Grade>,
    predictedGrades: List<PredictedGrade> = emptyList(),
    onPredictedGradeClick: (PredictedGrade) -> Unit = {},
    onGradeClick: (Grade) -> Unit = {},
)
{
    if (subjectPart != null)
        Text(
            text = "$subjectPart:",
            modifier = Modifier.padding(vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge
        )

    FlowRow(
        verticalSpacing = 5.dp,
        horizontalSpacing = 5.dp,
    ) {
        grades.forEach { grade ->
            Grade(
                grade = grade,
                modifier = Modifier.align(Alignment.CenterVertically),
                onClick = { onGradeClick(grade) }
            )
        }

        predictedGrades.forEach { predictedGrade ->
            PredictedGrade(
                predictedGrade = predictedGrade,
                modifier = Modifier.align(Alignment.CenterVertically).clickable(onClick = {onPredictedGradeClick(predictedGrade)})
            )
        }
    }
}

@Composable
private fun ListSubjectPart(
    subjectPart: String? = null,
    grades: List<Grade>,
    predictedGrades: List<PredictedGrade> = emptyList(),
    onPredictedGradeClick: (PredictedGrade) -> Unit = {},
    onGradeClick: (Grade) -> Unit = {},
)
{
    if (subjectPart != null)
        Text(
            text = "$subjectPart:",
            modifier = Modifier.padding(vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge
        )

    val gradesSorted by remember { derivedStateOf { grades.sortedByDescending { it.receiveDate } } }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top),
    ) {
        gradesSorted.forEachIndexed { i, grade ->
            ListGrade(
                grade = grade,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onGradeClick(grade) }
            )
            if (i != gradesSorted.lastIndex || predictedGrades.isNotEmpty())
                HorizontalDivider(
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(100.dp)
                )
        }

        predictedGrades.forEachIndexed { i, predictedGrade ->
            ListPredictedGrade(
                predictedGrade = predictedGrade,
                modifier = Modifier.fillMaxWidth().clickable(onClick = { onPredictedGradeClick(predictedGrade) })
            )
            if (i != predictedGrades.lastIndex)
                HorizontalDivider(
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(100.dp)
                )
        }
    }
}

@Composable
private fun ListGrade(
    grade: Grade,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
)
{
    val newModifier = if (onClick != null)
        modifier.clickable(onClick = onClick)
    else
        modifier

    val gradeColor = remember { getGradeColor(grade) }

    val gradeSizeBig = 30.dp
    val gradeSizeSmall = 20.dp

    Row(
        modifier = newModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            val size = if (grade.small) gradeSizeSmall else gradeSizeBig

            Box(
                modifier = Modifier.size(gradeSizeBig),
                contentAlignment = Alignment.Center
            ) {
                GradeBox(
                    text = grade.valueChar.toString(),
                    color = gradeColor,
                    modifier = Modifier.size(size)
                )
            }
            HorizontalSpacer(10.dp)
            Text(grade.description ?: "")
        }

        if (grade.receiveDate != null)
            Text(
                modifier = Modifier.padding(horizontal = 10.dp),
                text = grade.receiveDate!!.format(Constants.gradeDateFormatter)
            )
    }
}

@Composable
private fun GradeBox(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = Constants.gradeWidth,
    onClick: (() -> Unit)? = null
)
{
    var newModifier = modifier
        .size(Constants.gradeWidth, height)
        .clip(Constants.gradeShape)
        .background(color)

    if (onClick != null)
        newModifier = newModifier.clickable(onClick = onClick)

    Box(
        modifier = newModifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@Composable
private fun Grade(
    grade: Grade,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
)
{
    val gradeHeight = if (grade.small) Constants.gradeSmallHeight else Constants.gradeWidth
    val gradeColor = remember { getGradeColor(grade) }

    GradeBox(
        modifier = modifier,
        text = grade.valueChar.toString(),
        color = gradeColor,
        height = gradeHeight,
        onClick = onClick
    )
}

@Composable
private fun GradeAverage(
    value: Float,
    isPrediction: Boolean = false,
    onClick: (() -> Unit)? = null
)
{
    val valueString = remember(value) { Constants.gradeAverageDecimalFormat.format(value) }

    val content = @Composable {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = valueString,
                fontSize = 16.sp
            )
        }
    }

    val shape = Constants.gradeShape

    val border = BorderStroke(1.dp, getGradeColor(value.roundToInt()))
    val tonalElevation = 10.dp
    var modifier = Modifier.size(Constants.gradeWidth)
    modifier = if (isPrediction)
        modifier.dashedBorder(border, shape, 4.dp, 2.dp)
    else
        modifier.border(border, shape)

    if (onClick != null)
        modifier = modifier.clickable(onClick = onClick)

    Surface(
        modifier = modifier,
        shape = shape,
        tonalElevation = tonalElevation,
        content = content
    )
}

@Composable
private fun FinalGrade(
    finalGrade: FinalGrade,
    onClick: (() -> Unit)? = null
) = when (finalGrade)
{
    is FinalGrade.Grade                   -> Grade(Grade(finalGrade.value, false, gradeId = -1), onClick = onClick)
    is FinalGrade.GradesWarning           -> GradesWarning(onClick = onClick)
    is FinalGrade.AbsenceWarning          -> AbsenceWarning(onClick = onClick)
    is FinalGrade.GradesAndAbsenceWarning -> GradesAndAbsenceWarning(onClick = onClick)
    is FinalGrade.Excused                 -> Excused(onClick = onClick)
}

@Composable
private fun Excused(onClick: (() -> Unit)? = {})
{
    GradeBox(
        text = stringResource(R.string.grade_excused),
        color = grade_excused,
        onClick = onClick
    )
}


@Composable
private fun GradesWarning(onClick: (() -> Unit)? = {})
{
    GradeBox(
        text = stringResource(R.string.grade_grades_warning_content),
        color = grade_grades_warning,
        onClick = onClick
    )
}

@Composable
private fun AbsenceWarning(onClick: (() -> Unit)? = {})
{
    GradeBox(
        text = stringResource(R.string.grade_absence_warning_content),
        color = grade_absence_warning,
        onClick = onClick
    )
}

@Composable
private fun GradesAndAbsenceWarning(onClick: (() -> Unit)? = {})
{
    Column(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        GradesWarning()
        AbsenceWarning()
    }
}

@Composable
private fun GradeDialogContent(
    grade: Grade,
    onTeacherClick: (TeacherReference) -> Unit = {},
    onCloseClick: () -> Unit = {}
)
{
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.width(340.dp),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(2.dp, getGradeColor(grade))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(getGradeColor(grade))
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getGradeWord(grade),
                    color = Color.Black,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DialogRow(
                        stringResource(R.string.grade_receive_date),
                        grade.receiveDate?.format(Constants.gradeDateFormatter) ?: ""
                    )
                    DialogRow(stringResource(R.string.grade_description), grade.description ?: "")
                    val teacher = grade.teacher
                    if (teacher?.short == null)
                        DialogRow(stringResource(R.string.grade_teacher), teacher?.full ?: "")
                    else
                        DialogRow(
                            label = stringResource(R.string.grade_teacher),
                            value = teacher.full,
                            onClick = { onTeacherClick(TeacherReference(teacher.full, teacher.short!!)) }
                        )
                }

                VerticalSpacer(24.dp)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onCloseClick) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        }
    }
}

@Composable
private fun Behaviour(behaviour: Behaviour)
{
    Container(
        modifier = Modifier.fillMaxWidth(),
        title = Behaviour.SUBJECT_NAME,
        rightColumnContent = {
            if (behaviour.finalGrade is FinalGrade.Grade)
                Grade(Grade((behaviour.finalGrade as FinalGrade.Grade).value, false, gradeId = -1))
        }
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalSpacing = 5.dp,
            verticalSpacing = 5.dp
        ) {
            behaviour.notifications.forEach {
                BehaviourNotification(it)
            }
        }
    }
}

@Composable
private fun BehaviourNotification(behaviourNotification: NotificationReference)
{
    Surface(
        tonalElevation = 10.dp,
        shape = RoundedCornerShape(7.dp)
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(end = 5.dp),
                text = behaviourNotification.message
            )
            when (behaviourNotification.type)
            {
                NotificationReference.NotificationType.GOOD ->
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = getGradeColor(1)
                    )

                NotificationReference.NotificationType.BAD  ->
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = getGradeColor(5)
                    )
                NotificationReference.NotificationType.INFORMATION ->
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                    )
            }
        }
    }
}

@Composable
private fun PredictedGrade(
    predictedGrade: PredictedGrade,
    modifier: Modifier = Modifier
)
{
    val gradeHeight = if (predictedGrade.isSmall) Constants.gradeSmallHeight else Constants.gradeWidth
    val gradeColor = remember(predictedGrade) { getGradeColor(predictedGrade.value) }

    Box(
        modifier = modifier
            .size(Constants.gradeWidth, gradeHeight)
            .dashedBorder(1.dp, gradeColor, RoundedCornerShape(7.dp), 4.dp, 2.dp)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = predictedGrade.value.toString(),
            color = gradeColor,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@Composable
private fun ListPredictedGrade(
    predictedGrade: PredictedGrade,
    modifier: Modifier = Modifier
)
{
    val gradeColor = remember { getGradeColor(predictedGrade.value) }
    val gradeSizeBig = 30.dp
    val gradeSizeSmall = 20.dp

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            val size = if (predictedGrade.isSmall) gradeSizeSmall else gradeSizeBig

            Box(
                modifier = Modifier.size(gradeSizeBig),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(size)
                        .dashedBorder(1.dp, gradeColor, RoundedCornerShape(7.dp), 4.dp, 2.dp)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = predictedGrade.value.toString(),
                        color = gradeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            HorizontalSpacer(10.dp)
            Text(
                text = stringResource(R.string.grade_predictor_predicted_label),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AddPredictionDialog(
    subject: Subject,
    onConfirm: (PredictedGrade) -> Unit,
    onCancel: () -> Unit
)
{
    var selectedValue by rememberMutableStateOf(3)
    var isSmall by rememberMutableStateOf(false)

    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.width(300.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.grade_predictor_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.grade_predictor_type_label),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { isSmall = false }
                ) {
                    RadioButton(
                        selected = !isSmall,
                        onClick = { isSmall = false }
                    )
                    HorizontalSpacer(8.dp)
                    Text(stringResource(R.string.grade_predictor_type_large))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { isSmall = true }
                ) {
                    RadioButton(
                        selected = isSmall,
                        onClick = { isSmall = true }
                    )
                    HorizontalSpacer(8.dp)
                    Text(stringResource(R.string.grade_predictor_type_small))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.grade_predictor_value_label),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..5).forEach { value ->
                        val gradeColor = remember { getGradeColor(value) }
                        Box(
                            modifier = Modifier
                                .size(width = Constants.gradeWidth, height = if (isSmall) Constants.gradeSmallHeight else Constants.gradeWidth)
                                .clip(Constants.gradeShape)
                                .background(
                                    if (selectedValue == value) gradeColor
                                    else gradeColor.copy(alpha = 0.3f)
                                )
                                .clickable { selectedValue = value },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = value.toString(),
                                color = Color.Black,
                                fontWeight = if (selectedValue == value) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 20.sp
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.grade_predictor_cancel_button))
                }
                TextButton(onClick = {
                    onConfirm(PredictedGrade(selectedValue, isSmall))
                }) {
                    Text(stringResource(R.string.grade_predictor_add_button))
                }
            }
        }
    }
}

@Composable
private fun getGradeWord(grade: Grade) = when (grade.value)
{
    0    -> stringResource(R.string.grade_word_0)
    1    -> stringResource(R.string.grade_word_1)
    2    -> stringResource(R.string.grade_word_2)
    3    -> stringResource(R.string.grade_word_3)
    4    -> stringResource(R.string.grade_word_4)
    5    -> stringResource(R.string.grade_word_5)
    else -> throw IllegalArgumentException("Grade value must be between 0 and 5. (got ${grade.value})")
}

private object Constants
{
    val gradeWidth = 40.dp
    val gradeSmallHeight = 25.dp
    val gradeAverageDecimalFormat = DecimalFormat("#.##").apply {
        roundingMode = RoundingMode.HALF_UP
    }
    val gradeDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yyyy")
    val gradeShape = RoundedCornerShape(7.dp)
}
