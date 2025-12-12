package me.tomasan7.jecnamobile.absence

import io.github.tomhula.jecnaapi.data.absence.AbsencesPage
import io.github.tomhula.jecnaapi.util.SchoolYear

interface AbsencesRepository {
    suspend fun getAbsences(): AbsencesPage

    suspend fun getAbsences(schoolYear: SchoolYear): AbsencesPage
}