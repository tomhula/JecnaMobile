package me.tomasan7.jecnamobile.timetable

import io.github.stevekk11.dtos.DailySchedule
import io.github.stevekk11.dtos.LabeledTeacherAbsences
import io.github.stevekk11.dtos.ScheduleWithAbsences
import io.github.stevekk11.dtos.SubstitutedLesson
import io.github.stevekk11.dtos.SubstitutionStatus
import java.time.LocalDate

interface SubstitutionRepository {
    suspend fun getTeacherAbsences(): List<LabeledTeacherAbsences>?
    suspend fun getTeacherAbsences(date: LocalDate): LabeledTeacherAbsences?
    suspend fun getSubstitutions(): List<SubstitutedLesson>?
    suspend fun getSubstitutionsStatus(): SubstitutionStatus
    suspend fun getCompleteSchedule(): ScheduleWithAbsences?
    suspend fun getDailySubstitutions(): List<DailySchedule>?
    fun setClassSymbol(classSymbol: String)
}
