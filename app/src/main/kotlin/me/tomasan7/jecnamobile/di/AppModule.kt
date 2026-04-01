package me.tomasan7.jecnamobile.di

import cz.jzitnik.jecna_supl_client.JecnaSuplClient
import io.github.tomhula.jecnaapi.CanteenClient
import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.WebJecnaClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.tomasan7.jecnamobile.LoginStateProvider
import me.tomasan7.jecnamobile.absence.AbsencesRepository
import me.tomasan7.jecnamobile.absence.AbsencesRepositoryImpl
import me.tomasan7.jecnamobile.absence.AbsencesViewModel
import me.tomasan7.jecnamobile.attendances.AttendancesRepository
import me.tomasan7.jecnamobile.attendances.AttendancesRepositoryImpl
import me.tomasan7.jecnamobile.attendances.AttendancesViewModel
import me.tomasan7.jecnamobile.canteen.CanteenViewModel
import me.tomasan7.jecnamobile.gradenotifications.GradeCheckerWorker
import me.tomasan7.jecnamobile.gradenotifications.change.GradesChangeChecker
import me.tomasan7.jecnamobile.gradenotifications.change.GradesChangeCheckerImpl
import me.tomasan7.jecnamobile.grades.GradesRepository
import me.tomasan7.jecnamobile.grades.GradesRepositoryImpl
import me.tomasan7.jecnamobile.grades.GradesViewModel
import me.tomasan7.jecnamobile.login.AuthRepository
import me.tomasan7.jecnamobile.login.LoginViewModel
import me.tomasan7.jecnamobile.login.ObfuscationSharedPreferencesAuthRepository
import me.tomasan7.jecnamobile.mainscreen.MainScreenViewModel
import me.tomasan7.jecnamobile.news.NewsRepository
import me.tomasan7.jecnamobile.news.NewsRepositoryImpl
import me.tomasan7.jecnamobile.news.NewsViewModel
import me.tomasan7.jecnamobile.rooms.RoomsRepository
import me.tomasan7.jecnamobile.rooms.RoomsRepositoryImpl
import me.tomasan7.jecnamobile.rooms.RoomsViewModel
import me.tomasan7.jecnamobile.rooms.room.RoomViewModel
import me.tomasan7.jecnamobile.settings.SettingsViewModel
import me.tomasan7.jecnamobile.student.StudentProfileRepository
import me.tomasan7.jecnamobile.student.StudentProfileRepositoryImpl
import me.tomasan7.jecnamobile.student.StudentProfileViewModel
import me.tomasan7.jecnamobile.student.cert.StudentCertificatesViewModel
import me.tomasan7.jecnamobile.substitutions.SubstitutionViewModel
import me.tomasan7.jecnamobile.teachers.TeachersRepository
import me.tomasan7.jecnamobile.teachers.TeachersRepositoryImpl
import me.tomasan7.jecnamobile.teachers.TeachersViewModel
import me.tomasan7.jecnamobile.teachers.teacher.TeacherViewModel
import me.tomasan7.jecnamobile.timetable.TimetableRepository
import me.tomasan7.jecnamobile.timetable.TimetableRepositoryImpl
import me.tomasan7.jecnamobile.timetable.TimetableViewModel
import me.tomasan7.jecnamobile.util.settingsDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
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
    singleOf(::ObfuscationSharedPreferencesAuthRepository) bind AuthRepository::class
    singleOf(::GradesRepositoryImpl) bind GradesRepository::class
    singleOf(::TimetableRepositoryImpl) bind TimetableRepository::class
    singleOf(::NewsRepositoryImpl) bind NewsRepository::class
    singleOf(::AttendancesRepositoryImpl) bind AttendancesRepository::class
    singleOf(::TeachersRepositoryImpl) bind TeachersRepository::class
    singleOf(::GradesChangeCheckerImpl) bind GradesChangeChecker::class
    singleOf(::AbsencesRepositoryImpl) bind AbsencesRepository::class
    singleOf(::StudentProfileRepositoryImpl) bind StudentProfileRepository::class
    singleOf(::RoomsRepositoryImpl) bind RoomsRepository::class
    
    viewModel { NewsViewModel(get(), get(), get(named("news")), get()) }
    viewModel { AbsencesViewModel(get(), get(), get(named("absences"))) }
    viewModel { AttendancesViewModel(get(), get(), get(named("attendances"))) }
    viewModelOf(::CanteenViewModel)
    viewModel { GradesViewModel(get(), get(), get(named("grades")), get()) }
    viewModelOf(::LoginViewModel)
    viewModelOf(::MainScreenViewModel)
    viewModelOf(::RoomsViewModel)
    viewModelOf(::RoomViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::StudentProfileViewModel)
    viewModelOf(::StudentCertificatesViewModel)
    viewModelOf(::SubstitutionViewModel)
    viewModelOf(::TeachersViewModel)
    viewModelOf(::TeacherViewModel)
    viewModel { TimetableViewModel(get(), get(), get(named("timetable")), get()) }
    
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
