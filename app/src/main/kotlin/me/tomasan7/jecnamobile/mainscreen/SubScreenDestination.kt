package me.tomasan7.jecnamobile.mainscreen

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.ui.graphics.vector.ImageVector
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.destinations.AbsencesSubScreenDestination
import me.tomasan7.jecnamobile.destinations.AttendancesSubScreenDestination
import me.tomasan7.jecnamobile.destinations.CanteenSubScreenDestination
import me.tomasan7.jecnamobile.destinations.GradesSubScreenDestination
import me.tomasan7.jecnamobile.destinations.NewsSubScreenDestination
import me.tomasan7.jecnamobile.destinations.TeachersSubScreenDestination
import me.tomasan7.jecnamobile.destinations.TimetableSubScreenDestination

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
    Teachers(TeachersSubScreenDestination, R.string.sidebar_teachers, Icons.Outlined.People, Icons.Filled.People)
}
