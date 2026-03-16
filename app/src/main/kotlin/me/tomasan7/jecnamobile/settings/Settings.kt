package me.tomasan7.jecnamobile.settings

import kotlinx.serialization.Serializable
import me.tomasan7.jecnamobile.mainscreen.SidebarLink
import me.tomasan7.jecnamobile.navigation.SidebarDestination

@Serializable
data class Settings(
    val theme: Theme = Theme.SYSTEM,
    var canteenLegendDismissed: Boolean = false,
    var gradesViewMode: GradesViewMode = GradesViewMode.GRID,
    val openSubScreenRoute: String? = null,
    val defaultDestination: SidebarDestination = openSubScreenRoute?.let { legacySubScreenRouteToDefaultDestination(it) } ?: SidebarDestination.Timetable,
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
            SidebarDestination.entries.map { DrawerPage(it.name, true) }

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

private fun legacySubScreenRouteToDefaultDestination(route: String?): SidebarDestination? =
    when (route)
    {
        "absences_sub_screen" -> SidebarDestination.Absences
        "attendances_sub_screen" -> SidebarDestination.Attendances
        "canteen_sub_screen" -> SidebarDestination.Canteen
        "grades_sub_screen" -> SidebarDestination.Grades
        "news_sub_screen" -> SidebarDestination.News
        "teachers_sub_screen" -> SidebarDestination.Teachers
        "timetable_sub_screen" -> SidebarDestination.Timetable
        else -> null
    }
