package me.tomasan7.jecnamobile.news

import io.github.tomhula.jecnaapi.JecnaClient

class NewsRepositoryImpl(
    private val client: JecnaClient
) : NewsRepository
{
    override suspend fun getNewsPage() = client.getNewsPage()
}
