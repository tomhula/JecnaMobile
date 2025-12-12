package me.tomasan7.jecnamobile.gradenotifications.change

import io.github.tomhula.jecnaapi.data.grade.Grade

interface GradesChangeChecker
{
    fun checkForChanges(oldGrades: Collection<Grade>, newGrades: Collection<Grade>): Set<GradesChange>
}
