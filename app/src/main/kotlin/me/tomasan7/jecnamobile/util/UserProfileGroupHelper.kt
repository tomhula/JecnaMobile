package me.tomasan7.jecnamobile.util

import android.content.Context
import android.util.Log
import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.data.student.Student
import kotlinx.coroutines.flow.first
import me.tomasan7.jecnamobile.timetable.TimetableData
import me.tomasan7.jecnamobile.widget.TimetableWidgetRenderer
import me.tomasan7.jecnamobile.widget.TimetableWidgetUpdater

object UserProfileGroupHelper
{
    private const val TAG = "UserProfileGroupHelper"

    /**
     * Parses the student's assigned group from profile data:
     * - "A2" -> "2/2"
     * - "A1" -> "1/2"
     */
    fun parseProfileGroup(classGroups: String?, className: String? = null): String?
    {
        val texts = listOfNotNull(classGroups, className).filter { it.isNotBlank() }
        val a2Regex = Regex("""(?i)(?:^|[\s,;/\-_])(?:A2|B2|2/2)(?:$|[\s,;/\-_])""")
        val a1Regex = Regex("""(?i)(?:^|[\s,;/\-_])(?:A1|B1|1/2)(?:$|[\s,;/\-_])""")

        for (text in texts)
        {
            if (a2Regex.containsMatchIn(text)) return "2/2"
            if (a1Regex.containsMatchIn(text)) return "1/2"
        }

        for (text in texts)
        {
            val trimmed = text.trim()
            if (trimmed.equals("A2", ignoreCase = true) || trimmed.equals("2/2", ignoreCase = true) || trimmed.equals("2", ignoreCase = true)) return "2/2"
            if (trimmed.equals("A1", ignoreCase = true) || trimmed.equals("1/2", ignoreCase = true) || trimmed.equals("1", ignoreCase = true)) return "1/2"
        }

        return null
    }

    /**
     * Synchronizes the detected group from the student profile into settings and widget settings.
     */
    suspend fun syncUserGroupFromProfile(
        context: Context,
        student: Student,
        timetableData: TimetableData? = null
    ): String?
    {
        val detectedGroup = parseProfileGroup(student.classGroups, student.className)
        if (detectedGroup == null)
        {
            Log.d(TAG, "No profile group detected for classGroups: '${student.classGroups}', className: '${student.className}'")
            return null
        }

        Log.d(TAG, "Detected profile group: $detectedGroup from student profile (A2=2/2, A1=1/2)")

        val current = context.settingsDataStore.data.first()
        val previousGroup = current.userProfileGroup
        val newSubjectGroups = current.subjectGroups.toMutableMap()

        if (timetableData != null)
        {
            val groupSubjects = TimetableWidgetRenderer.extractGroupSubjects(timetableData)
            val halfSubjects = groupSubjects.filter { it.availableGroups.any { g -> g == "1/2" || g == "2/2" } }
            for (sub in halfSubjects)
            {
                if (!newSubjectGroups.containsKey(sub.subjectShort) || previousGroup == null || previousGroup != detectedGroup)
                {
                    newSubjectGroups[sub.subjectShort] = detectedGroup
                }
            }
        }

        val updatedPerWidgets = current.perWidgetSettings.mapValues { (_, ws) ->
            ws.copy(userProfileGroup = detectedGroup)
        }

        val updatedSettings = current.copy(
            userProfileGroup = detectedGroup,
            widgetSettings = current.widgetSettings.copy(userProfileGroup = detectedGroup),
            perWidgetSettings = updatedPerWidgets,
            subjectGroups = newSubjectGroups
        )

        context.settingsDataStore.updateData { updatedSettings }
        TimetableWidgetUpdater.updateAllWidgets(context, updatedSettings)
        return detectedGroup
    }

    /**
     * Applies the student's profile group to any half-divided subjects in a freshly loaded timetable.
     */
    suspend fun applyProfileGroupToTimetable(
        context: Context,
        timetableData: TimetableData
    )
    {
        val settings = context.settingsDataStore.data.first()
        val group = settings.userProfileGroup ?: return
        val groupSubjects = TimetableWidgetRenderer.extractGroupSubjects(timetableData)
        val halfSubjects = groupSubjects.filter { it.availableGroups.any { g -> g == "1/2" || g == "2/2" } }

        var changed = false
        val newSubjectGroups = settings.subjectGroups.toMutableMap()
        for (sub in halfSubjects)
        {
            if (!newSubjectGroups.containsKey(sub.subjectShort))
            {
                newSubjectGroups[sub.subjectShort] = group
                changed = true
            }
        }

        if (changed)
        {
            val updated = context.settingsDataStore.updateData { it.copy(subjectGroups = newSubjectGroups) }
            TimetableWidgetUpdater.updateAllWidgets(context, updated)
        }
    }

    /**
     * Fetches the student profile from the network and synchronizes the student's group.
     */
    suspend fun fetchAndSyncUserGroup(
        context: Context,
        jecnaClient: JecnaClient,
        timetableData: TimetableData? = null
    ): String?
    {
        return try
        {
            val student = jecnaClient.getStudentProfile()
            syncUserGroupFromProfile(context, student, timetableData)
        }
        catch (e: Exception)
        {
            Log.w(TAG, "Failed to fetch student profile for group sync", e)
            null
        }
    }
}
