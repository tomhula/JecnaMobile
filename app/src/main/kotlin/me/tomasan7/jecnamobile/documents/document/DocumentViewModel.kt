package me.tomasan7.jecnamobile.documents.document

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
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
}
