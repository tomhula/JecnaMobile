package me.tomasan7.jecnamobile.di

import me.tomasan7.jecnamobile.absence.AbsencesRepository
import me.tomasan7.jecnamobile.absence.AbsencesRepositoryImpl
import me.tomasan7.jecnamobile.attendances.AttendancesRepository
import me.tomasan7.jecnamobile.attendances.AttendancesRepositoryImpl
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
import me.tomasan7.jecnamobile.timetable.TimetableRepository
import me.tomasan7.jecnamobile.timetable.TimetableRepositoryImpl
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal val repositoriesModule = module {
    singleOf(::ObfuscationSharedPreferencesAuthRepository) bind AuthRepository::class
    singleOf(::GradesRepositoryImpl) bind GradesRepository::class
    singleOf(::TimetableRepositoryImpl) bind TimetableRepository::class
    singleOf(::NewsRepositoryImpl) bind NewsRepository::class
    singleOf(::AttendancesRepositoryImpl) bind AttendancesRepository::class
    singleOf(::TeachersRepositoryImpl) bind TeachersRepository::class
    singleOf(::AbsencesRepositoryImpl) bind AbsencesRepository::class
    singleOf(::StudentProfileRepositoryImpl) bind StudentProfileRepository::class
    singleOf(::RoomsRepositoryImpl) bind RoomsRepository::class
}
