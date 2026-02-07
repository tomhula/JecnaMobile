package me.tomasan7.jecnamobile.settings

import kotlinx.serialization.Serializable
import me.tomasan7.jecnamobile.mainscreen.SubScreenDestination

@Serializable
data class Settings(
    val theme: Theme = Theme.SYSTEM,
    var canteenImageTolerance: Float = 0.5f,
    var canteenHelpSeen: Boolean = false,
    var gradesViewMode: GradesViewMode = GradesViewMode.GRID,
    val openSubScreenRoute: String? = null,
    val defaultDestination: SubScreenDestination = openSubScreenRoute?.let { legacySubScreenRouteToDefaultDestination(it) } ?: SubScreenDestination.Timetable,
    val substitutionServerUrl: String = "https://jecnarozvrh.jzitnik.dev/",
)
{
    enum class GradesViewMode
    {
        LIST,
        GRID
    }
    enum class Theme
    {
        DARK,
        LIGHT,
        SYSTEM
    }
}

private fun legacySubScreenRouteToDefaultDestination(route: String?): SubScreenDestination? =
    when (route)
    {
        "absences_sub_screen" -> SubScreenDestination.Absences
        "attendances_sub_screen" -> SubScreenDestination.Attendances
        "canteen_sub_screen" -> SubScreenDestination.Canteen
        "grades_sub_screen" -> SubScreenDestination.Grades
        "news_sub_screen" -> SubScreenDestination.News
        "teachers_sub_screen" -> SubScreenDestination.Teachers
        "timetable_sub_screen" -> SubScreenDestination.Timetable
        else -> null
    }
