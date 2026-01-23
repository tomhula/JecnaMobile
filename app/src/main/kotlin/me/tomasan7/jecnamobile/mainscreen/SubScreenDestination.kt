package me.tomasan7.jecnamobile.mainscreen

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import me.tomasan7.jecnamobile.R

enum class SubScreenDestination(
    @StringRes
    val label: Int,
    val icon: ImageVector,
    val iconSelected: ImageVector = icon
) : NavKey
{
    News(R.string.sidebar_news, Icons.Outlined.Newspaper, Icons.Filled.Newspaper),
    Grades(R.string.sidebar_grades, Icons.Outlined.Grade, Icons.Filled.Grade),
    Timetable(R.string.sidebar_timetable, Icons.Outlined.TableChart, Icons.Filled.TableChart),
    Canteen(R.string.sidebar_canteen, Icons.Outlined.Restaurant, Icons.Filled.Restaurant),
    Attendances(R.string.sidebar_attendances, Icons.Outlined.DateRange, Icons.Filled.DateRange),
    Absences(R.string.sidebar_absences, Icons.Outlined.EventBusy, Icons.Filled.EventBusy),
    Teachers(R.string.sidebar_teachers, Icons.Outlined.People, Icons.Filled.People)
}
