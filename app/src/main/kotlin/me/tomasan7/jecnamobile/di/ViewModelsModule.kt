package me.tomasan7.jecnamobile.di

import me.tomasan7.jecnamobile.absence.AbsencesViewModel
import me.tomasan7.jecnamobile.attendances.AttendancesViewModel
import me.tomasan7.jecnamobile.canteen.CanteenViewModel
import me.tomasan7.jecnamobile.grades.GradesViewModel
import me.tomasan7.jecnamobile.login.LoginViewModel
import me.tomasan7.jecnamobile.mainscreen.MainScreenViewModel
import me.tomasan7.jecnamobile.news.NewsViewModel
import me.tomasan7.jecnamobile.rooms.RoomsViewModel
import me.tomasan7.jecnamobile.rooms.room.RoomViewModel
import me.tomasan7.jecnamobile.settings.SettingsViewModel
import me.tomasan7.jecnamobile.student.StudentProfileViewModel
import me.tomasan7.jecnamobile.student.cert.StudentCertificatesViewModel
import me.tomasan7.jecnamobile.substitutions.SubstitutionViewModel
import me.tomasan7.jecnamobile.teachers.TeachersViewModel
import me.tomasan7.jecnamobile.teachers.teacher.TeacherViewModel
import me.tomasan7.jecnamobile.timetable.TimetableViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val viewModelsModule = module {
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
}
