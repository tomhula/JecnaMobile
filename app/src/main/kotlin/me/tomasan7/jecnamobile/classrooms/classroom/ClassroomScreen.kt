package me.tomasan7.jecnamobile.classrooms.classroom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import de.palm.composestateevents.EventEffect
import io.github.tomhula.jecnaapi.data.classroom.ClassroomReference
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.destinations.ClassroomScreenDestination
import me.tomasan7.jecnamobile.mainscreen.SubScreensNavGraph
import me.tomasan7.jecnamobile.ui.component.Timetable
import me.tomasan7.jecnamobile.destinations.TeacherScreenDestination
import me.tomasan7.jecnamobile.ui.component.InfoRow

@OptIn(ExperimentalMaterial3Api::class)
@Destination<SubScreensNavGraph>
@Composable
fun ClassroomScreen(
    classroomReference: ClassroomReference,
    viewModel: ClassroomViewModel = hiltViewModel(),
    navigator: DestinationsNavigator
)
{
    DisposableEffect(Unit) {
        viewModel.enteredComposition(classroomReference)
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
        topBar = { TopAppBar(classroomReference.title, navigator::popBackStack) },
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(30.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (uiState.classroom != null)
                {
                    val classroom = uiState.classroom

                    classroom.floor?.let {
                        InfoRow(R.string.classroom_floor, classroom.floor!!)
                    }
                    classroom.manager?.let {
                        InfoRow(R.string.classroom_manager, classroom.manager!!.fullName, Modifier.clickable(onClick = {
                            navigator.navigate(
                                TeacherScreenDestination(classroom.manager!!)
                            )
                        }))
                    }

                    classroom.mainClassroomOf?.let {
                        InfoRow(R.string.classroom_main_class, it)
                    }

                    classroom.timetable?.let()
                    {
                        Text(
                            text = stringResource(R.string.classroom_title_timetable),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Timetable(
                            timetable = classroom.timetable!!,
                            onTeacherClick = { navigator.navigate(TeacherScreenDestination(it)) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBar(
    title: String,
    onBackClick: () -> Unit
)
{
    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.close)
                )
            }
        }
    )
}
