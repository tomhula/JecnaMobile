package me.tomasan7.jecnamobile.rooms.room

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import de.palm.composestateevents.EventEffect
import io.github.tomhula.jecnaapi.data.room.RoomReference
import io.github.tomhula.jecnaapi.data.schoolStaff.TeacherReference
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.ui.component.InfoRow
import me.tomasan7.jecnamobile.ui.component.Timetable
import me.tomasan7.jecnamobile.SubScreenViewModelHook

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomScreen(
    roomReference: RoomReference,
    onBackClick: () -> Unit,
    onTeacherClick: (TeacherReference) -> Unit,
    viewModel: RoomViewModel = hiltViewModel()
)
{
    SubScreenViewModelHook(
        key = viewModel,
        onEnter = {
            viewModel.setRoomReference(roomReference)
            viewModel.enteredComposition()
        },
        onLeave = viewModel::leftComposition
    )

    val uiState = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }

    EventEffect(
        event = uiState.snackBarMessageEvent,
        onConsumed = viewModel::onSnackBarMessageEventConsumed
    ) {
        snackbarHostState.showSnackbar(it)
    }

    Scaffold(
        topBar = { TopAppBar(roomReference.name, onBackClick) },
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
                if (uiState.room != null)
                {
                    val room = uiState.room

                    room.floor?.let {
                        InfoRow(R.string.room_floor, room.floor!!)
                    }
                    room.manager?.let {
                        InfoRow(R.string.room_manager, room.manager!!.fullName, Modifier.clickable(onClick = {
                            onTeacherClick(room.manager!!)
                        }))
                    }

                    room.homeroomOf?.let {
                        InfoRow(R.string.room_main_class, it)
                    }

                    room.timetable?.let()
                    {
                        Text(
                            text = stringResource(R.string.room_title_timetable),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Timetable(
                            timetable = room.timetable!!,
                            onTeacherClick = { onTeacherClick(it) })
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
