package me.tomasan7.jecnamobile.widget

import android.content.Context
import android.graphics.*
import androidx.core.content.ContextCompat
import io.github.tomhula.jecnaapi.data.timetable.Lesson
import io.github.tomhula.jecnaapi.data.timetable.LessonSpot
import kotlin.time.Clock
import kotlinx.datetime.*
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.settings.Settings
import me.tomasan7.jecnamobile.timetable.ChangeEntry
import me.tomasan7.jecnamobile.timetable.DailySchedule
import me.tomasan7.jecnamobile.timetable.TimetableData
import kotlin.math.max
import kotlin.math.min

object TimetableWidgetRenderer
{
    private val SCHOOL_DAYS = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    )

    fun isColorLight(colorLong: Long): Boolean
    {
        val r = ((colorLong shr 16) and 0xFF).toDouble()
        val g = ((colorLong shr 8) and 0xFF).toDouble()
        val b = (colorLong and 0xFF).toDouble()
        val luminance = 0.299 * r + 0.587 * g + 0.114 * b
        return luminance > 128.0
    }

    fun extractClassName(timetableData: TimetableData?): String?
    {
        val timetable = timetableData?.page?.timetable ?: return null
        for (day in SCHOOL_DAYS)
        {
            val spots = timetable.getLessonSpotsForDay(day) ?: continue
            for (spot in spots)
            {
                val clazz = spot.firstOrNull { !it.clazz.isNullOrBlank() }?.clazz
                if (!clazz.isNullOrBlank())
                {
                    return clazz
                }
            }
        }
        return null
    }

    data class SubjectGroupInfo(
        val subjectShort: String,
        val subjectFull: String?,
        val availableGroups: List<String>
    )

    fun extractGroupSubjects(timetableData: TimetableData?): List<SubjectGroupInfo>
    {
        val timetable = timetableData?.page?.timetable ?: return emptyList()
        val map = mutableMapOf<String, Pair<String?, MutableSet<String>>>()

        for (day in SCHOOL_DAYS)
        {
            val spots = timetable.getLessonSpotsForDay(day) ?: continue
            for (spot in spots)
            {
                for (lesson in spot)
                {
                    val short = lesson.subjectName.short
                    val grp = lesson.group
                    if (!short.isNullOrBlank() && !grp.isNullOrBlank())
                    {
                        val entry = map.getOrPut(short) { Pair(lesson.subjectName.full, mutableSetOf()) }
                        entry.second.add(grp)
                    }
                }
            }
        }

        return map.map { (short, pair) ->
            SubjectGroupInfo(
                subjectShort = short,
                subjectFull = pair.first,
                availableGroups = pair.second.sorted()
            )
        }.sortedBy { it.subjectShort }
    }

    /**
     * Resolves which lesson from a spot should be shown for a student according to their configured groups.
     * If the spot has multiple lessons, searches for a lesson matching the student's group.
     * If the subject is configured by the student but this spot only has other groups, returns null (student is free).
     * If unconfigured, falls back to spot.firstOrNull().
     */
    fun resolveLessonForSpot(
        spot: LessonSpot?,
        subjectGroups: Map<String, String>,
        defaultHalfGroup: String? = null
    ): Lesson?
    {
        if (spot == null || spot.isEmpty()) return null

        fun effectiveGroup(subjectShort: String?, lessonGroup: String?): String?
        {
            if (!subjectShort.isNullOrBlank() && !subjectGroups[subjectShort].isNullOrBlank())
            {
                return subjectGroups[subjectShort]
            }
            if ((lessonGroup == "1/2" || lessonGroup == "2/2") && !defaultHalfGroup.isNullOrBlank())
            {
                return defaultHalfGroup
            }
            return null
        }

        if (spot.size == 1)
        {
            val onlyLesson = spot.first()
            val short = onlyLesson.subjectName.short
            val userGroup = effectiveGroup(short, onlyLesson.group)
            if (!userGroup.isNullOrBlank() && !onlyLesson.group.isNullOrBlank() && onlyLesson.group != userGroup)
            {
                return null
            }
            return onlyLesson
        }

        // Multiple lessons in this spot: find the one matching user's configured group
        for (lesson in spot)
        {
            val short = lesson.subjectName.short
            val userGroup = effectiveGroup(short, lesson.group)
            if (!userGroup.isNullOrBlank() && lesson.group == userGroup)
            {
                return lesson
            }
        }

        val anyConfigured = spot.any {
            val s = it.subjectName.short
            val g = effectiveGroup(s, it.group)
            !g.isNullOrBlank()
        }
        if (anyConfigured)
        {
            return null
        }

        return spot.firstOrNull()
    }

    fun getEffectiveColors(settings: Settings.WidgetSettings): Settings.WidgetColors
    {
        val baseColors = when (settings.theme)
        {
            Settings.WidgetTheme.LIGHT -> Settings.WidgetColors(
                backgroundColor = 0xFFFFFFFF,
                borderColor = 0xFFD0D7DE,
                dayHeaderColor = 0xFFF1F3F5,
                dayHeaderTextColor = 0xFF1F2328,
                cellBackgroundColor = 0xFFF6F8FA,
                cellTextColor = 0xFF1F2328,
                teacherTextColor = 0xFF57606A,
                classroomTextColor = 0xFF57606A,
                groupTextColor = 0xFF57606A,
                activeHourBorderColor = 0xFF0969DA,
                activeHourFillColor = 0x330969DA,
                substitutionColor = 0xFFD97706,
                substitutionTextColor = 0xFFD97706,
                substitutionBackgroundColor = 0x33D97706
            )
            Settings.WidgetTheme.DARK -> Settings.WidgetColors(
                backgroundColor = 0xFF000000,
                borderColor = 0xFF30363D,
                dayHeaderColor = 0xFF161B22,
                dayHeaderTextColor = 0xFFE6EDF3,
                cellBackgroundColor = 0xFF0D1117,
                cellTextColor = 0xFFE6EDF3,
                teacherTextColor = 0xFF8B949E,
                classroomTextColor = 0xFF8B949E,
                groupTextColor = 0xFF8B949E,
                activeHourBorderColor = 0xFF2F81F7,
                activeHourFillColor = 0x332F81F7,
                substitutionColor = 0xFFE3B341,
                substitutionTextColor = 0xFFE3B341,
                substitutionBackgroundColor = 0x4DE3B341
            )
            Settings.WidgetTheme.CUSTOM -> settings.customColors
        }

        if (settings.solidHeaderBackground)
        {
            val isLightAccent = isColorLight(baseColors.borderColor)
            val headerText = if (isLightAccent) 0xFF000000L else 0xFFFFFFFFL
            return baseColors.copy(
                dayHeaderColor = baseColors.borderColor,
                dayHeaderTextColor = headerText
            )
        }

        return baseColors
    }

    fun getShortDayName(day: DayOfWeek): String = when (day)
    {
        DayOfWeek.MONDAY    -> "Po"
        DayOfWeek.TUESDAY   -> "Út"
        DayOfWeek.WEDNESDAY -> "St"
        DayOfWeek.THURSDAY  -> "Čt"
        DayOfWeek.FRIDAY    -> "Pá"
        DayOfWeek.SATURDAY  -> "So"
        DayOfWeek.SUNDAY    -> "Ne"
    }

    fun getSimulatedDay(settings: Settings.WidgetSettings, nowLocal: LocalDateTime): DayOfWeek
    {
        if (!settings.debugSimulationEnabled)
        {
            return when (nowLocal.dayOfWeek)
            {
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> DayOfWeek.MONDAY
                else -> nowLocal.dayOfWeek
            }
        }
        return settings.debugSimulatedDay?.let { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }
            ?: when (nowLocal.dayOfWeek)
            {
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> DayOfWeek.MONDAY
                else -> nowLocal.dayOfWeek
            }
    }

    fun isPeriodNonFree(
        timetableData: TimetableData?,
        timetable: io.github.tomhula.jecnaapi.data.timetable.Timetable?,
        day: DayOfWeek,
        periodIndex: Int,
        subjectGroups: Map<String, String> = emptyMap(),
        settings: Settings.WidgetSettings? = null,
        nowLocal: LocalDateTime? = null
    ): Boolean
    {
        val tt = timetable ?: timetableData?.page?.timetable ?: return false
        val lessonSpots = tt.getLessonSpotsForDay(day) ?: return false
        val spot = lessonSpots.getOrNull(periodIndex)
        val userLesson = resolveLessonForSpot(spot, subjectGroups, settings?.userProfileGroup) ?: spot?.firstOrNull()

        val substitutionsMap: Map<DayOfWeek, DailySchedule> = try {
            timetableData?.substitutions?.data?.associate { item ->
                LocalDate.parse(item.date).dayOfWeek to item
            } ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
        val daySubstitutions = substitutionsMap[day]?.changes ?: emptyList()
        val baseSub = daySubstitutions.getOrNull(periodIndex)

        val (isFreeOverride, effectiveSub) = if (settings != null && nowLocal != null) {
            getEffectiveSubstitutionForDayAndPeriod(
                settings = settings,
                day = day,
                periodIndex = periodIndex,
                baseSubstitution = baseSub,
                baseLesson = userLesson,
                nowLocal = nowLocal
            )
        } else {
            Pair(false, baseSub)
        }

        if (isFreeOverride) return false
        if (effectiveSub != null)
        {
            val parsed = parseSubstitution(effectiveSub)
            return !parsed.isCancelled
        }
        return userLesson != null
    }

    fun getLastNonFreePeriodIndex(
        timetableData: TimetableData?,
        timetable: io.github.tomhula.jecnaapi.data.timetable.Timetable?,
        day: DayOfWeek,
        subjectGroups: Map<String, String> = emptyMap(),
        settings: Settings.WidgetSettings? = null,
        nowLocal: LocalDateTime? = null
    ): Int
    {
        val tt = timetable ?: timetableData?.page?.timetable ?: return -1
        val lessonSpots = tt.getLessonSpotsForDay(day) ?: return -1
        val periods = tt.lessonPeriods
        val maxPeriod = minOf(lessonSpots.size, periods.size) - 1

        for (i in maxPeriod downTo 0)
        {
            if (isPeriodNonFree(timetableData, tt, day, i, subjectGroups, settings, nowLocal))
            {
                return i
            }
        }
        return -1
    }

    fun getLastNonFreeLessonEndTime(
        timetableData: TimetableData?,
        timetable: io.github.tomhula.jecnaapi.data.timetable.Timetable?,
        day: DayOfWeek,
        subjectGroups: Map<String, String> = emptyMap(),
        settings: Settings.WidgetSettings? = null,
        nowLocal: LocalDateTime? = null
    ): LocalTime?
    {
        val tt = timetable ?: timetableData?.page?.timetable ?: return null
        val lastIdx = getLastNonFreePeriodIndex(timetableData, tt, day, subjectGroups, settings, nowLocal)
        if (lastIdx < 0) return null
        return tt.lessonPeriods.getOrNull(lastIdx)?.to
    }

    fun getActiveOrUpcomingPeriod(
        timetable: io.github.tomhula.jecnaapi.data.timetable.Timetable?,
        settings: Settings.WidgetSettings,
        nowLocal: LocalDateTime
    ): Pair<DayOfWeek, Int>?
    {
        if (settings.debugSimulationEnabled)
        {
            if (settings.debugPeriodIndex < 0) return null
            val simDay = getSimulatedDay(settings, nowLocal)
            return Pair(simDay, settings.debugPeriodIndex)
        }

        if (timetable == null) return null
        val today = nowLocal.date
        if (today.dayOfWeek == DayOfWeek.SATURDAY || today.dayOfWeek == DayOfWeek.SUNDAY)
        {
            return null
        }

        val periods = timetable.lessonPeriods
        if (periods.isEmpty()) return null

        val currentTime = nowLocal.time
        val lastNonFreeIndex = getLastNonFreePeriodIndex(
            timetableData = null,
            timetable = timetable,
            day = today.dayOfWeek,
            settings = settings,
            nowLocal = nowLocal
        )
        val lastLessonEndTime = if (lastNonFreeIndex >= 0) periods.getOrNull(lastNonFreeIndex)?.to else null

        if (lastLessonEndTime != null && currentTime >= lastLessonEndTime)
        {
            return null
        }

        val maxSearchIndex = if (lastNonFreeIndex >= 0) lastNonFreeIndex else (periods.size - 1)

        // Inside an active period:
        for (i in 0..maxSearchIndex)
        {
            val p = periods.getOrNull(i) ?: continue
            if (currentTime >= p.from && currentTime < p.to)
            {
                return Pair(today.dayOfWeek, i)
            }
        }

        // During a break or before school: upcoming period
        for (i in 0..maxSearchIndex)
        {
            val p = periods.getOrNull(i) ?: continue
            if (currentTime < p.from)
            {
                return Pair(today.dayOfWeek, i)
            }
        }

        return null
    }

    fun isPeriodActive(
        settings: Settings.WidgetSettings,
        day: DayOfWeek,
        periodIndex: Int,
        spot: LessonSpot?,
        activeSpot: LessonSpot?,
        nowLocal: LocalDateTime,
        timetable: io.github.tomhula.jecnaapi.data.timetable.Timetable? = null
    ): Boolean
    {
        val activePair = getActiveOrUpcomingPeriod(timetable, settings, nowLocal)
        if (activePair != null)
        {
            return day == activePair.first && periodIndex == activePair.second
        }
        if (settings.debugSimulationEnabled)
        {
            val simDay = getSimulatedDay(settings, nowLocal)
            if (day != simDay) return false
            return periodIndex == settings.debugPeriodIndex
        }
        return spot != null && activeSpot != null && spot === activeSpot
    }

    fun getEffectiveSubstitutionForDayAndPeriod(
        settings: Settings.WidgetSettings,
        day: DayOfWeek,
        periodIndex: Int,
        baseSubstitution: ChangeEntry?,
        baseLesson: Lesson?,
        nowLocal: LocalDateTime
    ): Pair<Boolean, ChangeEntry?>
    {
        if (!settings.debugSimulationEnabled)
        {
            return Pair(false, baseSubstitution)
        }

        val overrideKey = "${day.name}_$periodIndex"
        val overrideVal = settings.debugWeekOverrides[overrideKey]
        if (overrideVal != null)
        {
            return when (overrideVal)
            {
                1 -> Pair(false, ChangeEntry(text = "Odpadá", backgroundColor = null, foregroundColor = null, willBeSpecified = false))
                2 -> {
                    val subjShort = baseLesson?.subjectName?.short ?: "Změna"
                    Pair(false, ChangeEntry(text = "$subjShort 21\nTu(Lc)", backgroundColor = null, foregroundColor = null, willBeSpecified = false))
                }
                3 -> Pair(true, null) // Volná hodina: explicit empty/free
                else -> Pair(false, baseSubstitution)
            }
        }

        val simDay = getSimulatedDay(settings, nowLocal)
        if (day == simDay && periodIndex == settings.debugPeriodIndex && settings.debugSubstitutionType != 0)
        {
            return when (settings.debugSubstitutionType)
            {
                1 -> Pair(false, ChangeEntry(text = "Odpadá", backgroundColor = null, foregroundColor = null, willBeSpecified = false))
                2 -> {
                    val subjShort = baseLesson?.subjectName?.short ?: "M"
                    Pair(false, ChangeEntry(text = "$subjShort 21\nTu(Lc)", backgroundColor = null, foregroundColor = null, willBeSpecified = false))
                }
                3 -> Pair(true, null) // Volná hodina: explicit empty/free
                else -> Pair(false, baseSubstitution)
            }
        }

        return Pair(false, baseSubstitution)
    }

    /**
     * Determines the maximum period across the week that contains at least one lesson or substitution.
     * For example, if the longest day in a week has lessons up to hour 9, this returns 9 so hour 10 is omitted.
     */
    fun getWeekPeriodCount(timetableData: TimetableData?): Int
    {
        val timetable = timetableData?.page?.timetable ?: return 10
        val substitutionsMap: Map<DayOfWeek, DailySchedule> = try {
            timetableData.substitutions?.data?.associate { item ->
                LocalDate.parse(item.date).dayOfWeek to item
            } ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }

        var maxPeriodIndex = -1

        for (day in SCHOOL_DAYS)
        {
            val spots = timetable.getLessonSpotsForDay(day) ?: emptyList()
            for ((idx, spot) in spots.withIndex())
            {
                if (spot.isNotEmpty())
                {
                    val endIdx = idx + (spot.periodSpan - 1).coerceAtLeast(0)
                    if (endIdx > maxPeriodIndex)
                    {
                        maxPeriodIndex = endIdx
                    }
                }
            }

            val changes = substitutionsMap[day]?.changes ?: emptyList()
            for ((idx, change) in changes.withIndex())
            {
                if (change != null && idx > maxPeriodIndex)
                {
                    maxPeriodIndex = idx
                }
            }
        }

        if (maxPeriodIndex < 0)
        {
            return if (timetable.lessonPeriods.isNotEmpty()) min(timetable.lessonPeriods.size, 10) else 10
        }

        val longestDayHours = maxPeriodIndex + 1
        return longestDayHours.coerceIn(5, 10)
    }

    /**
     * Renders the complete 5-day week grid onto a Bitmap.
     */
    fun renderWeekTimetableBitmap(
        context: Context,
        timetableData: TimetableData?,
        settings: Settings.WidgetSettings,
        width: Int,
        height: Int,
        activeSpot: LessonSpot?,
        subjectGroups: Map<String, String> = emptyMap()
    ): Bitmap
    {
        val w = if (width > 0) width else 900
        val h = if (height > 0) height else 550
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val colors = getEffectiveColors(settings)

        // Clear background
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        if (timetableData == null)
        {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.cellTextColor.toInt()
                textSize = h * 0.06f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Rozvrh není načten. Otevřete aplikaci.", w / 2f, h / 2f, textPaint)
            return bitmap
        }

        val timetable = timetableData.page.timetable
        val lessonPeriods = timetable.lessonPeriods
        val periodCount = getWeekPeriodCount(timetableData)

        val substitutionsMap: Map<DayOfWeek, DailySchedule> = try {
            timetableData.substitutions?.data?.associate { item ->
                val date = LocalDate.parse(item.date)
                date.dayOfWeek to item
            } ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }

        val pad = 4f
        val headerRowHeight = (h - pad * 2) * (0.8f / 10.8f)
        val dayColFraction = 0.75f / (0.75f + periodCount)
        val dayColWidth = (w - pad * 2) * dayColFraction
        val gridWidth = (w - pad * 2) - dayColWidth
        val gridHeight = (h - pad * 2) - headerRowHeight
        val colWidth = gridWidth / periodCount
        val rowHeight = gridHeight / SCHOOL_DAYS.size

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.borderColor.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.dayHeaderColor.toInt()
            style = Paint.Style.FILL
        }

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.dayHeaderTextColor.toInt()
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val cellBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.cellBackgroundColor.toInt()
            style = Paint.Style.FILL
        }

        val activeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.activeHourBorderColor.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        val activeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.activeHourFillColor.toInt()
            style = Paint.Style.FILL
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.cellTextColor.toInt()
            typeface = Typeface.DEFAULT
        }

        val boldTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.cellTextColor.toInt()
            typeface = Typeface.DEFAULT_BOLD
        }

        val teacherPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.teacherTextColor.toInt()
            typeface = Typeface.DEFAULT
        }

        val roomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.classroomTextColor.toInt()
            typeface = Typeface.DEFAULT
        }

        val groupPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.groupTextColor.toInt()
            typeface = Typeface.DEFAULT
        }

        val scale = settings.cellScale.coerceIn(0.7f, 1.6f)

        val tableRect = RectF(pad, pad, w - pad, h - pad)

        // Draw top-left corner
        val cornerRect = RectF(pad, pad, pad + dayColWidth, pad + headerRowHeight)
        canvas.drawRect(cornerRect, headerBgPaint)
        canvas.drawRect(cornerRect, borderPaint)

        // Draw refresh icon in top-left corner only if the top header bar is hidden
        if (settings.hideHeader)
        {
            val refreshIconColor = if (settings.solidHeaderBackground) colors.dayHeaderTextColor.toInt() else Color.WHITE
            drawRefreshIcon(context, canvas, cornerRect, refreshIconColor)
        }

        // Draw header row (periods)
        val periodHeaderMode = settings.getEffectivePeriodHeaderMode()
        for (i in 0 until periodCount)
        {
            val left = pad + dayColWidth + (i * colWidth)
            val right = left + colWidth
            val rect = RectF(left, pad, right, pad + headerRowHeight)

            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)

            when (periodHeaderMode)
            {
                Settings.PeriodHeaderMode.NUMBER_ONLY ->
                {
                    val periodText = (i + 1).toString()
                    val baseSize = headerRowHeight * 0.55f * scale
                    headerTextPaint.textSize = baseSize
                    val maxTextWidth = colWidth * 0.82f
                    val textWidth = headerTextPaint.measureText(periodText)
                    if (textWidth > maxTextWidth && textWidth > 0)
                    {
                        headerTextPaint.textSize = baseSize * (maxTextWidth / textWidth)
                    }
                    val fontMetrics = headerTextPaint.fontMetrics
                    val baseline = rect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2
                    canvas.drawText(periodText, rect.centerX(), baseline, headerTextPaint)
                }
                Settings.PeriodHeaderMode.TIME_ONLY ->
                {
                    val periodText = if (i < lessonPeriods.size) lessonPeriods[i].toString() else (i + 1).toString()
                    val baseSize = headerRowHeight * 0.32f * scale
                    headerTextPaint.textSize = baseSize
                    val maxTextWidth = colWidth * 0.88f
                    val textWidth = headerTextPaint.measureText(periodText)
                    if (textWidth > maxTextWidth && textWidth > 0)
                    {
                        headerTextPaint.textSize = baseSize * (maxTextWidth / textWidth)
                    }
                    val fontMetrics = headerTextPaint.fontMetrics
                    val baseline = rect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2
                    canvas.drawText(periodText, rect.centerX(), baseline, headerTextPaint)
                }
                Settings.PeriodHeaderMode.BOTH ->
                {
                    val numText = (i + 1).toString()
                    val timeText = if (i < lessonPeriods.size) lessonPeriods[i].toString() else ""

                    // Top line: period number
                    val numBaseSize = headerRowHeight * 0.38f * scale
                    headerTextPaint.textSize = numBaseSize
                    val maxNumWidth = colWidth * 0.82f
                    val numWidth = headerTextPaint.measureText(numText)
                    if (numWidth > maxNumWidth && numWidth > 0)
                    {
                        headerTextPaint.textSize = numBaseSize * (maxNumWidth / numWidth)
                    }
                    val numFm = headerTextPaint.fontMetrics
                    val numBaseline = rect.centerY() - (headerRowHeight * 0.16f) - (numFm.ascent + numFm.descent) / 2
                    canvas.drawText(numText, rect.centerX(), numBaseline, headerTextPaint)

                    // Bottom line: time range
                    if (timeText.isNotEmpty())
                    {
                        val timeBaseSize = headerRowHeight * 0.26f * scale
                        headerTextPaint.textSize = timeBaseSize
                        val maxTimeWidth = colWidth * 0.90f
                        val timeWidth = headerTextPaint.measureText(timeText)
                        if (timeWidth > maxTimeWidth && timeWidth > 0)
                        {
                            headerTextPaint.textSize = timeBaseSize * (maxTimeWidth / timeWidth)
                        }
                        val timeFm = headerTextPaint.fontMetrics
                        val timeBaseline = rect.centerY() + (headerRowHeight * 0.22f) - (timeFm.ascent + timeFm.descent) / 2
                        canvas.drawText(timeText, rect.centerX(), timeBaseline, headerTextPaint)
                    }
                }
            }
        }

        var activeHourRect: RectF? = null

        // Draw day rows and cells
        for ((dayIndex, day) in SCHOOL_DAYS.viewIndexed())
        {
            val top = pad + headerRowHeight + (dayIndex * rowHeight)
            val bottom = top + rowHeight

            // Day label
            val dayRect = RectF(pad, top, pad + dayColWidth, bottom)
            canvas.drawRect(dayRect, headerBgPaint)
            canvas.drawRect(dayRect, borderPaint)

            val dayText = getShortDayName(day)
            val baseDaySize = min(rowHeight * 0.35f * scale, dayColWidth * 0.48f * scale)
            headerTextPaint.textSize = baseDaySize
            val maxDayTextW = dayColWidth * 0.78f
            val dayTextW = headerTextPaint.measureText(dayText)
            if (dayTextW > maxDayTextW && dayTextW > 0)
            {
                headerTextPaint.textSize = baseDaySize * (maxDayTextW / dayTextW)
            }
            val fm = headerTextPaint.fontMetrics
            val dayBaseline = dayRect.centerY() - (fm.ascent + fm.descent) / 2
            canvas.drawText(dayText, dayRect.centerX(), dayBaseline, headerTextPaint)

            val lessonSpots = timetable.getLessonSpotsForDay(day) ?: emptyList()
            val daySubstitutions = substitutionsMap[day]?.changes ?: emptyList()
            val nowLocal = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

            for (colIndex in 0 until periodCount)
            {
                val left = pad + dayColWidth + (colIndex * colWidth)
                val right = left + colWidth
                val cellRect = RectF(left, top, right, bottom)

                val spot = lessonSpots.getOrNull(colIndex)
                val isCurrentHour = isPeriodActive(settings, day, colIndex, spot, activeSpot, nowLocal, timetable)
                val baseLesson = resolveLessonForSpot(spot, subjectGroups, settings.userProfileGroup) ?: spot?.firstOrNull()
                val (isFreeOverride, substitution) = getEffectiveSubstitutionForDayAndPeriod(
                    settings = settings,
                    day = day,
                    periodIndex = colIndex,
                    baseSubstitution = daySubstitutions.getOrNull(colIndex),
                    baseLesson = baseLesson,
                    nowLocal = nowLocal
                )

                // Background
                canvas.drawRect(cellRect, cellBgPaint)

                if (isCurrentHour)
                {
                    activeHourRect = RectF(cellRect)
                    canvas.drawRect(cellRect, activeFillPaint)
                }

                if (isFreeOverride)
                {
                    // Volná hodina: explicit empty cell
                }
                else if (substitution != null)
                {
                    drawSubstitutionCell(
                        canvas = canvas,
                        rect = cellRect,
                        substitution = substitution,
                        settings = settings,
                        colors = colors,
                        isCurrentHour = isCurrentHour,
                        useRoundRect = false
                    )
                }
                else if (spot != null && spot.isNotEmpty())
                {
                    if (settings.filterMyGroups)
                    {
                        val myLesson = resolveLessonForSpot(spot, subjectGroups, settings.userProfileGroup)
                        if (myLesson != null)
                        {
                            drawSingleLesson(
                                canvas = canvas,
                                rect = cellRect,
                                lesson = myLesson,
                                settings = settings,
                                boldPaint = boldTextPaint,
                                smallPaint = textPaint,
                                teacherPaint = teacherPaint,
                                roomPaint = roomPaint,
                                groupPaint = groupPaint,
                                isSubstituted = false
                            )
                        }
                    }
                    else
                    {
                        val lessons = spot.toList()
                        if (lessons.size == 1)
                        {
                            drawSingleLesson(
                                canvas = canvas,
                                rect = cellRect,
                                lesson = lessons[0],
                                settings = settings,
                                boldPaint = boldTextPaint,
                                smallPaint = textPaint,
                                teacherPaint = teacherPaint,
                                roomPaint = roomPaint,
                                groupPaint = groupPaint,
                                isSubstituted = false
                            )
                        }
                        else
                        {
                            val count = lessons.size
                            val slotHeight = (bottom - top) / count
                            for (i in 0 until count)
                            {
                                val slotTop = top + (i * slotHeight)
                                val slotBottom = slotTop + slotHeight
                                val slotRect = RectF(left, slotTop, right, slotBottom)

                                val lesson = lessons[i]
                                val isUserGroup = !lesson.group.isNullOrBlank() &&
                                    subjectGroups[lesson.subjectName.short] == lesson.group

                                if (isUserGroup)
                                {
                                    val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        color = (colors.activeHourFillColor.toInt() and 0x44FFFFFF)
                                        style = Paint.Style.FILL
                                    }
                                    canvas.drawRect(slotRect, highlightPaint)
                                }

                                if (i > 0)
                                {
                                    canvas.drawLine(left, slotTop, right, slotTop, borderPaint)
                                }

                                drawSplitLesson(
                                    canvas = canvas,
                                    rect = slotRect,
                                    lesson = lesson,
                                    settings = settings,
                                    boldPaint = boldTextPaint,
                                    smallPaint = textPaint,
                                    teacherPaint = teacherPaint,
                                    roomPaint = roomPaint,
                                    groupPaint = groupPaint,
                                    isUserGroup = isUserGroup
                                )
                            }
                        }
                    }
                }

                // Draw uniform cell border for all cells in the grid table
                canvas.drawRect(cellRect, borderPaint)
            }
        }

        // Overlay active hour border so it is completely even on all 4 sides and not overwritten by neighbors
        if (activeHourRect != null)
        {
            canvas.drawRect(activeHourRect, activeBorderPaint)
        }

        if (!settings.hideBorder)
        {
            canvas.drawRect(tableRect, borderPaint)
        }

        return bitmap
    }

    private fun drawSingleLesson(
        canvas: Canvas,
        rect: RectF,
        lesson: Lesson,
        settings: Settings.WidgetSettings,
        boldPaint: Paint,
        smallPaint: Paint,
        teacherPaint: Paint,
        roomPaint: Paint,
        groupPaint: Paint,
        isSubstituted: Boolean
    )
    {
        drawLessonCell(
            canvas = canvas,
            rect = rect,
            subject = lesson.subjectName.short ?: "",
            teacher = lesson.teacherName?.short,
            room = lesson.classroom,
            clazz = lesson.clazz,
            group = lesson.group,
            settings = settings,
            boldPaint = boldPaint,
            smallPaint = smallPaint,
            teacherPaint = teacherPaint,
            roomPaint = roomPaint,
            groupPaint = groupPaint
        )
    }

    fun drawLessonCell(
        canvas: Canvas,
        rect: RectF,
        subject: String,
        teacher: String?,
        room: String?,
        clazz: String?,
        group: String?,
        settings: Settings.WidgetSettings,
        boldPaint: Paint,
        smallPaint: Paint,
        teacherPaint: Paint,
        roomPaint: Paint,
        groupPaint: Paint
    )
    {
        val cellW = rect.width()
        val cellH = rect.height()
        val scale = settings.cellScale.coerceIn(0.7f, 1.6f)

        // 1. Calculate corner item text sizes with safety limits
        val cornerRefDimension = min(cellH, cellW * 1.15f)
        val cornerBaseSize = (cornerRefDimension * 0.18f * scale).coerceAtMost(cornerRefDimension * 0.24f)
        teacherPaint.textSize = cornerBaseSize
        roomPaint.textSize = cornerBaseSize
        smallPaint.textSize = cornerBaseSize
        groupPaint.textSize = cornerBaseSize

        val cornerPadding = 4f

        // Teacher (top-left) & Room (top-right)
        val hasTeacher = !settings.hideTeachers && !teacher.isNullOrBlank()
        val teacherStr = if (hasTeacher) teacher else ""
        val roomStr = room ?: ""

        val maxTopLeftW = cellW * 0.52f - cornerPadding
        val maxTopRightW = cellW * 0.44f - cornerPadding

        if (teacherStr.isNotEmpty())
        {
            val tWidth = teacherPaint.measureText(teacherStr)
            if (tWidth > maxTopLeftW && tWidth > 0)
            {
                teacherPaint.textSize = cornerBaseSize * (maxTopLeftW / tWidth)
            }
            teacherPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(teacherStr, rect.left + cornerPadding, rect.top + teacherPaint.textSize + 2f, teacherPaint)
        }

        if (roomStr.isNotEmpty())
        {
            val rWidth = roomPaint.measureText(roomStr)
            if (rWidth > maxTopRightW && rWidth > 0)
            {
                roomPaint.textSize = cornerBaseSize * (maxTopRightW / rWidth)
            }
            roomPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(roomStr, rect.right - cornerPadding, rect.top + roomPaint.textSize + 2f, roomPaint)
        }

        // Class (bottom-left) & Group (bottom-right)
        val hasClass = !settings.hideClass && !clazz.isNullOrBlank()
        val classStr = if (hasClass) clazz else ""
        val groupStr = group ?: ""

        val maxBotLeftW = cellW * 0.52f - cornerPadding
        val maxBotRightW = cellW * 0.44f - cornerPadding

        if (classStr.isNotEmpty())
        {
            val cWidth = smallPaint.measureText(classStr)
            if (cWidth > maxBotLeftW && cWidth > 0)
            {
                smallPaint.textSize = cornerBaseSize * (maxBotLeftW / cWidth)
            }
            smallPaint.textAlign = Paint.Align.LEFT
            val classBaseline = rect.bottom - smallPaint.fontMetrics.descent - 3.5f
            canvas.drawText(classStr, rect.left + cornerPadding, classBaseline, smallPaint)
        }

        if (groupStr.isNotEmpty())
        {
            val gWidth = groupPaint.measureText(groupStr)
            if (gWidth > maxBotRightW && gWidth > 0)
            {
                groupPaint.textSize = cornerBaseSize * (maxBotRightW / gWidth)
            }
            groupPaint.textAlign = Paint.Align.RIGHT
            val groupBaseline = rect.bottom - groupPaint.fontMetrics.descent - 3.5f
            canvas.drawText(groupStr, rect.right - cornerPadding, groupBaseline, groupPaint)
        }

        // 2. Subject name (center) - Guaranteed never to overflow in width or height
        if (subject.isNotEmpty())
        {
            val topOccupied = if (teacherStr.isNotEmpty() || roomStr.isNotEmpty()) cornerBaseSize + 4f else 2f
            val botOccupied = if (classStr.isNotEmpty() || groupStr.isNotEmpty()) cornerBaseSize + 6f else 2f
            val maxSubjectH = (cellH - topOccupied - botOccupied).coerceAtLeast(cellH * 0.25f)
            val maxSubjectW = (cellW - 8f).coerceAtLeast(10f)

            var subjectSize = (cellH * 0.38f * scale).coerceAtMost(maxSubjectH * 0.90f)
            boldPaint.textSize = subjectSize
            val measuredW = boldPaint.measureText(subject)
            if (measuredW > maxSubjectW && measuredW > 0)
            {
                subjectSize *= (maxSubjectW / measuredW)
                boldPaint.textSize = subjectSize
            }

            boldPaint.textAlign = Paint.Align.CENTER
            val fmSubject = boldPaint.fontMetrics
            val subjectY = rect.centerY() - (fmSubject.ascent + fmSubject.descent) / 2
            canvas.drawText(subject, rect.centerX(), subjectY, boldPaint)
        }
    }

    /**
     * Renders a representative live preview bitmap (1 day / 4 periods) matching the exact
     * drawing logic, paint metrics, and styling of the week widget.
     */
    fun renderPreviewBitmap(
        context: Context,
        settings: Settings.WidgetSettings,
        width: Int,
        height: Int,
        userClassName: String? = null,
        subjectGroups: Map<String, String> = emptyMap()
    ): Bitmap
    {
        val w = if (width > 0) width else 700
        val h = if (height > 0) height else 180
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val colors = getEffectiveColors(settings)

        // Clear background
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val pad = 2f
        val periodCount = 4
        // Proportions: header row weight 0.8f, day row weight 2.0f -> total 2.8f
        val headerRowHeight = (h - pad * 2) * (0.8f / 2.8f)
        val dayColFraction = 0.75f / (0.75f + periodCount)
        val dayColWidth = (w - pad * 2) * dayColFraction
        val gridWidth = (w - pad * 2) - dayColWidth
        val rowHeight = (h - pad * 2) - headerRowHeight
        val colWidth = gridWidth / periodCount

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.borderColor.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.dayHeaderColor.toInt()
            style = Paint.Style.FILL
        }

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.dayHeaderTextColor.toInt()
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val cellBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.cellBackgroundColor.toInt()
            style = Paint.Style.FILL
        }

        val activeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.activeHourBorderColor.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        val activeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.activeHourFillColor.toInt()
            style = Paint.Style.FILL
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.cellTextColor.toInt()
            typeface = Typeface.DEFAULT
        }

        val boldTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.cellTextColor.toInt()
            typeface = Typeface.DEFAULT_BOLD
        }

        val teacherPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.teacherTextColor.toInt()
            typeface = Typeface.DEFAULT
        }

        val roomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.classroomTextColor.toInt()
            typeface = Typeface.DEFAULT
        }

        val groupPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.groupTextColor.toInt()
            typeface = Typeface.DEFAULT
        }

        val scale = settings.cellScale.coerceIn(0.7f, 1.6f)

        val tableRect = RectF(pad, pad, w - pad, h - pad)

        // Draw top-left corner
        val cornerRect = RectF(pad, pad, pad + dayColWidth, pad + headerRowHeight)
        canvas.drawRect(cornerRect, headerBgPaint)
        canvas.drawRect(cornerRect, borderPaint)

        // Draw refresh icon in top-left corner only if the top header bar is hidden
        if (settings.hideHeader)
        {
            val refreshIconColor = if (settings.solidHeaderBackground) colors.dayHeaderTextColor.toInt() else Color.WHITE
            drawRefreshIcon(context, canvas, cornerRect, refreshIconColor)
        }

        // Draw header row (periods 1 to 4)
        val periodHeaderMode = settings.getEffectivePeriodHeaderMode()
        val sampleTimes = listOf("7:30-8:15", "8:25-9:10", "9:20-10:05", "10:15-11:00")

        for (i in 0 until periodCount)
        {
            val left = pad + dayColWidth + (i * colWidth)
            val right = left + colWidth
            val rect = RectF(left, pad, right, pad + headerRowHeight)

            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)

            when (periodHeaderMode)
            {
                Settings.PeriodHeaderMode.NUMBER_ONLY ->
                {
                    val periodText = (i + 1).toString()
                    val baseSize = headerRowHeight * 0.55f * scale
                    headerTextPaint.textSize = baseSize
                    val maxTextWidth = colWidth * 0.82f
                    val textWidth = headerTextPaint.measureText(periodText)
                    if (textWidth > maxTextWidth && textWidth > 0)
                    {
                        headerTextPaint.textSize = baseSize * (maxTextWidth / textWidth)
                    }
                    val fontMetrics = headerTextPaint.fontMetrics
                    val baseline = rect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2
                    canvas.drawText(periodText, rect.centerX(), baseline, headerTextPaint)
                }
                Settings.PeriodHeaderMode.TIME_ONLY ->
                {
                    val periodText = sampleTimes.getOrElse(i) { (i + 1).toString() }
                    val baseSize = headerRowHeight * 0.32f * scale
                    headerTextPaint.textSize = baseSize
                    val maxTextWidth = colWidth * 0.88f
                    val textWidth = headerTextPaint.measureText(periodText)
                    if (textWidth > maxTextWidth && textWidth > 0)
                    {
                        headerTextPaint.textSize = baseSize * (maxTextWidth / textWidth)
                    }
                    val fontMetrics = headerTextPaint.fontMetrics
                    val baseline = rect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2
                    canvas.drawText(periodText, rect.centerX(), baseline, headerTextPaint)
                }
                Settings.PeriodHeaderMode.BOTH ->
                {
                    val numText = (i + 1).toString()
                    val timeText = sampleTimes.getOrElse(i) { "" }

                    // Top line: period number
                    val numBaseSize = headerRowHeight * 0.38f * scale
                    headerTextPaint.textSize = numBaseSize
                    val maxNumWidth = colWidth * 0.82f
                    val numWidth = headerTextPaint.measureText(numText)
                    if (numWidth > maxNumWidth && numWidth > 0)
                    {
                        headerTextPaint.textSize = numBaseSize * (maxNumWidth / numWidth)
                    }
                    val numFm = headerTextPaint.fontMetrics
                    val numBaseline = rect.centerY() - (headerRowHeight * 0.16f) - (numFm.ascent + numFm.descent) / 2
                    canvas.drawText(numText, rect.centerX(), numBaseline, headerTextPaint)

                    // Bottom line: time range
                    if (timeText.isNotEmpty())
                    {
                        val timeBaseSize = headerRowHeight * 0.26f * scale
                        headerTextPaint.textSize = timeBaseSize
                        val maxTimeWidth = colWidth * 0.90f
                        val timeWidth = headerTextPaint.measureText(timeText)
                        if (timeWidth > maxTimeWidth && timeWidth > 0)
                        {
                            headerTextPaint.textSize = timeBaseSize * (maxTimeWidth / timeWidth)
                        }
                        val timeFm = headerTextPaint.fontMetrics
                        val timeBaseline = rect.centerY() + (headerRowHeight * 0.22f) - (timeFm.ascent + timeFm.descent) / 2
                        canvas.drawText(timeText, rect.centerX(), timeBaseline, headerTextPaint)
                    }
                }
            }
        }

        // Draw Day Row: "Po"
        val top = pad + headerRowHeight
        val bottom = top + rowHeight

        val dayRect = RectF(pad, top, pad + dayColWidth, bottom)
        canvas.drawRect(dayRect, headerBgPaint)
        canvas.drawRect(dayRect, borderPaint)

        headerTextPaint.textSize = rowHeight * 0.35f * scale
        val fm = headerTextPaint.fontMetrics
        val dayBaseline = dayRect.centerY() - (fm.ascent + fm.descent) / 2
        canvas.drawText("Po", dayRect.centerX(), dayBaseline, headerTextPaint)

        val simActiveIndex = if (settings.debugSimulationEnabled) {
            when {
                settings.debugPeriodIndex in 0..3 -> settings.debugPeriodIndex
                settings.debugPeriodIndex < 0 -> -1
                else -> 3
            }
        } else {
            1
        }

        var activeSampleRect: RectF? = null

        // Period 1 (index 0): Normal lesson: M, Ng, 21
        run {
            val left = pad + dayColWidth
            val right = left + colWidth
            val cellRect = RectF(left, top, right, bottom)
            val isCurrent = simActiveIndex == 0
            if (isCurrent) activeSampleRect = cellRect

            canvas.drawRect(cellRect, cellBgPaint)
            if (isCurrent) canvas.drawRect(cellRect, activeFillPaint)

            if (isCurrent && settings.debugSimulationEnabled && settings.debugSubstitutionType == 1)
            {
                drawSubstitutionCell(canvas, cellRect, ChangeEntry("Odpadá\n(Ng)", null, null, false), settings, colors, isCurrent, false)
            }
            else if (isCurrent && settings.debugSimulationEnabled && settings.debugSubstitutionType == 2)
            {
                drawSubstitutionCell(canvas, cellRect, ChangeEntry("M 21\nTu(Ng)", null, null, false), settings, colors, isCurrent, false)
            }
            else
            {
                drawLessonCell(
                    canvas = canvas,
                    rect = cellRect,
                    subject = "M",
                    teacher = "Ng",
                    room = "21",
                    clazz = userClassName,
                    group = null,
                    settings = settings,
                    boldPaint = boldTextPaint,
                    smallPaint = textPaint,
                    teacherPaint = teacherPaint,
                    roomPaint = roomPaint,
                    groupPaint = groupPaint
                )
            }
            canvas.drawRect(cellRect, borderPaint)
        }

        // Period 2 (index 1): Lesson: TV, Ja, T1
        run {
            val left = pad + dayColWidth + colWidth
            val right = left + colWidth
            val cellRect = RectF(left, top, right, bottom)
            val isCurrent = simActiveIndex == 1
            if (isCurrent) activeSampleRect = cellRect

            canvas.drawRect(cellRect, cellBgPaint)
            if (isCurrent) canvas.drawRect(cellRect, activeFillPaint)

            if (isCurrent && settings.debugSimulationEnabled && settings.debugSubstitutionType == 1)
            {
                drawSubstitutionCell(canvas, cellRect, ChangeEntry("Odpadá\n(Ja)", null, null, false), settings, colors, isCurrent, false)
            }
            else if (isCurrent && settings.debugSimulationEnabled && settings.debugSubstitutionType == 2)
            {
                drawSubstitutionCell(canvas, cellRect, ChangeEntry("TV T1\nSu(Ja)", null, null, false), settings, colors, isCurrent, false)
            }
            else
            {
                drawLessonCell(
                    canvas = canvas,
                    rect = cellRect,
                    subject = "TV",
                    teacher = "Ja",
                    room = "T1",
                    clazz = userClassName,
                    group = null,
                    settings = settings,
                    boldPaint = boldTextPaint,
                    smallPaint = textPaint,
                    teacherPaint = teacherPaint,
                    roomPaint = roomPaint,
                    groupPaint = groupPaint
                )
            }
            canvas.drawRect(cellRect, borderPaint)
        }

        // Period 3 (index 2): Lesson with group: PSS, Ms, 19c, group 1/2
        run {
            val left = pad + dayColWidth + (2 * colWidth)
            val right = left + colWidth
            val cellRect = RectF(left, top, right, bottom)
            val isCurrent = simActiveIndex == 2
            if (isCurrent) activeSampleRect = cellRect

            canvas.drawRect(cellRect, cellBgPaint)
            if (isCurrent) canvas.drawRect(cellRect, activeFillPaint)

            if (isCurrent && settings.debugSimulationEnabled && settings.debugSubstitutionType == 1)
            {
                drawSubstitutionCell(canvas, cellRect, ChangeEntry("Odpadá\n(Ms)", null, null, false), settings, colors, isCurrent, false)
            }
            else if (isCurrent && settings.debugSimulationEnabled && settings.debugSubstitutionType == 2)
            {
                drawSubstitutionCell(canvas, cellRect, ChangeEntry("PSS 19c\nTu(Ms)", null, null, false), settings, colors, isCurrent, false)
            }
            else
            {
                drawLessonCell(
                    canvas = canvas,
                    rect = cellRect,
                    subject = "PSS",
                    teacher = "Ms",
                    room = "19c",
                    clazz = userClassName,
                    group = "1/2",
                    settings = settings,
                    boldPaint = boldTextPaint,
                    smallPaint = textPaint,
                    teacherPaint = teacherPaint,
                    roomPaint = roomPaint,
                    groupPaint = groupPaint
                )
            }
            canvas.drawRect(cellRect, borderPaint)
        }

        // Period 4 (index 3): Substitution / normal: "Odpadá\n(Dr)"
        run {
            val left = pad + dayColWidth + (3 * colWidth)
            val right = left + colWidth
            val cellRect = RectF(left, top, right, bottom)
            val isCurrent = simActiveIndex == 3
            if (isCurrent) activeSampleRect = cellRect

            canvas.drawRect(cellRect, cellBgPaint)
            if (isCurrent) canvas.drawRect(cellRect, activeFillPaint)

            val change = if (settings.debugSimulationEnabled && settings.debugSubstitutionType == 0)
            {
                null
            }
            else if (settings.debugSimulationEnabled && settings.debugSubstitutionType == 2)
            {
                ChangeEntry(text = "WA 10\nLc(Dr)", backgroundColor = null, foregroundColor = null, willBeSpecified = false)
            }
            else
            {
                ChangeEntry(text = "Odpadá\n(Dr)", backgroundColor = null, foregroundColor = null, willBeSpecified = false)
            }

            if (change != null)
            {
                drawSubstitutionCell(
                    canvas = canvas,
                    rect = cellRect,
                    substitution = change,
                    settings = settings,
                    colors = colors,
                    isCurrentHour = isCurrent,
                    useRoundRect = false
                )
            }
            else
            {
                drawLessonCell(
                    canvas = canvas,
                    rect = cellRect,
                    subject = "WA",
                    teacher = "Dr",
                    room = "10",
                    clazz = userClassName,
                    group = null,
                    settings = settings,
                    boldPaint = boldTextPaint,
                    smallPaint = textPaint,
                    teacherPaint = teacherPaint,
                    roomPaint = roomPaint,
                    groupPaint = groupPaint
                )
            }
            canvas.drawRect(cellRect, borderPaint)
        }

        // Overlay active hour border
        if (activeSampleRect != null)
        {
            canvas.drawRect(activeSampleRect, activeBorderPaint)
        }

        if (!settings.hideBorder)
        {
            canvas.drawRect(tableRect, borderPaint)
        }

        return bitmap
    }

    data class ParsedSubstitution(
        val subject: String,
        val room: String? = null,
        val teacher: String? = null,
        val note: String? = null,
        val isCancelled: Boolean = false,
        val isStructured: Boolean = false
    )

    fun parseSubstitution(substitution: ChangeEntry): ParsedSubstitution
    {
        val raw = substitution.text.trim()
        val noteSuffix = if (substitution.willBeSpecified == true) "(upřesní se)" else null

        if (raw.startsWith("Odpadá", ignoreCase = true) || raw.endsWith("odpadá", ignoreCase = true))
        {
            val rest = raw.replace(Regex("(?i)odpadá"), "").trim()
            val teacher = if (rest.isNotEmpty()) rest else null
            return ParsedSubstitution(
                subject = "Odpadá",
                teacher = teacher,
                note = noteSuffix,
                isCancelled = true,
                isStructured = true
            )
        }

        if (raw.startsWith("Zrušeno", ignoreCase = true) || raw.endsWith("zrušeno", ignoreCase = true))
        {
            val rest = raw.replace(Regex("(?i)zrušeno"), "").trim()
            return ParsedSubstitution(
                subject = "Zrušeno",
                teacher = if (rest.isNotEmpty()) rest else null,
                note = noteSuffix,
                isCancelled = true,
                isStructured = true
            )
        }

        fun looksLikeRoom(s: String): Boolean =
            s.any { it.isDigit() } || s.lowercase() in setOf("aula", "těl", "dílny", "lab", "lab1", "lab2", "tv")

        val isSpoj = Regex("(?i)\\bspoj\\.?(?:\\s+|$)").containsMatchIn(raw)
        val cleanRaw = raw.replace(Regex("(?i)\\bspoj\\.?(?:\\s+|$)"), " ").replace(Regex("\\s+"), " ").trim()
        val tokens = cleanRaw.split(Regex("\\s+")).filter { it.isNotBlank() }

        if (tokens.size >= 3 && tokens[0].length <= 6 && looksLikeRoom(tokens[1]))
        {
            val baseSubject = tokens[0]
            val subject = if (isSpoj && !baseSubject.contains("spoj", ignoreCase = true)) "$baseSubject spoj" else baseSubject
            val room = tokens[1]
            val teacher = tokens.drop(2).joinToString(" ")
            return ParsedSubstitution(
                subject = subject,
                room = room,
                teacher = teacher,
                note = noteSuffix,
                isCancelled = false,
                isStructured = true
            )
        }

        if (tokens.size == 2 && tokens[0].length <= 6)
        {
            val baseSubject = tokens[0]
            val subject = if (isSpoj && !baseSubject.contains("spoj", ignoreCase = true)) "$baseSubject spoj" else baseSubject
            val second = tokens[1]
            val isRoom = looksLikeRoom(second)
            val room = if (isRoom) second else null
            val teacher = if (!isRoom) second else null
            return ParsedSubstitution(
                subject = subject,
                room = room,
                teacher = teacher,
                note = noteSuffix,
                isCancelled = false,
                isStructured = true
            )
        }

        if (tokens.size == 1 && tokens[0].length <= 5)
        {
            val baseSubject = tokens[0]
            val subject = if (isSpoj && !baseSubject.contains("spoj", ignoreCase = true)) "$baseSubject spoj" else baseSubject
            return ParsedSubstitution(
                subject = subject,
                note = noteSuffix,
                isCancelled = false,
                isStructured = true
            )
        }

        val subject = if (isSpoj && !raw.contains("spoj", ignoreCase = true)) "$raw spoj" else raw
        return ParsedSubstitution(
            subject = subject,
            note = noteSuffix,
            isCancelled = false,
            isStructured = false
        )
    }

    private fun drawSplitLesson(
        canvas: Canvas,
        rect: RectF,
        lesson: Lesson,
        settings: Settings.WidgetSettings,
        boldPaint: Paint,
        smallPaint: Paint,
        teacherPaint: Paint,
        roomPaint: Paint,
        groupPaint: Paint,
        isUserGroup: Boolean = false
    )
    {
        val cellW = rect.width()
        val cellH = rect.height()
        val scale = settings.cellScale.coerceIn(0.7f, 1.6f)

        val subject = lesson.subjectName.short ?: ""
        val teacherStr = if (!settings.hideTeachers && lesson.teacherName?.short != null) lesson.teacherName!!.short!! else ""
        val roomStr = lesson.classroom ?: ""
        val groupStr = lesson.group ?: ""

        val padX = 4f

        if (isUserGroup)
        {
            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = boldPaint.color
                style = Paint.Style.FILL
            }
            canvas.drawRect(rect.left, rect.top, rect.left + 3f, rect.bottom, barPaint)
        }

        val maxSubjSize = (cellH * 0.58f * scale).coerceIn(10f, 22f)
        val maxDetSize = (cellH * 0.44f * scale).coerceIn(9f, 16f)

        val subjP = Paint(boldPaint).apply {
            textSize = maxSubjSize
            textAlign = Paint.Align.LEFT
        }
        val detP = Paint(teacherPaint).apply {
            textSize = maxDetSize
            textAlign = Paint.Align.RIGHT
        }

        val fullDetails = listOf(roomStr, teacherStr, groupStr).filter { it.isNotBlank() }.joinToString(" ")
        val compactDetails = listOf(roomStr, groupStr).filter { it.isNotBlank() }.joinToString(" ")
        val availW = cellW - padX * 2 - (if (isUserGroup) 4f else 0f) - 4f

        val subjW = subjP.measureText(subject)
        val fullDetW = detP.measureText(fullDetails)

        val displayDetails: String
        if (subjW + fullDetW <= availW)
        {
            displayDetails = fullDetails
        }
        else
        {
            val compactW = detP.measureText(compactDetails)
            if (subjW + compactW <= availW)
            {
                displayDetails = compactDetails
            }
            else
            {
                val ratio = (availW / (subjW + compactW)).coerceIn(0.65f, 1.0f)
                subjP.textSize *= ratio
                detP.textSize *= ratio
                displayDetails = compactDetails
            }
        }

        val fmSubj = subjP.fontMetrics
        val subjY = rect.centerY() - (fmSubj.ascent + fmSubj.descent) / 2
        val leftX = rect.left + padX + (if (isUserGroup) 3.5f else 0f)
        canvas.drawText(subject, leftX, subjY, subjP)

        if (displayDetails.isNotEmpty())
        {
            val fmDet = detP.fontMetrics
            val detY = rect.centerY() - (fmDet.ascent + fmDet.descent) / 2
            canvas.drawText(displayDetails, rect.right - padX, detY, detP)
        }
    }

    data class DayPeriodItem(
        val periodIndex: Int,
        val periodRangeStr: String,
        val hourLabel: String,
        val isCurrent: Boolean,
        val lesson: Lesson?,
        val substitution: ChangeEntry?,
        val isFree: Boolean
    )

    fun getDayPeriodItems(
        timetableData: TimetableData?,
        settings: Settings.WidgetSettings,
        targetDay: DayOfWeek,
        activeSpot: LessonSpot?,
        subjectGroups: Map<String, String> = emptyMap(),
        nowLocal: LocalDateTime
    ): List<DayPeriodItem>
    {
        if (timetableData == null) return emptyList()

        val timetable = timetableData.page.timetable
        val lessonPeriods = timetable.lessonPeriods
        val lessonSpots = timetable.getLessonSpotsForDay(targetDay) ?: emptyList()
        val substitutionsMap: Map<DayOfWeek, DailySchedule> = try {
            timetableData.substitutions?.data?.associate { item ->
                LocalDate.parse(item.date).dayOfWeek to item
            } ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
        val daySubstitutions = substitutionsMap[targetDay]?.changes ?: emptyList()

        val lastSpotIndex = lessonSpots.indexOfLast { it.isNotEmpty() }
        val lastSubIndex = daySubstitutions.indexOfLast { it != null }
        val simPeriodIndex = if (settings.debugSimulationEnabled && settings.debugPeriodIndex >= 0) settings.debugPeriodIndex else -1
        val lastOverrideIndex = settings.debugWeekOverrides.filter { it.key.startsWith("${targetDay.name}_") && it.value != 0 }
            .keys.mapNotNull { it.substringAfterLast('_').toIntOrNull() }.maxOrNull() ?: -1

        val maxContentPeriod = maxOf(lastSpotIndex, lastSubIndex, simPeriodIndex, lastOverrideIndex)
        if (maxContentPeriod < 0) return emptyList()

        val items = mutableListOf<DayPeriodItem>()
        for (periodIndex in 0..maxContentPeriod)
        {
            val spot = lessonSpots.getOrNull(periodIndex)
            val userLesson = resolveLessonForSpot(spot, subjectGroups, settings.userProfileGroup)
            val baseSub = daySubstitutions.getOrNull(periodIndex)
            val (isFreeOverride, effectiveSub) = getEffectiveSubstitutionForDayAndPeriod(
                settings = settings,
                day = targetDay,
                periodIndex = periodIndex,
                baseSubstitution = baseSub,
                baseLesson = userLesson ?: spot?.firstOrNull(),
                nowLocal = nowLocal
            )

            val periodStr = if (periodIndex < lessonPeriods.size) lessonPeriods[periodIndex].toString() else "${periodIndex + 1}. hodina"
            val hourLabel = when (settings.getEffectivePeriodHeaderMode()) {
                Settings.PeriodHeaderMode.NUMBER_ONLY -> "${periodIndex + 1}. hodina"
                Settings.PeriodHeaderMode.TIME_ONLY -> periodStr
                Settings.PeriodHeaderMode.BOTH -> "${periodIndex + 1}. ($periodStr)"
            }

            val isCurrent = isPeriodActive(settings, targetDay, periodIndex, spot, activeSpot, nowLocal, timetable)

            val (finalLesson, finalSub, isFree) = when {
                isFreeOverride -> Triple(null, null, true)
                effectiveSub != null -> Triple(userLesson ?: spot?.firstOrNull(), effectiveSub, false)
                userLesson != null -> Triple(userLesson, null, false)
                else -> Triple(null, null, true)
            }

            items.add(
                DayPeriodItem(
                    periodIndex = periodIndex,
                    periodRangeStr = periodStr,
                    hourLabel = hourLabel,
                    isCurrent = isCurrent,
                    lesson = finalLesson,
                    substitution = finalSub,
                    isFree = isFree
                )
            )
        }
        return items
    }

    fun renderDayLessonRowBitmap(
        context: Context,
        item: DayPeriodItem,
        settings: Settings.WidgetSettings,
        width: Int,
        height: Int
    ): Bitmap
    {
        val w = if (width > 0) width else 600
        val h = if (height > 0) height else 150
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val colors = getEffectiveColors(settings)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val pad = 4f
        val rect = RectF(pad, pad, w - pad, h - pad)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.borderColor.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val cellBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.cellBackgroundColor.toInt()
            style = Paint.Style.FILL
        }
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.dayHeaderColor.toInt()
            style = Paint.Style.FILL
        }
        val activeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.activeHourBorderColor.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        val activeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.activeHourFillColor.toInt()
            style = Paint.Style.FILL
        }

        val subBgColor = colors.substitutionBackgroundColor.toInt()
        if (item.substitution != null && (subBgColor and 0xFF000000.toInt()) != 0)
        {
            val subBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = subBgColor
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(rect, 8f, 8f, subBgPaint)
        }
        else
        {
            canvas.drawRoundRect(rect, 8f, 8f, cellBgPaint)
        }

        if (item.isCurrent)
        {
            canvas.drawRoundRect(rect, 8f, 8f, activeFillPaint)
        }

        val scale = settings.cellScale.coerceIn(0.7f, 1.6f)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.teacherTextColor.toInt()
            textSize = h * 0.28f * scale
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.cellTextColor.toInt()
            typeface = Typeface.DEFAULT_BOLD
            textSize = h * 0.40f * scale
        }
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.dayHeaderTextColor.toInt()
            typeface = Typeface.DEFAULT_BOLD
            textSize = h * 0.35f * scale
        }

        // Left badge with Hour label
        val badgeWidth = (w * 0.32f).coerceAtLeast(80f)
        val badgeRect = RectF(rect.left + 4f, rect.top + 4f, rect.left + badgeWidth, rect.bottom - 4f)

        if (settings.solidHeaderBackground)
        {
            canvas.drawRoundRect(badgeRect, 6f, 6f, headerBgPaint)
            if (!settings.hideBorder)
            {
                canvas.drawRoundRect(badgeRect, 6f, 6f, borderPaint)
            }
        }

        val maxLabelW = badgeWidth - 16f
        var labelSize = h * 0.34f * scale
        headerTextPaint.textSize = labelSize
        val labelW = headerTextPaint.measureText(item.hourLabel)
        if (labelW > maxLabelW && labelW > 0)
        {
            labelSize *= (maxLabelW / labelW)
            headerTextPaint.textSize = labelSize
        }
        headerTextPaint.textAlign = if (settings.solidHeaderBackground) Paint.Align.CENTER else Paint.Align.LEFT
        val labelX = if (settings.solidHeaderBackground) badgeRect.centerX() else rect.left + 16f
        val fmLabel = headerTextPaint.fontMetrics
        val labelBaseline = badgeRect.centerY() - (fmLabel.ascent + fmLabel.descent) / 2
        canvas.drawText(item.hourLabel, labelX, labelBaseline, headerTextPaint)

        val lessonRect = RectF(rect.left + badgeWidth + 8f, rect.top + 3f, rect.right - 8f, rect.bottom - 3f)

        if (item.isFree)
        {
            val freePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.cellTextColor.toInt() and 0x99FFFFFF.toInt()
                textSize = (h * 0.35f * scale).coerceAtLeast(14f)
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.LEFT
            }
            val freeFm = freePaint.fontMetrics
            val freeY = rect.centerY() - (freeFm.ascent + freeFm.descent) / 2
            canvas.drawText("Volná hodina", lessonRect.left + 6f, freeY, freePaint)
        }
        else if (item.substitution != null)
        {
            val parsed = parseSubstitution(item.substitution)
            val subTextColor = colors.substitutionTextColor.toInt()
            val subSubjPaint = Paint(boldPaint).apply { color = subTextColor }
            val subDetPaint = Paint(textPaint).apply { color = subTextColor }

            val subject = parsed.subject
            val detailList = listOfNotNull(
                parsed.room?.let { if (it.isNotBlank()) "Učebna $it" else null },
                parsed.teacher,
                parsed.note
            ).filter { it.isNotBlank() }
            val details = detailList.joinToString(" · ")

            val availLessonW = lessonRect.width() - 14f
            var curSubjSize = h * 0.40f * scale
            var curDetSize = h * 0.28f * scale
            subSubjPaint.textSize = curSubjSize
            subDetPaint.textSize = curDetSize

            val totalW = subSubjPaint.measureText(subject) + (if (details.isNotEmpty()) subDetPaint.measureText(details) + 12f else 0f)
            if (totalW > availLessonW && totalW > 0)
            {
                val factor = (availLessonW / totalW).coerceIn(0.60f, 1.0f)
                curSubjSize *= factor
                curDetSize *= factor
                subSubjPaint.textSize = curSubjSize
                subDetPaint.textSize = curDetSize
            }

            subSubjPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(subject, lessonRect.left + 6f, rect.centerY() + (subSubjPaint.textSize / 3f), subSubjPaint)

            if (details.isNotEmpty())
            {
                subDetPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(details, lessonRect.right - 8f, rect.centerY() + (subDetPaint.textSize / 3f), subDetPaint)
            }
        }
        else if (item.lesson != null)
        {
            val lesson = item.lesson
            val subject = lesson.subjectName.short ?: ""
            val teacher = if (!settings.hideTeachers) lesson.teacherName?.short ?: "" else ""
            val room = lesson.classroom ?: ""
            val group = lesson.group ?: ""
            val details = listOf(room, teacher, group).filter { it.isNotBlank() }.joinToString(" · ")

            val availLessonW = lessonRect.width() - 14f
            var curSubjSize = h * 0.40f * scale
            var curDetSize = h * 0.28f * scale
            boldPaint.textSize = curSubjSize
            textPaint.textSize = curDetSize

            val totalW = boldPaint.measureText(subject) + textPaint.measureText(details) + 12f
            if (totalW > availLessonW && totalW > 0)
            {
                val factor = (availLessonW / totalW).coerceIn(0.65f, 1.0f)
                curSubjSize *= factor
                curDetSize *= factor
                boldPaint.textSize = curSubjSize
                textPaint.textSize = curDetSize
            }

            boldPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(subject, lessonRect.left + 6f, rect.centerY() + (boldPaint.textSize / 3f), boldPaint)

            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(details, lessonRect.right - 8f, rect.centerY() + (textPaint.textSize / 3f), textPaint)
        }

        if (item.isCurrent)
        {
            canvas.drawRoundRect(rect, 8f, 8f, activeBorderPaint)
        }
        else if (item.substitution != null)
        {
            val subBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.substitutionTextColor.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRoundRect(rect, 8f, 8f, subBorderPaint)
        }
        else if (!settings.hideBorder)
        {
            canvas.drawRoundRect(rect, 8f, 8f, borderPaint)
        }

        return bitmap
    }

    /**
     * Renders a single-day timetable view onto a Bitmap.
     */
    fun renderDayTimetableBitmap(
        context: Context,
        timetableData: TimetableData?,
        settings: Settings.WidgetSettings,
        targetDay: DayOfWeek,
        width: Int,
        height: Int,
        activeSpot: LessonSpot?,
        subjectGroups: Map<String, String> = emptyMap()
    ): Bitmap
    {
        val w = if (width > 0) width else 600
        val h = if (height > 0) height else 550
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val colors = getEffectiveColors(settings)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val nowLocal = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val items = getDayPeriodItems(
            timetableData = timetableData,
            settings = settings,
            targetDay = targetDay,
            activeSpot = activeSpot,
            subjectGroups = subjectGroups,
            nowLocal = nowLocal
        )

        if (items.isEmpty())
        {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.cellTextColor.toInt()
                textSize = h * 0.055f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Žádné vyučování v tento den", w / 2f, h / 2f, textPaint)
            return bitmap
        }

        val pad = 6f
        val count = items.size
        val rowHeight = (h - (pad * (count + 1))) / count

        for ((i, item) in items.withIndex())
        {
            val top = pad + (i * (rowHeight + pad))
            val rowBitmap = renderDayLessonRowBitmap(
                context = context,
                item = item,
                settings = settings,
                width = w,
                height = rowHeight.toInt()
            )
            canvas.drawBitmap(rowBitmap, 0f, top, null)
        }

        return bitmap
    }

    internal fun drawRefreshIcon(context: Context, canvas: Canvas, rect: RectF, color: Int = Color.WHITE)
    {
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_refresh)?.mutate()
        if (drawable != null)
        {
            drawable.setTint(color)
            val iconSize = (min(rect.width(), rect.height()) * 0.58f).toInt()
            val left = (rect.centerX() - iconSize / 2f).toInt()
            val top = (rect.centerY() - iconSize / 2f).toInt()
            drawable.setBounds(left, top, left + iconSize, top + iconSize)
            drawable.draw(canvas)
            return
        }

        // Fallback manual vector drawing
        val cx = rect.centerX()
        val cy = rect.centerY()
        val size = min(rect.width(), rect.height()) * 0.52f
        val r = size / 2f

        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = maxOf(3f, size * 0.16f)
            strokeCap = Paint.Cap.ROUND
        }

        val oval = RectF(cx - r, cy - r, cx + r, cy + r)
        canvas.drawArc(oval, 45f, 270f, false, arcPaint)

        val endAngleRad = Math.toRadians(315.0)
        val arrowX = (cx + r * Math.cos(endAngleRad)).toFloat()
        val arrowY = (cy + r * Math.sin(endAngleRad)).toFloat()

        val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        val arrowSize = size * 0.42f
        val arrowPath = Path().apply {
            moveTo(arrowX, arrowY)
            lineTo(arrowX + arrowSize * 0.85f, arrowY - arrowSize * 0.15f)
            lineTo(arrowX + arrowSize * 0.15f, arrowY + arrowSize * 0.85f)
            close()
        }
        canvas.drawPath(arrowPath, arrowPaint)
    }

    fun drawSubstitutionCell(
        canvas: Canvas,
        rect: RectF,
        substitution: ChangeEntry,
        settings: Settings.WidgetSettings,
        colors: Settings.WidgetColors,
        isCurrentHour: Boolean,
        useRoundRect: Boolean = true
    )
    {
        val scale = settings.cellScale.coerceIn(0.7f, 1.6f)
        val textColor = colors.substitutionTextColor.toInt()
        val bgColor = colors.substitutionBackgroundColor.toInt()

        // 1. Background fill
        if ((bgColor and 0xFF000000.toInt()) != 0)
        {
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = bgColor
                style = Paint.Style.FILL
            }
            if (useRoundRect)
            {
                canvas.drawRoundRect(rect, 6f, 6f, fillPaint)
            }
            else
            {
                canvas.drawRect(rect, fillPaint)
            }
        }

        // 2. Structured or raw text content: Always show clean stacked lines centered like "A spoj"
        val parsed = parseSubstitution(substitution)
        val fullLines = mutableListOf<String>()

        if (parsed.isStructured)
        {
            fullLines.add(parsed.subject)
            if (!parsed.room.isNullOrBlank())
            {
                fullLines.add(parsed.room)
            }
            if (!parsed.teacher.isNullOrBlank() && !settings.hideTeachers)
            {
                val formattedTeacher = parsed.teacher.replace(Regex("([a-zA-Zá-žÁ-Ž]+)\\((.*?)\\)"), "$1 ($2)")
                fullLines.add(formattedTeacher)
            }
            if (!parsed.note.isNullOrBlank())
            {
                fullLines.add(parsed.note)
            }
        }
        else
        {
            val fullText = if (parsed.note != null) "${parsed.subject}\n${parsed.note}" else parsed.subject
            fullLines.addAll(fullText.split("\n"))
        }

        val padding = 4f
        val availWidth = rect.width() - (padding * 2)
        val availHeight = rect.height() - (padding * 2)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        var fontSize = (availHeight * 0.28f * scale).coerceIn(10f, 28f)
        var finalLines = listOf<String>()
        var finalLineHeight = 0f

        for (attempt in 0..6)
        {
            textPaint.textSize = fontSize
            val fm = textPaint.fontMetrics
            val lHeight = (fm.descent - fm.ascent) * 1.05f

            val lines = mutableListOf<String>()
            for (rawLine in fullLines)
            {
                val words = rawLine.split(" ")
                var cur = ""
                for (w in words)
                {
                    val candidate = if (cur.isEmpty()) w else "$cur $w"
                    if (textPaint.measureText(candidate) <= availWidth)
                    {
                        cur = candidate
                    }
                    else
                    {
                        if (cur.isNotEmpty()) lines.add(cur)
                        cur = w
                    }
                }
                if (cur.isNotEmpty()) lines.add(cur)
            }

            val totalHeight = lines.size * lHeight
            val maxLineWidth = lines.maxOfOrNull { textPaint.measureText(it) } ?: 0f
            if ((totalHeight <= availHeight && maxLineWidth <= availWidth) || fontSize <= 8f)
            {
                finalLines = lines
                finalLineHeight = lHeight
                break
            }
            fontSize = maxOf(7.5f, fontSize * 0.85f)
        }

        if (finalLines.isEmpty())
        {
            finalLines = fullLines
            textPaint.textSize = fontSize
            val fm = textPaint.fontMetrics
            finalLineHeight = (fm.descent - fm.ascent) * 1.05f
        }

        val fm = textPaint.fontMetrics
        val startY = rect.centerY() - ((finalLines.size - 1) * finalLineHeight) / 2f - (fm.ascent + fm.descent) / 2f

        finalLines.forEachIndexed { i, line ->
            var linePaint = textPaint
            val measured = textPaint.measureText(line)
            if (measured > availWidth && availWidth > 0)
            {
                val lineScale = availWidth / measured
                linePaint = Paint(textPaint).apply { textSize = textPaint.textSize * lineScale }
            }
            canvas.drawText(line, rect.centerX(), startY + (i * finalLineHeight), linePaint)
        }

        // 3. Border
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isCurrentHour) colors.activeHourBorderColor.toInt() else textColor
            style = Paint.Style.STROKE
            strokeWidth = if (isCurrentHour) 4f else 2f
        }
        if (useRoundRect)
        {
            canvas.drawRoundRect(rect, 6f, 6f, strokePaint)
        }
        else
        {
            canvas.drawRect(rect, strokePaint)
        }
    }

    /**
     * Renders a 1-row horizontal timetable view for today onto a Bitmap.
     */
    fun renderTodayRowTimetableBitmap(
        context: Context,
        timetableData: TimetableData?,
        settings: Settings.WidgetSettings,
        targetDay: DayOfWeek,
        width: Int,
        height: Int,
        activeSpot: LessonSpot?,
        subjectGroups: Map<String, String> = emptyMap()
    ): Bitmap
    {
        val w = if (width > 0) width else 900
        val h = if (height > 0) height else 160
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val colors = getEffectiveColors(settings)

        // Clear background
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        if (timetableData == null)
        {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.cellTextColor.toInt()
                textSize = h * 0.22f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Rozvrh není načten. Otevřete aplikaci.", w / 2f, h / 2f + (textPaint.textSize / 3f), textPaint)
            return bitmap
        }

        val timetable = timetableData.page.timetable
        val lessonPeriods = timetable.lessonPeriods
        val lessonSpots = timetable.getLessonSpotsForDay(targetDay) ?: emptyList()

        val substitutionsMap: Map<DayOfWeek, DailySchedule> = try {
            timetableData.substitutions?.data?.associate { item ->
                LocalDate.parse(item.date).dayOfWeek to item
            } ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
        val daySubstitutions = substitutionsMap[targetDay]?.changes ?: emptyList()

        val periodCount = getWeekPeriodCount(timetableData)

        val pad = 4f
        val headerRowHeight = (h - pad * 2) * (0.8f / 2.8f)
        val dayColFraction = 0.75f / (0.75f + periodCount)
        val dayColWidth = (w - pad * 2) * dayColFraction
        val gridWidth = (w - pad * 2) - dayColWidth
        val colWidth = gridWidth / periodCount
        val rowHeight = (h - pad * 2) - headerRowHeight

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.borderColor.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.dayHeaderColor.toInt()
            style = Paint.Style.FILL
        }

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.dayHeaderTextColor.toInt()
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val cellBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.cellBackgroundColor.toInt()
            style = Paint.Style.FILL
        }

        val activeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.activeHourBorderColor.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        val activeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.activeHourFillColor.toInt()
            style = Paint.Style.FILL
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.cellTextColor.toInt()
            typeface = Typeface.DEFAULT
        }

        val boldTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.cellTextColor.toInt()
            typeface = Typeface.DEFAULT_BOLD
        }

        val teacherPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.teacherTextColor.toInt()
            typeface = Typeface.DEFAULT
        }

        val roomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.classroomTextColor.toInt()
            typeface = Typeface.DEFAULT
        }

        val groupPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.groupTextColor.toInt()
            typeface = Typeface.DEFAULT
        }

        val scale = settings.cellScale.coerceIn(0.7f, 1.6f)
        val tableRect = RectF(pad, pad, w - pad, h - pad)

        // Draw top-left corner
        val cornerRect = RectF(pad, pad, pad + dayColWidth, pad + headerRowHeight)
        canvas.drawRect(cornerRect, headerBgPaint)
        canvas.drawRect(cornerRect, borderPaint)

        val refreshIconColor = if (settings.solidHeaderBackground) colors.dayHeaderTextColor.toInt() else Color.WHITE
        drawRefreshIcon(context, canvas, cornerRect, refreshIconColor)

        // Draw header row (periods)
        val periodHeaderMode = settings.getEffectivePeriodHeaderMode()
        for (i in 0 until periodCount)
        {
            val left = pad + dayColWidth + (i * colWidth)
            val right = left + colWidth
            val rect = RectF(left, pad, right, pad + headerRowHeight)

            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)

            when (periodHeaderMode)
            {
                Settings.PeriodHeaderMode.NUMBER_ONLY ->
                {
                    val periodText = (i + 1).toString()
                    val baseSize = headerRowHeight * 0.55f * scale
                    headerTextPaint.textSize = baseSize
                    val maxTextWidth = colWidth * 0.82f
                    val textWidth = headerTextPaint.measureText(periodText)
                    if (textWidth > maxTextWidth && textWidth > 0)
                    {
                        headerTextPaint.textSize = baseSize * (maxTextWidth / textWidth)
                    }
                    val fontMetrics = headerTextPaint.fontMetrics
                    val baseline = rect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2
                    canvas.drawText(periodText, rect.centerX(), baseline, headerTextPaint)
                }
                Settings.PeriodHeaderMode.TIME_ONLY ->
                {
                    val periodText = if (i < lessonPeriods.size) lessonPeriods[i].toString() else (i + 1).toString()
                    val baseSize = headerRowHeight * 0.32f * scale
                    headerTextPaint.textSize = baseSize
                    val maxTextWidth = colWidth * 0.88f
                    val textWidth = headerTextPaint.measureText(periodText)
                    if (textWidth > maxTextWidth && textWidth > 0)
                    {
                        headerTextPaint.textSize = baseSize * (maxTextWidth / textWidth)
                    }
                    val fontMetrics = headerTextPaint.fontMetrics
                    val baseline = rect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2
                    canvas.drawText(periodText, rect.centerX(), baseline, headerTextPaint)
                }
                Settings.PeriodHeaderMode.BOTH ->
                {
                    val numText = (i + 1).toString()
                    val timeText = if (i < lessonPeriods.size) lessonPeriods[i].toString() else ""

                    val numBaseSize = headerRowHeight * 0.38f * scale
                    headerTextPaint.textSize = numBaseSize
                    val maxNumWidth = colWidth * 0.82f
                    val numWidth = headerTextPaint.measureText(numText)
                    if (numWidth > maxNumWidth && numWidth > 0)
                    {
                        headerTextPaint.textSize = numBaseSize * (maxNumWidth / numWidth)
                    }
                    val numFm = headerTextPaint.fontMetrics
                    val numBaseline = rect.centerY() - (headerRowHeight * 0.16f) - (numFm.ascent + numFm.descent) / 2
                    canvas.drawText(numText, rect.centerX(), numBaseline, headerTextPaint)

                    if (timeText.isNotEmpty())
                    {
                        val timeBaseSize = headerRowHeight * 0.26f * scale
                        headerTextPaint.textSize = timeBaseSize
                        val maxTimeWidth = colWidth * 0.90f
                        val timeWidth = headerTextPaint.measureText(timeText)
                        if (timeWidth > maxTimeWidth && timeWidth > 0)
                        {
                            headerTextPaint.textSize = timeBaseSize * (maxTimeWidth / timeWidth)
                        }
                        val timeFm = headerTextPaint.fontMetrics
                        val timeBaseline = rect.centerY() + (headerRowHeight * 0.22f) - (timeFm.ascent + timeFm.descent) / 2
                        canvas.drawText(timeText, rect.centerX(), timeBaseline, headerTextPaint)
                    }
                }
            }
        }

        // Draw day row
        val top = pad + headerRowHeight
        val bottom = top + rowHeight

        val dayRect = RectF(pad, top, pad + dayColWidth, bottom)
        canvas.drawRect(dayRect, headerBgPaint)
        canvas.drawRect(dayRect, borderPaint)

        val dayText = getShortDayName(targetDay)
        val baseDaySize = min(rowHeight * 0.35f * scale, dayColWidth * 0.48f * scale)
        headerTextPaint.textSize = baseDaySize
        val maxDayTextW = dayColWidth * 0.78f
        val dayTextW = headerTextPaint.measureText(dayText)
        if (dayTextW > maxDayTextW && dayTextW > 0)
        {
            headerTextPaint.textSize = baseDaySize * (maxDayTextW / dayTextW)
        }
        val fm = headerTextPaint.fontMetrics
        val dayBaseline = dayRect.centerY() - (fm.ascent + fm.descent) / 2
        canvas.drawText(dayText, dayRect.centerX(), dayBaseline, headerTextPaint)

        val nowLocal = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        var activeHourRect: RectF? = null

        for (colIndex in 0 until periodCount)
        {
            val left = pad + dayColWidth + (colIndex * colWidth)
            val right = left + colWidth
            val cellRect = RectF(left, top, right, bottom)

            val spot = lessonSpots.getOrNull(colIndex)
            val isCurrentHour = isPeriodActive(settings, targetDay, colIndex, spot, activeSpot, nowLocal, timetable)
            val baseLesson = resolveLessonForSpot(spot, subjectGroups, settings.userProfileGroup) ?: spot?.firstOrNull()
            val (isFreeOverride, substitution) = getEffectiveSubstitutionForDayAndPeriod(
                settings = settings,
                day = targetDay,
                periodIndex = colIndex,
                baseSubstitution = daySubstitutions.getOrNull(colIndex),
                baseLesson = baseLesson,
                nowLocal = nowLocal
            )

            canvas.drawRect(cellRect, cellBgPaint)

            if (isCurrentHour)
            {
                activeHourRect = RectF(cellRect)
                canvas.drawRect(cellRect, activeFillPaint)
            }

            if (isFreeOverride)
            {
                // Volná hodina: explicit empty
            }
            else if (substitution != null)
            {
                drawSubstitutionCell(
                    canvas = canvas,
                    rect = cellRect,
                    substitution = substitution,
                    settings = settings,
                    colors = colors,
                    isCurrentHour = isCurrentHour,
                    useRoundRect = false
                )
            }
            else if (spot != null && spot.isNotEmpty())
            {
                if (settings.filterMyGroups)
                {
                    val myLesson = resolveLessonForSpot(spot, subjectGroups, settings.userProfileGroup)
                    if (myLesson != null)
                    {
                        drawSingleLesson(
                            canvas = canvas,
                            rect = cellRect,
                            lesson = myLesson,
                            settings = settings,
                            boldPaint = boldTextPaint,
                            smallPaint = textPaint,
                            teacherPaint = teacherPaint,
                            roomPaint = roomPaint,
                            groupPaint = groupPaint,
                            isSubstituted = false
                        )
                    }
                }
                else
                {
                    val lessons = spot.toList()
                    if (lessons.size == 1)
                    {
                        drawSingleLesson(
                            canvas = canvas,
                            rect = cellRect,
                            lesson = lessons[0],
                            settings = settings,
                            boldPaint = boldTextPaint,
                            smallPaint = textPaint,
                            teacherPaint = teacherPaint,
                            roomPaint = roomPaint,
                            groupPaint = groupPaint,
                            isSubstituted = false
                        )
                    }
                    else
                    {
                        val count = lessons.size
                        val slotHeight = (bottom - top) / count
                        for (i in 0 until count)
                        {
                            val slotTop = top + (i * slotHeight)
                            val slotBottom = slotTop + slotHeight
                            val slotRect = RectF(left, slotTop, right, slotBottom)

                            val lesson = lessons[i]
                            val isUserGroup = !lesson.group.isNullOrBlank() &&
                                subjectGroups[lesson.subjectName.short] == lesson.group

                            if (isUserGroup)
                            {
                                val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = (colors.activeHourFillColor.toInt() and 0x44FFFFFF)
                                    style = Paint.Style.FILL
                                }
                                canvas.drawRect(slotRect, highlightPaint)
                            }

                            if (i > 0)
                            {
                                canvas.drawLine(left, slotTop, right, slotTop, borderPaint)
                            }

                            drawSplitLesson(
                                canvas = canvas,
                                rect = slotRect,
                                lesson = lesson,
                                settings = settings,
                                boldPaint = boldTextPaint,
                                smallPaint = textPaint,
                                teacherPaint = teacherPaint,
                                roomPaint = roomPaint,
                                groupPaint = groupPaint,
                                isUserGroup = isUserGroup
                            )
                        }
                    }
                }
            }

            canvas.drawRect(cellRect, borderPaint)
        }

        if (activeHourRect != null)
        {
            canvas.drawRect(activeHourRect, activeBorderPaint)
        }

        if (!settings.hideBorder)
        {
            canvas.drawRect(tableRect, borderPaint)
        }

        return bitmap
    }

    data class CurrentHourInfo(
        val lesson: Lesson?,
        val substitution: ChangeEntry?,
        val periodIndex: Int,
        val periodRange: String,
        val statusText: String,
        val isSwitchedEarly: Boolean,
        val isFinishedDay: Boolean,
        val isUpcomingDay: Boolean = false
    )

    fun resolveCurrentHourInfo(
        timetableData: TimetableData?,
        now: LocalDateTime,
        subjectGroups: Map<String, String> = emptyMap(),
        settings: Settings.WidgetSettings? = null
    ): CurrentHourInfo
    {
        val timetable = timetableData?.page?.timetable
        val periods = timetable?.lessonPeriods ?: emptyList()
        val today = now.date
        val currentTime = now.time

        val substitutionsMap: Map<DayOfWeek, DailySchedule> = try {
            timetableData?.substitutions?.data?.associate { item ->
                LocalDate.parse(item.date).dayOfWeek to item
            } ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }

        val targetDay = if (settings?.debugSimulationEnabled == true) {
            getSimulatedDay(settings, now)
        } else {
            when (now.dayOfWeek) {
                DayOfWeek.SATURDAY -> DayOfWeek.MONDAY
                DayOfWeek.SUNDAY -> DayOfWeek.MONDAY
                else -> today.dayOfWeek
            }
        }

        val lessonSpots = timetable?.getLessonSpotsForDay(targetDay) ?: emptyList()
        val daySubstitutions = substitutionsMap[targetDay]?.changes ?: emptyList()

        if (settings?.debugSimulationEnabled == true)
        {
            if (settings.debugPeriodIndex == -2)
            {
                return CurrentHourInfo(
                    lesson = null,
                    substitution = null,
                    periodIndex = -1,
                    periodRange = "",
                    statusText = "Dnes konec vyučování",
                    isSwitchedEarly = false,
                    isFinishedDay = true
                )
            }
            if (settings.debugPeriodIndex == -1)
            {
                val firstSpot = lessonSpots.firstOrNull { it.isNotEmpty() }
                val firstLesson = resolveLessonForSpot(firstSpot, subjectGroups, settings.userProfileGroup)
                return CurrentHourInfo(
                    lesson = firstLesson,
                    substitution = null,
                    periodIndex = 0,
                    periodRange = periods.firstOrNull()?.toString() ?: "07:30 - 08:15",
                    statusText = "1. hodina",
                    isSwitchedEarly = false,
                    isFinishedDay = false
                )
            }

            val maxPeriod = maxOf(periods.size - 1, 0)
            val simIdx = settings.debugPeriodIndex.coerceIn(0, maxPeriod)
            val simRange = periods.getOrNull(simIdx)?.toString() ?: "${simIdx + 1}. hodina"
            val simSpot = lessonSpots.getOrNull(simIdx)
            val baseLessonRaw = resolveLessonForSpot(simSpot, subjectGroups, settings.userProfileGroup)

            val (isFreeOverride, simSub) = getEffectiveSubstitutionForDayAndPeriod(
                settings = settings,
                day = targetDay,
                periodIndex = simIdx,
                baseSubstitution = daySubstitutions.getOrNull(simIdx),
                baseLesson = baseLessonRaw,
                nowLocal = now
            )

            val effectiveLesson = if (isFreeOverride) null else baseLessonRaw
            val effectiveSub = if (isFreeOverride) null else simSub
            val simStatus = if (settings.debugIsBreak) "Přestávka · ${simIdx + 1}. hodina" else "${simIdx + 1}. hodina"

            return CurrentHourInfo(
                lesson = effectiveLesson,
                substitution = effectiveSub,
                periodIndex = simIdx,
                periodRange = simRange,
                statusText = simStatus,
                isSwitchedEarly = false,
                isFinishedDay = false
            )
        }

        if (now.dayOfWeek == DayOfWeek.SATURDAY || now.dayOfWeek == DayOfWeek.SUNDAY)
        {
            val monSpots = timetable?.getLessonSpotsForDay(DayOfWeek.MONDAY) ?: emptyList()
            val firstIdx = monSpots.indexOfFirst { it.isNotEmpty() }.coerceAtLeast(0)
            val spot = monSpots.getOrNull(firstIdx)
            val sub = substitutionsMap[DayOfWeek.MONDAY]?.changes?.getOrNull(firstIdx)
            val pStr = periods.getOrNull(firstIdx)?.toString() ?: "${firstIdx + 1}. hodina"
            return CurrentHourInfo(
                lesson = resolveLessonForSpot(spot, subjectGroups, settings?.userProfileGroup),
                substitution = sub,
                periodIndex = firstIdx,
                periodRange = pStr,
                statusText = "V pondělí · ${firstIdx + 1}. hodina",
                isSwitchedEarly = false,
                isFinishedDay = false,
                isUpcomingDay = true
            )
        }

        var activeIndex = -1

        for (i in periods.indices)
        {
            val p = periods[i]
            if (currentTime >= p.from && currentTime < p.to)
            {
                activeIndex = i
                break
            }
        }

        // Inside an active period: display current period info without countdown
        if (activeIndex >= 0)
        {
            val spot = lessonSpots.getOrNull(activeIndex)
            val sub = daySubstitutions.getOrNull(activeIndex)
            val range = periods[activeIndex].toString()
            return CurrentHourInfo(
                lesson = resolveLessonForSpot(spot, subjectGroups, settings?.userProfileGroup),
                substitution = sub,
                periodIndex = activeIndex,
                periodRange = range,
                statusText = "${activeIndex + 1}. hodina",
                isSwitchedEarly = false,
                isFinishedDay = false,
                isUpcomingDay = false
            )
        }

        val lastNonFreeIndex = getLastNonFreePeriodIndex(
            timetableData = timetableData,
            timetable = timetable,
            day = today.dayOfWeek,
            subjectGroups = subjectGroups,
            settings = settings,
            nowLocal = now
        )
        val lastLessonEndTime = if (lastNonFreeIndex >= 0) periods.getOrNull(lastNonFreeIndex)?.to else null

        // If today's classes ended (past the last non-free hour of the day):
        if (lastLessonEndTime != null && currentTime >= lastLessonEndTime)
        {
            val totalSec = lastLessonEndTime.toSecondOfDay() + 3600
            val switchTime = if (totalSec >= 86400) LocalTime(23, 59, 59) else LocalTime.fromSecondOfDay(totalSec)
            if (currentTime < switchTime)
            {
                return CurrentHourInfo(
                    lesson = null,
                    substitution = null,
                    periodIndex = -1,
                    periodRange = "",
                    statusText = "Dnes konec vyučování",
                    isSwitchedEarly = false,
                    isFinishedDay = true,
                    isUpcomingDay = false
                )
            }
            else
            {
                val (targetDay, _, titlePrefix) = TimetableWidgetUpdater.calculateTargetDay(timetableData, timetable, now, subjectGroups, settings)
                val nextDaySpots = timetable?.getLessonSpotsForDay(targetDay) ?: emptyList()
                val firstIdx = nextDaySpots.indexOfFirst { it.isNotEmpty() }.coerceAtLeast(0)
                val spot = nextDaySpots.getOrNull(firstIdx)
                val sub = substitutionsMap[targetDay]?.changes?.getOrNull(firstIdx)
                val pStr = periods.getOrNull(firstIdx)?.toString() ?: "${firstIdx + 1}. hodina"
                return CurrentHourInfo(
                    lesson = resolveLessonForSpot(spot, subjectGroups, settings?.userProfileGroup),
                    substitution = sub,
                    periodIndex = firstIdx,
                    periodRange = pStr,
                    statusText = "$titlePrefix · ${firstIdx + 1}. hodina",
                    isSwitchedEarly = false,
                    isFinishedDay = false,
                    isUpcomingDay = true
                )
            }
        }

        val maxSearchIndex = if (lastNonFreeIndex >= 0) lastNonFreeIndex else (periods.size - 1)

        // Not inside a lesson and before end of school: check if before school or during break
        for (i in 0..maxSearchIndex)
        {
            val p = periods.getOrNull(i) ?: continue
            if (currentTime < p.from)
            {
                val spot = lessonSpots.getOrNull(i)
                val sub = daySubstitutions.getOrNull(i)
                val range = p.toString()
                val status = if (i == 0) "1. hodina" else "Přestávka · ${i + 1}. hodina"

                return CurrentHourInfo(
                    lesson = resolveLessonForSpot(spot, subjectGroups, settings?.userProfileGroup),
                    substitution = sub,
                    periodIndex = i,
                    periodRange = range,
                    statusText = status,
                    isSwitchedEarly = false,
                    isFinishedDay = false,
                    isUpcomingDay = false
                )
            }
        }

        // Fallback after all periods today
        val fallbackSwitchTime = LocalTime(16, 30)
        if (currentTime < fallbackSwitchTime)
        {
            return CurrentHourInfo(
                lesson = null,
                substitution = null,
                periodIndex = -1,
                periodRange = "",
                statusText = "Dnes konec vyučování",
                isSwitchedEarly = false,
                isFinishedDay = true,
                isUpcomingDay = false
            )
        }
        else
        {
            val (targetDay, _, titlePrefix) = TimetableWidgetUpdater.calculateTargetDay(timetable, now)
            val nextDaySpots = timetable?.getLessonSpotsForDay(targetDay) ?: emptyList()
            val firstIdx = nextDaySpots.indexOfFirst { it.isNotEmpty() }.coerceAtLeast(0)
            val spot = nextDaySpots.getOrNull(firstIdx)
            val sub = substitutionsMap[targetDay]?.changes?.getOrNull(firstIdx)
            val pStr = periods.getOrNull(firstIdx)?.toString() ?: "${firstIdx + 1}. hodina"
            return CurrentHourInfo(
                lesson = resolveLessonForSpot(spot, subjectGroups, settings?.userProfileGroup),
                substitution = sub,
                periodIndex = firstIdx,
                periodRange = pStr,
                statusText = "$titlePrefix · ${firstIdx + 1}. hodina",
                isSwitchedEarly = false,
                isFinishedDay = false,
                isUpcomingDay = true
            )
        }
    }

    /**
     * Renders a square single-cell widget for the current lesson, styled exactly like one cell of the timetable.
     */
    fun renderCurrentHourBitmap(
        context: Context,
        timetableData: TimetableData?,
        settings: Settings.WidgetSettings,
        now: LocalDateTime,
        width: Int,
        height: Int,
        subjectGroups: Map<String, String> = emptyMap(),
        userClassName: String? = null
    ): Bitmap
    {
        val w = if (width > 0) width else 320
        val h = if (height > 0) height else 320
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val colors = getEffectiveColors(settings)

        // Clear background
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val pad = 4f
        val cardRect = RectF(pad, pad, w - pad, h - pad)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.borderColor.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.cellBackgroundColor.toInt()
            style = Paint.Style.FILL
        }
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.dayHeaderColor.toInt()
            style = Paint.Style.FILL
        }

        // Draw card background
        canvas.drawRoundRect(cardRect, 14f, 14f, cardBgPaint)

        val info = resolveCurrentHourInfo(timetableData, now, subjectGroups, settings)
        val density = context.resources.displayMetrics.density
        val scale = settings.cellScale.coerceIn(0.7f, 1.6f)

        if (info.isFinishedDay)
        {
            val cornerPadding = (cardRect.width() * 0.05f).coerceIn(8f, 18f)
            val cornerTextSize = (min(cardRect.width(), cardRect.height()) * 0.165f * scale).coerceIn(16f, 48f)

            // Bottom-left: Class name (e.g. C4b)
            val classStr = if (!settings.hideClass) {
                userClassName?.takeIf { it.isNotBlank() }
                    ?: extractClassName(timetableData)
                    ?: ""
            } else ""

            if (classStr.isNotEmpty())
            {
                val classPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colors.cellTextColor.toInt()
                    textSize = cornerTextSize
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.LEFT
                }
                val maxClassW = cardRect.width() * 0.45f
                val cWidth = classPaint.measureText(classStr)
                if (cWidth > maxClassW && cWidth > 0)
                {
                    classPaint.textSize = cornerTextSize * (maxClassW / cWidth)
                }
                val fmC = classPaint.fontMetrics
                val baseline = cardRect.bottom - cornerPadding - fmC.descent
                canvas.drawText(classStr, cardRect.left + cornerPadding, baseline, classPaint)
            }

            val isWide = cardRect.width() > cardRect.height() * 1.35f
            val maxTextW = cardRect.width() - cornerPadding * 2 - 8f

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.cellTextColor.toInt()
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.cellTextColor.toInt() and 0x99FFFFFF.toInt()
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
            }

            if (isWide)
            {
                var mainTextSize = (cardRect.height() * 0.28f * scale).coerceIn(16f, 64f)
                textPaint.textSize = mainTextSize
                val measuredW = textPaint.measureText("Konec vyučování")
                if (measuredW > maxTextW && measuredW > 0)
                {
                    mainTextSize *= (maxTextW / measuredW)
                    textPaint.textSize = mainTextSize
                }

                var subTextSize = (mainTextSize * 0.65f).coerceAtLeast(12f)
                subTextPaint.textSize = subTextSize

                val centerY = cardRect.centerY() - (subTextSize * 0.4f)
                val fmMain = textPaint.fontMetrics
                val mainY = centerY - (fmMain.ascent + fmMain.descent) / 2
                canvas.drawText("Konec vyučování", cardRect.centerX(), mainY, textPaint)

                val subY = mainY + (subTextSize * 1.4f)
                canvas.drawText("Volno", cardRect.centerX(), subY, subTextPaint)
            }
            else
            {
                var lineSize = (cardRect.height() * 0.16f * scale).coerceIn(14f, 48f)
                textPaint.textSize = lineSize

                val maxWordW = maxOf(textPaint.measureText("Konec"), textPaint.measureText("vyučování"))
                if (maxWordW > maxTextW && maxWordW > 0)
                {
                    lineSize *= (maxTextW / maxWordW)
                    textPaint.textSize = lineSize
                }

                var subTextSize = (lineSize * 0.70f).coerceAtLeast(11f)
                subTextPaint.textSize = subTextSize

                val lineSpacing = lineSize * 1.15f
                val totalBlockH = lineSpacing * 2 + subTextSize * 1.3f
                val topY = cardRect.centerY() - (totalBlockH / 2f)

                val fm = textPaint.fontMetrics
                val line1Y = topY - fm.ascent
                val line2Y = line1Y + lineSpacing
                val subY = line2Y + (subTextSize * 1.35f)

                canvas.drawText("Konec", cardRect.centerX(), line1Y, textPaint)
                canvas.drawText("vyučování", cardRect.centerX(), line2Y, textPaint)
                canvas.drawText("Volno", cardRect.centerX(), subY, subTextPaint)
            }

            if (!settings.hideBorder)
            {
                canvas.drawRoundRect(cardRect, 14f, 14f, borderPaint)
            }
            return bitmap
        }

        // 1. Top Header strip (Od kdy do kdy je ta hodina + co to je za hodinu)
        val headerHeight = (cardRect.height() * 0.20f * scale).coerceIn(48f, 135f)
        val headerRect = RectF(cardRect.left, cardRect.top, cardRect.right, cardRect.top + headerHeight)

        val headerPath = Path().apply {
            addRoundRect(
                headerRect,
                floatArrayOf(14f, 14f, 14f, 14f, 0f, 0f, 0f, 0f),
                Path.Direction.CW
            )
        }
        canvas.drawPath(headerPath, headerBgPaint)

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.dayHeaderTextColor.toInt()
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val maxHeaderW = headerRect.width() - 16f
        val statusText = info.statusText
        val periodRange = info.periodRange

        val singleLineCandidate = if (periodRange.isNotEmpty() && !statusText.contains(periodRange)) {
            "$statusText · $periodRange"
        } else {
            statusText
        }

        val targetSingleTextSize = (headerHeight * 0.52f * scale).coerceIn(20f, 56f)
        headerTextPaint.textSize = targetSingleTextSize
        val singleLineW = headerTextPaint.measureText(singleLineCandidate)

        if (periodRange.isNotEmpty() && !statusText.contains(periodRange) && (singleLineW > maxHeaderW) && headerHeight >= 64f)
        {
            // Two-line layout: Status text on top line, period time range on bottom line
            var line1Size = (headerHeight * 0.44f * scale).coerceIn(18f, 54f)
            headerTextPaint.textSize = line1Size
            val l1W = headerTextPaint.measureText(statusText)
            if (l1W > maxHeaderW && l1W > 0)
            {
                line1Size *= (maxHeaderW / l1W)
                headerTextPaint.textSize = line1Size
            }
            val fm1 = headerTextPaint.fontMetrics
            val line1Baseline = headerRect.top + headerHeight * 0.43f - (fm1.ascent + fm1.descent) / 2f
            canvas.drawText(statusText, headerRect.centerX(), line1Baseline, headerTextPaint)

            var line2Size = (headerHeight * 0.38f * scale).coerceIn(16f, 48f)
            headerTextPaint.textSize = line2Size
            val l2W = headerTextPaint.measureText(periodRange)
            if (l2W > maxHeaderW && l2W > 0)
            {
                line2Size *= (maxHeaderW / l2W)
                headerTextPaint.textSize = line2Size
            }
            val fm2 = headerTextPaint.fontMetrics
            val line2Baseline = headerRect.top + headerHeight * 0.81f - (fm2.ascent + fm2.descent) / 2f
            canvas.drawText(periodRange, headerRect.centerX(), line2Baseline, headerTextPaint)
        }
        else
        {
            // Single line layout
            var headerTextSize = targetSingleTextSize
            if (singleLineW > maxHeaderW && singleLineW > 0)
            {
                headerTextSize *= (maxHeaderW / singleLineW)
                headerTextPaint.textSize = headerTextSize
            }
            val headerFm = headerTextPaint.fontMetrics
            val headerBaseline = headerRect.centerY() - (headerFm.ascent + headerFm.descent) / 2f
            canvas.drawText(singleLineCandidate, headerRect.centerX(), headerBaseline, headerTextPaint)
        }

        // Header bottom divider line
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.borderColor.toInt()
            strokeWidth = 1.5f
        }
        canvas.drawLine(cardRect.left, headerRect.bottom, cardRect.right, headerRect.bottom, dividerPaint)

        // 2. Cell body below header: exactly like one cell of the timetable
        val cellRect = RectF(cardRect.left + 8f, headerRect.bottom + 6f, cardRect.right - 8f, cardRect.bottom - 8f)
        val cornerPadding = (cellRect.width() * 0.045f).coerceIn(8f, 18f)
        val cornerTextSize = (min(cellRect.width(), cellRect.height()) * 0.165f * scale).coerceIn(22f, 72f)

        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = cornerTextSize
            typeface = Typeface.DEFAULT_BOLD
            color = colors.cellTextColor.toInt()
        }
        val teacherPaint = Paint(smallPaint).apply { color = colors.teacherTextColor.toInt() }
        val roomPaint = Paint(smallPaint).apply { color = colors.classroomTextColor.toInt() }
        val groupPaint = Paint(smallPaint).apply { color = colors.groupTextColor.toInt() }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.DEFAULT_BOLD
            color = colors.cellTextColor.toInt()
        }

        val maxTopLeftW = cellRect.width() * 0.48f - cornerPadding
        val maxTopRightW = cellRect.width() * 0.48f - cornerPadding
        val maxBotLeftW = cellRect.width() * 0.48f - cornerPadding
        val maxBotRightW = cellRect.width() * 0.48f - cornerPadding

        if (info.substitution != null)
        {
            val parsed = parseSubstitution(info.substitution)
            val subTextColor = colors.substitutionTextColor.toInt()
            val subBg = colors.substitutionBackgroundColor.toInt()

            if ((subBg and 0xFF000000.toInt()) != 0)
            {
                val subFillP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = subBg
                    style = Paint.Style.FILL
                }
                canvas.drawRect(RectF(cardRect.left, headerRect.bottom, cardRect.right, cardRect.bottom), subFillP)
            }

            // Top-left: teacher
            if (!parsed.teacher.isNullOrBlank() && !settings.hideTeachers)
            {
                teacherPaint.color = subTextColor
                val tWidth = teacherPaint.measureText(parsed.teacher)
                if (tWidth > maxTopLeftW && tWidth > 0)
                {
                    teacherPaint.textSize = cornerTextSize * (maxTopLeftW / tWidth)
                }
                teacherPaint.textAlign = Paint.Align.LEFT
                val fmT = teacherPaint.fontMetrics
                val baseline = cellRect.top + cornerPadding - fmT.ascent
                canvas.drawText(parsed.teacher, cellRect.left + cornerPadding, baseline, teacherPaint)
            }

            // Top-right: classroom
            if (!parsed.room.isNullOrBlank())
            {
                roomPaint.color = subTextColor
                val rWidth = roomPaint.measureText(parsed.room)
                if (rWidth > maxTopRightW && rWidth > 0)
                {
                    roomPaint.textSize = cornerTextSize * (maxTopRightW / rWidth)
                }
                roomPaint.textAlign = Paint.Align.RIGHT
                val fmR = roomPaint.fontMetrics
                val baseline = cellRect.top + cornerPadding - fmR.ascent
                canvas.drawText(parsed.room, cellRect.right - cornerPadding, baseline, roomPaint)
            }

            // Bottom-left: class
            val subClassStr = info.lesson?.clazz?.takeIf { it.isNotBlank() }
                ?: userClassName?.takeIf { it.isNotBlank() }
                ?: extractClassName(timetableData)
                ?: ""
            if (subClassStr.isNotEmpty())
            {
                smallPaint.color = subTextColor
                val cWidth = smallPaint.measureText(subClassStr)
                if (cWidth > maxBotLeftW && cWidth > 0)
                {
                    smallPaint.textSize = cornerTextSize * (maxBotLeftW / cWidth)
                }
                smallPaint.textAlign = Paint.Align.LEFT
                val fmC = smallPaint.fontMetrics
                val baseline = cellRect.bottom - cornerPadding - fmC.descent
                canvas.drawText(subClassStr, cellRect.left + cornerPadding, baseline, smallPaint)
            }

            // Bottom-right: group
            val subGroupStr = info.lesson?.group ?: ""
            if (subGroupStr.isNotEmpty())
            {
                groupPaint.color = subTextColor
                val gWidth = groupPaint.measureText(subGroupStr)
                if (gWidth > maxBotRightW && gWidth > 0)
                {
                    groupPaint.textSize = cornerTextSize * (maxBotRightW / gWidth)
                }
                groupPaint.textAlign = Paint.Align.RIGHT
                val fmG = groupPaint.fontMetrics
                val baseline = cellRect.bottom - cornerPadding - fmG.descent
                canvas.drawText(subGroupStr, cellRect.right - cornerPadding, baseline, groupPaint)
            }

            // Center: Subject or Odpadá
            val subject = parsed.subject
            val topOccupied = cornerTextSize + cornerPadding + 6f
            val botOccupied = cornerTextSize + cornerPadding + 6f
            val maxSubjH = (cellRect.height() - topOccupied - botOccupied).coerceAtLeast(cellRect.height() * 0.3f)
            val maxSubjW = cellRect.width() - cornerPadding * 2 - 12f

            var subjSize = (maxSubjH * 0.65f * scale).coerceIn(24f, 140f)
            boldPaint.color = subTextColor
            boldPaint.textSize = subjSize
            val measuredSubjW = boldPaint.measureText(subject)
            if (measuredSubjW > maxSubjW && measuredSubjW > 0)
            {
                subjSize *= (maxSubjW / measuredSubjW)
                boldPaint.textSize = subjSize
            }
            boldPaint.textAlign = Paint.Align.CENTER
            val subjFm = boldPaint.fontMetrics
            val subjY = cellRect.centerY() - (subjFm.ascent + subjFm.descent) / 2
            canvas.drawText(subject, cellRect.centerX(), subjY, boldPaint)

            // Bottom-center: Note (if any)
            if (!parsed.note.isNullOrBlank())
            {
                val notePaint = Paint(smallPaint).apply {
                    color = subTextColor
                    textSize = (cornerTextSize * 0.85f).coerceAtLeast(14f)
                    textAlign = Paint.Align.CENTER
                }
                val noteBaseline = cellRect.bottom - cornerPadding - notePaint.fontMetrics.descent
                canvas.drawText(parsed.note, cellRect.centerX(), noteBaseline, notePaint)
            }
        }
        else if (info.lesson != null)
        {
            val lesson = info.lesson
            val subject = lesson.subjectName.short ?: ""
            val teacherStr = if (!settings.hideTeachers) lesson.teacherName?.short ?: "" else ""
            val roomStr = lesson.classroom ?: ""
            val classStr = lesson.clazz?.takeIf { it.isNotBlank() }
                ?: userClassName?.takeIf { it.isNotBlank() }
                ?: extractClassName(timetableData)
                ?: ""
            val groupStr = lesson.group ?: ""

            // Top-left: Teacher
            if (teacherStr.isNotEmpty())
            {
                val tWidth = teacherPaint.measureText(teacherStr)
                if (tWidth > maxTopLeftW && tWidth > 0)
                {
                    teacherPaint.textSize = cornerTextSize * (maxTopLeftW / tWidth)
                }
                teacherPaint.textAlign = Paint.Align.LEFT
                val fmT = teacherPaint.fontMetrics
                val baseline = cellRect.top + cornerPadding - fmT.ascent
                canvas.drawText(teacherStr, cellRect.left + cornerPadding, baseline, teacherPaint)
            }

            // Top-right: Classroom (jaká učebna)
            if (roomStr.isNotEmpty())
            {
                val rWidth = roomPaint.measureText(roomStr)
                if (rWidth > maxTopRightW && rWidth > 0)
                {
                    roomPaint.textSize = cornerTextSize * (maxTopRightW / rWidth)
                }
                roomPaint.textAlign = Paint.Align.RIGHT
                val fmR = roomPaint.fontMetrics
                val baseline = cellRect.top + cornerPadding - fmR.ascent
                canvas.drawText(roomStr, cellRect.right - cornerPadding, baseline, roomPaint)
            }

            // Bottom-left: Class
            if (classStr.isNotEmpty())
            {
                val cWidth = smallPaint.measureText(classStr)
                if (cWidth > maxBotLeftW && cWidth > 0)
                {
                    smallPaint.textSize = cornerTextSize * (maxBotLeftW / cWidth)
                }
                smallPaint.textAlign = Paint.Align.LEFT
                val fmC = smallPaint.fontMetrics
                val baseline = cellRect.bottom - cornerPadding - fmC.descent
                canvas.drawText(classStr, cellRect.left + cornerPadding, baseline, smallPaint)
            }

            // Bottom-right: Group
            if (groupStr.isNotEmpty())
            {
                val gWidth = groupPaint.measureText(groupStr)
                if (gWidth > maxBotRightW && gWidth > 0)
                {
                    groupPaint.textSize = cornerTextSize * (maxBotRightW / gWidth)
                }
                groupPaint.textAlign = Paint.Align.RIGHT
                val fmG = groupPaint.fontMetrics
                val baseline = cellRect.bottom - cornerPadding - fmG.descent
                canvas.drawText(groupStr, cellRect.right - cornerPadding, baseline, groupPaint)
            }

            // Center: Large bold Subject
            if (subject.isNotEmpty())
            {
                val topOccupied = cornerTextSize + cornerPadding + 6f
                val botOccupied = cornerTextSize + cornerPadding + 6f
                val maxSubjH = (cellRect.height() - topOccupied - botOccupied).coerceAtLeast(cellRect.height() * 0.3f)
                val maxSubjW = cellRect.width() - cornerPadding * 2 - 12f

                var subjSize = (maxSubjH * 0.65f * scale).coerceIn(24f, 140f)
                boldPaint.textSize = subjSize
                val measuredSubjW = boldPaint.measureText(subject)
                if (measuredSubjW > maxSubjW && measuredSubjW > 0)
                {
                    subjSize *= (maxSubjW / measuredSubjW)
                    boldPaint.textSize = subjSize
                }
                boldPaint.textAlign = Paint.Align.CENTER
                val subjFm = boldPaint.fontMetrics
                val subjY = cellRect.centerY() - (subjFm.ascent + subjFm.descent) / 2
                canvas.drawText(subject, cellRect.centerX(), subjY, boldPaint)
            }
        }
        else
        {
            // Bottom-left: Class
            val freeClassStr = userClassName?.takeIf { it.isNotBlank() }
                ?: extractClassName(timetableData)
                ?: ""
            if (freeClassStr.isNotEmpty())
            {
                smallPaint.color = colors.cellTextColor.toInt()
                val cWidth = smallPaint.measureText(freeClassStr)
                if (cWidth > maxBotLeftW && cWidth > 0)
                {
                    smallPaint.textSize = cornerTextSize * (maxBotLeftW / cWidth)
                }
                smallPaint.textAlign = Paint.Align.LEFT
                val fmC = smallPaint.fontMetrics
                val baseline = cellRect.bottom - cornerPadding - fmC.descent
                canvas.drawText(freeClassStr, cellRect.left + cornerPadding, baseline, smallPaint)
            }

            // Volná hodina
            val maxFreeW = cellRect.width() - cornerPadding * 2 - 12f
            val freePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.cellTextColor.toInt() and 0x99FFFFFF.toInt()
                textSize = (cellRect.height() * 0.22f * scale).coerceAtLeast(14f)
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            val measuredFreeW = freePaint.measureText("Volná hodina")
            if (measuredFreeW > maxFreeW && measuredFreeW > 0)
            {
                freePaint.textSize *= (maxFreeW / measuredFreeW)
            }
            val fmFree = freePaint.fontMetrics
            val freeY = cellRect.centerY() - (fmFree.ascent + fmFree.descent) / 2
            canvas.drawText("Volná hodina", cellRect.centerX(), freeY, freePaint)
        }

        // Active period border / highlight
        if (info.periodIndex >= 0 && !info.isUpcomingDay)
        {
            val activeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.activeHourBorderColor.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 3.5f
            }
            canvas.drawRoundRect(cardRect, 14f, 14f, activeBorderPaint)
        }
        else if (!settings.hideBorder)
        {
            canvas.drawRoundRect(cardRect, 14f, 14f, borderPaint)
        }

        return bitmap
    }

    private fun <T> List<T>.viewIndexed(): List<Pair<Int, T>> = mapIndexed { index, item -> index to item }
}
