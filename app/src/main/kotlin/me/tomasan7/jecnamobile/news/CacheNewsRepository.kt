package me.tomasan7.jecnamobile.news

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import io.github.tomhula.jecnaapi.data.article.NewsPage
import me.tomasan7.jecnamobile.util.CachedData
import java.io.File
import javax.inject.Inject

class CacheNewsRepository @Inject constructor(
    @ApplicationContext
    private val appContext: Context,
    private val newsRepository: NewsRepository
)
{
    private val cacheFile = File(appContext.cacheDir, FILE_NAME)

    fun isCacheAvailable() = cacheFile.exists()

    @OptIn(ExperimentalSerializationApi::class)
    fun getCachedNews(): CachedData<NewsPage>?
    {
        if (!isCacheAvailable())
            return null

        val inputStream = cacheFile.inputStream()

        return try
        {
            Json.decodeFromStream(inputStream)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
            null
        }
        finally
        {
            inputStream.close()
        }
    }

    suspend fun getRealNews(): NewsPage
    {
        val newsPage = newsRepository.getNewsPage()
        cacheFile.writeText(Json.encodeToString(CachedData(newsPage)))
        return newsPage
    }

    companion object
    {
        private const val FILE_NAME = "news.json"
    }
}
