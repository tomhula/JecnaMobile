package me.tomasan7.jecnamobile.news

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import coil.request.ImageRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.WebJecnaClient
import io.github.tomhula.jecnaapi.data.article.ArticleFile
import io.github.tomhula.jecnaapi.data.article.NewsPage
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.tomasan7.jecnamobile.LoginStateProvider
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.SubScreenCacheViewModel
import me.tomasan7.jecnamobile.caching.CacheRepository
import me.tomasan7.jecnamobile.caching.NoParams
import me.tomasan7.jecnamobile.util.CachedDataNew
import me.tomasan7.jecnamobile.util.createBroadcastReceiver
import java.io.File
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant


@HiltViewModel
class NewsViewModel @Inject constructor(
    @ApplicationContext
    appContext: Context,
    loginStateProvider: LoginStateProvider,
    repository: CacheRepository<NewsPage, NoParams>,
    private val jecnaClient: JecnaClient
) : SubScreenCacheViewModel<NewsPage, NoParams>(appContext, loginStateProvider, repository)
{
    override val parseErrorMessage = appContext.getString(R.string.error_unsupported_articles)
    override val loadErrorMessage = appContext.getString(R.string.article_load_error)

    var uiState by mutableStateOf(NewsState())
        private set

    private val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private val downloadFinishedBroadcastReceiver = createBroadcastReceiver { _, intent ->
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        openDownloadedFile(downloadId)
    }

    override fun setDataUiState(data: NewsPage) = changeUiState(
        newsPage = data,
        lastUpdateTimestamp = Clock.System.now(),
        isCache = false
    )
    
    override fun setCacheDataUiState(data: CachedDataNew<NewsPage, NoParams>) = changeUiState(
        newsPage = data.data,
        lastUpdateTimestamp = data.timestamp,
        isCache = true
    )

    override fun getParams() = NoParams
    
    override fun getLastUpdateTimestamp() = uiState.lastUpdateTimestamp
    override fun isCurrentlyShowingCache() = uiState.isCache
    override fun showSnackBarMessage(message: String) = changeUiState(snackBarMessageEvent = triggered(message))
    override fun setLoadingUiState(loading: Boolean) = changeUiState(loading = loading)

    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())

    override fun enteredComposition()
    {
        super.enteredComposition()
        appContext.registerReceiver(
            downloadFinishedBroadcastReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    fun downloadAndOpenArticleFile(articleFile: ArticleFile) = viewModelScope.launch {
        val url = WebJecnaClient.getUrlForPath(articleFile.downloadPath)
        val request = DownloadManager.Request(url.toUri()).apply {
            setTitle(articleFile.label)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, articleFile.filename)
            setMimeType(getMimeTypeByExtension(articleFile.fileExtension))

            val sessionCookie = getSessionCookie() ?: return@launch
            addRequestHeader("Cookie", sessionCookie.toHeaderString())
        }

        downloadManager.enqueue(request)
    }

    private fun getSessionCookieBlocking() = runBlocking { (jecnaClient as WebJecnaClient).getSessionCookie() }

    private suspend fun getSessionCookie() = (jecnaClient as WebJecnaClient).getSessionCookie()

    private fun Cookie.toHeaderString() = "$name=$value"

    @SuppressLint("Range")
    private fun openDownloadedFile(downloadId: Long)
    {
        val query = DownloadManager.Query().setFilterById(downloadId)

        val uri: Uri
        val mimeType: String

        downloadManager.query(query).use { cursor ->
            if (!cursor.moveToFirst())
                return

            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))

            if (status != DownloadManager.STATUS_SUCCESSFUL)
                return

            uri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)).toUri()
            mimeType = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE))
        }

        val providedUri =
            FileProvider.getUriForFile(appContext, appContext.packageName + ".fileprovider", File(uri.path!!))

        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(providedUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            /* So you can start the activity from app context. (normally it has to be done from activity context) */
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try
        {
            appContext.startActivity(openIntent)
        }
        catch (e: ActivityNotFoundException)
        {
            changeUiState(snackBarMessageEvent = triggered(appContext.getString(R.string.error_unable_to_open_file)))
        }
    }

    private fun getMimeTypeByExtension(extension: String) =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"

    fun createImageRequest(path: String) = ImageRequest.Builder(appContext).apply {
        data(WebJecnaClient.getUrlForPath(path))
        crossfade(true)
        val sessionCookie = getSessionCookieBlocking() ?: return@apply
        setHeader("Cookie", sessionCookie.toHeaderString())
        (jecnaClient as WebJecnaClient).userAgent?.let { setHeader("User-Agent", it) }
    }.build()
    
    private fun changeUiState(
        loading: Boolean = uiState.loading,
        newsPage: NewsPage? = uiState.newsPage,
        lastUpdateTimestamp: Instant? = uiState.lastUpdateTimestamp,
        isCache: Boolean = uiState.isCache,
        snackBarMessageEvent: StateEventWithContent<String> = uiState.snackBarMessageEvent,
    )
    {
        uiState = uiState.copy(
            loading = loading,
            newsPage = newsPage,
            lastUpdateTimestamp = lastUpdateTimestamp,
            isCache = isCache,
            snackBarMessageEvent = snackBarMessageEvent
        )
    }
}
