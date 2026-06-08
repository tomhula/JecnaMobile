package me.tomasan7.jecnamobile.documents

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import coil.request.ImageRequest
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.WebJecnaClient
import io.github.tomhula.jecnaapi.data.document.DocumentFile
import io.github.tomhula.jecnaapi.data.document.DocumentsPage
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.SubScreenViewModel
import me.tomasan7.jecnamobile.util.FileDownloader

class DocumentsViewModel(
    private val initialPath: String,
    appContext: Context,
    private val jecnaClient: JecnaClient
) : SubScreenViewModel<DocumentsPage>(appContext)
{
    override val parseErrorMessage = appContext.getString(R.string.documents_parse_error)
    override val loadErrorMessage = appContext.getString(R.string.documents_load_error)

    var uiState by mutableStateOf(DocumentsState())
        private set

    private val fileDownloader = FileDownloader(
        appContext = appContext,
        webJecnaClient = jecnaClient as WebJecnaClient,
        onError = { message -> changeUiState(snackBarMessageEvent = triggered(message)) }
    )

    override suspend fun fetchRealData() = jecnaClient.getDocumentsPage(initialPath)

    override fun setDataUiState(data: DocumentsPage) = changeUiState(documentsPage = data)

    override fun showSnackBarMessage(message: String) = changeUiState(snackBarMessageEvent = triggered(message))

    override fun setLoadingUiState(loading: Boolean) = changeUiState(loading = loading)

    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())

    override fun enteredComposition()
    {
        super.enteredComposition()
        fileDownloader.register()
    }

    override fun leftComposition()
    {
        super.leftComposition()
        fileDownloader.unregister()
    }

    fun downloadAndOpenDocumentFile(documentFile: DocumentFile) = viewModelScope.launch {
        val filename = documentFile.path.split("/").lastOrNull() ?: documentFile.label
        val extension = filename.split(".").lastOrNull()?.lowercase() ?: ""

        val success = fileDownloader.downloadAndOpen(
            urlPath = documentFile.path,
            filename = filename,
            label = documentFile.label,
            extension = extension,
        )

        if (!success)
            changeUiState(snackBarMessageEvent = triggered(appContext.getString(R.string.error_unable_to_open_file)))
    }

    fun createImageRequest(path: String) = ImageRequest.Builder(appContext).apply {
        data((jecnaClient as WebJecnaClient).getUrlForPath(path))
        crossfade(true)
        val cookies = runBlocking { buildCookiesString() } ?: return@apply
        setHeader("Cookie", cookies)

        // Requests require valid browser User-Agent and Accept-Language (see https://github.com/tomhula/JecnaMobile/issues/48)
        setHeader("Accept-Language", "en-US,en;q=0.9")
        setHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:147.0) Gecko/20100101 Firefox/147.0")
    }.build()

    private suspend fun buildCookiesString(): String?
    {
        val webClient = jecnaClient as WebJecnaClient
        val cookies = listOfNotNull(
            webClient.getSessionCookie(),
            webClient.getCookie("WTDGUID")
        )

        if (cookies.isEmpty()) return null
        return cookies.joinToString("; ") { "${it.name}=${it.value}" }
    }

    private fun changeUiState(
        loading: Boolean = uiState.loading,
        documentsPage: DocumentsPage? = uiState.documentsPage,
        snackBarMessageEvent: StateEventWithContent<String> = uiState.snackBarMessageEvent,
    )
    {
        uiState = uiState.copy(
            loading = loading,
            documentsPage = documentsPage,
            snackBarMessageEvent = snackBarMessageEvent
        )
    }
}
