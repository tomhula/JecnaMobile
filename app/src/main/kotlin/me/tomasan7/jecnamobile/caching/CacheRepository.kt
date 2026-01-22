package me.tomasan7.jecnamobile.caching

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import me.tomasan7.jecnamobile.util.CachedDataNew
import java.io.File
import kotlin.time.Clock

private const val LOG_TAG = "CacheRepository"

class CacheRepository<T, P>(
    @param:ApplicationContext
    private val appContext: Context,
    val key: String,
    dataSerializer: KSerializer<T>,
    paramsSerializer: KSerializer<P>,
    private val fetcher: suspend (P) -> T
)
{
    private val entireCacheSerializer = MapSerializer(paramsSerializer, CachedDataNew.serializer(dataSerializer, paramsSerializer))
    
    private val cacheFile = File(appContext.cacheDir, "$key.json")
    
    fun isCacheAvailable() = cacheFile.exists()

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun getCache(params: P): CachedDataNew<T, P>?
    {
        Log.d(LOG_TAG, "$key: Reading cache")
        val entireCache = loadEntireCache()
        return entireCache?.get(params)
    }

    suspend fun getRealAndCache(params: P): T
    {
        Log.d(LOG_TAG, "$key: Fetching real data")
        val data = fetcher(params)
        val entireCache = loadEntireCache()?.toMutableMap() ?: mutableMapOf()
        entireCache[params] = CachedDataNew(data, params, timestamp = Clock.System.now())
        writeEntireCache(entireCache)
        return data
    }
    
    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun loadEntireCache(): Map<P, CachedDataNew<T, P>>?
    {
        if (!isCacheAvailable())
            return null

        return withContext(Dispatchers.IO) {
            val inputStream = cacheFile.inputStream()

            try
            {
                Json.decodeFromStream(entireCacheSerializer, inputStream)
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
    }
    
    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun writeEntireCache(entireCache: Map<P, CachedDataNew<T, P>>)
    {
        withContext(Dispatchers.IO) {
            val outputStream = cacheFile.outputStream()

            try
            {
                Json.encodeToStream(entireCacheSerializer, entireCache, outputStream)
            }
            catch (e: Exception)
            {
                e.printStackTrace()
            }
            finally
            {
                outputStream.close()
            }   
        }
    }
}
