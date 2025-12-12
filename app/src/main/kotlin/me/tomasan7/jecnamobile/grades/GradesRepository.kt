package me.tomasan7.jecnamobile.grades

import io.github.tomhula.jecnaapi.data.grade.GradesPage
import io.github.tomhula.jecnaapi.util.SchoolYear
import io.github.tomhula.jecnaapi.util.SchoolYearHalf

interface GradesRepository
{
    suspend fun getGradesPage(): GradesPage
    suspend fun getGradesPage(schoolYear: SchoolYear, schoolYearHalf: SchoolYearHalf): GradesPage
}
