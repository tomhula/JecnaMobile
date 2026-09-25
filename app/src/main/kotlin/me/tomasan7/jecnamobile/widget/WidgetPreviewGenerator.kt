package me.tomasan7.jecnamobile.widget

import android.content.Context
import android.graphics.*
import android.util.Log
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import me.tomasan7.jecnamobile.settings.Settings
import me.tomasan7.jecnamobile.timetable.TimetableData
import java.io.File
import java.io.FileOutputStream

object WidgetPreviewGenerator
{
    private const val TAG = "WidgetPreviewGenerator"

    fun generateAll(context: Context, timetableData: TimetableData?, outputDir: File)
    {
        outputDir.mkdirs()

        val settings = Settings.WidgetSettings(
            theme = Settings.WidgetTheme.DARK,
            hideHeader = false,
            hideBorder = false,
            hideTeachers = false,
            hideClass = false,
            filterMyGroups = false,
            solidHeaderBackground = false,
            periodHeaderMode = Settings.PeriodHeaderMode.BOTH
        )

        val userClassName = "C4b"

        // 1. Current Hour Preview (Square 360x360)
        try
        {
            val currentHourBmp = generateCurrentHourPreview(context, timetableData, settings, userClassName)
            saveBitmap(currentHourBmp, File(outputDir, "preview_widget_timetable_current_hour.png"))
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Failed to generate current hour preview", e)
        }

        // 2. Day Preview (560x480)
        try
        {
            val dayBmp = generateDayPreview(context, timetableData, settings, userClassName)
            saveBitmap(dayBmp, File(outputDir, "preview_widget_timetable_day.png"))
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Failed to generate day preview", e)
        }

        // 3. Week Preview (820x480)
        try
        {
            val weekBmp = generateWeekPreview(context, timetableData, settings, userClassName)
            saveBitmap(weekBmp, File(outputDir, "preview_widget_timetable_week.png"))
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Failed to generate week preview", e)
        }

        // 4. Today Row Preview (780x180)
        try
        {
            val todayRowBmp = generateTodayRowPreview(context, timetableData, settings, userClassName)
            saveBitmap(todayRowBmp, File(outputDir, "preview_widget_timetable_today_row.png"))
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Failed to generate today row preview", e)
        }
    }

    private fun saveBitmap(bitmap: Bitmap, file: File)
    {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        Log.d(TAG, "Saved preview: ${file.absolutePath} (${bitmap.width}x${bitmap.height})")
    }

    fun generateCurrentHourPreview(
        context: Context,
        timetableData: TimetableData?,
        settings: Settings.WidgetSettings,
        userClassName: String
    ): Bitmap
    {
        val simSettings = settings.copy(
            debugSimulationEnabled = true,
            debugSimulatedDay = DayOfWeek.TUESDAY.name,
            debugPeriodIndex = 3,
            debugIsBreak = false,
            debugSubstitutionType = 0
        )
        val now = LocalDateTime(2026, 9, 22, 10, 30)
        return TimetableWidgetRenderer.renderCurrentHourBitmap(
            context = context,
            timetableData = timetableData,
            settings = simSettings,
            now = now,
            width = 360,
            height = 360,
            userClassName = userClassName
        )
    }

    fun generateDayPreview(
        context: Context,
        timetableData: TimetableData?,
        settings: Settings.WidgetSettings,
        userClassName: String
    ): Bitmap
    {
        val w = 560
        val h = 480
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(0xFF000000.toInt())

        // Top header bar matching widget_timetable_day.xml
        val headerHeight = 40f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.LEFT
        }
        val titleY = 27f
        canvas.drawText("Rozvrh $userClassName · Úterý", 4f, titleY, titlePaint)

        val updatedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x99FFFFFF.toInt()
            textSize = 15f
            textAlign = Paint.Align.RIGHT
        }
        val refreshIconSize = 24f
        val refreshRect = RectF(w - refreshIconSize - 4f, (headerHeight - refreshIconSize) / 2f, w - 4f, (headerHeight + refreshIconSize) / 2f)
        TimetableWidgetRenderer.drawRefreshIcon(context, canvas, refreshRect, color = 0xCCFFFFFF.toInt())

        val updatedX = refreshRect.left - 10f
        val updatedY = 27f
        canvas.drawText("Akt. 12:45", updatedX, updatedY, updatedPaint)

        // Period rows below header
        val listTop = headerHeight + 6f
        val listHeight = h - listTop
        val simSettings = settings.copy(
            debugSimulationEnabled = true,
            debugSimulatedDay = DayOfWeek.TUESDAY.name,
            debugPeriodIndex = 3,
            debugIsBreak = false,
            debugSubstitutionType = 0
        )
        val now = LocalDateTime(2026, 9, 22, 10, 30)

        val items = TimetableWidgetRenderer.getDayPeriodItems(
            timetableData = timetableData,
            settings = simSettings,
            targetDay = DayOfWeek.TUESDAY,
            activeSpot = null,
            subjectGroups = emptyMap(),
            nowLocal = now
        )

        val visibleCount = minOf(items.size, 6)
        if (visibleCount > 0)
        {
            val pad = 6f
            val rowHeight = (listHeight - (pad * (visibleCount - 1))) / visibleCount

            for (i in 0 until visibleCount)
            {
                val item = items[i]
                val rowTop = listTop + (i * (rowHeight + pad))
                val rowBmp = TimetableWidgetRenderer.renderDayLessonRowBitmap(
                    context = context,
                    item = item,
                    settings = simSettings,
                    width = w,
                    height = rowHeight.toInt()
                )
                canvas.drawBitmap(rowBmp, 0f, rowTop, null)
            }
        }

        return bitmap
    }

    fun generateWeekPreview(
        context: Context,
        timetableData: TimetableData?,
        settings: Settings.WidgetSettings,
        userClassName: String
    ): Bitmap
    {
        val w = 820
        val h = 480
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(0xFF000000.toInt())

        // Top header bar
        val headerHeight = 40f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.LEFT
        }
        val titleY = 27f
        canvas.drawText("Rozvrh $userClassName", 4f, titleY, titlePaint)

        val updatedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x99FFFFFF.toInt()
            textSize = 15f
            textAlign = Paint.Align.RIGHT
        }
        val refreshIconSize = 24f
        val refreshRect = RectF(w - refreshIconSize - 4f, (headerHeight - refreshIconSize) / 2f, w - 4f, (headerHeight + refreshIconSize) / 2f)
        TimetableWidgetRenderer.drawRefreshIcon(context, canvas, refreshRect, color = 0xCCFFFFFF.toInt())

        val updatedX = refreshRect.left - 10f
        val updatedY = 27f
        canvas.drawText("Akt. 12:45", updatedX, updatedY, updatedPaint)

        // Grid below header
        val gridTop = headerHeight + 6f
        val gridHeight = (h - gridTop).toInt()
        val simSettings = settings.copy(
            debugSimulationEnabled = true,
            debugSimulatedDay = DayOfWeek.TUESDAY.name,
            debugPeriodIndex = 3,
            debugIsBreak = false,
            debugSubstitutionType = 0
        )
        val now = LocalDateTime(2026, 9, 22, 10, 30)
        val gridBmp = TimetableWidgetRenderer.renderWeekTimetableBitmap(
            context = context,
            timetableData = timetableData,
            settings = simSettings,
            width = w,
            height = gridHeight,
            activeSpot = null,
            subjectGroups = emptyMap()
        )
        canvas.drawBitmap(gridBmp, 0f, gridTop, null)

        return bitmap
    }

    fun generateTodayRowPreview(
        context: Context,
        timetableData: TimetableData?,
        settings: Settings.WidgetSettings,
        userClassName: String
    ): Bitmap
    {
        val w = 780
        val h = 180
        val simSettings = settings.copy(
            debugSimulationEnabled = true,
            debugSimulatedDay = DayOfWeek.TUESDAY.name,
            debugPeriodIndex = 3,
            debugIsBreak = false,
            debugSubstitutionType = 0
        )
        return TimetableWidgetRenderer.renderTodayRowTimetableBitmap(
            context = context,
            timetableData = timetableData,
            settings = simSettings,
            targetDay = DayOfWeek.TUESDAY,
            width = w,
            height = h,
            activeSpot = null,
            subjectGroups = emptyMap()
        )
    }
}
