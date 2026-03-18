package me.tomasan7.jecnamobile.settings

import kotlinx.serialization.Serializable
import me.tomasan7.jecnamobile.mainscreen.SidebarLink
import me.tomasan7.jecnamobile.navigation.NavDrawerDestination

@Serializable
data class Settings(
    val theme: Theme = Theme.SYSTEM,
    var canteenLegendDismissed: Boolean = false,
    var gradesViewMode: GradesViewMode = GradesViewMode.GRID,
    val openSubScreenRoute: String? = null,
    val defaultDestination: NavDrawerDestination = openSubScreenRoute?.let { legacySubScreenRouteToDefaultDestination(it) } ?: NavDrawerDestination.Timetable,
    val substitutionServerUrl: String = DEFAULT_SUBSTITUTION_SERVER_URL,
    val substitutionTimetableEnabled: Boolean = true,
    var hasSeenWelcomeScreen: Boolean = false,
    var notificationsEnabled: Boolean? = null,
    var drawerPages: List<DrawerPage> = DEFAULT_DRAWER_PAGES,
    var drawerLinks: List<DrawerLink> = DEFAULT_DRAWER_LINKS
)
{
    companion object
    {
        const val DEFAULT_SUBSTITUTION_SERVER_URL = "https://jecnarozvrh.jzitnik.dev"

        val DEFAULT_DRAWER_PAGES =
            NavDrawerDestination.entries.map { DrawerPage(it.name, true) }

        val DEFAULT_DRAWER_LINKS =
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

private fun legacySubScreenRouteToDefaultDestination(route: String?): NavDrawerDestination? =
    when (route)
    {
        "absences_sub_screen" -> NavDrawerDestination.Absences
        "attendances_sub_screen" -> NavDrawerDestination.Attendances
        "canteen_sub_screen" -> NavDrawerDestination.Canteen
        "grades_sub_screen" -> NavDrawerDestination.Grades
        "news_sub_screen" -> NavDrawerDestination.News
        "teachers_sub_screen" -> NavDrawerDestination.Teachers
        "timetable_sub_screen" -> NavDrawerDestination.Timetable
        else -> null
    }
