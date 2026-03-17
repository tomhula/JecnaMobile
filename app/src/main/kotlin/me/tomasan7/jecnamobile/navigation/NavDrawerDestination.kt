package me.tomasan7.jecnamobile.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import me.tomasan7.jecnamobile.R

@Serializable
enum class NavDrawerDestination(
    @StringRes
    val label: Int,
    val icon: ImageVector,
    val iconSelected: ImageVector = icon,
    val destination: AppDestination
)
{
    News(R.string.sidebar_news, Icons.Outlined.Newspaper, Icons.Filled.Newspaper, AppDestination.News),
    Grades(R.string.sidebar_grades, Icons.Outlined.Grade, Icons.Filled.Grade, AppDestination.Grades()),
    Timetable(R.string.sidebar_timetable, Icons.Outlined.TableChart, Icons.Filled.TableChart, AppDestination.Timetable),
    Substitution(R.string.sidebar_link_substitution_timetable, Icons.Outlined.TableChart, Icons.Filled.TableChart, AppDestination.Substitution),
    Canteen(R.string.sidebar_canteen, Icons.Outlined.Restaurant, Icons.Filled.Restaurant, AppDestination.Canteen),
    Attendances(R.string.sidebar_attendances, Icons.Outlined.DateRange, Icons.Filled.DateRange, AppDestination.Attendances),
    Absences(R.string.sidebar_absences, Icons.Outlined.EventBusy, Icons.Filled.EventBusy, AppDestination.Absences),
    Teachers(R.string.sidebar_teachers, Icons.Outlined.People, Icons.Filled.People, AppDestination.Teachers),
    Rooms(R.string.sidebar_rooms, Icons.Outlined.MeetingRoom, Icons.Filled.MeetingRoom, AppDestination.Rooms)
}
