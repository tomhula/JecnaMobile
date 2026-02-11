package me.tomasan7.jecnamobile.timetable

import io.github.tomhula.jecnaapi.data.timetable.TimetablePage
import io.github.tomhula.jecnaapi.util.SchoolYear
import cz.jzitnik.jecna_supl_client.ReportLocation

interface TimetableRepository
{
    suspend fun getTimetableData(fetchSubstitutions: Boolean): TimetableData
    suspend fun getTimetableData(schoolYear: SchoolYear, timetablePeriod: TimetablePage.PeriodOption?, fetchSubstitutions: Boolean) = getTimetableData(schoolYear, timetablePeriod?.id, fetchSubstitutions)
    suspend fun getTimetableData(schoolYear: SchoolYear, periodId: Int?, fetchSubstitutions: Boolean): TimetableData

    suspend fun getAllSubstitutions(): SubstitutionAllData

    suspend fun reportSubstitutionError(content: String, reportLocation: ReportLocation): Result<Unit>
}
