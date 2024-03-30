package me.tomasan7.jecnamobile.gradenotifications.change

import me.tomasan7.jecnaapi.data.grade.Grade

interface GradesChangeChecker
{
    fun checkForChanges(oldGrades: Collection<Grade>, newGrades: Collection<Grade>): Set<GradesChange>
}
