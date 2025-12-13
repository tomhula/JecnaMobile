package me.tomasan7.jecnamobile.absence

import io.github.tomhula.jecnaapi.data.absence.AbsencesPage
import io.github.tomhula.jecnaapi.util.SchoolYear

interface AbsencesRepository {

    suspend fun getAbsencesPage(schoolYear: SchoolYear): AbsencesPage
    suspend fun getAbsencesPage() : AbsencesPage
}