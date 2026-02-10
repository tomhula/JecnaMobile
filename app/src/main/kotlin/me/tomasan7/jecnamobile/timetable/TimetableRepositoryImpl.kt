package me.tomasan7.jecnamobile.timetable

import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.util.SchoolYear
import javax.inject.Inject

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

import cz.jzitnik.jecna_supl_client.JecnaSuplClient

class TimetableRepositoryImpl @Inject constructor(
    private val jecnaClient: JecnaClient,
    private val substitutionClient: JecnaSuplClient,
) : TimetableRepository
{
    override suspend fun getTimetableData(): TimetableData = coroutineScope {
        val substitutionsDeferred = async { fetchSubstitutions() }
        val page = async { jecnaClient.getTimetablePage() }
        TimetableData(page.await(), substitutionsDeferred.await())
    }

    override suspend fun getTimetableData(
        schoolYear: SchoolYear,
        periodId: Int?
    ): TimetableData = coroutineScope {
        val substitutionsDeferred = async { fetchSubstitutions() }
        val page = async { jecnaClient.getTimetablePage(schoolYear, periodId) }
        TimetableData(page.await(), substitutionsDeferred.await())
    }

    private var cachedClassName: String? = null

    private suspend fun fetchSubstitutions(): SubstitutionData? {
        return try {
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
                val subs = withContext(Dispatchers.IO) {
                    substitutionClient.getSchedule(className)
                }
                SubstitutionData(subs.status.lastUpdated, subs.status.currentUpdateSchedule.toInt(), subs.schedule.map { (date, schedule) -> schedule.toSerializable(date) })
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    override suspend fun getAllSubstitutions(): SubstitutionAllData? = withContext(Dispatchers.IO) {
        try {
            substitutionClient.getAll().toSerializable()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
