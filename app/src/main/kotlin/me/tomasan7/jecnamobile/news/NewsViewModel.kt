package me.tomasan7.jecnamobile.news

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
import io.github.tomhula.jecnaapi.data.article.ArticleFile
import io.github.tomhula.jecnaapi.data.article.NewsPage
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.tomasan7.jecnamobile.LoginStateProvider
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.SubScreenCacheViewModel
import me.tomasan7.jecnamobile.caching.NoParams
import me.tomasan7.jecnamobile.di.NewsCacheRepository
import me.tomasan7.jecnamobile.util.CachedDataNew
import me.tomasan7.jecnamobile.util.JecnaFileDownloader
import kotlin.time.Clock
import io.ktor.http.Cookie
import kotlin.time.Instant

class NewsViewModel(
    appContext: Context,
    loginStateProvider: LoginStateProvider,
    repository: NewsCacheRepository,
    private val jecnaClient: JecnaClient
) : SubScreenCacheViewModel<NewsPage, NoParams>(appContext, loginStateProvider, repository)
{
    override val parseErrorMessage = appContext.getString(R.string.error_unsupported_articles)
    override val loadErrorMessage = appContext.getString(R.string.article_load_error)

    var uiState by mutableStateOf(NewsState())
        private set

    private val jecnaFileDownloader = JecnaFileDownloader(
        appContext = appContext,
        webJecnaClient = jecnaClient as WebJecnaClient,
        onError = { message -> changeUiState(snackBarMessageEvent = triggered(message)) }
    )

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
    override fun isCurrentlyShowingReal() = uiState.newsPage != null && !uiState.isCache
    override fun showSnackBarMessage(message: String) = changeUiState(snackBarMessageEvent = triggered(message))
    override fun setLoadingUiState(loading: Boolean) = changeUiState(loading = loading)

    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())

    override fun enteredComposition()
    {
        super.enteredComposition()
        jecnaFileDownloader.register()
    }

    override fun leftComposition()
    {
        super.leftComposition()
        jecnaFileDownloader.unregister()
    }

    fun downloadAndOpenArticleFile(articleFile: ArticleFile) = viewModelScope.launch {
        val success = jecnaFileDownloader.downloadAndOpen(
            urlPath = articleFile.downloadPath,
            filename = articleFile.filename,
            label = articleFile.label,
            extension = articleFile.fileExtension,
        )

        if (!success)
            changeUiState(snackBarMessageEvent = triggered(appContext.getString(R.string.error_unable_to_open_file)))
    }

    fun createImageRequest(path: String) = ImageRequest.Builder(appContext).apply {
        data((jecnaClient as WebJecnaClient).getUrlForPath(path))
        crossfade(true)
        val sessionCookie = getSessionCookieBlocking() ?: return@apply
        setHeader("Cookie", sessionCookie.toHeaderString())

        // News image requests require valid browser User-Agent and Accept-Language (see https://github.com/tomhula/JecnaMobile/issues/48)
        setHeader("Accept-Language", "en-US,en;q=0.9")
        setHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:147.0) Gecko/20100101 Firefox/147.0")
    }.build()

    private fun getSessionCookieBlocking() = runBlocking { (jecnaClient as WebJecnaClient).getSessionCookie() }

    private fun Cookie.toHeaderString() = "$name=$value"

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
