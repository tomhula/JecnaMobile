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
import org.koin.dsl.module
import org.koin.plugin.module.dsl.bind
import org.koin.plugin.module.dsl.single

internal val repositoriesModule = module {
    single<ObfuscationSharedPreferencesAuthRepository>().bind(AuthRepository::class)
    single<GradesRepositoryImpl>().bind(GradesRepository::class)
    single<TimetableRepositoryImpl>().bind(TimetableRepository::class)
    single<NewsRepositoryImpl>().bind(NewsRepository::class)
    single<AttendancesRepositoryImpl>().bind(AttendancesRepository::class)
    single<TeachersRepositoryImpl>().bind(TeachersRepository::class)
    single<AbsencesRepositoryImpl>().bind(AbsencesRepository::class)
    single<StudentProfileRepositoryImpl>().bind(StudentProfileRepository::class)
    single<RoomsRepositoryImpl>().bind(RoomsRepository::class)
}
