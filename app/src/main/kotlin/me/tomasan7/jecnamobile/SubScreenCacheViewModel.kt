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
import me.tomasan7.jecnamobile.util.CachedData
import me.tomasan7.jecnamobile.util.now
import kotlin.time.Instant

abstract class SubScreenCacheViewModel<T>(
    @ApplicationContext
    appContext: Context,
) : SubScreenViewModel<T>(appContext)
{
    override val noInternetConnectionMessage: String
        get() = if (getLastUpdateTimestamp() != null && isCurrentlyShowingCache())
            getOfflineMessage()!!
        else
            appContext.getString(R.string.no_internet_connection)
    
    protected var enteredCompositionCounter = 0

    abstract fun setCacheDataUiState(data: CachedData<T>)
    abstract fun getCache(): CachedData<T>?
    abstract fun isCacheAvailable(): Boolean
    abstract fun getLastUpdateTimestamp(): Instant?
    abstract fun isCurrentlyShowingCache(): Boolean
    
    override fun enteredComposition()
    {
        registerLoginBroadcastReceiver()
        if (enteredCompositionCounter == 0)
            loadCache()
        loadReal()
        enteredCompositionCounter++
    }

    fun loadCache()
    {
        if (!isCacheAvailable())
            return

        viewModelScope.launch {
            val cachedData = getCache() ?: return@launch
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
