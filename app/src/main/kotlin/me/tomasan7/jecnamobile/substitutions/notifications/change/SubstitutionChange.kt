package me.tomasan7.jecnamobile.substitutions.notifications.change

import me.tomasan7.jecnamobile.timetable.Announcement
import me.tomasan7.jecnamobile.timetable.ChangeEntry

sealed interface SubstitutionChange
{
    /**
     * A lesson substitution was added or modified for a given hour on a date.
     * [hour] is 1-indexed (1st hour, 2nd hour, etc.).
     */
    data class LessonChange(
        val date: String,
        val hour: Int,
        val oldChange: ChangeEntry?,
        val newChange: ChangeEntry
    ) : SubstitutionChange

    /**
     * A previously posted substitution change was removed / cancelled.
     */
    data class LessonChangeRemoved(
        val date: String,
        val hour: Int,
        val removedChange: ChangeEntry
    ) : SubstitutionChange

    /**
     * Takes place / daily notice note changed for the class on a date.
     */
    data class TakesPlaceChange(
        val date: String,
        val oldTakesPlace: String,
        val newTakesPlace: String
    ) : SubstitutionChange

    /**
     * A new announcement was posted.
     */
    data class NewAnnouncement(
        val announcement: Announcement
    ) : SubstitutionChange
}
