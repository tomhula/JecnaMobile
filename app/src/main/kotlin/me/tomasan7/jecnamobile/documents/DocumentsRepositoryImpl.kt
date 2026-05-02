package me.tomasan7.jecnamobile.documents

import android.R.attr.path
import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.data.document.DocumentFile
import io.github.tomhula.jecnaapi.data.document.DocumentFolder
import io.github.tomhula.jecnaapi.data.document.DocumentsPage
import java.nio.file.Path
import javax.inject.Inject

class DocumentsRepositoryImpl @Inject constructor(
    private val client: JecnaClient
) : DocumentsRepository
{
    override suspend fun getDocumentsPage(): DocumentsPage = client.getDocumentsPage()
    override suspend fun getDocumentFolder(path: Path) : DocumentFolder = client.getDocumentsPage().documents.find { it.path == path.toString() } as DocumentFolder
    override suspend fun getDocument(path: Path): DocumentFile = client.getDocumentsPage().documents.find { it.path == path.toString() } as DocumentFile
}
