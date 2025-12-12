package me.tomasan7.jecnamobile.absence

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.tomhula.jecnaapi.data.absence.AbsencesPage
import io.github.tomhula.jecnaapi.data.attendance.AttendancesPage
import io.github.tomhula.jecnaapi.util.SchoolYear
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import me.tomasan7.jecnamobile.util.CachedData
import java.io.File
import javax.inject.Inject
/**
 * Repository that caches absences data in a file.
 */
class CacheAbsencesRepository @Inject constructor(
    @ApplicationContext
    private val appContext: Context,
    private val absencesRepository: AbsencesRepository
) {
    private val cacheFile = File(appContext.cacheDir, FILE_NAME)
    fun isCacheAvailable() = cacheFile.exists()
    @OptIn(ExperimentalSerializationApi::class)

    fun getCachedAbsences(): CachedData<AttendancesPage>?
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

    suspend fun getRealAbsences(): AbsencesPage{
        val absencesPage = absencesRepository.getAbsencesPage()
        cacheFile.writeText(Json.encodeToString(CachedData(absencesPage)))
        return absencesPage
    }

    suspend fun getRealAbsences(schoolYear: SchoolYear) = absencesRepository.getAbsencesPage(schoolYear)

    companion object
    {
        private const val FILE_NAME = "absences.json"
    }
}