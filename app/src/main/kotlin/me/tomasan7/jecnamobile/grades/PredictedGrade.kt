package me.tomasan7.jecnamobile.grades

import androidx.compose.runtime.Immutable

/**
 * Represents a hypothetical grade used for grade average predictions.
 * These are separate from real [me.tomasan7.jecnaapi.data.grade.Grade] objects from the API.
 */
@Immutable
data class PredictedGrade(
    val value: Int,
    val isSmall: Boolean
)
{
    init
    {
        require(value in 1..5) { "Grade value must be between 1 and 5" }
    }
}

