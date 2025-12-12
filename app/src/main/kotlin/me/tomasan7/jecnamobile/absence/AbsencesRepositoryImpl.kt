package me.tomasan7.jecnamobile.absence

import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.util.SchoolYear
import javax.inject.Inject

class AbsencesRepositoryImpl @Inject constructor(
    val jecnaClient: JecnaClient
) : AbsencesRepository
{
    override suspend fun getAbsences() = jecnaClient.getAbsencesPage()

    override suspend fun getAbsences(schoolYear: SchoolYear) = jecnaClient.getAbsencesPage(schoolYear)
}