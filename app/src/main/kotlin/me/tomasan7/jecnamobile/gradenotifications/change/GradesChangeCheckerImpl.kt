package me.tomasan7.jecnamobile.gradenotifications.change

import me.tomasan7.jecnaapi.data.grade.Grade
import javax.inject.Inject


class GradesChangeCheckerImpl @Inject constructor() : GradesChangeChecker
{
    override fun checkForChanges(
        oldGrades: Collection<Grade>,
        newGrades: Collection<Grade>
    ): Set<GradesChange>
    {
        val changes = mutableSetOf<GradesChange>()

        for (oldGrade in oldGrades)
        {
            val sameNewGrade = newGrades.find { it sameAs oldGrade }

            if (sameNewGrade == null)
                changes.add(GradesChange.GradeRemoved(oldGrade))
            else if (oldGrade != sameNewGrade)
                changes.add(GradesChange.GradeChange(oldGrade, sameNewGrade))
        }

        for (newGrade in newGrades)
            if (!oldGrades.any { it sameAs newGrade })
                changes.add(GradesChange.NewGrade(newGrade))

        return changes
    }

    /** Returns `true` if [grade ids][Grade.gradeId] match. */
    private infix fun Grade.sameAs(other: Grade) = gradeId == other.gradeId
}
