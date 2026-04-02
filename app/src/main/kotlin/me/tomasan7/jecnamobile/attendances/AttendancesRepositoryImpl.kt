package me.tomasan7.jecnamobile.attendances

import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.util.SchoolYear
import kotlinx.datetime.Month

class AttendancesRepositoryImpl(
    val jecnaClient: JecnaClient
) : AttendancesRepository
{
    override suspend fun getAttendancesPage() = jecnaClient.getAttendancesPage()

    override suspend fun getAttendancesPage(schoolYear: SchoolYear, month: Month) = jecnaClient.getAttendancesPage(schoolYear, month)
}
