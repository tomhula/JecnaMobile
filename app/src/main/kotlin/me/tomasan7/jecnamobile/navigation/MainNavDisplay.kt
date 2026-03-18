package me.tomasan7.jecnamobile.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import me.tomasan7.jecnamobile.absence.AbsencesSubScreen
import me.tomasan7.jecnamobile.attendances.AttendancesSubScreen
import me.tomasan7.jecnamobile.canteen.CanteenSubScreen
import me.tomasan7.jecnamobile.grades.GradesSubScreen
import me.tomasan7.jecnamobile.news.NewsSubScreen
import me.tomasan7.jecnamobile.rooms.RoomsSubScreen
import me.tomasan7.jecnamobile.rooms.room.RoomScreen
import me.tomasan7.jecnamobile.settings.settingsNavEntry
import me.tomasan7.jecnamobile.student.StudentProfileScreen
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
        entryProvider = { key ->
            when (key)
            {
                is AppDestination.News -> NavEntry(key) {
                    NewsSubScreen()
                }
                is AppDestination.Grades -> NavEntry(key) {
                    GradesSubScreen(
                        onTeacherClick = { navBackStack.add(AppDestination.Teacher(it)) }
                    )
                }
                is AppDestination.Timetable -> NavEntry(key) {
                    TimetableSubScreen(
                        onTeacherClick = { navBackStack.add(AppDestination.Teacher(it)) },
                        onRoomClick = { navBackStack.add(AppDestination.Room(it)) }
                    )
                }
                is AppDestination.Canteen -> NavEntry(key) {
                    CanteenSubScreen()
                }
                is AppDestination.Attendances -> NavEntry(key) {
                    AttendancesSubScreen()
                }
                is AppDestination.Absences -> NavEntry(key) {
                    AbsencesSubScreen()
                }
                is AppDestination.Teachers -> NavEntry(key) {
                    TeachersSubScreen(
                        onTeacherClick = { navBackStack.add(AppDestination.Teacher(it)) }
                    )
                }
                is AppDestination.Rooms -> NavEntry(key) {
                    RoomsSubScreen(
                        onRoomClick = { navBackStack.add(AppDestination.Room(it)) }
                    )
                }
                is AppDestination.Substitution -> NavEntry(key) {
                    SubstitutionSubScreen(
                        onTeacherClick = { navBackStack.add(AppDestination.Teacher(it)) }
                    )
                }
                is AppDestination.Teacher -> NavEntry(key) {
                    TeacherScreen(
                        teacherReference = key.reference,
                        onBackClick = onBack,
                        onRoomClick = { navBackStack.add(AppDestination.Room(it)) }
                    )
                }
                is AppDestination.Room -> NavEntry(key) {
                    RoomScreen(
                        roomReference = key.reference,
                        onBackClick = onBack,
                        onTeacherClick = { navBackStack.add(AppDestination.Teacher(it)) }
                    )
                }
                is AppDestination.StudentProfile -> NavEntry(key) {
                    StudentProfileScreen(
                        onBackClick = onBack
                    )
                }
                is AppDestination.Settings -> settingsNavEntry(
                    key = key,
                    onNavigate = { navBackStack.add(it) },
                    onBackClick = onBack
                )
            }
        },
        transitionSpec = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(TRANSITION_DURATION, easing = FastOutSlowInEasing)
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(TRANSITION_DURATION, easing = FastOutSlowInEasing)
            )
        },
        popTransitionSpec = {
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(TRANSITION_DURATION, easing = FastOutSlowInEasing)
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(TRANSITION_DURATION, easing = FastOutSlowInEasing)
            )
        },
        predictivePopTransitionSpec = { _ ->
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(TRANSITION_DURATION, easing = FastOutSlowInEasing)
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(TRANSITION_DURATION, easing = FastOutSlowInEasing)
            )
        }
    )
}
