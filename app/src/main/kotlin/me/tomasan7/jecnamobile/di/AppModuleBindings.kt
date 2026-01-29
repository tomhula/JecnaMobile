package me.tomasan7.jecnamobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.migration.DisableInstallInCheck
import me.tomasan7.jecnamobile.absence.AbsencesRepository
import me.tomasan7.jecnamobile.absence.AbsencesRepositoryImpl
import me.tomasan7.jecnamobile.attendances.AttendancesRepository
import me.tomasan7.jecnamobile.attendances.AttendancesRepositoryImpl
import me.tomasan7.jecnamobile.gradenotifications.change.GradesChangeChecker
import me.tomasan7.jecnamobile.gradenotifications.change.GradesChangeCheckerImpl
import me.tomasan7.jecnamobile.grades.GradesRepository
import me.tomasan7.jecnamobile.grades.GradesRepositoryImpl
import me.tomasan7.jecnamobile.login.AuthRepository
import me.tomasan7.jecnamobile.login.ObfuscationSharedPreferencesAuthRepository
import me.tomasan7.jecnamobile.news.NewsRepository
import me.tomasan7.jecnamobile.news.NewsRepositoryImpl
import me.tomasan7.jecnamobile.rooms.RoomsRepository
import me.tomasan7.jecnamobile.rooms.RoomsRepositoryImpl
import me.tomasan7.jecnamobile.student.StudentProfileRepository
import me.tomasan7.jecnamobile.student.StudentProfileRepositoryImpl
import me.tomasan7.jecnamobile.teachers.TeachersRepository
import me.tomasan7.jecnamobile.teachers.TeachersRepositoryImpl
import me.tomasan7.jecnamobile.substitution.SubstitutionRepository
import me.tomasan7.jecnamobile.substitution.SubstitutionRepositoryImpl
import me.tomasan7.jecnamobile.timetable.TimetableRepository
import me.tomasan7.jecnamobile.timetable.TimetableRepositoryImpl
import javax.inject.Singleton

@DisableInstallInCheck
@Module
interface AppModuleBindings {
    @Binds
    @Singleton
    fun bindAuthRepository(repository: ObfuscationSharedPreferencesAuthRepository): AuthRepository

    @Binds
    @Singleton
    fun bindGradesRepository(repository: GradesRepositoryImpl): GradesRepository

    @Binds
    @Singleton
    fun bindTimetableRepository(repository: TimetableRepositoryImpl): TimetableRepository

    @Binds
    @Singleton
    fun bindSubstitutionRepository(repository: SubstitutionRepositoryImpl): SubstitutionRepository

    @Binds
    @Singleton
    fun bindNewsRepository(repository: NewsRepositoryImpl): NewsRepository

    @Binds
    @Singleton
    fun bindAttendancesRepository(repository: AttendancesRepositoryImpl): AttendancesRepository

    @Binds
    @Singleton
    fun bindTeachersRepository(repository: TeachersRepositoryImpl): TeachersRepository

    @Binds
    @Singleton
    fun bindGradesChangeChecker(gradesChecker: GradesChangeCheckerImpl): GradesChangeChecker

    @Binds
    @Singleton
    fun bindAbsencesRepository(repository: AbsencesRepositoryImpl): AbsencesRepository

    @Binds
    @Singleton
    fun bindStudentProfileRepository(repository: StudentProfileRepositoryImpl): StudentProfileRepository

    @Binds
    @Singleton
    fun bindRoomsRepository(repository: RoomsRepositoryImpl): RoomsRepository
}
