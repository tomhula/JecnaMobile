package me.tomasan7.jecnamobile.di

import io.github.tomhula.jecnaapi.data.absence.AbsencesPage
import io.github.tomhula.jecnaapi.data.article.NewsPage
import io.github.tomhula.jecnaapi.data.attendance.AttendancesPage
import io.github.tomhula.jecnaapi.data.grade.GradesPage
import io.github.tomhula.jecnaapi.util.SchoolYear
import kotlinx.coroutines.flow.first
import kotlinx.serialization.serializer
import me.tomasan7.jecnamobile.absence.AbsencesRepository
import me.tomasan7.jecnamobile.attendances.AttendancesRepository
import me.tomasan7.jecnamobile.caching.*
import me.tomasan7.jecnamobile.grades.GradesRepository
import me.tomasan7.jecnamobile.news.NewsRepository
import me.tomasan7.jecnamobile.timetable.TimetableCacheRepository
import me.tomasan7.jecnamobile.timetable.TimetableData
import me.tomasan7.jecnamobile.timetable.TimetableRepository
import me.tomasan7.jecnamobile.util.settingsDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val cacheRepositoriesModule = module {
    single(named("attendances")) {
        val repository: AttendancesRepository = get()
        CacheRepository(
            androidContext(),
            "attendances",
            serializer<AttendancesPage>(),
            serializer<SchoolYearMonthParams>()
        ) {
            repository.getAttendancesPage(it.schoolYear, it.month)
        }
    }

    single(named("news")) {
        val repository: NewsRepository = get()
        CacheRepository(
            androidContext(),
            "news",
            serializer<NewsPage>(),
            serializer<NoParams>()
        ) {
            repository.getNewsPage()
        }
    }

    single(named("grades")) {
        val repository: GradesRepository = get()
        CacheRepository(
            androidContext(),
            "grades",
            serializer<GradesPage>(),
            serializer<SchoolYearHalfParams>()
        ) {
            repository.getGradesPage(it.schoolYear, it.schoolYearHalf)
        }
    }

    single<CacheRepository<TimetableData, SchoolYearPeriodParams>>(named("timetable")) {
        val repository: TimetableRepository = get()
        val appContext = androidContext()
        TimetableCacheRepository(
            key = "timetable",
            appContext = appContext,
            fetcher = {
                val settings = appContext.settingsDataStore.data.first()
                if (it.periodId == SchoolYearPeriodParams.CURRENT_PERIOD_ID)
                    repository.getTimetableData(it.schoolYear, null as Int?, settings.substitutionTimetableEnabled)
                else
                    repository.getTimetableData(it.schoolYear, it.periodId, settings.substitutionTimetableEnabled)
            }
        )
    }

    single(named("absences")) {
        val repository: AbsencesRepository = get()
        CacheRepository(
            androidContext(),
            "absences",
            serializer<AbsencesPage>(),
            serializer<SchoolYear>()
        ) {
            repository.getAbsencesPage(it)
        }
    }
}
