package me.tomasan7.jecnamobile.timetable

import io.github.tomhula.jecnaapi.data.timetable.TimetablePage
import kotlinx.serialization.Serializable
import uniffi.jecna_supl_client.DailySchedule as JecnaDailySchedule

@Serializable
data class TimetableData(
    val page: TimetablePage,
    val substitutions: SubstitutionData?,
)

@Serializable
data class DailySchedule(
    val date: String,
    val lessons: List<String?>
)

fun JecnaDailySchedule.toSerializable() = DailySchedule(
    date = date,
    lessons = lessons
)

@Serializable
data class SubstitutionData(
    val lastUpdated: String,
    val currentUpdateSchedule: Int,
    val data: List<DailySchedule> = emptyList(),
)
