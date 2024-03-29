package me.tomasan7.jecnamobile.timetable

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import me.tomasan7.jecnaapi.data.timetable.TimetablePage
import me.tomasan7.jecnaapi.util.SchoolYear
import me.tomasan7.jecnamobile.util.CachedData
import java.io.File
import javax.inject.Inject

class CacheTimetableRepository @Inject constructor(
    @ApplicationContext
    private val appContext: Context,
    private val timetableRepository: TimetableRepository
)
{
    private val cacheFile = File(appContext.cacheDir, FILE_NAME)

    fun isCacheAvailable() = cacheFile.exists()

    @OptIn(ExperimentalSerializationApi::class)
    fun getCachedTimetable(): CachedData<TimetablePage>?
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

    suspend fun getRealTimetable(): TimetablePage
    {
        val timetablePage = timetableRepository.getTimetablePage()
        cacheFile.writeText(Json.encodeToString(CachedData(timetablePage)))
        return timetablePage
    }

    /** Will not cache anything. */
    suspend fun getRealTimetable(schoolYear: SchoolYear, timetablePeriod: TimetablePage.PeriodOption) =
        timetableRepository.getTimetablePage(schoolYear, timetablePeriod)

    companion object
    {
        private const val FILE_NAME = "timetable.json"
    }
}
