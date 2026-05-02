package me.tomasan7.jecnamobile.documents.document

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import me.tomasan7.jecnamobile.ui.component.LinearPullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import de.palm.composestateevents.EventEffect
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.ui.component.InfoRow
import me.tomasan7.jecnamobile.SubScreenViewModelHook

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScreen(
    path: String,
    onBackClick: () -> Unit,
    viewModel: DocumentViewModel = hiltViewModel(),
)
{
    SubScreenViewModelHook(
        key = path,
        onEnter = {
            viewModel.setDocumentPath(path)
            viewModel.enteredComposition()
        },
        onLeave = viewModel::leftComposition
    )

    val uiState = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    EventEffect(
        event = uiState.snackBarMessageEvent,
        onConsumed = viewModel::onSnackBarMessageEventConsumed
    ) {
        snackbarHostState.showSnackbar(it)
    }

    Scaffold(
        topBar = { TopAppBar(label = path, onBackClick = onBackClick) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LinearPullToRefreshBox(
            isRefreshing = uiState.loading,
            onRefresh = { viewModel.reload() },
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(30.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (uiState.document != null)
                {
                    InfoRow(
                        label = stringResource(R.string.document_name),
                        content = uiState.document.label
                    )

                    InfoRow(
                        label = stringResource(R.string.document_path),
                        content = uiState.document.path
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, uiState.document.path.toUri())
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                viewModel.onErrorOpeningFile()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.document_open))
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBar(
    label: String,
    onBackClick: () -> Unit
)
{
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = label,
                maxLines = 1,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        }
    )
}
