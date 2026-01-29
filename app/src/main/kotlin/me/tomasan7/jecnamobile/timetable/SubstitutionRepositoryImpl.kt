package me.tomasan7.jecnamobile.timetable

import io.github.stevekk11.api.SubstitutionClient
import io.github.stevekk11.dtos.DailySchedule
import io.github.stevekk11.dtos.LabeledTeacherAbsences
import io.github.stevekk11.dtos.ScheduleWithAbsences
import io.github.stevekk11.dtos.SubstitutedLesson
import io.github.stevekk11.dtos.SubstitutionStatus
import java.time.LocalDate
import javax.inject.Inject

class SubstitutionRepositoryImpl @Inject constructor(
    private val substitutionClient: SubstitutionClient
) : SubstitutionRepository {
    override suspend fun getTeacherAbsences(): List<LabeledTeacherAbsences>? =
        substitutionClient.getTeacherAbsences()

    override suspend fun getTeacherAbsences(date: LocalDate): LabeledTeacherAbsences? =
        substitutionClient.getTeacherAbsences(date)

    override suspend fun getSubstitutions(): List<SubstitutedLesson>? =
        substitutionClient.getSubstitutions()

    override suspend fun getSubstitutionsStatus(): SubstitutionStatus =
        substitutionClient.getSubstitutionsStatus()

    override suspend fun getCompleteSchedule(): ScheduleWithAbsences? =
        substitutionClient.getCompleteSchedule()

    override suspend fun getDailySubstitutions(): List<DailySchedule>? =
        substitutionClient.getDailySubstitutions()

    override fun setClassSymbol(classSymbol: String) {
        substitutionClient.setClassSymbol(classSymbol)
    }
}
