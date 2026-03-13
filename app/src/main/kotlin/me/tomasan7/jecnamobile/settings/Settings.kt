package me.tomasan7.jecnamobile.settings

import kotlinx.serialization.Serializable
import me.tomasan7.jecnamobile.mainscreen.SidebarLink
import me.tomasan7.jecnamobile.mainscreen.SubScreenDestination

@Serializable
data class Settings(
    val theme: Theme = Theme.SYSTEM,
    var canteenLegendDismissed: Boolean = false,
    var gradesViewMode: GradesViewMode = GradesViewMode.GRID,
    val openSubScreenRoute: String? = null,
    val defaultDestination: SubScreenDestination = openSubScreenRoute?.let { legacySubScreenRouteToDefaultDestination(it) } ?: SubScreenDestination.Timetable,
    val substitutionServerUrl: String = DEFAULT_SUBSTITUTION_SERVER_URL,
    val substitutionTimetableEnabled: Boolean = true,
    var hasSeenWelcomeScreen: Boolean = false,
    var drawerPages: List<DrawerPage> = getDefaultDrawerPages(),
    var drawerLinks: List<DrawerLink> = getDefaultDrawerLinks()
)
{
    companion object
    {
        const val DEFAULT_SUBSTITUTION_SERVER_URL = "https://jecnarozvrh.jzitnik.dev"

        fun getDefaultDrawerPages(): List<DrawerPage> =
            SubScreenDestination.entries.map { DrawerPage(it.name, true) }

        fun getDefaultDrawerLinks(): List<DrawerLink> =
            SidebarLink.entries.map { DrawerLink(it.name, true) }
    }

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

    @Serializable
    data class DrawerPage(
        val destinationName: String,
        val isVisible: Boolean
    )

    @Serializable
    data class DrawerLink(
        val linkName: String,
        val isVisible: Boolean
    )
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
