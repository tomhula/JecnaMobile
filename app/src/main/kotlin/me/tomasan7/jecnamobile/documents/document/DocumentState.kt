package me.tomasan7.jecnamobile.documents.document

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import io.github.tomhula.jecnaapi.data.document.DocumentFile

data class DocumentState(
    val loading: Boolean = false,
    val document: DocumentFile? = null,
    val snackBarMessageEvent: StateEventWithContent<String> = consumed()
)
