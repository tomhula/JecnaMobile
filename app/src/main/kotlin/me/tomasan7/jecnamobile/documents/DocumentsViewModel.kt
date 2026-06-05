package me.tomasan7.jecnamobile.documents

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import coil.request.ImageRequest
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.WebJecnaClient
import io.github.tomhula.jecnaapi.data.document.DocumentFile
import io.github.tomhula.jecnaapi.data.document.DocumentsPage
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.tomasan7.jecnamobile.LoginStateProvider
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.SubScreenCacheViewModel
import me.tomasan7.jecnamobile.di.DocumentsCacheRepository
import me.tomasan7.jecnamobile.util.CachedDataNew
import me.tomasan7.jecnamobile.util.createBroadcastReceiver
import java.io.File
import kotlin.time.Clock
import kotlin.time.Instant

class DocumentsViewModel(
    private val initialPath: String,
    appContext: Context,
    loginStateProvider: LoginStateProvider,
    repository: DocumentsCacheRepository,
    private val jecnaClient: JecnaClient
) : SubScreenCacheViewModel<DocumentsPage, String>(appContext, loginStateProvider, repository)
{
    override val parseErrorMessage = appContext.getString(R.string.documents_parse_error)
    override val loadErrorMessage = appContext.getString(R.string.documents_load_error)

    var uiState by mutableStateOf(DocumentsState())
        private set

    private val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private val downloadFinishedBroadcastReceiver = createBroadcastReceiver { _, intent ->
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        openDownloadedFile(downloadId)
    }

    override fun getParams(): String
    {
        return initialPath
    }

    override fun setDataUiState(data: DocumentsPage)
    {
        changeUiState(
            documentsPage = data,
            lastUpdateTimestamp = Clock.System.now(),
            isCache = false
        )
    }

    override fun setCacheDataUiState(data: CachedDataNew<DocumentsPage, String>)
    {
        changeUiState(
            documentsPage = data.data,
            lastUpdateTimestamp = data.timestamp,
            isCache = true
        )
    }

    override fun getLastUpdateTimestamp() = uiState.lastUpdateTimestamp
    override fun isCurrentlyShowingReal() = uiState.documentsPage != null && !uiState.isCache
    override fun showSnackBarMessage(message: String) = changeUiState(snackBarMessageEvent = triggered(message))
    override fun setLoadingUiState(loading: Boolean) = changeUiState(loading = loading)

    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun enteredComposition()
    {
        super.enteredComposition()

        appContext.registerReceiver(
            downloadFinishedBroadcastReceiver,
            android.content.IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    override fun leftComposition()
    {
        super.leftComposition()
        runCatching { appContext.unregisterReceiver(downloadFinishedBroadcastReceiver) }
    }

    fun downloadAndOpenDocumentFile(documentFile: DocumentFile) = viewModelScope.launch {
        val webClient = jecnaClient as WebJecnaClient
        val url = webClient.getUrlForPath(documentFile.path)
        val filename = documentFile.path.split("/").lastOrNull() ?: documentFile.label
        val extension = filename.split(".").lastOrNull()?.lowercase() ?: ""

        val request = DownloadManager.Request(url.toUri()).apply {
            setTitle(documentFile.label)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            setMimeType(getMimeTypeByExtension(extension))

            val cookies = buildCookiesString(webClient) ?: return@launch
            addRequestHeader("Cookie", cookies)
            addRequestHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:147.0) Gecko/20100101 Firefox/147.0")
            addRequestHeader("Accept-Language", "en-US,en;q=0.9")
            addRequestHeader("Accept-Encoding", "gzip, deflate")
        }

        downloadManager.enqueue(request)
    }

    private suspend fun buildCookiesString(webClient: WebJecnaClient): String?
    {
        val cookies = mutableListOf<String>()

        val sessionCookie = webClient.getSessionCookie()
        if (sessionCookie != null)
            cookies.add(cookieToHeaderString(sessionCookie))

        val wtGuidCookie = webClient.getCookie("WTDGUID")
        if (wtGuidCookie != null)
            cookies.add(cookieToHeaderString(wtGuidCookie))

        if (cookies.isEmpty()) return null
        return cookies.joinToString("; ")
    }

    private fun cookieToHeaderString(cookie: Cookie) = "${cookie.name}=${cookie.value}"

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
        data((jecnaClient as WebJecnaClient).getUrlForPath(path))
        crossfade(true)
        val cookies = buildCookiesStringBlocking() ?: return@apply
        setHeader("Cookie", cookies)

        // Image requests require valid browser User-Agent and Accept-Language (see https://github.com/tomhula/JecnaMobile/issues/48)
        setHeader("Accept-Language", "en-US,en;q=0.9")
        setHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:147.0) Gecko/20100101 Firefox/147.0")
    }.build()

    private fun buildCookiesStringBlocking(): String? = runBlocking { buildCookiesString(jecnaClient as WebJecnaClient) }

    private fun changeUiState(
        loading: Boolean = uiState.loading,
        documentsPage: DocumentsPage? = uiState.documentsPage,
        lastUpdateTimestamp: Instant? = uiState.lastUpdateTimestamp,
        isCache: Boolean = uiState.isCache,
        snackBarMessageEvent: StateEventWithContent<String> = uiState.snackBarMessageEvent,
    )
    {
        uiState = uiState.copy(
            loading = loading,
            documentsPage = documentsPage,
            lastUpdateTimestamp = lastUpdateTimestamp,
            isCache = isCache,
            snackBarMessageEvent = snackBarMessageEvent
        )
    }
}
