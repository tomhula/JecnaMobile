package me.tomasan7.jecnamobile.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.migration.DisableInstallInCheck
import io.github.tomhula.jecnaapi.data.attendance.AttendancesPage
import kotlinx.serialization.serializer
import me.tomasan7.jecnamobile.CacheRepository
import me.tomasan7.jecnamobile.SchoolYearMonthParams
import me.tomasan7.jecnamobile.attendances.AttendancesRepository
import javax.inject.Singleton

@DisableInstallInCheck
@Module
internal object CacheRepositoriesModule
{
    @Provides
    @Singleton
    fun provideAttendancesCacheRepository(
        @ApplicationContext
        appContext: Context,
        repository: AttendancesRepository
    ) = CacheRepository(
        appContext,
        "attendances",
        serializer<AttendancesPage>(),
        serializer<SchoolYearMonthParams>()
    ) {
        repository.getAttendancesPage(it.schoolYear, it.month)
    }
}
