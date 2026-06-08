package me.tomasan7.jecnamobile.documents

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.tomhula.jecnaapi.JecnaClient
import me.tomasan7.jecnamobile.SubScreenViewModelHook
import me.tomasan7.jecnamobile.ui.component.LinearPullToRefreshBox
import me.tomasan7.jecnamobile.ui.component.SubScreenTopAppBar
import me.tomasan7.jecnamobile.navigation.LocalNavDrawerHandle
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.ui.ElevationLevel
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import de.palm.composestateevents.EventEffect
import io.github.tomhula.jecnaapi.data.document.DocumentFile
import io.github.tomhula.jecnaapi.data.document.DocumentFolder
import me.tomasan7.jecnamobile.ui.component.ObjectDialog
import me.tomasan7.jecnamobile.ui.component.rememberObjectDialogState
import org.koin.compose.koinInject

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsSubScreen(
    path: String = "/dokumenty",
    onNavigateToFolder: (String) -> Unit,
    onBackClick: (() -> Unit)? = null,
)
{
    val appContext: Context = koinInject()
    val jecnaClient: JecnaClient = koinInject()

    val viewModel: DocumentsViewModel = viewModel(
        key = path,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DocumentsViewModel(
                    initialPath = path,
                    appContext = appContext,
                    jecnaClient = jecnaClient
                ) as T
        }
    )

    SubScreenViewModelHook(
        key = path,
        onEnter = { viewModel.enteredComposition() },
        onLeave = viewModel::leftComposition
    )

    val uiState = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    val imagePreviewDialogState = rememberObjectDialogState<DocumentFile>()

    EventEffect(
        event = uiState.snackBarMessageEvent,
        onConsumed = viewModel::onSnackBarMessageEventConsumed
    ) {
        snackbarHostState.showSnackbar(it)
    }

    val isRoot = path == "/dokumenty"

    Scaffold(
        topBar = {
            if (isRoot)
            {
                SubScreenTopAppBar(R.string.sidebar_documents, LocalNavDrawerHandle.current)
            }
            else
            {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            uiState.documentsPage?.let { folderNameFromPath(it.path) }
                                ?: stringResource(R.string.sidebar_documents)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            onBackClick?.invoke()
                        }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                        }
                    }
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val documents = uiState.documentsPage?.documents
                val doneLoading = !uiState.loading

                if (doneLoading && documents.isNullOrEmpty())
                {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.documents_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else if (!documents.isNullOrEmpty())
                {
                    documents.forEach { document ->
                        when (document)
                        {
                            is DocumentFolder -> {
                                val folderPath = document.path
                                FolderCard(
                                    folder = document,
                                    onClick = {
                                        onNavigateToFolder(folderPath)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            is DocumentFile -> FileCard(
                                file = document,
                                onClick = {
                                    if (isImageFile(document))
                                        imagePreviewDialogState.show(document)
                                    else
                                        viewModel.downloadAndOpenDocumentFile(document)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    ObjectDialog(
        state = imagePreviewDialogState,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = imagePreviewDialogState::hide
    ) {
        ImagePreviewContent(
            imageRequest = viewModel.createImageRequest(it.path),
            onDismiss = imagePreviewDialogState::hide
        )
    }
}

@Composable
private fun ImagePreviewContent(
    imageRequest: ImageRequest,
    onDismiss: () -> Unit
)
{
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun FolderCard(
    folder: DocumentFolder,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
)
{
    Surface(
        modifier = modifier,
        tonalElevation = ElevationLevel.level1,
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )

            Spacer(Modifier.width(16.dp))

            Text(
                text = folder.label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Default.NavigateNext,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FileCard(
    file: DocumentFile,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
)
{
    Surface(
        modifier = modifier,
        tonalElevation = ElevationLevel.level1,
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(28.dp)
            )

            Spacer(Modifier.width(16.dp))

            Text(
                text = file.label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg")

private fun isImageFile(file: DocumentFile): Boolean
{
    val extension = file.path.split(".").lastOrNull()?.lowercase() ?: return false
    return extension in IMAGE_EXTENSIONS
}

private fun folderNameFromPath(path: String): String
{
    val segments = path.trim('/').split("/")
    return if (segments.isNotEmpty() && segments.last() != "dokumenty") segments.last() else path
}
