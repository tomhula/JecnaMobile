package me.tomasan7.jecnamobile.timetable

import io.github.tomhula.jecnaapi.data.timetable.TimetablePage
import io.github.tomhula.jecnaapi.util.SchoolYear
import cz.jzitnik.jecna_supl_client.ReportLocation

interface TimetableRepository
{
    suspend fun getTimetableData(): TimetableData
    suspend fun getTimetableData(schoolYear: SchoolYear, timetablePeriod: TimetablePage.PeriodOption?) = getTimetableData(schoolYear, timetablePeriod?.id)
    suspend fun getTimetableData(schoolYear: SchoolYear, periodId: Int?): TimetableData

    suspend fun getAllSubstitutions(): SubstitutionAllData?

    suspend fun reportSubstitutionError(content: String, reportLocation: ReportLocation): Result<Unit>
}
