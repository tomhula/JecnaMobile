package me.tomasan7.jecnamobile.widget

import me.tomasan7.jecnamobile.timetable.ChangeEntry
import org.junit.Assert.*
import org.junit.Test

class TimetableWidgetRendererTest
{
    @Test
    fun testParseSubstitutionStandardWithSubstituteTeacher()
    {
        val entry = ChangeEntry(text = "A 21 Tu(Lc)", backgroundColor = null, foregroundColor = null, willBeSpecified = false)
        val parsed = TimetableWidgetRenderer.parseSubstitution(entry)

        assertEquals("A", parsed.subject)
        assertEquals("21", parsed.room)
        assertEquals("Tu(Lc)", parsed.teacher)
        assertFalse(parsed.isCancelled)
        assertTrue(parsed.isStructured)
    }

    @Test
    fun testParseSubstitutionMergedClasses()
    {
        val entry1 = ChangeEntry(text = "PIS 19ab Zi(Bc) spoj.", backgroundColor = null, foregroundColor = null, willBeSpecified = false)
        val parsed1 = TimetableWidgetRenderer.parseSubstitution(entry1)

        assertEquals("PIS spoj", parsed1.subject)
        assertEquals("19ab", parsed1.room)
        assertEquals("Zi(Bc)", parsed1.teacher)
        assertFalse(parsed1.isCancelled)
        assertTrue(parsed1.isStructured)

        val entry2 = ChangeEntry(text = "A 21 Bu(Ir) spoj.", backgroundColor = null, foregroundColor = null, willBeSpecified = false)
        val parsed2 = TimetableWidgetRenderer.parseSubstitution(entry2)

        assertEquals("A spoj", parsed2.subject)
        assertEquals("21", parsed2.room)
        assertEquals("Bu(Ir)", parsed2.teacher)
        assertFalse(parsed2.isCancelled)
        assertTrue(parsed2.isStructured)
    }

    @Test
    fun testParseSubstitutionCancelledWithTeacher()
    {
        val entry = ChangeEntry(text = "Odpadá (Dr)", backgroundColor = null, foregroundColor = null, willBeSpecified = false)
        val parsed = TimetableWidgetRenderer.parseSubstitution(entry)

        assertEquals("Odpadá", parsed.subject)
        assertEquals("(Dr)", parsed.teacher)
        assertTrue(parsed.isCancelled)
        assertTrue(parsed.isStructured)
    }

    @Test
    fun testParseSubstitutionWillBeSpecified()
    {
        val entry = ChangeEntry(text = "DS 19a Ka", backgroundColor = null, foregroundColor = null, willBeSpecified = true)
        val parsed = TimetableWidgetRenderer.parseSubstitution(entry)

        assertEquals("DS", parsed.subject)
        assertEquals("19a", parsed.room)
        assertEquals("Ka", parsed.teacher)
        assertEquals("(upřesní se)", parsed.note)
        assertTrue(parsed.isStructured)
    }

    @Test
    fun testParseSubstitutionGeneralText()
    {
        val entry = ChangeEntry(text = "Exkurze do planetária", backgroundColor = null, foregroundColor = null, willBeSpecified = false)
        val parsed = TimetableWidgetRenderer.parseSubstitution(entry)

        assertEquals("Exkurze do planetária", parsed.subject)
        assertNull(parsed.room)
        assertNull(parsed.teacher)
        assertFalse(parsed.isStructured)
    }

    @Test
    fun testCalculateTargetDaySwitchAfterOneHour()
    {
        val wednesdayAfternoon = kotlinx.datetime.LocalDateTime(2026, 9, 23, 15, 0)
        val (_, _, titleBefore) = TimetableWidgetUpdater.calculateTargetDay(null, wednesdayAfternoon)
        assertEquals("Dnes", titleBefore)

        val wednesdayEvening = kotlinx.datetime.LocalDateTime(2026, 9, 23, 17, 0)
        val (nextDay, _, titleAfter) = TimetableWidgetUpdater.calculateTargetDay(null, wednesdayEvening)
        assertEquals(kotlinx.datetime.DayOfWeek.THURSDAY, nextDay)
        assertEquals("Zítra", titleAfter)

        val fridayEvening = kotlinx.datetime.LocalDateTime(2026, 9, 25, 17, 0)
        val (mondayDay, _, fridayTitle) = TimetableWidgetUpdater.calculateTargetDay(null, fridayEvening)
        assertEquals(kotlinx.datetime.DayOfWeek.MONDAY, mondayDay)
        assertEquals("V pondělí", fridayTitle)

        val saturday = kotlinx.datetime.LocalDateTime(2026, 9, 26, 12, 0)
        val (satTargetDay, _, satTitle) = TimetableWidgetUpdater.calculateTargetDay(null, saturday)
        assertEquals(kotlinx.datetime.DayOfWeek.MONDAY, satTargetDay)
        assertEquals("V pondělí", satTitle)
    }

    @Test
    fun testBreakHighlightsUpcomingPeriodInSimulation()
    {
        val settings = me.tomasan7.jecnamobile.settings.Settings.WidgetSettings(
            debugSimulationEnabled = true,
            debugSimulatedDay = "WEDNESDAY",
            debugPeriodIndex = 2,
            debugIsBreak = true
        )
        val now = kotlinx.datetime.LocalDateTime(2026, 9, 23, 10, 0)
        val activePair = TimetableWidgetRenderer.getActiveOrUpcomingPeriod(null, settings, now)
        assertNotNull(activePair)
        assertEquals(kotlinx.datetime.DayOfWeek.WEDNESDAY, activePair?.first)
        assertEquals(2, activePair?.second)

        val isActive = TimetableWidgetRenderer.isPeriodActive(
            settings = settings,
            day = kotlinx.datetime.DayOfWeek.WEDNESDAY,
            periodIndex = 2,
            spot = null,
            activeSpot = null,
            nowLocal = now
        )
        assertTrue(isActive)
    }

    @Test
    fun testLastNonFreeHourCalculation()
    {
        val lastIdx = TimetableWidgetRenderer.getLastNonFreePeriodIndex(
            timetableData = null,
            timetable = null,
            day = kotlinx.datetime.DayOfWeek.FRIDAY
        )
        assertEquals(-1, lastIdx)

        val lastEndTime = TimetableWidgetRenderer.getLastNonFreeLessonEndTime(
            timetableData = null,
            timetable = null,
            day = kotlinx.datetime.DayOfWeek.FRIDAY
        )
        assertNull(lastEndTime)
    }
}
