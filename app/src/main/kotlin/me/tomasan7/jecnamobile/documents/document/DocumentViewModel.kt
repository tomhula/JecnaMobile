package me.tomasan7.jecnamobile.documents.document

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.github.tomhula.jecnaapi.data.document.DocumentFile
import io.github.tomhula.jecnaapi.data.document.SchoolDocument
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.SubScreenViewModel
import me.tomasan7.jecnamobile.documents.DocumentsRepository
import javax.inject.Inject

class DocumentViewModel @Inject constructor(
    @ApplicationContext
    appContext: Context,
    private val repository: DocumentsRepository
) : SubScreenViewModel<SchoolDocument>(appContext)
{
    override val parseErrorMessage = appContext.getString(R.string.error_unsupported_document)
    override val loadErrorMessage = appContext.getString(R.string.document_load_error)
    override suspend fun fetchRealData(): DocumentFile = throw NotImplementedError()

    override fun showSnackBarMessage(message: String) = changeUiState(snackBarMessageEvent = triggered(message))

    override fun setLoadingUiState(loading: Boolean) = changeUiState(loading = loading)

    override fun setDataUiState(data: SchoolDocument) = changeUiState(document = data)

    var uiState by mutableStateOf(DocumentState())
        private set
    
    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())
    
    private fun changeUiState(
        loading: Boolean = uiState.loading,
        document: SchoolDocument? = uiState.document,
        snackBarMessageEvent: StateEventWithContent<String> = uiState.snackBarMessageEvent,
    ){
        uiState = uiState.copy(
            loading = loading,
            document = document,
            snackBarMessageEvent = snackBarMessageEvent
        )
    }
}
