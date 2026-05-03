package me.tomasan7.jecnamobile.documents.folder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import me.tomasan7.jecnamobile.ui.component.LinearPullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import de.palm.composestateevents.EventEffect
import io.github.tomhula.jecnaapi.data.document.DocumentFile
import io.github.tomhula.jecnaapi.data.document.DocumentFolder
import io.github.tomhula.jecnaapi.data.document.SchoolDocument
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.SubScreenViewModelHook

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    path: String,
    onBackClick: () -> Unit,
    onFolderClick: (String) -> Unit,
    viewModel: FolderViewModel = hiltViewModel(),
)
{
    SubScreenViewModelHook(
        key = path,
        onEnter = {
            viewModel.setFolderPath(path)
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.documentsPage != null)
                {
                    uiState.documentsPage.documents.forEach { document ->
                        DocumentItem(
                            document = document,
                            onFileClick = { file ->
                                viewModel.downloadAndOpenDocument(file.path)
                            },
                            onFolderClick = onFolderClick
                        )
                    }
                }
                else if (!uiState.loading)
                {
                    Text(
                        text = stringResource(R.string.no_documents),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
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

@Composable
private fun DocumentItem(
    document: SchoolDocument,
    onFileClick: (DocumentFile) -> Unit,
    onFolderClick: (String) -> Unit = {}
)
{
    when (document)
    {
        is DocumentFile -> {
            FileItem(file = document, onFileClick = onFileClick)
        }
        is DocumentFolder -> {
            FolderItem(folder = document, onFolderClick = onFolderClick)
        }
    }
}

@Composable
private fun FileItem(
    file: DocumentFile,
    onFileClick: (DocumentFile) -> Unit
)
{
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFileClick(file) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = file.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = file.path,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FolderItem(
    folder: DocumentFolder,
    onFolderClick: (String) -> Unit = {}
)
{
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFolderClick(folder.path) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = folder.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = folder.path,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
