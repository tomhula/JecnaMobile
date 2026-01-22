package me.tomasan7.jecnamobile.util

import io.github.tomhula.jecnaapi.data.grade.Grade
import io.github.tomhula.jecnaapi.data.grade.Grades
import me.tomasan7.jecnamobile.grades.PredictedGrade
import me.tomasan7.jecnamobile.ui.theme.*

fun getGradeColor(gradeValue: Int) = when (gradeValue)
{
    0    -> grade_0
    1    -> grade_1
    2    -> grade_2
    3    -> grade_3
    4    -> grade_4
    5    -> grade_5
    else -> throw IllegalArgumentException("Grade value must be between 0 and 5. (got $gradeValue)")
}

fun getGradeColor(grade: Grade) = getGradeColor(grade.value)

/**
 * Calculates weighted average including predicted grades.
 * Large grades have weight 2, small grades have weight 1.
 */
fun Grades.calculateAverageWithPredictions(predictedGrades: List<PredictedGrade>): Float?
{
    var totalWeightedSum = 0.0
    var totalWeight = 0.0

    // Add real grades
    subjectParts.forEach { subjectPart ->
        this[subjectPart]?.forEach { grade ->
            if (grade.value == 0)
                return@forEach
            val weight = if (grade.small) 1.0 else 2.0
            totalWeightedSum += grade.value * weight
            totalWeight += weight
        }
    }

    // Add predicted grades
    predictedGrades.forEach { predicted ->
        val weight = if (predicted.isSmall) 1.0 else 2.0
        totalWeightedSum += predicted.value * weight
        totalWeight += weight
    }

    return if (totalWeight > 0) (totalWeightedSum / totalWeight).toFloat() else null
}
