package me.tomasan7.jecnamobile.grades

import androidx.compose.runtime.Immutable

/**
 * Represents a hypothetical grade that a user predicts for grade calculation purposes.
 * These are separate from real [me.tomasan7.jecnaapi.data.grade.Grade] objects from the API.
 */
@Immutable
data class PredictedGrade(
    val subjectName: String,
    val subjectPart: String?,
    val value: Int,
    val isSmall: Boolean
)
{
    init
    {
        require(value in 1..5) { "Grade value must be between 1 and 5" }
    }
}

