package me.tomasan7.jecnamobile.di

import me.tomasan7.jecnamobile.login.AuthRepository
import me.tomasan7.jecnamobile.login.ObfuscationSharedPreferencesAuthRepository
import me.tomasan7.jecnamobile.timetable.TimetableRepository
import me.tomasan7.jecnamobile.timetable.TimetableRepositoryImpl
import org.koin.dsl.module
import org.koin.plugin.module.dsl.bind
import org.koin.plugin.module.dsl.single

internal val repositoriesModule = module {
    single<ObfuscationSharedPreferencesAuthRepository>().bind(AuthRepository::class)

    single<TimetableRepositoryImpl>().bind(TimetableRepository::class)
}
