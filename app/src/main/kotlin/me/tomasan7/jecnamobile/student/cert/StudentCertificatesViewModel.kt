package me.tomasan7.jecnamobile.student.cert

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.github.tomhula.jecnaapi.data.cert.Certificate
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.SubScreenViewModel
import me.tomasan7.jecnamobile.student.StudentProfileRepository


class StudentCertificatesViewModel(
    appContext: Context,
    private val repository: StudentProfileRepository
) : SubScreenViewModel<List<Certificate>?>(appContext)
{
    override val parseErrorMessage = appContext.getString(R.string.student_certifications_load_error)
    override val loadErrorMessage = appContext.getString(R.string.student_certifications_load_error)

    var uiState by mutableStateOf(StudentCertificatesState())
        private set

    override fun setDataUiState(data: List<Certificate>?) = changeUiState(certificates = data)

    override suspend fun fetchRealData() = repository.getCertificates()

    override fun showSnackBarMessage(message: String) = changeUiState(snackBarMessageEvent = triggered(message))

    override fun setLoadingUiState(loading: Boolean) = changeUiState(loading = loading)

    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())

    fun changeUiState(
        loading: Boolean = uiState.loading,
        certificates: List<Certificate>? = uiState.certificates,
        snackBarMessageEvent: StateEventWithContent<String> = uiState.snackBarMessageEvent
    )
    {
        uiState = StudentCertificatesState(
            loading = loading,
            certificates = certificates,
            snackBarMessageEvent = snackBarMessageEvent
        )
    }
}
