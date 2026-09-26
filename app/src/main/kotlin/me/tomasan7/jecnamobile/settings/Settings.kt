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
    val substitutionNotificationsEnabled: Boolean = true,
    var drawerPages: List<DrawerPage> = DEFAULT_DRAWER_PAGES,
    var drawerLinks: List<DrawerLink> = DEFAULT_DRAWER_LINKS,
    val widgetSettings: WidgetSettings = WidgetSettings(),
    val perWidgetSettings: Map<Int, WidgetSettings> = emptyMap(),
    val subjectGroups: Map<String, String> = emptyMap(),
    val userProfileGroup: String? = null
)
{
    fun getWidgetSettings(widgetId: Int): WidgetSettings
    {
        val base = if (widgetId <= 0) widgetSettings else (perWidgetSettings[widgetId] ?: widgetSettings)
        return base.copy(
            debugSimulationEnabled = widgetSettings.debugSimulationEnabled,
            debugSimulatedDay = widgetSettings.debugSimulatedDay,
            debugPeriodIndex = widgetSettings.debugPeriodIndex,
            debugMinutesRemaining = widgetSettings.debugMinutesRemaining,
            debugIsBreak = widgetSettings.debugIsBreak,
            debugSubstitutionType = widgetSettings.debugSubstitutionType,
            debugWeekOverrides = widgetSettings.debugWeekOverrides,
            userProfileGroup = userProfileGroup ?: widgetSettings.userProfileGroup
        )
    }

    fun withWidgetSettings(widgetId: Int, update: (WidgetSettings) -> WidgetSettings): Settings
    {
        val current = getWidgetSettings(widgetId)
        val updated = update(current)
        val newGlobalWidgetSettings = widgetSettings.copy(
            debugSimulationEnabled = updated.debugSimulationEnabled,
            debugSimulatedDay = updated.debugSimulatedDay,
            debugPeriodIndex = updated.debugPeriodIndex,
            debugMinutesRemaining = updated.debugMinutesRemaining,
            debugIsBreak = updated.debugIsBreak,
            debugSubstitutionType = updated.debugSubstitutionType,
            debugWeekOverrides = updated.debugWeekOverrides,
            userProfileGroup = updated.userProfileGroup ?: userProfileGroup
        )
        val updatedPerWidgets = perWidgetSettings.mapValues { (id, ws) ->
            if (id == widgetId) updated else ws.copy(
                debugSimulationEnabled = updated.debugSimulationEnabled,
                debugSimulatedDay = updated.debugSimulatedDay,
                debugPeriodIndex = updated.debugPeriodIndex,
                debugMinutesRemaining = updated.debugMinutesRemaining,
                debugIsBreak = updated.debugIsBreak,
                debugSubstitutionType = updated.debugSubstitutionType,
                debugWeekOverrides = updated.debugWeekOverrides,
                userProfileGroup = updated.userProfileGroup ?: userProfileGroup
            )
        }
        return if (widgetId <= 0)
        {
            copy(
                widgetSettings = updated,
                perWidgetSettings = updatedPerWidgets
            )
        }
        else
        {
            copy(
                widgetSettings = newGlobalWidgetSettings,
                perWidgetSettings = updatedPerWidgets + (widgetId to updated)
            )
        }
    }
    companion object
    {
        const val DEFAULT_SUBSTITUTION_SERVER_URL = "https://jecnarozvrh.jzitnik.dev"

        val DEFAULT_DRAWER_PAGES =
            NavDrawerDestination.entries.map { DrawerPage(it.name, true) }

        val DEFAULT_DRAWER_LINKS =
            SidebarLink.entries.map { DrawerLink(it.name, true) }
    }

    @Serializable
    data class WidgetSettings(
        val theme: WidgetTheme = WidgetTheme.DARK,
        val hideClass: Boolean = false,
        val hideTeachers: Boolean = false,
        val showTimeRange: Boolean = false,
        val periodHeaderMode: PeriodHeaderMode = PeriodHeaderMode.NUMBER_ONLY,
        val hideHeader: Boolean = false,
        val hideBorder: Boolean = false,
        val solidHeaderBackground: Boolean = false,
        val filterMyGroups: Boolean = false,
        val cellScale: Float = 1.0f,
        val customColors: WidgetColors = WidgetColors(),
        val lastRefreshedTimestamp: String? = null,
        val debugSimulationEnabled: Boolean = false,
        val debugSimulatedDay: String? = null,
        val debugPeriodIndex: Int = 1,
        val debugMinutesRemaining: Int = 25,
        val debugIsBreak: Boolean = false,
        val debugSubstitutionType: Int = 0,
        val debugWeekOverrides: Map<String, Int> = emptyMap(),
        val userProfileGroup: String? = null
    )
    {
        fun getEffectivePeriodHeaderMode(): PeriodHeaderMode
        {
            if (periodHeaderMode != PeriodHeaderMode.NUMBER_ONLY) return periodHeaderMode
            return if (showTimeRange) PeriodHeaderMode.TIME_ONLY else PeriodHeaderMode.NUMBER_ONLY
        }
    }

    enum class WidgetTheme
    {
        LIGHT,
        DARK,
        CUSTOM
    }

    @Serializable
    enum class PeriodHeaderMode
    {
        NUMBER_ONLY,
        TIME_ONLY,
        BOTH
    }

    @Serializable
    data class WidgetColors(
        val backgroundColor: Long = 0xFF000000,
        val borderColor: Long = 0xFF30363D,
        val dayHeaderColor: Long = 0xFF161B22,
        val dayHeaderTextColor: Long = 0xFFE6EDF3,
        val cellBackgroundColor: Long = 0xFF0D1117,
        val cellTextColor: Long = 0xFFE6EDF3,
        val teacherTextColor: Long = 0xFF8B949E,
        val classroomTextColor: Long = 0xFF8B949E,
        val groupTextColor: Long = 0xFF8B949E,
        val activeHourBorderColor: Long = 0xFF2F81F7,
        val activeHourFillColor: Long = 0x332F81F7,
        val substitutionColor: Long = 0xFFE3B341,
        val substitutionTextColor: Long = 0xFFE3B341,
        val substitutionBackgroundColor: Long = 0x4DE3B341
    )

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
