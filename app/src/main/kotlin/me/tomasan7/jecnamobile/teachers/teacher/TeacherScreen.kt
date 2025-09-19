package me.tomasan7.jecnamobile.teachers.teacher

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import de.palm.composestateevents.EventEffect
import me.tomasan7.jecnaapi.data.schoolStaff.Teacher
import me.tomasan7.jecnaapi.data.schoolStaff.TeacherReference
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.mainscreen.SubScreensNavGraph
import me.tomasan7.jecnamobile.ui.component.HorizontalSpacer
import me.tomasan7.jecnamobile.ui.component.Timetable
import me.tomasan7.jecnamobile.util.PullToRefreshHandler

@OptIn(ExperimentalMaterial3Api::class)
@Destination<SubScreensNavGraph>
@Composable
fun TeacherScreen(
    teacherReference: TeacherReference,
    viewModel: TeacherViewModel = hiltViewModel(),
    navigator: DestinationsNavigator
)
{
    DisposableEffect(Unit) {
        viewModel.enteredComposition(teacherReference)
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
        topBar = { TopAppBar(teacherReference.fullName, navigator::popBackStack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
                .padding(paddingValues)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(30.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (uiState.teacher != null)
                    TeacherPicture(
                        picturePath = uiState.teacher.profilePicturePath,
                        imageRequestCreator = viewModel::createImageRequest
                    )

                if (uiState.teacher != null)
                    InfoTable(uiState.teacher)

                uiState.teacher?.timetable?.let {
                    Text(
                        text = stringResource(R.string.teacher_title_timetable),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Surface(
                        tonalElevation = 1.dp,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Timetable(
                            timetable = it,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBar(
    title: String,
    onBackClick: () -> Unit = {},
)
{
    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
            }
        }
    )
}

@Composable
private fun TeacherPicture(
    picturePath: String?,
    modifier: Modifier = Modifier,
    imageRequestCreator: (String) -> ImageRequest
)
{
    Surface(
        modifier = modifier,
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(4.dp)
    ) {
        if (picturePath != null)
            AsyncImage(
                modifier = Modifier
                    .padding(12.dp)
                    .aspectRatio(200f / 257f)
                    .clip(RoundedCornerShape(4.dp)),
                model = imageRequestCreator(picturePath),
                contentDescription = null
            )
        else
            Box(
                modifier = Modifier
                    .padding(30.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.teacher_no_pfp))
            }
    }
}

@Composable
private fun InfoTable(teacher: Teacher)
{
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        InfoRow(R.string.teacher_name, teacher.fullName)
        InfoRow(R.string.teacher_tag, teacher.tag)
        InfoRow(R.string.teacher_school_mail, value = teacher.schoolMail)
        teacher.privateMail?.let {
            InfoRow(R.string.teacher_private_email, it)
        }
        if (teacher.phoneNumbers.isNotEmpty())
            InfoRow(R.string.teacher_phone, value = teacher.phoneNumbers.joinToString(", "))
        teacher.landline?.let {
            InfoRow(R.string.teacher_landline, value = it)
        }
        teacher.privatePhoneNumber?.let {
            InfoRow(R.string.teacher_private_phone, value = it)
        }
        teacher.cabinet?.let {
            InfoRow(R.string.teacher_cabinet, value = it)
        }
        teacher.tutorOfClass?.let {
            InfoRow(R.string.teacher_class, value = it)
        }
        teacher.consultationHours?.let {
            InfoRow(R.string.teacher_consultation_hours, value = it)
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
)
{
    Row(modifier.height(IntrinsicSize.Min)) {
        Surface(
            tonalElevation = 20.dp,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxHeight()
                .width(150.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        HorizontalSpacer(size = 5.dp)

        Surface(
            tonalElevation = 4.dp,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart
            ) {
                SelectionContainer {
                    Text(
                        text = value,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    @StringRes
    label: Int,
    value: String,
    modifier: Modifier = Modifier
) = InfoRow(stringResource(label), value, modifier)
