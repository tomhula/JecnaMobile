package me.tomasan7.jecnamobile.documents

import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.data.document.DocumentsPage
import javax.inject.Inject

class DocumentsRepositoryImpl @Inject constructor(
    private val client: JecnaClient
) : DocumentsRepository
{
    override suspend fun getDocumentsPage(): DocumentsPage = client.getDocumentsPage()
}
