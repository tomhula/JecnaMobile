package me.tomasan7.jecnamobile.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.chrynan.parcelable.core.putExtra
import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.data.timetable.Timetable
import io.github.tomhula.jecnaapi.util.SchoolYear
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.datetime.*
import me.tomasan7.jecnamobile.EXTRA_NAVIGATE_TO
import me.tomasan7.jecnamobile.MainActivity
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.caching.SchoolYearPeriodParams
import me.tomasan7.jecnamobile.di.SubstitutionsCacheRepository
import me.tomasan7.jecnamobile.login.AuthRepository
import me.tomasan7.jecnamobile.navigation.AppDestination
import me.tomasan7.jecnamobile.settings.Settings
import me.tomasan7.jecnamobile.timetable.TimetableCacheRepository
import me.tomasan7.jecnamobile.timetable.TimetableData
import me.tomasan7.jecnamobile.timetable.TimetableRepository
import me.tomasan7.jecnamobile.util.settingsDataStore
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
object TimetableWidgetUpdater : KoinComponent
{
    private const val TAG = "TimetableWidgetUpdater"

    const val EXTRA_LESSON_DAY = "extra_lesson_day"
    const val EXTRA_LESSON_HOUR = "extra_lesson_hour"

    private val authRepository: AuthRepository by inject()
    private val timetableRepository: TimetableRepository by inject()
    private val timetableCacheRepository: TimetableCacheRepository by inject()
    private val substitutionsCacheRepository: SubstitutionsCacheRepository by inject()

    private val cellIds = arrayOf(
        intArrayOf(R.id.cell_0_0, R.id.cell_0_1, R.id.cell_0_2, R.id.cell_0_3, R.id.cell_0_4, R.id.cell_0_5, R.id.cell_0_6, R.id.cell_0_7, R.id.cell_0_8, R.id.cell_0_9),
        intArrayOf(R.id.cell_1_0, R.id.cell_1_1, R.id.cell_1_2, R.id.cell_1_3, R.id.cell_1_4, R.id.cell_1_5, R.id.cell_1_6, R.id.cell_1_7, R.id.cell_1_8, R.id.cell_1_9),
        intArrayOf(R.id.cell_2_0, R.id.cell_2_1, R.id.cell_2_2, R.id.cell_2_3, R.id.cell_2_4, R.id.cell_2_5, R.id.cell_2_6, R.id.cell_2_7, R.id.cell_2_8, R.id.cell_2_9),
        intArrayOf(R.id.cell_3_0, R.id.cell_3_1, R.id.cell_3_2, R.id.cell_3_3, R.id.cell_3_4, R.id.cell_3_5, R.id.cell_3_6, R.id.cell_3_7, R.id.cell_3_8, R.id.cell_3_9),
        intArrayOf(R.id.cell_4_0, R.id.cell_4_1, R.id.cell_4_2, R.id.cell_4_3, R.id.cell_4_4, R.id.cell_4_5, R.id.cell_4_6, R.id.cell_4_7, R.id.cell_4_8, R.id.cell_4_9)
    )

    private val headerColIds = intArrayOf(
        R.id.header_col_0, R.id.header_col_1, R.id.header_col_2, R.id.header_col_3, R.id.header_col_4,
        R.id.header_col_5, R.id.header_col_6, R.id.header_col_7, R.id.header_col_8, R.id.header_col_9
    )

    private val schoolDays = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    )

    fun updateAllWidgets(context: Context, explicitSettings: Settings? = null)
    {
        CoroutineScope(Dispatchers.IO).launch {
            try
            {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return@launch

                val weekIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, TimetableWeekWidgetReceiver::class.java)
                )
                val dayIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, TimetableDayWidgetReceiver::class.java)
                )
                val todayRowIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, TimetableTodayRowWidgetReceiver::class.java)
                )
                val currentHourIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, TimetableCurrentHourWidgetReceiver::class.java)
                )

                if (weekIds.isEmpty() && dayIds.isEmpty() && todayRowIds.isEmpty() && currentHourIds.isEmpty()) return@launch

                val settings = explicitSettings ?: context.settingsDataStore.data.first()
                val timetableData = loadCachedTimetableData(context)

                if (weekIds.isNotEmpty())
                {
                    updateWeekWidgetsInternal(context, appWidgetManager, weekIds, settings, timetableData)
                }

                if (dayIds.isNotEmpty())
                {
                    updateDayWidgetsInternal(context, appWidgetManager, dayIds, settings, timetableData)
                }

                if (todayRowIds.isNotEmpty())
                {
                    updateTodayRowWidgetsInternal(context, appWidgetManager, todayRowIds, settings, timetableData)
                }

                if (currentHourIds.isNotEmpty())
                {
                    updateCurrentHourWidgetsInternal(context, appWidgetManager, currentHourIds, settings, timetableData)
                }

                scheduleNextTransitionAlarm(context, timetableData?.page?.timetable)
            }
            catch (e: Exception)
            {
                Log.e(TAG, "Failed to update widgets", e)
            }
        }
    }

    fun updateWeekWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray)
    {
        CoroutineScope(Dispatchers.IO).launch {
            try
            {
                val settings = context.settingsDataStore.data.first()
                val timetableData = loadCachedTimetableData(context)
                updateWeekWidgetsInternal(context, appWidgetManager, appWidgetIds, settings, timetableData)
                scheduleNextTransitionAlarm(context, timetableData?.page?.timetable)
            }
            catch (e: Exception)
            {
                Log.e(TAG, "Failed to update week widgets", e)
            }
        }
    }

    fun updateDayWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray)
    {
        CoroutineScope(Dispatchers.IO).launch {
            try
            {
                val settings = context.settingsDataStore.data.first()
                val timetableData = loadCachedTimetableData(context)
                updateDayWidgetsInternal(context, appWidgetManager, appWidgetIds, settings, timetableData)
                scheduleNextTransitionAlarm(context, timetableData?.page?.timetable)
            }
            catch (e: Exception)
            {
                Log.e(TAG, "Failed to update day widgets", e)
            }
        }
    }

    fun updateTodayRowWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray)
    {
        CoroutineScope(Dispatchers.IO).launch {
            try
            {
                val settings = context.settingsDataStore.data.first()
                val timetableData = loadCachedTimetableData(context)
                updateTodayRowWidgetsInternal(context, appWidgetManager, appWidgetIds, settings, timetableData)
                scheduleNextTransitionAlarm(context, timetableData?.page?.timetable)
            }
            catch (e: Exception)
            {
                Log.e(TAG, "Failed to update today-row widgets", e)
            }
        }
    }

    fun updateCurrentHourWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray)
    {
        CoroutineScope(Dispatchers.IO).launch {
            try
            {
                val settings = context.settingsDataStore.data.first()
                val timetableData = loadCachedTimetableData(context)
                updateCurrentHourWidgetsInternal(context, appWidgetManager, appWidgetIds, settings, timetableData)
                scheduleNextTransitionAlarm(context, timetableData?.page?.timetable)
            }
            catch (e: Exception)
            {
                Log.e(TAG, "Failed to update current-hour widgets", e)
            }
        }
    }

    internal suspend fun loadCachedTimetableData(context: Context): TimetableData?
    {
        return try
        {
            val params = SchoolYearPeriodParams(SchoolYear.current(), SchoolYearPeriodParams.CURRENT_PERIOD_ID)
            val cached = timetableCacheRepository.getCache(params)
            val data = cached?.data ?: return null

            val className = timetableRepository.getStudentClassName()
                ?: TimetableWidgetRenderer.extractClassName(data)

            val freshSubs = if (!className.isNullOrBlank()) {
                substitutionsCacheRepository.getCache(className)?.data
            } else null

            if (freshSubs != null) {
                data.copy(substitutions = freshSubs)
            } else {
                data
            }
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Error loading cached timetable data", e)
            null
        }
    }

    private fun updateWeekWidgetsInternal(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        settings: Settings,
        timetableData: TimetableData?
    )
    {
        val now = Clock.System.now()
        val activeSpot = timetableData?.page?.timetable?.getLessonSpot(now)

        for (widgetId in appWidgetIds)
        {
            val widgetSettings = settings.getWidgetSettings(widgetId)
            val colors = TimetableWidgetRenderer.getEffectiveColors(widgetSettings)
            val views = RemoteViews(context.packageName, R.layout.widget_timetable_week)

            val density = context.resources.displayMetrics.density
            val shouldShowTime = widgetSettings.shouldShowTimestamp()
            val timeStr = if (shouldShowTime) widgetSettings.resolveEffectiveTimestamp(timetableData) else ""
            val hasBottomUpdated = shouldShowTime && widgetSettings.hideHeader
            val horizPadPx = (8 * density).toInt()

            val bgAlpha = (colors.backgroundColor shr 24) and 0xFFL
            val isExplicitlyTransparent = bgAlpha == 0L
            val isSemiTransparent = bgAlpha in 1L..254L
            val isTransparentBg = isExplicitlyTransparent || (widgetSettings.hideBorder && !isSemiTransparent)
            if (isTransparentBg)
            {
                views.setInt(R.id.widget_root, "setBackgroundColor", Color.TRANSPARENT)
                val topPadPx = horizPadPx
                val bottomPadPx = if (hasBottomUpdated) (1 * density).toInt() else horizPadPx
                views.setViewPadding(R.id.widget_root, horizPadPx, topPadPx, horizPadPx, bottomPadPx)
            }
            else if (isSemiTransparent)
            {
                views.setInt(R.id.widget_root, "setBackgroundColor", colors.backgroundColor.toInt())
                val padPx = (10 * density).toInt()
                val bottomPadPx = if (hasBottomUpdated) (3 * density).toInt() else padPx
                views.setViewPadding(R.id.widget_root, padPx, padPx, padPx, bottomPadPx)
            }
            else
            {
                if (colors.backgroundColor == 0xFF000000L)
                {
                    views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_container_background)
                }
                else
                {
                    views.setInt(R.id.widget_root, "setBackgroundColor", colors.backgroundColor.toInt())
                }
            }
            val padPx = (8 * density).toInt()
            views.setViewPadding(R.id.widget_root, padPx, padPx, padPx, padPx)

            // Header bar visibility (Rozvrh <třída>)
            if (widgetSettings.hideHeader)
            {
                views.setViewVisibility(R.id.widget_header_bar, View.GONE)
                views.setViewVisibility(R.id.widget_corner_refresh_button, View.VISIBLE)
                val cornerRefreshIntent = Intent(context, TimetableWeekWidgetReceiver::class.java).apply {
                    action = TimetableWeekWidgetReceiver.ACTION_REFRESH
                }
                val cornerRefreshPending = PendingIntent.getBroadcast(
                    context,
                    1002,
                    cornerRefreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_corner_refresh_button, cornerRefreshPending)

                val bottomUpdatedStr = if (shouldShowTime) "Aktualizováno: $timeStr" else ""
                views.setViewVisibility(R.id.widget_bottom_last_updated, if (bottomUpdatedStr.isNotEmpty()) View.VISIBLE else View.GONE)
                views.setTextViewText(R.id.widget_bottom_last_updated, bottomUpdatedStr)
                views.setTextColor(R.id.widget_bottom_last_updated, ((colors.cellTextColor and 0x00FFFFFF) or 0xCC000000L).toInt())

                val bottomConfigIntent = Intent(context, WidgetConfigurationActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                }
                val bottomConfigPending = PendingIntent.getActivity(
                    context,
                    widgetId + 70000,
                    bottomConfigIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_bottom_last_updated, bottomConfigPending)
            }
            else
            {
                views.setViewVisibility(R.id.widget_header_bar, View.VISIBLE)
                views.setViewVisibility(R.id.widget_corner_refresh_button, View.GONE)
                views.setViewVisibility(R.id.widget_bottom_last_updated, View.GONE)

                // Header title: keep Rozvrh <class> even if hideClass is enabled (hideClass only hides class in cells)
                val className = TimetableWidgetRenderer.extractClassName(timetableData)
                val titleText = if (className.isNullOrBlank()) "Rozvrh hodin" else "Rozvrh $className"
                views.setTextViewText(R.id.widget_title, titleText)
                views.setTextColor(R.id.widget_title, colors.cellTextColor.toInt())

                // Last updated timestamp
                val updatedStr = if (shouldShowTime) "Akt. $timeStr" else ""
                views.setViewVisibility(R.id.widget_last_updated, if (updatedStr.isNotEmpty()) View.VISIBLE else View.GONE)
                views.setTextViewText(R.id.widget_last_updated, updatedStr)
                views.setTextColor(R.id.widget_last_updated, colors.cellTextColor.toInt() and 0x99FFFFFF.toInt())

                // Refresh button
                val refreshIntent = Intent(context, TimetableWeekWidgetReceiver::class.java).apply {
                    action = TimetableWeekWidgetReceiver.ACTION_REFRESH
                }
                val refreshPending = PendingIntent.getBroadcast(
                    context,
                    1001,
                    refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPending)

                // Title click to configure
                val configIntent = Intent(context, WidgetConfigurationActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                }
                val configPending = PendingIntent.getActivity(
                    context,
                    widgetId + 50000,
                    configIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_title, configPending)
            }

            val (targetWidth, targetHeight) = getWidgetDimensions(context, appWidgetManager, widgetId, 900, 550)
            val todayLocal = now.toLocalDateTime(TimeZone.currentSystemDefault())
            val effectiveActiveSpot = if (widgetSettings.debugSimulationEnabled && widgetSettings.debugPeriodIndex >= 0) {
                val simDay = TimetableWidgetRenderer.getSimulatedDay(widgetSettings, todayLocal)
                timetableData?.page?.timetable?.getLessonSpotsForDay(simDay)?.getOrNull(widgetSettings.debugPeriodIndex)
            } else {
                timetableData?.page?.timetable?.let { tt ->
                    val pair = TimetableWidgetRenderer.getActiveOrUpcomingPeriod(tt, widgetSettings, todayLocal)
                    if (pair != null) tt.getLessonSpotsForDay(pair.first)?.getOrNull(pair.second) else null
                }
            }
            val bitmap = TimetableWidgetRenderer.renderWeekTimetableBitmap(
                context = context,
                timetableData = timetableData,
                settings = widgetSettings,
                width = targetWidth,
                height = targetHeight,
                activeSpot = effectiveActiveSpot,
                subjectGroups = settings.subjectGroups
            )
            views.setImageViewBitmap(R.id.widget_timetable_image, bitmap)

            // Adjust header spacers and cell targets to periodCount
            val periodCount = TimetableWidgetRenderer.getWeekPeriodCount(timetableData)
            for (hourIndex in 0 until 10)
            {
                views.setViewVisibility(headerColIds[hourIndex], if (hourIndex < periodCount) View.VISIBLE else View.GONE)
            }

            // Attach click targets for each cell to open hour specifics in app
            for ((dayIndex, day) in schoolDays.withIndex())
            {
                for (hourIndex in 0 until 10)
                {
                    val cellViewId = cellIds[dayIndex][hourIndex]
                    if (hourIndex < periodCount)
                    {
                        views.setViewVisibility(cellViewId, View.VISIBLE)
                        val dest: AppDestination = AppDestination.Timetable(lessonDay = day.name, lessonHour = hourIndex)
                        val openIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(EXTRA_NAVIGATE_TO, dest, AppDestination.serializer())
                            putExtra(EXTRA_LESSON_DAY, day.name)
                            putExtra(EXTRA_LESSON_HOUR, hourIndex)
                        }
                        val pendingIntent = PendingIntent.getActivity(
                            context,
                            dayIndex * 100 + hourIndex,
                            openIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(cellViewId, pendingIntent)
                    }
                    else
                    {
                        views.setViewVisibility(cellViewId, View.GONE)
                    }
                }
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun updateDayWidgetsInternal(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        settings: Settings,
        timetableData: TimetableData?
    )
    {
        val now = Clock.System.now()
        val localNow = now.toLocalDateTime(TimeZone.currentSystemDefault())

        // Target day determination (today vs tomorrow after school)
        val (targetDay, targetDate, titlePrefix) = calculateTargetDay(timetableData, timetableData?.page?.timetable, localNow, settings.subjectGroups)

        val activeSpot = timetableData?.page?.timetable?.getLessonSpot(now)

        for (widgetId in appWidgetIds)
        {
            val widgetSettings = settings.getWidgetSettings(widgetId)
            val effectiveTargetDay = if (widgetSettings.debugSimulationEnabled) {
                TimetableWidgetRenderer.getSimulatedDay(widgetSettings, localNow)
            } else {
                targetDay
            }
            val effectiveTitlePrefix = if (widgetSettings.debugSimulationEnabled) "Simulace" else titlePrefix
            val colors = TimetableWidgetRenderer.getEffectiveColors(widgetSettings)
            val views = RemoteViews(context.packageName, R.layout.widget_timetable_day)

            val density = context.resources.displayMetrics.density
            val shouldShowTime = widgetSettings.shouldShowTimestamp()
            val timeStr = if (shouldShowTime) widgetSettings.resolveEffectiveTimestamp(timetableData) else ""
            val hasBottomUpdated = shouldShowTime && widgetSettings.hideHeader
            val horizPadPx = (8 * density).toInt()

            val bgAlpha = (colors.backgroundColor shr 24) and 0xFFL
            val isExplicitlyTransparent = bgAlpha == 0L
            val isSemiTransparent = bgAlpha in 1L..254L
            val isTransparentBg = isExplicitlyTransparent || (widgetSettings.hideBorder && !isSemiTransparent)
            if (isTransparentBg)
            {
                views.setInt(R.id.widget_day_root, "setBackgroundColor", Color.TRANSPARENT)
                val topPadPx = horizPadPx
                val bottomPadPx = if (hasBottomUpdated) (1 * density).toInt() else horizPadPx
                views.setViewPadding(R.id.widget_day_root, horizPadPx, topPadPx, horizPadPx, bottomPadPx)
            }
            else if (isSemiTransparent)
            {
                views.setInt(R.id.widget_day_root, "setBackgroundColor", colors.backgroundColor.toInt())
                val padPx = (10 * density).toInt()
                val bottomPadPx = if (hasBottomUpdated) (3 * density).toInt() else padPx
                views.setViewPadding(R.id.widget_day_root, padPx, padPx, padPx, bottomPadPx)
            }
            else
            {
                if (colors.backgroundColor == 0xFF000000L)
                {
                    views.setInt(R.id.widget_day_root, "setBackgroundResource", R.drawable.widget_container_background)
                }
                else
                {
                    views.setInt(R.id.widget_day_root, "setBackgroundColor", colors.backgroundColor.toInt())
                }
            }
            val padPx = (8 * density).toInt()
            views.setViewPadding(R.id.widget_day_root, padPx, padPx, padPx, padPx)

            // Header visibility
            if (widgetSettings.hideHeader)
            {
                views.setViewVisibility(R.id.widget_day_header_bar, View.GONE)
                val bottomUpdatedStr = if (shouldShowTime) "Aktualizováno: $timeStr" else ""
                views.setViewVisibility(R.id.widget_day_bottom_last_updated, if (bottomUpdatedStr.isNotEmpty()) View.VISIBLE else View.GONE)
                views.setTextViewText(R.id.widget_day_bottom_last_updated, bottomUpdatedStr)
                views.setTextColor(R.id.widget_day_bottom_last_updated, ((colors.cellTextColor and 0x00FFFFFF) or 0xCC000000L).toInt())
            }
            else
            {
                views.setViewVisibility(R.id.widget_day_header_bar, View.VISIBLE)
                views.setViewVisibility(R.id.widget_day_bottom_last_updated, View.GONE)

                val dayName = TimetableWidgetRenderer.getShortDayName(effectiveTargetDay)
                val titleText = "$effectiveTitlePrefix · $dayName ${targetDate.dayOfMonth}. ${targetDate.monthNumber}."
                views.setTextViewText(R.id.widget_day_title, titleText)
                views.setTextColor(R.id.widget_day_title, colors.cellTextColor.toInt())

                // Last updated timestamp
                val updatedStr = if (shouldShowTime) "Akt. $timeStr" else ""
                views.setViewVisibility(R.id.widget_day_last_updated, if (updatedStr.isNotEmpty()) View.VISIBLE else View.GONE)
                views.setTextViewText(R.id.widget_day_last_updated, updatedStr)
                views.setTextColor(R.id.widget_day_last_updated, colors.cellTextColor.toInt() and 0x99FFFFFF.toInt())

                // Refresh button
                val refreshIntent = Intent(context, TimetableDayWidgetReceiver::class.java).apply {
                    action = TimetableDayWidgetReceiver.ACTION_REFRESH
                }
                val refreshPending = PendingIntent.getBroadcast(
                    context,
                    2001,
                    refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_day_refresh_button, refreshPending)

                // Day title click to configure
                val configDayIntent = Intent(context, WidgetConfigurationActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                }
                val configDayPending = PendingIntent.getActivity(
                    context,
                    widgetId + 60000,
                    configDayIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_day_title, configDayPending)
            }

            views.setTextColor(R.id.widget_day_empty_view, colors.cellTextColor.toInt())

            // Setup ListView adapter
            val serviceIntent = Intent(context, TimetableDayWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = android.net.Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_day_list, serviceIntent)
            views.setEmptyView(R.id.widget_day_list, R.id.widget_day_empty_view)

            // Setup PendingIntent template for list items
            val listClickIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val listClickPendingIntent = PendingIntent.getActivity(
                context,
                widgetId + 75000,
                listClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_day_list, listClickPendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_day_list)
        }
    }

    private fun updateTodayRowWidgetsInternal(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        settings: Settings,
        timetableData: TimetableData?
    )
    {
        val now = Clock.System.now()
        val localNow = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val (targetDay, targetDate, titlePrefix) = calculateTargetDay(timetableData, timetableData?.page?.timetable, localNow, settings.subjectGroups)
        val activeSpot = timetableData?.page?.timetable?.getLessonSpot(now)

        for (widgetId in appWidgetIds)
        {
            val widgetSettings = settings.getWidgetSettings(widgetId)
            val effectiveTargetDay = if (widgetSettings.debugSimulationEnabled) {
                TimetableWidgetRenderer.getSimulatedDay(widgetSettings, localNow)
            } else {
                targetDay
            }
            val effectiveTitlePrefix = if (widgetSettings.debugSimulationEnabled) "Simulace" else titlePrefix
            val colors = TimetableWidgetRenderer.getEffectiveColors(widgetSettings)
            val views = RemoteViews(context.packageName, R.layout.widget_timetable_today_row)

            val density = context.resources.displayMetrics.density
            val shouldShowTime = widgetSettings.shouldShowTimestamp()
            val timeStr = if (shouldShowTime) widgetSettings.resolveEffectiveTimestamp(timetableData) else ""
            val hasBottomUpdated = shouldShowTime
            val horizPadPx = (8 * density).toInt()

            val bgAlpha = (colors.backgroundColor shr 24) and 0xFFL
            val isExplicitlyTransparent = bgAlpha == 0L
            val isSemiTransparent = bgAlpha in 1L..254L
            val isTransparentBg = isExplicitlyTransparent || (widgetSettings.hideBorder && !isSemiTransparent)
            if (isTransparentBg)
            {
                views.setInt(R.id.widget_today_root, "setBackgroundColor", Color.TRANSPARENT)
            }
            else if (isSemiTransparent)
            {
                views.setInt(R.id.widget_today_root, "setBackgroundColor", colors.backgroundColor.toInt())
            }
            else
            {
                if (colors.backgroundColor == 0xFF000000L)
                {
                    views.setInt(R.id.widget_today_root, "setBackgroundResource", R.drawable.widget_container_background)
                }
                else
                {
                    views.setInt(R.id.widget_today_root, "setBackgroundColor", colors.backgroundColor.toInt())
                }
            }
            val padPx = (6 * density).toInt()
            views.setViewPadding(R.id.widget_today_root, padPx, padPx, padPx, padPx)

            // Hide external header bar: 1-row widget contains period headers and corner refresh inside bitmap
            views.setViewVisibility(R.id.widget_today_header_bar, View.GONE)

            // Corner refresh button
            val cornerRefreshIntent = Intent(context, TimetableTodayRowWidgetReceiver::class.java).apply {
                action = TimetableTodayRowWidgetReceiver.ACTION_REFRESH
            }
            val cornerRefreshPending = PendingIntent.getBroadcast(
                context,
                3002,
                cornerRefreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_today_corner_refresh, cornerRefreshPending)

            // Click on image opens app to today's timetable
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_NAVIGATE_TO, AppDestination.Timetable(lessonDay = targetDay.name), AppDestination.serializer())
            }
            val openAppPending = PendingIntent.getActivity(
                context,
                widgetId + 82000,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_today_image, openAppPending)

            val (targetWidth, targetHeight) = getWidgetDimensions(context, appWidgetManager, widgetId, 900, 160)
            val effectiveActiveSpot = if (widgetSettings.debugSimulationEnabled && widgetSettings.debugPeriodIndex >= 0) {
                timetableData?.page?.timetable?.getLessonSpotsForDay(effectiveTargetDay)?.getOrNull(widgetSettings.debugPeriodIndex)
            } else {
                timetableData?.page?.timetable?.let { tt ->
                    val pair = TimetableWidgetRenderer.getActiveOrUpcomingPeriod(tt, widgetSettings, localNow)
                    if (pair != null) tt.getLessonSpotsForDay(pair.first)?.getOrNull(pair.second) else null
                }
            }
            val bitmap = TimetableWidgetRenderer.renderTodayRowTimetableBitmap(
                context = context,
                timetableData = timetableData,
                settings = widgetSettings,
                targetDay = effectiveTargetDay,
                width = targetWidth,
                height = targetHeight,
                activeSpot = effectiveActiveSpot,
                subjectGroups = settings.subjectGroups
            )
            views.setImageViewBitmap(R.id.widget_today_image, bitmap)

            val bottomUpdatedStr = if (shouldShowTime) "Aktualizováno: $timeStr" else ""
            views.setViewVisibility(R.id.widget_today_bottom_last_updated, if (bottomUpdatedStr.isNotEmpty()) View.VISIBLE else View.GONE)
            views.setTextViewText(R.id.widget_today_bottom_last_updated, bottomUpdatedStr)
            views.setTextColor(R.id.widget_today_bottom_last_updated, ((colors.cellTextColor and 0x00FFFFFF) or 0xCC000000L).toInt())

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private suspend fun updateCurrentHourWidgetsInternal(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        settings: Settings,
        timetableData: TimetableData?
    )
    {
        val now = Clock.System.now()
        val localNow = now.toLocalDateTime(TimeZone.currentSystemDefault())

        for (widgetId in appWidgetIds)
        {
            val widgetSettings = settings.getWidgetSettings(widgetId)
            val colors = TimetableWidgetRenderer.getEffectiveColors(widgetSettings)
            val views = RemoteViews(context.packageName, R.layout.widget_timetable_current_hour)

            val density = context.resources.displayMetrics.density
            val padPx = (6 * density).toInt()
            val bgAlpha = (colors.backgroundColor shr 24) and 0xFFL
            val isExplicitlyTransparent = bgAlpha == 0L
            val isSemiTransparent = bgAlpha in 1L..254L
            val isTransparentBg = isExplicitlyTransparent || (widgetSettings.hideBorder && !isSemiTransparent)
            if (isTransparentBg)
            {
                views.setInt(R.id.widget_current_root, "setBackgroundColor", Color.TRANSPARENT)
                views.setViewPadding(R.id.widget_current_root, padPx, padPx, padPx, padPx)
            }
            else if (isSemiTransparent)
            {
                views.setInt(R.id.widget_current_root, "setBackgroundColor", colors.backgroundColor.toInt())
                views.setViewPadding(R.id.widget_current_root, padPx, padPx, padPx, padPx)
            }
            else
            {
                if (colors.backgroundColor == 0xFF000000L)
                {
                    views.setInt(R.id.widget_current_root, "setBackgroundResource", R.drawable.widget_container_background)
                }
                else
                {
                    views.setInt(R.id.widget_current_root, "setBackgroundColor", colors.backgroundColor.toInt())
                }
                views.setViewPadding(R.id.widget_current_root, padPx, padPx, padPx, padPx)
            }

            // Refresh button
            val refreshIntent = Intent(context, TimetableCurrentHourWidgetReceiver::class.java).apply {
                action = TimetableCurrentHourWidgetReceiver.ACTION_REFRESH
            }
            val refreshPending = PendingIntent.getBroadcast(
                context,
                4001,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_current_refresh_button, refreshPending)

            // Resolve hour info to open destination in MainActivity
            val hourInfo = TimetableWidgetRenderer.resolveCurrentHourInfo(timetableData, localNow, settings.subjectGroups, widgetSettings)
            val targetDay = when (localNow.dayOfWeek) {
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> DayOfWeek.MONDAY
                else -> localNow.dayOfWeek
            }
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(
                    EXTRA_NAVIGATE_TO,
                    AppDestination.Timetable(lessonDay = targetDay.name, lessonHour = hourInfo.periodIndex.coerceAtLeast(0)),
                    AppDestination.serializer()
                )
            }
            val openAppPending = PendingIntent.getActivity(
                context,
                widgetId + 91000,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_current_image, openAppPending)

            val userClassName = runCatching { timetableRepository.getStudentClassName() }.getOrNull()
                ?: TimetableWidgetRenderer.extractClassName(timetableData)

            val (targetWidth, targetHeight) = getWidgetDimensions(context, appWidgetManager, widgetId, 400, 400)
            val bitmap = TimetableWidgetRenderer.renderCurrentHourBitmap(
                context = context,
                timetableData = timetableData,
                settings = widgetSettings,
                now = localNow,
                width = targetWidth,
                height = targetHeight,
                subjectGroups = settings.subjectGroups,
                userClassName = userClassName
            )
            views.setImageViewBitmap(R.id.widget_current_image, bitmap)

            val shouldShowTime = widgetSettings.shouldShowTimestamp()
            val timeStr = if (shouldShowTime) widgetSettings.resolveEffectiveTimestamp(timetableData) else ""
            val bottomUpdatedStr = if (shouldShowTime) "Aktualizováno: $timeStr" else ""
            views.setViewVisibility(R.id.widget_current_bottom_last_updated, if (bottomUpdatedStr.isNotEmpty()) View.VISIBLE else View.GONE)
            views.setTextViewText(R.id.widget_current_bottom_last_updated, bottomUpdatedStr)
            views.setTextColor(R.id.widget_current_bottom_last_updated, ((colors.cellTextColor and 0x00FFFFFF) or 0xCC000000L).toInt())

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    internal fun getWidgetDimensions(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        defaultWidth: Int,
        defaultHeight: Int
    ): Pair<Int, Int>
    {
        return try
        {
            val options = appWidgetManager.getAppWidgetOptions(widgetId) ?: return Pair(defaultWidth, defaultHeight)
            val density = context.resources.displayMetrics.density
            val isPortrait = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

            val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val maxWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
            val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            val maxHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)

            val widthDp = if (isPortrait)
            {
                if (minWidthDp > 0) minWidthDp else maxWidthDp
            }
            else
            {
                if (maxWidthDp > 0) maxWidthDp else minWidthDp
            }

            val heightDp = if (isPortrait)
            {
                if (maxHeightDp > 0) maxHeightDp else minHeightDp
            }
            else
            {
                if (minHeightDp > 0) minHeightDp else maxHeightDp
            }

            val w = if (widthDp > 0) (widthDp * density).toInt() else defaultWidth
            val h = if (heightDp > 0) (heightDp * density).toInt() else defaultHeight

            Pair(w.coerceIn(minOf(defaultWidth, 350), 2400), h.coerceIn(minOf(defaultHeight, 140), 2000))
        }
        catch (e: Exception)
        {
            Pair(defaultWidth, defaultHeight)
        }
    }

    fun calculateTargetDay(
        timetableData: TimetableData?,
        timetable: Timetable?,
        now: LocalDateTime,
        subjectGroups: Map<String, String> = emptyMap(),
        settings: Settings.WidgetSettings? = null
    ): Triple<DayOfWeek, LocalDate, String>
    {
        val today = now.date
        val currentTime = now.time

        val lastLessonEndTime = TimetableWidgetRenderer.getLastNonFreeLessonEndTime(
            timetableData = timetableData,
            timetable = timetable,
            day = today.dayOfWeek,
            subjectGroups = subjectGroups,
            settings = settings,
            nowLocal = now
        )

        val switchTime = lastLessonEndTime?.let { endTime ->
            val totalSec = endTime.toSecondOfDay() + 3600
            if (totalSec >= 86400) LocalTime(23, 59, 59) else LocalTime.fromSecondOfDay(totalSec)
        } ?: LocalTime(16, 30)

        val dayFinished = currentTime >= switchTime

        return when
        {
            now.dayOfWeek == DayOfWeek.SATURDAY -> {
                Triple(DayOfWeek.MONDAY, today.plus(DatePeriod(days = 2)), "V pondělí")
            }
            now.dayOfWeek == DayOfWeek.SUNDAY -> {
                Triple(DayOfWeek.MONDAY, today.plus(DatePeriod(days = 1)), "V pondělí")
            }
            dayFinished -> {
                if (now.dayOfWeek == DayOfWeek.FRIDAY)
                {
                    Triple(DayOfWeek.MONDAY, today.plus(DatePeriod(days = 3)), "V pondělí")
                }
                else
                {
                    val nextDate = today.plus(DatePeriod(days = 1))
                    Triple(nextDate.dayOfWeek, nextDate, "Zítra")
                }
            }
            else -> Triple(today.dayOfWeek, today, "Dnes")
        }
    }

    fun calculateTargetDay(timetable: Timetable?, now: LocalDateTime): Triple<DayOfWeek, LocalDate, String>
    {
        return calculateTargetDay(null, timetable, now)
    }

    /**
     * Schedules the next exact low-power alarm only at school lesson boundaries.
     * ZERO battery drain outside transition moments.
     */
    fun scheduleNextTransitionAlarm(context: Context, timetable: Timetable?)
    {
        val periods = timetable?.lessonPeriods ?: return
        if (periods.isEmpty()) return

        val now = Clock.System.now()
        val localNow = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val today = localNow.date
        val currentTime = localNow.time

        // Only schedule during Monday-Friday
        if (today.dayOfWeek == DayOfWeek.SATURDAY || today.dayOfWeek == DayOfWeek.SUNDAY)
        {
            return
        }

        // Find next transition today:
        // 1. Period starts (period.from)
        // 2. 10 minutes before period ends (period.to - 10 min) for the early switch rule!
        // 3. Period ends (period.to)
        val transitionTimes = mutableListOf<LocalTime>()
        for (period in periods)
        {
            transitionTimes.add(period.from)
            transitionTimes.add(period.to)
            val switchSec = period.to.toSecondOfDay() - 10 * 60
            if (switchSec > period.from.toSecondOfDay())
            {
                val hour = (switchSec / 3600).coerceIn(0, 23)
                val minute = ((switchSec % 3600) / 60).coerceIn(0, 59)
                val sec = (switchSec % 60).coerceIn(0, 59)
                transitionTimes.add(LocalTime(hour, minute, sec))
            }
        }

        val switchTime = TimetableWidgetRenderer.getLastNonFreeLessonEndTime(
            timetableData = null,
            timetable = timetable,
            day = today.dayOfWeek,
            nowLocal = localNow
        )?.let { endTime ->
            val totalSec = endTime.toSecondOfDay() + 3600
            if (totalSec < 86400) LocalTime.fromSecondOfDay(totalSec) else null
        } ?: LocalTime(16, 30)

        transitionTimes.add(switchTime)
        transitionTimes.sort()

        val nextTime = transitionTimes.firstOrNull { it > currentTime }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, TimetableWidgetAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            5001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (nextTime != null)
        {
            val nextDateTime = LocalDateTime(today, nextTime)
            val triggerMillis = nextDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

            try
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                {
                    if (alarmManager.canScheduleExactAlarms())
                    {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    }
                    else
                    {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    }
                }
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
                else
                {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
                Log.d(TAG, "Scheduled next lesson transition alarm at $nextTime")
            }
            catch (e: SecurityException)
            {
                Log.w(TAG, "Exact alarm permission not granted, falling back to inexact alarm", e)
                try
                {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    }
                    else
                    {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    }
                }
                catch (e2: Exception)
                {
                    Log.e(TAG, "Failed to schedule fallback alarm", e2)
                }
            }
        }
    }

    /**
     * Triggers an immediate one-shot refresh of timetable data and updates all widgets.
     */
    fun triggerManualRefresh(context: Context)
    {
        CoroutineScope(Dispatchers.IO).launch {
            try
            {
                val className = runCatching { timetableRepository.getStudentClassName() }.getOrNull()
                    ?: TimetableWidgetRenderer.extractClassName(
                        try {
                            val params = SchoolYearPeriodParams(SchoolYear.current(), SchoolYearPeriodParams.CURRENT_PERIOD_ID)
                            timetableCacheRepository.getCache(params)?.data
                        } catch (e: Exception) {
                            null
                        }
                    )
                if (!className.isNullOrBlank())
                {
                    runCatching { substitutionsCacheRepository.getRealAndCache(className) }
                }
                val params = SchoolYearPeriodParams(SchoolYear.current(), SchoolYearPeriodParams.CURRENT_PERIOD_ID)
                runCatching { timetableCacheRepository.getRealAndCache(params) }

                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val timestamp = timeFormat.format(Date())
                val nowMs = System.currentTimeMillis()

                context.settingsDataStore.updateData { current ->
                    val updatedPerWidget = current.perWidgetSettings.mapValues { (_, ws) ->
                        ws.copy(lastRefreshedTimestamp = timestamp, lastManualRefreshTimeMs = nowMs)
                    }
                    current.copy(
                        widgetSettings = current.widgetSettings.copy(lastRefreshedTimestamp = timestamp, lastManualRefreshTimeMs = nowMs),
                        perWidgetSettings = updatedPerWidget
                    )
                }

                updateAllWidgets(context)
            }
            catch (e: Exception)
            {
                Log.e(TAG, "Manual refresh failed", e)
            }
        }
    }
}
