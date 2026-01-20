package me.tomasan7.jecnamobile.attendances

import io.github.tomhula.jecnaapi.data.attendance.AttendancesPage
import io.github.tomhula.jecnaapi.util.SchoolYear
import kotlinx.datetime.Month

interface AttendancesRepository
{
    suspend fun getAttendancesPage(): AttendancesPage

    suspend fun getAttendancesPage(schoolYear: SchoolYear, month: Month): AttendancesPage
}
