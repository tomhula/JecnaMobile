package me.tomasan7.jecnamobile.timetable

import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.util.SchoolYear
import javax.inject.Inject

import uniffi.jecna_supl_client.JecnaSuplClient
import me.tomasan7.jecnamobile.util.settingsDataStore
import kotlinx.coroutines.flow.first
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

class TimetableRepositoryImpl @Inject constructor(
    private val jecnaClient: JecnaClient,
    private val substitutionClient: JecnaSuplClient,
    @ApplicationContext private val context: Context
) : TimetableRepository
{
    override suspend fun getTimetablePage(): TimetableData {
        val page = jecnaClient.getTimetablePage()
        return TimetableData(page, fetchSubstitutions())
    }

    override suspend fun getTimetablePage(
        schoolYear: SchoolYear,
        periodId: Int?
    ): TimetableData {
        val page = jecnaClient.getTimetablePage(schoolYear, periodId)
        return TimetableData(page, fetchSubstitutions())
    }

    private var cachedClassName: String? = null

    private suspend fun fetchSubstitutions(): SubstitutionData? {
        return try {
            val settings = context.settingsDataStore.data.first()
            val serverUrl = settings.substitutionServerUrl.trim()
            substitutionClient.setProvider(serverUrl)
            
            if (cachedClassName == null) {
                try {
                    val student = jecnaClient.getStudentProfile()
                    cachedClassName = student.className
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } 
            
            val className = cachedClassName

            if (className != null) {
                val subs = substitutionClient.getSchedule(className)
                SubstitutionData(subs.status.lastUpdated, subs.status.currentUpdateSchedule.toInt(), subs.schedule.map { it.toSerializable() })
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
