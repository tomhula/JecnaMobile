package me.tomasan7.jecnamobile.documents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowRight
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
import io.github.tomhula.jecnaapi.data.document.DocumentsPage
import io.github.tomhula.jecnaapi.data.document.SchoolDocument
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.navigation.NavDrawerDestination
import me.tomasan7.jecnamobile.ui.component.*
import me.tomasan7.jecnamobile.SubScreenViewModelHook
import me.tomasan7.jecnamobile.navigation.LocalNavDrawerHandle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsSubScreen(
    viewModel: DocumentsViewModel = hiltViewModel()
)
{
    SubScreenViewModelHook(viewModel)

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
            SubScreenTopAppBar(R.string.sidebar_documents, LocalNavDrawerHandle.current) {
                OfflineDataIndicator(
                    modifier = Modifier.padding(end = 16.dp),
                    underlyingIcon = NavDrawerDestination.Documents.iconSelected,
                    lastUpdateTimestamp = uiState.lastUpdateTimestamp,
                    visible = uiState.isCache
                )
            }
        },
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
                        DocumentItemWithExpand(
                            document = document,
                            onFileClick = { file ->
                                viewModel.downloadAndOpenDocument(file.path)
                            },
                            onFolderClick = { path ->
                                viewModel.toggleFolderExpansion(path)
                            },
                            expandedFolders = uiState.expandedFolders,
                            loadingFolders = uiState.loadingFolders,
                            depth = 0
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

@Composable
private fun DocumentItemWithExpand(
    document: SchoolDocument,
    onFileClick: (DocumentFile) -> Unit,
    onFolderClick: (String) -> Unit,
    expandedFolders: Map<String, DocumentsPage> = emptyMap(),
    loadingFolders: Set<String> = emptySet(),
    depth: Int = 0
)
{
    when (document) {
        is DocumentFile -> {
            FileItem(file = document, onFileClick = onFileClick, depth = depth)
        }
        is DocumentFolder -> {
            val isExpanded = expandedFolders.containsKey(document.path)
            val isLoading = loadingFolders.contains(document.path)
            val expandedContent = expandedFolders[document.path]
            
            Column {
                FolderItem(
                    folder = document,
                    onFolderClick = onFolderClick,
                    isExpanded = isExpanded,
                    isLoading = isLoading,
                    depth = depth
                )
                
                if (isExpanded && expandedContent != null) {
                    expandedContent.documents.forEach { nestedDoc ->
                        DocumentItemWithExpand(
                            document = nestedDoc,
                            onFileClick = onFileClick,
                            onFolderClick = onFolderClick,
                            expandedFolders = expandedFolders,
                            loadingFolders = loadingFolders,
                            depth = depth + 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileItem(
    file: DocumentFile,
    onFileClick: (DocumentFile) -> Unit,
    depth: Int = 0
)
{
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFileClick(file) }
            .padding(start = (12 + depth * 16).dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
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
    onFolderClick: (String) -> Unit,
    expandedFolders: Map<String, DocumentsPage> = emptyMap(),
    loadingFolders: Set<String> = emptySet(),
    depth: Int = 0
)
{
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFolderClick(folder.path) }
            .padding(start = (12 + depth * 16).dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Chevron icon
        Icon(
            imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowRight,
            contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        
        // Folder icon
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
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = folder.path,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
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
