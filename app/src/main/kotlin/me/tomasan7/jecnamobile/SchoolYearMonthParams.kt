package me.tomasan7.jecnamobile

import io.github.tomhula.jecnaapi.util.SchoolYear
import kotlinx.datetime.Month
import kotlinx.serialization.Serializable

@Serializable(with = SchoolYearMonthParamsSerializer::class)
data class SchoolYearMonthParams(
    val schoolYear: SchoolYear,
    val month: Month
)
