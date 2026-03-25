package me.tomasan7.jecnamobile.student.cert

import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import io.github.tomhula.jecnaapi.data.cert.Certificate

@Immutable
data class StudentCertificatesState(
    val loading: Boolean = false,
    val certificates: List<Certificate>? = null,
    val snackBarMessageEvent: StateEventWithContent<String> = consumed()
)
