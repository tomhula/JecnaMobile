package me.tomasan7.jecnamobile.attendances

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import me.tomasan7.jecnaapi.data.attendance.AttendancesPage
import me.tomasan7.jecnaapi.util.SchoolYear
import me.tomasan7.jecnamobile.util.CachedData
import java.io.File
import java.time.Month
import javax.inject.Inject

class CacheAttendancesRepository @Inject constructor(
    @ApplicationContext
    private val appContext: Context,
    private val attendancesRepository: AttendancesRepository
)
{
    private val cacheFile = File(appContext.cacheDir, FILE_NAME)

    fun isCacheAvailable() = cacheFile.exists()

    @OptIn(ExperimentalSerializationApi::class)
    fun getCachedAttendances(): CachedData<AttendancesPage>?
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

    suspend fun getRealAttendances(): AttendancesPage
    {
        val gradesPage = attendancesRepository.getAttendancesPage()
        cacheFile.writeText(Json.encodeToString(CachedData(gradesPage)))
        return gradesPage
    }

    /** Will not cache anything. */
    suspend fun getRealAttendances(schoolYear: SchoolYear, month: Month) = attendancesRepository.getAttendancesPage(schoolYear, month)

    companion object
    {
        private const val FILE_NAME = "attendances.json"
    }
}
