package me.tomasan7.jecnamobile.news

import io.github.tomhula.jecnaapi.data.article.NewsPage

interface NewsRepository
{
    suspend fun getNewsPage(): NewsPage
}
