package me.tomasan7.jecnamobile.timetable

import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.util.SchoolYear
import javax.inject.Inject

class TimetableRepositoryImpl @Inject constructor(
    private val jecnaClient: JecnaClient
) : TimetableRepository
{
    override suspend fun getTimetablePage() = jecnaClient.getTimetablePage()

    override suspend fun getTimetablePage(
        schoolYear: SchoolYear,
        periodId: Int
    ) = jecnaClient.getTimetablePage(schoolYear, periodId)
}
