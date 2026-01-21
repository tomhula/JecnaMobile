package me.tomasan7.jecnamobile.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.migration.DisableInstallInCheck
import io.github.tomhula.jecnaapi.data.article.NewsPage
import io.github.tomhula.jecnaapi.data.attendance.AttendancesPage
import io.github.tomhula.jecnaapi.data.grade.GradesPage
import kotlinx.serialization.serializer
import me.tomasan7.jecnamobile.CacheRepository
import me.tomasan7.jecnamobile.NoParams
import me.tomasan7.jecnamobile.SchoolYearHalfParams
import me.tomasan7.jecnamobile.SchoolYearMonthParams
import me.tomasan7.jecnamobile.attendances.AttendancesRepository
import me.tomasan7.jecnamobile.grades.GradesRepository
import me.tomasan7.jecnamobile.news.NewsRepository
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

    @Provides
    @Singleton
    fun provideNewsCacheRepository(
        @ApplicationContext
        appContext: Context,
        repository: NewsRepository
    ) = CacheRepository(
        appContext,
        "news",
        serializer<NewsPage>(),
        serializer<NoParams>()
    ) {
        repository.getNewsPage()
    }

    @Provides
    @Singleton
    fun provideGradesCacheRepository(
        @ApplicationContext
        appContext: Context,
        repository: GradesRepository
    ) = CacheRepository(
        appContext,
        "news",
        serializer<GradesPage>(),
        serializer<SchoolYearHalfParams>()
    ) {
        repository.getGradesPage(it.schoolYear, it.schoolYearHalf)
    }
}
