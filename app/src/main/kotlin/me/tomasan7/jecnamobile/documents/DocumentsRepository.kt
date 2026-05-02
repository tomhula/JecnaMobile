package me.tomasan7.jecnamobile.documents

import io.github.tomhula.jecnaapi.data.document.DocumentsPage

interface DocumentsRepository
{
    suspend fun getDocumentsPage(): DocumentsPage
}
