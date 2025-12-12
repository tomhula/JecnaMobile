package me.tomasan7.jecnamobile.news

import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import io.github.tomhula.jecnaapi.data.article.NewsPage
import java.time.Instant

@Immutable
data class NewsState(
    val loading: Boolean = false,
    val newsPage: NewsPage? = null,
    val lastUpdateTimestamp: Instant? = null,
    val isCache: Boolean = false,
    val snackBarMessageEvent: StateEventWithContent<String> = consumed()
)
