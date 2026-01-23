package me.tomasan7.jecnamobile.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.migration.DisableInstallInCheck
import io.github.tomhula.jecnaapi.data.absence.AbsencesPage
import io.github.tomhula.jecnaapi.data.article.NewsPage
import io.github.tomhula.jecnaapi.data.attendance.AttendancesPage
import io.github.tomhula.jecnaapi.data.grade.GradesPage
import io.github.tomhula.jecnaapi.data.timetable.TimetablePage
import io.github.tomhula.jecnaapi.util.SchoolYear
import kotlinx.serialization.serializer
import me.tomasan7.jecnamobile.absence.AbsencesRepository
import me.tomasan7.jecnamobile.attendances.AttendancesRepository
import me.tomasan7.jecnamobile.caching.*
import me.tomasan7.jecnamobile.grades.GradesRepository
import me.tomasan7.jecnamobile.news.NewsRepository
import me.tomasan7.jecnamobile.timetable.TimetableCacheRepository
import me.tomasan7.jecnamobile.timetable.TimetableRepository
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
        "grades",
        serializer<GradesPage>(),
        serializer<SchoolYearHalfParams>()
    ) {
        repository.getGradesPage(it.schoolYear, it.schoolYearHalf)
    }

    @Provides
    @Singleton
    fun provideTimetableCacheRepository(
        @ApplicationContext
        appContext: Context,
        repository: TimetableRepository
    ): CacheRepository<TimetablePage, SchoolYearPeriodParams> = TimetableCacheRepository(
        key = "timetable",
        appContext = appContext,
        fetcher = { 
            if (it.periodId == SchoolYearPeriodParams.CURRENT_PERIOD_ID)
                repository.getTimetablePage(it.schoolYear, null as Int?)
            else
                repository.getTimetablePage(it.schoolYear, it.periodId)
        }
    )

    @Provides
    @Singleton
    fun provideAbsencesCacheRepository(
        @ApplicationContext
        appContext: Context,
        repository: AbsencesRepository
    ) = CacheRepository(
        appContext,
        "absences",
        serializer<AbsencesPage>(),
        serializer<SchoolYear>()
    ) {
        repository.getAbsencesPage(it)
    }
}
