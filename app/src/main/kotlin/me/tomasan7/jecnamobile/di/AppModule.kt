package me.tomasan7.jecnamobile.di

import android.content.Context
import cz.jzitnik.jecna_supl_client.JecnaSuplClient
import io.github.tomhula.jecnaapi.CanteenClient
import io.github.tomhula.jecnaapi.JecnaClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.tomasan7.jecnamobile.JecnaClientLoginStateProvider
import me.tomasan7.jecnamobile.LoginStateProvider
import me.tomasan7.jecnamobile.gradenotifications.GradeCheckerWorker
import me.tomasan7.jecnamobile.gradenotifications.change.GradesChangeChecker
import me.tomasan7.jecnamobile.gradenotifications.change.GradesChangeCheckerImpl
import me.tomasan7.jecnamobile.util.settingsDataStore
import me.tomasan7.jecnamobile.substitutions.notifications.SubstitutionCheckerWorker
import me.tomasan7.jecnamobile.substitutions.notifications.change.SubstitutionChangeChecker
import me.tomasan7.jecnamobile.substitutions.notifications.change.SubstitutionChangeCheckerImpl
import me.tomasan7.jecnamobile.widget.TimetableWidgetWorker
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.plugin.module.dsl.create
import org.koin.plugin.module.dsl.bind
import org.koin.plugin.module.dsl.single
import kotlin.time.Duration.Companion.seconds

internal val appModule = module {
    includes(repositoriesModule, cacheRepositoriesModule, viewModelsModule)
    
    single { create(::jecnaClient) }
    single { create(::canteenClient) }
    single { create(::jecnaSuplClient) }
    
    single<JecnaClientLoginStateProvider>().bind(LoginStateProvider::class)
    single<GradesChangeCheckerImpl>().bind(GradesChangeChecker::class)
    single<SubstitutionChangeCheckerImpl>().bind(SubstitutionChangeChecker::class)
    
    worker { params ->
        GradeCheckerWorker(
            appContext = params.get(),
            params = params.get(),
            jecnaClient = get(),
            authRepository = get(),
            cacheGradesRepository = get(named("grades")),
            gradeChangeChecker = GradesChangeCheckerImpl()
        ) 
    }

    worker { params ->
        SubstitutionCheckerWorker(
            appContext = params.get(),
            params = params.get(),
            jecnaClient = get(),
            authRepository = get(),
            timetableRepository = get(),
            substitutionsCacheRepository = get(),
            substitutionChangeChecker = get()
        )
    }

    worker { params ->
        TimetableWidgetWorker(
            appContext = params.get(),
            params = params.get(),
            jecnaClient = get(),
            authRepository = get(),
            timetableRepository = get(),
            timetableCacheRepository = get(),
            substitutionsCacheRepository = get()
        )
    }
}

private fun jecnaClient(): JecnaClient = JecnaClient(autoLogin = true, userAgent = "JM", requestTimeout = 15.seconds)
private fun canteenClient(): CanteenClient = CanteenClient(autoLogin = true, userAgent = "JM")
private fun jecnaSuplClient(context: Context): JecnaSuplClient
{
    val jecnaSuplClient = JecnaSuplClient()
    runBlocking {
        jecnaSuplClient.setProvider(context.settingsDataStore.data.first().substitutionServerUrl.trim())
    }
    return jecnaSuplClient
}
