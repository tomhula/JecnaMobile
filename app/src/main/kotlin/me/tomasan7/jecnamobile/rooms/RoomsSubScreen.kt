package me.tomasan7.jecnamobile.rooms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import de.palm.composestateevents.EventEffect
import io.github.tomhula.jecnaapi.data.room.RoomReference
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.destinations.RoomScreenDestination
import me.tomasan7.jecnamobile.mainscreen.NavDrawerController
import me.tomasan7.jecnamobile.mainscreen.SubScreensNavGraph
import me.tomasan7.jecnamobile.ui.component.SubScreenTopAppBar
import me.tomasan7.jecnamobile.ui.component.VerticalSpacer
import me.tomasan7.jecnamobile.ui.theme.teacher_search_query_highlight
import me.tomasan7.jecnamobile.util.removeAccent

@OptIn(ExperimentalMaterial3Api::class)
@Destination<SubScreensNavGraph>
@Composable
fun RoomsSubScreen(
    navDrawerController: NavDrawerController,
    navigator: DestinationsNavigator,
    viewModel: RoomsViewModel = hiltViewModel()
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
        topBar = { SubScreenTopAppBar(R.string.sidebar_classrooms, navDrawerController) },
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                FilterFieldRow(
                    value = uiState.filterFieldValue,
                    onValueChange = viewModel::onFilterFieldValueChange
                )

                VerticalSpacer(16.dp)

                uiState.RoomReferencesSortedFiltered?.forEach {
                    ClassroomCard(
                        RoomReference = it,
                        onClick = { navigator.navigate(RoomScreenDestination(it)) },
                        searchQuery = uiState.filterFieldValue,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                VerticalSpacer(16.dp)
            }
        }
    }
}

@Composable
private fun FilterFieldRow(
    value: String = "",
    onValueChange: (String) -> Unit = {}
)
{
    val trailingIcon: (@Composable () -> Unit)? = remember(value) {
        if (value.isNotEmpty())
        {
            {
                IconButton({ onValueChange("") }) {
                    Icon(imageVector = Icons.Outlined.Cancel, contentDescription = null)
                }
            }
        }
        else
            null
    }

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            modifier = Modifier,
            placeholder = { Text(stringResource(R.string.classrooms_search_placeholder)) },
            label = { Text(stringResource(R.string.teachers_search)) },
            value = value,
            singleLine = true,
            trailingIcon = trailingIcon,
            onValueChange = onValueChange
        )
    }
}

@Composable
fun ClassroomCard(
    RoomReference: RoomReference,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    onClick: () -> Unit = {}
)
{
    Surface(
        tonalElevation = 4.dp,
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(buildHighlightedAnnotatedString(text = RoomReference.name, searchQuery = searchQuery))
        }
    }
}

@Composable
private fun buildHighlightedAnnotatedString(
    text: String,
    searchQuery: String
): AnnotatedString
{
    val annotatedString = AnnotatedString.Builder(text)

    if (searchQuery.isNotEmpty())
    {
        val searchQueryRegex = Regex(searchQuery.removeAccent(), RegexOption.IGNORE_CASE)
        val match = searchQueryRegex.find(text.removeAccent()) ?: return annotatedString.toAnnotatedString()

        annotatedString.addStyle(
            style = SpanStyle(color = teacher_search_query_highlight),
            start = match.range.first,
            end = match.range.last + 1
        )
    }

    return annotatedString.toAnnotatedString()
}

