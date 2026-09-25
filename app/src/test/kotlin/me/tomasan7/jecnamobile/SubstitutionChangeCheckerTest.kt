package me.tomasan7.jecnamobile

import me.tomasan7.jecnamobile.substitutions.notifications.change.SubstitutionChange
import me.tomasan7.jecnamobile.substitutions.notifications.change.SubstitutionChangeCheckerImpl
import me.tomasan7.jecnamobile.timetable.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubstitutionChangeCheckerTest
{
    private val checker = SubstitutionChangeCheckerImpl()

    @Test
    fun testNoChangesWhenIdentical()
    {
        val data1 = SubstitutionData(
            lastUpdated = "10:00",
            currentUpdateSchedule = 5,
            data = listOf(
                DailySchedule(
                    date = "2026-09-25",
                    info = DayInfo(inWork = false),
                    changes = listOf(
                        ChangeEntry(text = "M 1 Hr(Zn)", backgroundColor = null, foregroundColor = null, willBeSpecified = null)
                    ),
                    absence = emptyList(),
                    takesPlace = "Exkurze"
                )
            )
        )
        val data2 = data1.copy()

        val changes = checker.checkForChanges(data1, data2)
        assertTrue(changes.isEmpty())
    }

    @Test
    fun testDetectsNewLessonChange()
    {
        val oldData = SubstitutionData(
            lastUpdated = "10:00",
            currentUpdateSchedule = 5,
            data = listOf(
                DailySchedule(
                    date = "2026-09-25",
                    info = DayInfo(inWork = false),
                    changes = listOf(null, null),
                    absence = emptyList(),
                    takesPlace = ""
                )
            )
        )

        val newChange = ChangeEntry(text = "A 21 Ho(Lc) spoj.", backgroundColor = null, foregroundColor = null, willBeSpecified = null)
        val newData = SubstitutionData(
            lastUpdated = "10:15",
            currentUpdateSchedule = 5,
            data = listOf(
                DailySchedule(
                    date = "2026-09-25",
                    info = DayInfo(inWork = false),
                    changes = listOf(null, newChange),
                    absence = emptyList(),
                    takesPlace = ""
                )
            )
        )

        val changes = checker.checkForChanges(oldData, newData)
        assertEquals(1, changes.size)
        val change = changes[0] as SubstitutionChange.LessonChange
        assertEquals("2026-09-25", change.date)
        assertEquals(2, change.hour) // 2nd hour (1-indexed)
        assertEquals("A 21 Ho(Lc) spoj.", change.newChange.text)
    }

    @Test
    fun testDetectsCancelledLesson()
    {
        val oldChange = ChangeEntry(text = "TV (Lc) odpadá", backgroundColor = null, foregroundColor = null, willBeSpecified = null)
        val oldData = SubstitutionData(
            lastUpdated = "10:00",
            currentUpdateSchedule = 5,
            data = listOf(
                DailySchedule(
                    date = "2026-09-25",
                    info = DayInfo(inWork = false),
                    changes = listOf(oldChange),
                    absence = emptyList(),
                    takesPlace = ""
                )
            )
        )

        val newData = SubstitutionData(
            lastUpdated = "10:15",
            currentUpdateSchedule = 5,
            data = listOf(
                DailySchedule(
                    date = "2026-09-25",
                    info = DayInfo(inWork = false),
                    changes = listOf(null),
                    absence = emptyList(),
                    takesPlace = ""
                )
            )
        )

        val changes = checker.checkForChanges(oldData, newData)
        assertEquals(1, changes.size)
        val change = changes[0] as SubstitutionChange.LessonChangeRemoved
        assertEquals(1, change.hour)
        assertEquals("TV (Lc) odpadá", change.removedChange.text)
    }

    @Test
    fun testDetectsTakesPlaceChange()
    {
        val oldData = SubstitutionData(
            lastUpdated = "10:00",
            currentUpdateSchedule = 5,
            data = listOf(
                DailySchedule(
                    date = "2026-09-25",
                    info = DayInfo(inWork = false),
                    changes = emptyList(),
                    absence = emptyList(),
                    takesPlace = "Původní text"
                )
            )
        )

        val newData = SubstitutionData(
            lastUpdated = "10:15",
            currentUpdateSchedule = 5,
            data = listOf(
                DailySchedule(
                    date = "2026-09-25",
                    info = DayInfo(inWork = false),
                    changes = emptyList(),
                    absence = emptyList(),
                    takesPlace = "Změněný text pro třídu"
                )
            )
        )

        val changes = checker.checkForChanges(oldData, newData)
        assertEquals(1, changes.size)
        val change = changes[0] as SubstitutionChange.TakesPlaceChange
        assertEquals("Změněný text pro třídu", change.newTakesPlace)
    }
}
