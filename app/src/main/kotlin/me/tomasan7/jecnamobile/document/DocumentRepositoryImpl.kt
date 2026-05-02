package me.tomasan7.jecnamobile.document

import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.data.document.DocumentsPage
import javax.inject.Inject

class DocumentRepositoryImpl @Inject constructor(
    private val client: JecnaClient
) : DocumentRepository
{
    override suspend fun getDocumentsPage(): DocumentsPage = client.getDocumentsPage()
}
