package me.tomasan7.jecnamobile.documents

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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.WebJecnaClient
import io.github.tomhula.jecnaapi.data.document.DocumentsPage
import kotlinx.coroutines.launch
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.SubScreenViewModel
import me.tomasan7.jecnamobile.util.createBroadcastReceiver
import java.io.File
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    @ApplicationContext
    appContext: Context,
    private val repository: DocumentsRepository,
    private val jecnaClient: JecnaClient
) : SubScreenViewModel<DocumentsPage>(appContext)
{
    override val parseErrorMessage = appContext.getString(R.string.error_unsupported_document)
    override val loadErrorMessage = appContext.getString(R.string.document_load_error)
    
    var uiState by mutableStateOf(DocumentsState())
        private set
    
    private val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private val downloadFinishedBroadcastReceiver = createBroadcastReceiver { _, intent ->
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        openDownloadedFile(downloadId)
    }
    
    override suspend fun fetchRealData(): DocumentsPage = repository.getDocumentsPage()
    
    override fun showSnackBarMessage(message: String) = changeUiState(snackBarMessageEvent = triggered(message))
    
    override fun setLoadingUiState(loading: Boolean) = changeUiState(loading = loading)
    
    override fun setDataUiState(data: DocumentsPage) = changeUiState(
        documentsPage = data,
        lastUpdateTimestamp = Clock.System.now(),
        isCache = false
    )
    
    override fun enteredComposition() {
        super.enteredComposition()
        appContext.registerReceiver(
            downloadFinishedBroadcastReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    override fun leftComposition() {
        super.leftComposition()
        appContext.unregisterReceiver(downloadFinishedBroadcastReceiver)
    }
    
    fun downloadAndOpenDocument(path: String) = viewModelScope.launch {
        val url = (jecnaClient as WebJecnaClient).getUrlForPath(path)
        val filename = path.substringAfterLast("/")
        val extension = filename.substringAfterLast(".", "")
        
        val request = DownloadManager.Request(url.toUri()).apply {
            setTitle(filename)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            setMimeType(getMimeTypeByExtension(extension))
            
            val sessionCookie = (jecnaClient as WebJecnaClient).getSessionCookie()
            if (sessionCookie != null) {
                addRequestHeader("Cookie", sessionCookie.toHeaderString())
            }
        }

        downloadManager.enqueue(request)
    }

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

    private data class Cookie(val name: String, val value: String) {
        fun toHeaderString() = "$name=$value"
    }
    
    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())
    
    private fun changeUiState(
        loading: Boolean = uiState.loading,
        documentsPage: DocumentsPage? = uiState.documentsPage,
        lastUpdateTimestamp: Instant? = uiState.lastUpdateTimestamp,
        isCache: Boolean = uiState.isCache,
        snackBarMessageEvent: StateEventWithContent<String> = uiState.snackBarMessageEvent,
    ){
        uiState = uiState.copy(
            loading = loading,
            documentsPage = documentsPage,
            lastUpdateTimestamp = lastUpdateTimestamp,
            isCache = isCache,
            snackBarMessageEvent = snackBarMessageEvent
        )
    }
}
