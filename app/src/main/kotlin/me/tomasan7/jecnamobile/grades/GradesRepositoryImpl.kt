package me.tomasan7.jecnamobile.grades

import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.util.SchoolYear
import io.github.tomhula.jecnaapi.util.SchoolYearHalf

class GradesRepositoryImpl(
    private val jecnaClient: JecnaClient
) : GradesRepository
{
    override suspend fun getGradesPage() = jecnaClient.getGradesPage()

    override suspend fun getGradesPage(schoolYear: SchoolYear, schoolYearHalf: SchoolYearHalf) = jecnaClient.getGradesPage(schoolYear, schoolYearHalf)
}
