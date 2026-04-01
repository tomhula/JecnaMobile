package me.tomasan7.jecnamobile.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel
import androidx.navigation3.runtime.MetadataScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.metadata
import androidx.navigation3.ui.NavDisplay
import me.tomasan7.jecnamobile.absence.AbsencesSubScreen
import me.tomasan7.jecnamobile.attendances.AttendancesSubScreen
import me.tomasan7.jecnamobile.canteen.CanteenSubScreen
import me.tomasan7.jecnamobile.grades.GradesSubScreen
import me.tomasan7.jecnamobile.news.NewsSubScreen
import me.tomasan7.jecnamobile.rooms.RoomsSubScreen
import me.tomasan7.jecnamobile.rooms.room.RoomScreen
import me.tomasan7.jecnamobile.settings.*
import me.tomasan7.jecnamobile.student.StudentProfileScreen
import me.tomasan7.jecnamobile.student.cert.StudentCertificatesScreen
import me.tomasan7.jecnamobile.substitutions.SubstitutionSubScreen
import me.tomasan7.jecnamobile.teachers.TeachersSubScreen
import me.tomasan7.jecnamobile.teachers.teacher.TeacherScreen
import me.tomasan7.jecnamobile.timetable.TimetableSubScreen

private const val TRANSITION_DURATION = 200

@Composable
fun MainNavDisplay(
    navBackStack: NavBackStack<AppDestination>,
    onBack: () -> Unit
)
{
    NavDisplay(
        backStack = navBackStack,
        onBack = onBack,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        entryProvider = entryProvider {
            entry<AppDestination.News> {
                NewsSubScreen()
            }
            entry<AppDestination.Grades> {
                GradesSubScreen(
                    onTeacherClick = { navBackStack.add(AppDestination.Teacher(it)) }
                )
            }
            entry<AppDestination.Timetable> {
                TimetableSubScreen(
                    onTeacherClick = { navBackStack.add(AppDestination.Teacher(it)) },
                    onRoomClick = { navBackStack.add(AppDestination.Room(it)) }
                )
            }
            entry<AppDestination.Canteen> {
                CanteenSubScreen()
            }
            entry<AppDestination.Attendances> {
                AttendancesSubScreen()
            }
            entry<AppDestination.Absences> {
                AbsencesSubScreen()
            }
            entry<AppDestination.Teachers> {
                TeachersSubScreen(
                    onTeacherClick = { navBackStack.add(AppDestination.Teacher(it)) }
                )
            }
            entry<AppDestination.Rooms> {
                RoomsSubScreen(
                    onRoomClick = { navBackStack.add(AppDestination.Room(it)) }
                )
            }
            entry<AppDestination.Substitution> {
                SubstitutionSubScreen(
                    onTeacherClick = { navBackStack.add(AppDestination.Teacher(it)) }
                )
            }
            entry<AppDestination.Teacher>(
                metadata = metadata { slideTransitions() }
            ) { key ->
                TeacherScreen(
                    teacherReference = key.reference,
                    onBackClick = onBack,
                    onRoomClick = { navBackStack.add(AppDestination.Room(it)) }
                )
            }
            entry<AppDestination.Room>(
                metadata = metadata { slideTransitions() }
            ) { key ->
                RoomScreen(
                    roomReference = key.reference,
                    onBackClick = onBack,
                    onTeacherClick = { navBackStack.add(AppDestination.Teacher(it)) }
                )
            }
            entry<AppDestination.StudentProfile>(
                metadata = metadata { slideTransitions() }
            ) {
                StudentProfileScreen(
                    onCertificatesClick = { navBackStack.add(AppDestination.StudentCertificates) },
                    onBackClick = onBack
                )
            }
            entry<AppDestination.StudentCertificates>(
                metadata = metadata { slideTransitions() }
            ) {
                StudentCertificatesScreen(
                    onBackClick = onBack
                )
            }
            entry<AppDestination.Settings.Main>(
                metadata = metadata { slideTransitions() }
            ) {
                SettingsMainScreen(
                    onNavigate = { navBackStack.add(it) },
                    onBackClick = onBack
                )
            }
            entry<AppDestination.Settings.General>(
                metadata = metadata { slideTransitions() }
            ) {
                SettingsGeneralScreen(
                    viewModel = koinViewModel(),
                    onBackClick = onBack
                )
            }
            entry<AppDestination.Settings.Notifications>(
                metadata = metadata { slideTransitions() }
            ) {
                SettingsNotificationsScreen(
                    viewModel = koinViewModel(),
                    onBackClick = onBack
                )
            }
            entry<AppDestination.Settings.Appearance>(
                metadata = metadata { slideTransitions() }
            ) {
                SettingsAppearanceScreen(
                    viewModel = koinViewModel(),
                    onBackClick = onBack
                )
            }
            entry<AppDestination.Settings.Substitution>(
                metadata = metadata { slideTransitions() }
            ) {
                SettingsSubstitutionScreen(
                    viewModel = koinViewModel(),
                    onBackClick = onBack
                )
            }
            entry<AppDestination.Settings.Canteen>(
                metadata = metadata { slideTransitions() }
            ) {
                SettingsCanteenScreen(
                    viewModel = koinViewModel(),
                    onBackClick = onBack
                )
            }
            entry<AppDestination.Settings.About>(
                metadata = metadata { slideTransitions() }
            ) {
                SettingsAboutScreen(
                    onBackClick = onBack
                )
            }
            entry<AppDestination.Settings.Drawer>(
                metadata = metadata { slideTransitions() }
            ) {
                SettingsDrawerScreen(
                    viewModel = koinViewModel(),
                    onBackClick = onBack
                )
            }
        }
    )
}

private fun MetadataScope.slideTransitions()
{
    val popTransition: ContentTransform = EnterTransition.None togetherWith slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(TRANSITION_DURATION, easing = FastOutSlowInEasing)
    )
    
    put(NavDisplay.TransitionKey) {
        slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(TRANSITION_DURATION, easing = FastOutSlowInEasing)
        ) togetherWith ExitTransition.KeepUntilTransitionsFinished
    }
    put(NavDisplay.PopTransitionKey) { popTransition }
    put(NavDisplay.PredictivePopTransitionKey) { popTransition }
}
