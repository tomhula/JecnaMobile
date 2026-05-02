package me.tomasan7.jecnamobile.document

import io.github.tomhula.jecnaapi.data.document.DocumentsPage

interface DocumentRepository
{
    suspend fun getDocuments(): DocumentsPage
}
