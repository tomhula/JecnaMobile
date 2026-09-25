package me.tomasan7.jecnamobile.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.chrynan.parcelable.core.putExtra
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.tomasan7.jecnamobile.EXTRA_NAVIGATE_TO
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.navigation.AppDestination
import me.tomasan7.jecnamobile.settings.Settings
import me.tomasan7.jecnamobile.timetable.TimetableData
import me.tomasan7.jecnamobile.util.settingsDataStore

class TimetableDayWidgetService : RemoteViewsService()
{
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory
    {
        return TimetableDayRemoteViewsFactory(applicationContext, intent)
    }
}

class TimetableDayRemoteViewsFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory
{
    private val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    private var items: List<TimetableWidgetRenderer.DayPeriodItem> = emptyList()
    private var widgetSettings: Settings.WidgetSettings = Settings.WidgetSettings()
    private var targetDay: DayOfWeek = DayOfWeek.MONDAY
    private var widgetWidth: Int = 600
    private var rowHeightPx: Int = 160

    override fun onCreate()
    {
        loadData()
    }

    override fun onDataSetChanged()
    {
        loadData()
    }

    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    private fun loadData()
    {
        try
        {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val (w, _) = TimetableWidgetUpdater.getWidgetDimensions(context, appWidgetManager, widgetId, 650, 550)
            widgetWidth = w

            val density = context.resources.displayMetrics.density
            val now = Clock.System.now()
            val localNow = now.toLocalDateTime(TimeZone.currentSystemDefault())

            val settings: Settings
            val timetableData: TimetableData?
            runBlocking {
                settings = context.settingsDataStore.data.first()
                timetableData = TimetableWidgetUpdater.loadCachedTimetableData(context)
            }

            widgetSettings = settings.getWidgetSettings(widgetId)
            val (calculatedDay, _, _) = TimetableWidgetUpdater.calculateTargetDay(timetableData, timetableData?.page?.timetable, localNow, settings.subjectGroups)
            targetDay = if (widgetSettings.debugSimulationEnabled) {
                TimetableWidgetRenderer.getSimulatedDay(widgetSettings, localNow)
            } else {
                calculatedDay
            }

            val scale = widgetSettings.cellScale.coerceIn(0.7f, 1.6f)
            rowHeightPx = (52f * density * scale).toInt().coerceIn((44f * density).toInt(), (90f * density).toInt())

            val activeSpot = if (widgetSettings.debugSimulationEnabled && widgetSettings.debugPeriodIndex >= 0) {
                timetableData?.page?.timetable?.getLessonSpotsForDay(targetDay)?.getOrNull(widgetSettings.debugPeriodIndex)
            } else {
                timetableData?.page?.timetable?.let { tt ->
                    val pair = TimetableWidgetRenderer.getActiveOrUpcomingPeriod(tt, widgetSettings, localNow)
                    if (pair != null) tt.getLessonSpotsForDay(pair.first)?.getOrNull(pair.second) else null
                }
            }

            items = TimetableWidgetRenderer.getDayPeriodItems(
                timetableData = timetableData,
                settings = widgetSettings,
                targetDay = targetDay,
                activeSpot = activeSpot,
                subjectGroups = settings.subjectGroups,
                nowLocal = localNow
            )
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Error loading day widget items", e)
            items = emptyList()
        }
    }

    override fun onDestroy()
    {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    override fun getViewAt(position: Int): RemoteViews?
    {
        val item = items.getOrNull(position) ?: return null
        val views = RemoteViews(context.packageName, R.layout.widget_timetable_day_item)

        val rowBitmap = TimetableWidgetRenderer.renderDayLessonRowBitmap(
            context = context,
            item = item,
            settings = widgetSettings,
            width = widgetWidth,
            height = rowHeightPx
        )
        views.setImageViewBitmap(R.id.widget_day_row_image, rowBitmap)

        val dest = AppDestination.Timetable(lessonDay = targetDay.name, lessonHour = item.periodIndex)
        val fillInIntent = Intent().apply {
            putExtra(EXTRA_NAVIGATE_TO, dest, AppDestination.serializer())
            putExtra(TimetableWidgetUpdater.EXTRA_LESSON_DAY, targetDay.name)
            putExtra(TimetableWidgetUpdater.EXTRA_LESSON_HOUR, item.periodIndex)
        }
        views.setOnClickFillInIntent(R.id.widget_day_row_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    companion object
    {
        private const val TAG = "TimetableDayService"
    }
}
