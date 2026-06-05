package me.tomasan7.jecnamobile.di

import android.content.Context
import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.data.absence.AbsencesPage
import io.github.tomhula.jecnaapi.data.article.NewsPage
import io.github.tomhula.jecnaapi.data.attendance.AttendancesPage
import io.github.tomhula.jecnaapi.data.document.DocumentsPage
import io.github.tomhula.jecnaapi.data.grade.GradesPage
import io.github.tomhula.jecnaapi.util.SchoolYear
import kotlinx.serialization.serializer
import me.tomasan7.jecnamobile.caching.CacheRepository
import me.tomasan7.jecnamobile.caching.NoParams
import me.tomasan7.jecnamobile.caching.SchoolYearHalfParams
import me.tomasan7.jecnamobile.caching.SchoolYearMonthParams
import me.tomasan7.jecnamobile.timetable.TimetableCacheRepository
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

internal val cacheRepositoriesModule = module {
    single<AttendancesCacheRepository>()
    single<NewsCacheRepository>()
    single<GradesCacheRepository>()
    single<TimetableCacheRepository>()
    single<AbsencesCacheRepository>()
    single<DocumentsCacheRepository>()
}

class DocumentsCacheRepository(jecnaClient: JecnaClient, context: Context) :
    CacheRepository<DocumentsPage, String>(
        appContext = context,
        key = "documents",
        dataSerializer = serializer<DocumentsPage>(),
        paramsSerializer = serializer<String>(),
        fetcher = { jecnaClient.getDocumentsPage(it) }
    )

class AttendancesCacheRepository(jecnaClient: JecnaClient, context: Context) :
    CacheRepository<AttendancesPage, SchoolYearMonthParams>(
        appContext = context,
        key = "attendances",
        dataSerializer = serializer<AttendancesPage>(),
        paramsSerializer = serializer<SchoolYearMonthParams>(),
        fetcher = { jecnaClient.getAttendancesPage(it.schoolYear, it.month) }
    )

class NewsCacheRepository(jecnaClient: JecnaClient, context: Context) :
    CacheRepository<NewsPage, NoParams>(
        appContext = context,
        key = "news",
        dataSerializer = serializer<NewsPage>(),
        paramsSerializer = serializer<NoParams>(),
        fetcher = { jecnaClient.getNewsPage() }
    )

class GradesCacheRepository(jecnaClient: JecnaClient, context: Context) :
    CacheRepository<GradesPage, SchoolYearHalfParams>(
        appContext = context,
        key = "grades",
        dataSerializer = serializer<GradesPage>(),
        paramsSerializer = serializer<SchoolYearHalfParams>(),
        fetcher = { jecnaClient.getGradesPage(it.schoolYear, it.schoolYearHalf) }
    )

class AbsencesCacheRepository(jecnaClient: JecnaClient, context: Context) :
    CacheRepository<AbsencesPage, SchoolYear>(
        appContext = context,
        key = "absences",
        dataSerializer = serializer<AbsencesPage>(),
        paramsSerializer = serializer<SchoolYear>(),
        fetcher = { jecnaClient.getAbsencesPage(it) }
    )
