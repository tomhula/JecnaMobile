package me.tomasan7.jecnamobile.widgets.grades.average

import io.github.tomhula.jecnaapi.data.grade.GradesPage
import kotlinx.serialization.Serializable

@Serializable
data class AverageGradesWidgetState(
    val gradesPage: GradesPage? = null,
    val lastUpdated: Long = 0L,
    val isLoading: Boolean = true,
    val isManualRefresh: Boolean = false,
    val error: String? = null
)
