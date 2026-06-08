package me.tomasan7.jecnamobile.util

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import io.github.tomhula.jecnaapi.WebJecnaClient
import me.tomasan7.jecnamobile.R
import java.io.File

class JecnaFileDownloader(
    private val appContext: Context,
    private val webJecnaClient: WebJecnaClient,
    private val onError: (message: String) -> Unit,
) {

    private val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private val broadcastReceiver = createBroadcastReceiver { _, intent ->
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (downloadId == -1L) return@createBroadcastReceiver
        
        val query = DownloadManager.Query().setFilterById(downloadId)
        
        downloadManager.query(query).use { cursor ->
            if (cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    val uri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)).toUri()
                    val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE))
                    
                    openFile(uri, mimeType).onFailure {
                        onError(it.message ?: "Unknown error")
                    }
                }
            }
        }
    }

    fun openFile(uri: Uri, mimeType: String): Result<Unit> {
        val providedUri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            File(uri.path!!)
        )

        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(providedUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            appContext.startActivity(openIntent)
            Result.success(Unit)
        } catch (e: ActivityNotFoundException) {
            Result.failure(Exception(appContext.getString(R.string.error_unable_to_open_file)))
        }
    }

    private var isRegistered = false

    fun register() {
        if (isRegistered) return
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Context.RECEIVER_EXPORTED else 0
        appContext.registerReceiver(
            broadcastReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            flags
        )
        isRegistered = true
    }

    fun unregister() {
        if (!isRegistered) return
        runCatching { appContext.unregisterReceiver(broadcastReceiver) }
        isRegistered = false
    }

    suspend fun downloadAndOpen(
        urlPath: String,
        filename: String,
        label: String,
        extension: String,
    ): Boolean {
        val url = webJecnaClient.getUrlForPath(urlPath)
        val cookies = buildCookiesString() ?: return false

        val request = DownloadManager.Request(url.toUri()).apply {
            setTitle(label)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            setMimeType(mimeTypeForExtension(extension))
            addRequestHeader("Cookie", cookies)
            addRequestHeader("User-Agent", USER_AGENT)
            addRequestHeader("Accept-Language", ACCEPT_LANGUAGE)
            addRequestHeader("Accept-Encoding", ACCEPT_ENCODING)
        }

        downloadManager.enqueue(request)
        return true
    }

    private suspend fun buildCookiesString(): String? {
        val cookies = listOfNotNull(
            webJecnaClient.getSessionCookie(),
            webJecnaClient.getCookie("WTDGUID")
        )

        if (cookies.isEmpty()) return null
        return cookies.joinToString("; ") { "${it.name}=${it.value}" }
    }

    private fun mimeTypeForExtension(extension: String) =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:147.0) Gecko/20100101 Firefox/147.0"
        private const val ACCEPT_LANGUAGE = "en-US,en;q=0.9"
        private const val ACCEPT_ENCODING = "gzip, deflate"
    }
}
