package me.tomasan7.jecnamobile.timetable

import io.github.tomhula.jecnaapi.data.timetable.TimetablePage
import io.github.tomhula.jecnaapi.util.SchoolYear

interface TimetableRepository
{
    suspend fun getTimetablePage(): TimetablePage
    suspend fun getTimetablePage(schoolYear: SchoolYear, timetablePeriod: TimetablePage.PeriodOption) = getTimetablePage(schoolYear, timetablePeriod.id)
    suspend fun getTimetablePage(schoolYear: SchoolYear, periodId: Int): TimetablePage
}
