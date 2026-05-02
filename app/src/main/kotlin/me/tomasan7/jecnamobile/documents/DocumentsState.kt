package me.tomasan7.jecnamobile.documents

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import io.github.tomhula.jecnaapi.data.document.DocumentsPage
import kotlin.time.Instant

data class DocumentsState(
    val loading: Boolean = false,
    val documentsPage: DocumentsPage? = null,
    val lastUpdateTimestamp: Instant? = null,
    val isCache: Boolean = false,
    val snackBarMessageEvent: StateEventWithContent<String> = consumed()
)

