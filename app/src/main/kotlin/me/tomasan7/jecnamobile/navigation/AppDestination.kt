package me.tomasan7.jecnamobile.navigation

import androidx.navigation3.runtime.NavKey
import io.github.tomhula.jecnaapi.data.room.RoomReference
import io.github.tomhula.jecnaapi.data.schoolStaff.TeacherReference
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppDestination : NavKey
{
    @Serializable
    data object News : AppDestination
    @Serializable
    data class Grades(val gradeId: Int? = null) : AppDestination
    @Serializable
    data object Timetable : AppDestination
    @Serializable
    data object Substitution : AppDestination
    @Serializable
    data object Canteen : AppDestination
    @Serializable
    data object Attendances : AppDestination
    @Serializable
    data object Absences : AppDestination
    @Serializable
    data object Teachers : AppDestination
    @Serializable
    data object Rooms : AppDestination

    @Serializable
    data class Teacher(val reference: TeacherReference) : AppDestination
    @Serializable
    data class Room(val reference: RoomReference) : AppDestination

    @Serializable
    data object StudentProfile : AppDestination

    @Serializable
    sealed interface Settings : AppDestination
    {
        @Serializable
        data object Main : Settings
        @Serializable
        data object General : Settings
        @Serializable
        data object Notifications : Settings
        @Serializable
        data object Appearance : Settings
        @Serializable
        data object Substitution : Settings
        @Serializable
        data object Canteen : Settings
        @Serializable
        data object About : Settings
        @Serializable
        data object Drawer : Settings
    }
}
