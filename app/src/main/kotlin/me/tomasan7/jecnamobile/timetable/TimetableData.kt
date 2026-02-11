package me.tomasan7.jecnamobile.timetable

import io.github.tomhula.jecnaapi.data.timetable.TimetablePage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import cz.jzitnik.jecna_supl_client.AbsenceEntry as JecnaAbsenceEntry
import cz.jzitnik.jecna_supl_client.ApiResponse as JecnaApiResponse
import cz.jzitnik.jecna_supl_client.ChangeEntry as JecnaChangeEntry
import cz.jzitnik.jecna_supl_client.DailyData as JecnaDailyData
import cz.jzitnik.jecna_supl_client.DailySchedule as JecnaDailySchedule

@Serializable
data class TimetableData(
    val page: TimetablePage,
    val substitutions: SubstitutionData?,
)

@Serializable
data class SubstitutionAllData(
    val lastUpdated: String,
    val currentUpdateSchedule: Int,
    val schedule: Map<String, GlobalDailyData>
)

@Serializable
data class GlobalDailyData(
    val info: DayInfo,
    val changes: Map<String, List<ChangeEntry?>>,
    val absence: List<AbsenceEntry>,
    val takesPlace: String
)

@Serializable
data class DailySchedule(
    val date: String,
    val info: DayInfo,
    val changes: List<ChangeEntry?>,
    val absence: List<AbsenceEntry>,
    val takesPlace: String
)

@Serializable
data class DayInfo(
    val inWork: Boolean
)

@Serializable
data class ChangeEntry(
    val text: String,
    val backgroundColor: String?,
    val foregroundColor: String?,
    val willBeSpecified: Boolean?
)

@Serializable
sealed class AbsenceEntry {
    @Serializable
    @SerialName("wholeDay")
    data class WholeDay(
        val teacher: String?,
        val teacherCode: String
    ) : AbsenceEntry()

    @Serializable
    @SerialName("single")
    data class Single(
        val teacher: String?,
        val teacherCode: String,
        val hours: Int
    ) : AbsenceEntry()

    @Serializable
    @SerialName("range")
    data class Range(
        val teacher: String?,
        val teacherCode: String,
        val hours: AbsenceRange
    ) : AbsenceEntry()

    @Serializable
    @SerialName("exkurze")
    data class Exkurze(
        val teacher: String?,
        val teacherCode: String
    ) : AbsenceEntry()

    @Serializable
    @SerialName("zastoupen")
    data class Zastoupen(
        val teacher: String?,
        val teacherCode: String,
        val zastupuje: SubstituteInfo
    ) : AbsenceEntry()

    @Serializable
    @SerialName("invalid")
    data class Invalid(
        val original: String
    ) : AbsenceEntry()
}

@Serializable
data class AbsenceRange(
    val from: Int,
    val to: Int
)

@Serializable
data class SubstituteInfo(
    val teacher: String?,
    val teacherCode: String
)

fun JecnaApiResponse.toSubstitutionAllData() = SubstitutionAllData(
    lastUpdated = status.lastUpdated,
    currentUpdateSchedule = status.currentUpdateSchedule.toInt(),
    schedule = schedule.mapValues { it.value.toGlobalDailyData() }
)

fun JecnaDailyData.toGlobalDailyData() = GlobalDailyData(
    info = DayInfo(info.inWork),
    changes = changes.mapValues { it.value.map { entry -> entry?.toChangeEntry() } },
    absence = absence.map { it.toAbsenceEntry() },
    takesPlace = takesPlace
)

fun JecnaDailySchedule.toDailySchedule(date: String) = DailySchedule(
    date = date,
    info = DayInfo(info.inWork),
    changes = changes.map { it?.toChangeEntry() },
    absence = absence.map { it.toAbsenceEntry() },
    takesPlace = takesPlace
)

fun JecnaChangeEntry.toChangeEntry() = ChangeEntry(
    text = text,
    backgroundColor = backgroundColor,
    foregroundColor = foregroundColor,
    willBeSpecified = willBeSpecified
)

fun JecnaAbsenceEntry.toAbsenceEntry(): AbsenceEntry = when (this) {
    is JecnaAbsenceEntry.WholeDay -> AbsenceEntry.WholeDay(teacher, teacherCode)
    is JecnaAbsenceEntry.Single -> AbsenceEntry.Single(teacher, teacherCode, hours.toInt())
    is JecnaAbsenceEntry.Range -> AbsenceEntry.Range(teacher, teacherCode, AbsenceRange(hours.from.toInt(), hours.to.toInt()))
    is JecnaAbsenceEntry.Exkurze -> AbsenceEntry.Exkurze(teacher, teacherCode)
    is JecnaAbsenceEntry.Zastoupen -> AbsenceEntry.Zastoupen(teacher, teacherCode, SubstituteInfo(zastupuje.teacher, zastupuje.teacherCode))
    is JecnaAbsenceEntry.Invalid -> AbsenceEntry.Invalid(original)
}

@Serializable
data class SubstitutionData(
    val lastUpdated: String,
    val currentUpdateSchedule: Int,
    val data: List<DailySchedule> = emptyList(),
)
