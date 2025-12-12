package me.tomasan7.jecnamobile.attendances

import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.util.SchoolYear
import java.time.Month
import javax.inject.Inject

class AttendancesRepositoryImpl @Inject constructor(
    val jecnaClient: JecnaClient
) : AttendancesRepository
{
    override suspend fun getAttendancesPage() = jecnaClient.getAttendancesPage()

    override suspend fun getAttendancesPage(schoolYear: SchoolYear, month: Month) = jecnaClient.getAttendancesPage(schoolYear, month)
}
