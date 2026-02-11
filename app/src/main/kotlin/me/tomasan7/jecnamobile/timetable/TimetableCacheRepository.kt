package me.tomasan7.jecnamobile.timetable

import android.content.Context
import kotlinx.serialization.serializer
import me.tomasan7.jecnamobile.caching.CacheRepository
import me.tomasan7.jecnamobile.caching.SchoolYearPeriodParams
import me.tomasan7.jecnamobile.util.CachedDataNew
import kotlin.time.Clock

class TimetableCacheRepository(
    key: String,
    appContext: Context,
    fetcher: suspend (SchoolYearPeriodParams) -> TimetableData
) : CacheRepository<TimetableData, SchoolYearPeriodParams>(
    appContext,
    key,
    serializer<TimetableData>(),
    serializer<SchoolYearPeriodParams>(),
    fetcher
)
{
    override suspend fun getCache(params: SchoolYearPeriodParams): CachedDataNew<TimetableData, SchoolYearPeriodParams>?
    {
        logReadingCache()
        val entireCache = loadEntireCache() ?: return null
        
        if (params.periodId != SchoolYearPeriodParams.CURRENT_PERIOD_ID) 
            return entireCache[params]
        
        val schoolYearTimetables = entireCache.filterKeys { it.schoolYear == params.schoolYear }
        val mostRecentOne = schoolYearTimetables.maxByOrNull { entry ->
            entry.value.data.page.periodOptions.find { it.selected }!!.from
        }?.value ?: return null
        
        return mostRecentOne
    }

    override suspend fun getRealAndCache(params: SchoolYearPeriodParams): TimetableData
    {
        logFetchingRealData()
        val data = fetcher(params)
        val entireCache = loadEntireCache()?.toMutableMap() ?: mutableMapOf()
        val actualParams = SchoolYearPeriodParams(data.page.selectedSchoolYear, data.page.periodOptions.find { it.selected }?.id ?: return data)
        entireCache[actualParams] = CachedDataNew(data, actualParams, timestamp = Clock.System.now())
        writeEntireCache(entireCache)
        return data
    }
}
