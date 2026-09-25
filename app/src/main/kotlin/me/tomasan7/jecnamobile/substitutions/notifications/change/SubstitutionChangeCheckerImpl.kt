package me.tomasan7.jecnamobile.substitutions.notifications.change

import me.tomasan7.jecnamobile.timetable.SubstitutionData
import kotlin.math.max

class SubstitutionChangeCheckerImpl : SubstitutionChangeChecker
{
    override fun checkForChanges(
        oldData: SubstitutionData,
        newData: SubstitutionData
    ): List<SubstitutionChange>
    {
        val changes = mutableListOf<SubstitutionChange>()
        val oldSchedulesByDate = oldData.data.associateBy { it.date }

        for (newSchedule in newData.data)
        {
            val oldSchedule = oldSchedulesByDate[newSchedule.date]

            if (oldSchedule == null)
            {
                for ((index, change) in newSchedule.changes.withIndex())
                {
                    if (change != null)
                    {
                        changes.add(
                            SubstitutionChange.LessonChange(
                                date = newSchedule.date,
                                hour = index + 1,
                                oldChange = null,
                                newChange = change
                            )
                        )
                    }
                }

                if (newSchedule.takesPlace.isNotBlank())
                {
                    changes.add(
                        SubstitutionChange.TakesPlaceChange(
                            date = newSchedule.date,
                            oldTakesPlace = "",
                            newTakesPlace = newSchedule.takesPlace
                        )
                    )
                }
            }
            else
            {
                val maxHours = max(oldSchedule.changes.size, newSchedule.changes.size)
                for (hourIndex in 0 until maxHours)
                {
                    val oldChange = oldSchedule.changes.getOrNull(hourIndex)
                    val newChange = newSchedule.changes.getOrNull(hourIndex)

                    if (newChange != null)
                    {
                        if (oldChange == null || oldChange.text != newChange.text)
                        {
                            changes.add(
                                SubstitutionChange.LessonChange(
                                    date = newSchedule.date,
                                    hour = hourIndex + 1,
                                    oldChange = oldChange,
                                    newChange = newChange
                                )
                            )
                        }
                    }
                    else if (oldChange != null)
                    {
                        changes.add(
                            SubstitutionChange.LessonChangeRemoved(
                                date = newSchedule.date,
                                hour = hourIndex + 1,
                                removedChange = oldChange
                            )
                        )
                    }
                }

                if (newSchedule.takesPlace != oldSchedule.takesPlace && newSchedule.takesPlace.isNotBlank())
                {
                    changes.add(
                        SubstitutionChange.TakesPlaceChange(
                            date = newSchedule.date,
                            oldTakesPlace = oldSchedule.takesPlace,
                            newTakesPlace = newSchedule.takesPlace
                        )
                    )
                }
            }
        }

        val oldAnnouncementIds = oldData.announcements?.values?.flatten()?.map { it.id }?.toSet() ?: emptySet()
        val newAnnouncements = newData.announcements?.values?.flatten() ?: emptyList()
        for (announcement in newAnnouncements)
        {
            if (announcement.id !in oldAnnouncementIds)
            {
                changes.add(SubstitutionChange.NewAnnouncement(announcement))
            }
        }

        return changes
    }
}
