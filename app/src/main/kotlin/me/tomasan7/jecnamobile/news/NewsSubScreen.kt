package me.tomasan7.jecnamobile.news

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ireward.htmlcompose.HtmlText
import com.ramcosta.composedestinations.annotation.Destination
import de.palm.composestateevents.EventEffect
import io.github.tomhula.jecnaapi.data.article.Article
import io.github.tomhula.jecnaapi.data.article.ArticleFile
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.mainscreen.NavDrawerController
import me.tomasan7.jecnamobile.mainscreen.SubScreenDestination
import me.tomasan7.jecnamobile.mainscreen.SubScreensNavGraph
import me.tomasan7.jecnamobile.ui.component.Card
import me.tomasan7.jecnamobile.ui.component.ObjectDialog
import me.tomasan7.jecnamobile.ui.component.OfflineDataIndicator
import me.tomasan7.jecnamobile.ui.component.SubScreenTopAppBar
import me.tomasan7.jecnamobile.ui.component.rememberObjectDialogState
import me.tomasan7.jecnamobile.ui.theme.jm_label
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Destination<SubScreensNavGraph>
@Composable
fun NewsSubScreen(
    navDrawerController: NavDrawerController,
    viewModel: NewsViewModel = hiltViewModel()
)
{
    val uiState = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    val dialogState = rememberObjectDialogState<List<String>>()

    DisposableEffect(Unit) {
        viewModel.enteredComposition()
        onDispose { viewModel.leftComposition() }
    }

    EventEffect(
        event = uiState.snackBarMessageEvent,
        onConsumed = viewModel::onSnackBarMessageEventConsumed
    ) {
        snackbarHostState.showSnackbar(it)
    }

    Scaffold(
        topBar = {
            SubScreenTopAppBar(R.string.sidebar_news, navDrawerController) {
                OfflineDataIndicator(
                    modifier = Modifier.padding(end = 16.dp),
                    underlyingIcon = SubScreenDestination.News.iconSelected,
                    lastUpdateTimestamp = uiState.lastUpdateTimestamp,
                    visible = uiState.isCache
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
                if (uiState.newsPage != null)
                    uiState.newsPage.articles.forEach { article ->
                        Article(
                            article = article,
                            onImagesClick = { dialogState.show(it) },
                            onArticleFileClick = { viewModel.downloadAndOpenArticleFile(it) }
                        )
                    }

                Spacer(Modifier.height(16.dp))
            }

            ObjectDialog(
                state = dialogState,
                properties = DialogProperties(
                    usePlatformDefaultWidth = false
                ),
                onDismissRequest = dialogState::hide
            ) {
                dialogState.value?.let { images ->
                    ImagesDialogContent(
                        imageRequestCreator = viewModel::createImageRequest,
                        images = images
                    )
                }
            }
        }
    }
}

@Composable
private fun Article(
    article: Article,
    modifier: Modifier = Modifier,
    onImagesClick: (List<String>) -> Unit = {},
    onArticleFileClick: (ArticleFile) -> Unit
)
{
    Card(
        title = {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth()
            )
        },
        modifier = modifier
    ) {
        val context = LocalContext.current

        HtmlText(
            text = article.htmlContent,
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            linkClicked = {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(it)
                }
                context.startActivity(intent)
            }
        )

        if (article.images.isNotEmpty())
        {
            Spacer(Modifier.height(10.dp))
            ArticleImagesButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onImagesClick(article.images) }
            )
        }

        article.files.forEach { articleFile ->
            Spacer(Modifier.height(10.dp))
            ArticleFile(
                articleFile = articleFile,
                onClick = { onArticleFileClick(articleFile) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = if (article.schoolOnly)
                "${article.date.format(DATE_FORMATTER)} | ${article.author} | ${
                    stringResource(
                        R.string.article_school_only
                    )
                }"
            else
                "${article.date.format(DATE_FORMATTER)} | ${article.author}",
            style = MaterialTheme.typography.labelSmall,
            color = jm_label
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticleAdditionCard(
    label: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)
{
    Surface(
        modifier = modifier,
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(5.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Spacer(Modifier.width(10.dp))
            label()
        }
    }
}

@Composable
private fun BasicArticleAdditionCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = ArticleAdditionCard(
    label = { Text(label) },
    icon = { Icon(icon, contentDescription = null) },
    onClick = onClick,
    modifier = modifier
)

@Composable
private fun ArticleFile(
    articleFile: ArticleFile,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) = ArticleAdditionCard(
    label = {
        Text(
            text = buildAnnotatedString {
                append(articleFile.label)

                withStyle(SpanStyle(fontSize = 10.sp, color = jm_label)) {
                    append("." + articleFile.fileExtension)
                }
            }
        )
    },
    icon = { Icon(Icons.Default.Description, contentDescription = null) },
    onClick = onClick,
    modifier = modifier
)

@Composable
private fun ArticleImagesButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) = BasicArticleAdditionCard(
    label = stringResource(R.string.article_images),
    icon = Icons.Default.Image,
    onClick = onClick,
    modifier = modifier
)

@Composable
private fun ImagesDialogContent(
    imageRequestCreator: (String) -> ImageRequest,
    images: List<String>
)
{
    var currentImageIndex by remember(images) { mutableStateOf(0) }

    Column {
        Box(
            Modifier
                .height(IntrinsicSize.Min)
                .padding(30.dp)
                .clip(RoundedCornerShape(10.dp))
        ) {
            AsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                model = imageRequestCreator(images[currentImageIndex]),
                contentDescription = null,
            )

            if (currentImageIndex != 0)
                ImageDialogSideButton(
                    icon = Icons.AutoMirrored.Default.NavigateBefore,
                    onClick = { currentImageIndex = (currentImageIndex - 1).coerceAtLeast(0) },
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(100.dp)
                        .align(Alignment.CenterStart),
                )
            if (currentImageIndex != images.size - 1)
                ImageDialogSideButton(
                    icon = Icons.AutoMirrored.Default.NavigateNext,
                    onClick = { currentImageIndex = (currentImageIndex + 1).coerceAtMost(images.size - 1) },
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(100.dp)
                        .align(Alignment.CenterEnd),
                )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("${currentImageIndex + 1}/${images.size}")
        }
    }
}

@Composable
private fun ImageDialogSideButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
)
{
    Box(
        modifier = modifier.clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null)
    }
}

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("d. MMMM")
