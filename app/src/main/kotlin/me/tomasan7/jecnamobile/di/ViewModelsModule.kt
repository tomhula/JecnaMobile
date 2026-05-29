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
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

internal val viewModelsModule = module {
    viewModel<NewsViewModel>()
    viewModel<AbsencesViewModel>()
    viewModel<AttendancesViewModel>()
    viewModel<CanteenViewModel>()
    viewModel<GradesViewModel>()
    viewModel<LoginViewModel>()
    viewModel<MainScreenViewModel>()
    viewModel<RoomsViewModel>()
    viewModel<RoomViewModel>()
    viewModel<SettingsViewModel>()
    viewModel<StudentProfileViewModel>()
    viewModel<StudentCertificatesViewModel>()
    viewModel<SubstitutionViewModel>()
    viewModel<TeachersViewModel>()
    viewModel<TeacherViewModel>()
    viewModel<TimetableViewModel>()
}
