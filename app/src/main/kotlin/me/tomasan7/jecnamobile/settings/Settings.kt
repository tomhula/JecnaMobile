package me.tomasan7.jecnamobile.settings

import kotlinx.serialization.Serializable
import me.tomasan7.jecnamobile.mainscreen.SubScreenDestination

@Serializable
data class Settings(
    val theme: Theme = Theme.SYSTEM,
    var canteenImageTolerance: Float = 0.5f,
    var canteenHelpSeen: Boolean = false,
    var gradesViewMode: GradesViewMode = GradesViewMode.GRID,
    val openSubScreenRoute: SubScreenDestination = SubScreenDestination.Timetable,
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
