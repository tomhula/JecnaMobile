package me.tomasan7.jecnamobile

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import me.tomasan7.jecnamobile.caching.CacheRepository
import me.tomasan7.jecnamobile.util.CachedDataNew
import me.tomasan7.jecnamobile.util.createBroadcastReceiver
import me.tomasan7.jecnamobile.util.now
import kotlin.time.Instant

private const val SSCVM_LOG_TAG = "SubScreenCacheViewModel"

abstract class SubScreenCacheViewModel<T, P>(
    @ApplicationContext
    appContext: Context,
    protected val loginStateProvider: LoginStateProvider,
    private val cacheRepository: CacheRepository<T, P>
) : SubScreenViewModel<T>(appContext)
{
    override val noInternetConnectionMessage: String
        get() = if (getLastUpdateTimestamp() != null && isCurrentlyShowingReal())
            getOfflineMessage()!!
        else
            appContext.getString(R.string.no_internet_connection)
    
    protected var enteredCompositionCounter = 0
    
    override val loginBroadcastReceiver = createBroadcastReceiver { _, _ ->
        if (loadRealJob != null && loadRealJob!!.isActive)
            return@createBroadcastReceiver
        
        if (!isCurrentlyShowingReal())
            loadReal()
    }

    abstract fun setCacheDataUiState(data: CachedDataNew<T, P>)
    abstract fun getLastUpdateTimestamp(): Instant?
    abstract fun isCurrentlyShowingReal(): Boolean
    abstract fun getParams(): P

    override suspend fun fetchRealData() = cacheRepository.getRealAndCache(getParams())
    
    override fun enteredComposition()
    {
        Log.d(SSCVM_LOG_TAG, "${this::class.simpleName}: Entered composition")
        registerLoginBroadcastReceiver()
        if (enteredCompositionCounter == 0)
        {
            if (loginStateProvider.afterFirstLogin)
                loadReal()
            loadCache()
        }
        else if (!isCurrentlyShowingReal() && loginStateProvider.afterFirstLogin)
            loadReal()
        
        enteredCompositionCounter++
    }

    override fun leftComposition()
    {
        Log.d(SSCVM_LOG_TAG, "${this::class.simpleName}: Left composition")
        loadRealJob?.cancel()
        unregisterLoginBroadcastReceiver()
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
            char('.')
            monthNumber(padding = Padding.NONE)
            char('.')
        }
    }
}
