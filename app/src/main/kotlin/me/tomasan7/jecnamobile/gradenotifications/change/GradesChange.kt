package me.tomasan7.jecnamobile.gradenotifications.change

import me.tomasan7.jecnaapi.data.grade.Grade

sealed interface GradesChange
{
    data class NewGrade(val newGrade: Grade) : GradesChange
    data class GradeChange(val oldGrade: Grade, val newGrade: Grade) : GradesChange
    data class GradeRemoved(val removedGrade: Grade) : GradesChange
}
