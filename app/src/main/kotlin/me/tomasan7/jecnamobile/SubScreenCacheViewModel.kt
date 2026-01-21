package me.tomasan7.jecnamobile

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import me.tomasan7.jecnamobile.caching.CacheRepository
import me.tomasan7.jecnamobile.util.CachedDataNew
import me.tomasan7.jecnamobile.util.now
import kotlin.time.Instant

abstract class SubScreenCacheViewModel<T, P>(
    @ApplicationContext
    appContext: Context,
    private val cacheRepository: CacheRepository<T, P>
) : SubScreenViewModel<T>(appContext)
{
    override val noInternetConnectionMessage: String
        get() = if (getLastUpdateTimestamp() != null && isCurrentlyShowingCache())
            getOfflineMessage()!!
        else
            appContext.getString(R.string.no_internet_connection)
    
    protected var enteredCompositionCounter = 0

    abstract fun setCacheDataUiState(data: CachedDataNew<T, P>)
    abstract fun getLastUpdateTimestamp(): Instant?
    abstract fun isCurrentlyShowingCache(): Boolean
    abstract fun getParams(): P

    override suspend fun fetchRealData() = cacheRepository.getRealAndCache(getParams())
    
    override fun enteredComposition()
    {
        registerLoginBroadcastReceiver()
        if (enteredCompositionCounter == 0)
        {
            loadReal()
            loadCache()
        }
        else if (isCurrentlyShowingCache())
            loadReal()
        
        enteredCompositionCounter++
    }

    fun loadCache()
    {
        if (!cacheRepository.isCacheAvailable())
            return

        viewModelScope.launch {
            val cachedData = cacheRepository.getCache(getParams()) ?: return@launch
            setCacheDataUiState(cachedData)
        }
    }

    private fun getOfflineMessage(): String?
    {
        val cacheTimestamp = getLastUpdateTimestamp() ?: return null
        val localDateTime = cacheTimestamp.toLocalDateTime(TimeZone.currentSystemDefault())
        val localDate = localDateTime.date

        return if (localDate == LocalDate.now())
        {
            val timeStr = localDateTime.time.format(OFFLINE_MESSAGE_TIME_FORMATTER)
            appContext.getString(R.string.showing_offline_data_time, timeStr)
        }
        else
        {
            val dateStr = localDate.format(OFFLINE_MESSAGE_DATE_FORMATTER)
            appContext.getString(R.string.showing_offline_data_date, dateStr)
        }
    }

    companion object
    {
        val OFFLINE_MESSAGE_TIME_FORMATTER = LocalTime.Format {
            hour(padding = Padding.ZERO)
            char(':')
            minute(padding = Padding.ZERO)
        }
        val OFFLINE_MESSAGE_DATE_FORMATTER = LocalDate.Format {
            day(padding = Padding.NONE)
            chars(". ")
            monthNumber(padding = Padding.NONE)
        }
    }
}
