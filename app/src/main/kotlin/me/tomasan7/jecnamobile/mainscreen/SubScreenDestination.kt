package me.tomasan7.jecnamobile.mainscreen

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.destinations.*

enum class SubScreenDestination(
    val destination: DirectionDestinationSpec,
    @StringRes
    val label: Int,
    val icon: ImageVector,
    val iconSelected: ImageVector = icon
)
{
    News(NewsSubScreenDestination, R.string.sidebar_news, Icons.Outlined.Newspaper, Icons.Filled.Newspaper),
    Grades(GradesSubScreenDestination, R.string.sidebar_grades, Icons.Outlined.Grade, Icons.Filled.Grade),
    Timetable(TimetableSubScreenDestination, R.string.sidebar_timetable, Icons.Outlined.TableChart, Icons.Filled.TableChart),
    Canteen(CanteenSubScreenDestination, R.string.sidebar_canteen, Icons.Outlined.Restaurant, Icons.Filled.Restaurant),
    Attendances(AttendancesSubScreenDestination, R.string.sidebar_attendances, Icons.Outlined.DateRange, Icons.Filled.DateRange),
    Absences(AbsencesSubScreenDestination, R.string.sidebar_absences, Icons.Outlined.EventBusy, Icons.Filled.EventBusy),
    Teachers(TeachersSubScreenDestination, R.string.sidebar_teachers, Icons.Outlined.People, Icons.Filled.People),
    Classrooms(RoomsSubScreenDestination, R.string.sidebar_classrooms, Icons.Outlined.MeetingRoom, Icons.Filled.MeetingRoom),
}
