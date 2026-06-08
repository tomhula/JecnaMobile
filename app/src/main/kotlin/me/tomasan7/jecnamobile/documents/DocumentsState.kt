package me.tomasan7.jecnamobile.documents

import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import io.github.tomhula.jecnaapi.data.document.DocumentsPage

@Immutable
data class DocumentsState(
    val loading: Boolean = false,
    val documentsPage: DocumentsPage? = null,
    val snackBarMessageEvent: StateEventWithContent<String> = consumed()
)
