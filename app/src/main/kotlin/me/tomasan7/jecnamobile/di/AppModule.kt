package me.tomasan7.jecnamobile.di

import cz.jzitnik.jecna_supl_client.JecnaSuplClient
import io.github.tomhula.jecnaapi.CanteenClient
import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.WebJecnaClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.tomasan7.jecnamobile.LoginStateProvider
import me.tomasan7.jecnamobile.gradenotifications.GradeCheckerWorker
import me.tomasan7.jecnamobile.gradenotifications.change.GradesChangeChecker
import me.tomasan7.jecnamobile.gradenotifications.change.GradesChangeCheckerImpl
import me.tomasan7.jecnamobile.util.settingsDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds

class DefaultLoginStateProvider(private val jecnaClient: JecnaClient) : LoginStateProvider
{
    override val afterFirstLogin: Boolean
        get() = (jecnaClient as WebJecnaClient).autoLoginAuth != null
}

val appModule = module {
    // TODO: Migrate to compiler DSL: https://github.com/InsertKoinIO/koin/issues/2390
    single { JecnaClient(autoLogin = true, userAgent = "JM", requestTimeout = 15.seconds) } bind JecnaClient::class
    single { CanteenClient(autoLogin = true, userAgent = "JM") } bind CanteenClient::class
    single {
        JecnaSuplClient().apply {
            setProvider(runBlocking {
                androidContext().settingsDataStore.data.first().substitutionServerUrl.trim()
            })
        }
    }
    
    singleOf(::DefaultLoginStateProvider) bind LoginStateProvider::class
    singleOf(::GradesChangeCheckerImpl) bind GradesChangeChecker::class
    
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
}
