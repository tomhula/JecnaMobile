package me.tomasan7.jecnamobile.documents

import io.github.tomhula.jecnaapi.data.document.DocumentFile
import io.github.tomhula.jecnaapi.data.document.DocumentFolder
import io.github.tomhula.jecnaapi.data.document.DocumentsPage
import io.github.tomhula.jecnaapi.data.document.SchoolDocument
import java.nio.file.Path

interface DocumentsRepository
{
    suspend fun getDocumentsPage(): DocumentsPage
    suspend fun getDocumentsPage(path: String): DocumentsPage
    suspend fun getDocument(path: Path): DocumentFile
    suspend fun getDocumentFolder(path: Path): DocumentFolder
}
