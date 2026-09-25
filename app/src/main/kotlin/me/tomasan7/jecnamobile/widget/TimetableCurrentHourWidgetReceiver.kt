package me.tomasan7.jecnamobile.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.tomasan7.jecnamobile.util.settingsDataStore

class TimetableCurrentHourWidgetReceiver : AppWidgetProvider()
{
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray)
    {
        Log.d(TAG, "onUpdate called for ${appWidgetIds.size} current-hour widgets")
        TimetableWidgetUpdater.updateCurrentHourWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    )
    {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        Log.d(TAG, "onAppWidgetOptionsChanged for current-hour widget $appWidgetId")
        TimetableWidgetUpdater.updateCurrentHourWidgets(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onReceive(context: Context, intent: Intent)
    {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH)
        {
            Log.d(TAG, "Manual refresh action received on current-hour widget")
            TimetableWidgetUpdater.triggerManualRefresh(context)
        }
        else if (intent.action == ACTION_GENERATE_PREVIEWS)
        {
            Log.d(TAG, "Generating widget previews...")
            CoroutineScope(Dispatchers.IO).launch {
                val data = TimetableWidgetUpdater.loadCachedTimetableData(context)
                val outDir = java.io.File(context.getExternalFilesDir(null), "previews")
                WidgetPreviewGenerator.generateAll(context, data, outDir)
                Log.d(TAG, "Finished generating widget previews to ${outDir.absolutePath}")
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray)
    {
        super.onDeleted(context, appWidgetIds)
        val idSet = appWidgetIds.toSet()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                context.settingsDataStore.updateData { current ->
                    current.copy(perWidgetSettings = current.perWidgetSettings.filterKeys { it !in idSet })
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clean up per-widget settings", e)
            }
        }
    }

    override fun onEnabled(context: Context)
    {
        super.onEnabled(context)
        Log.d(TAG, "First current-hour widget enabled, scheduling periodic worker...")
        TimetableWidgetWorker.schedulePeriodic(context)
        TimetableWidgetUpdater.updateAllWidgets(context)
    }

    override fun onDisabled(context: Context)
    {
        super.onDisabled(context)
        Log.d(TAG, "Last current-hour widget disabled")
    }

    companion object
    {
        private const val TAG = "TimetableCurrentHourWidget"
        const val ACTION_REFRESH = "me.tomasan7.jecnamobile.widget.ACTION_REFRESH_CURRENT_HOUR"
        const val ACTION_GENERATE_PREVIEWS = "me.tomasan7.jecnamobile.widget.ACTION_GENERATE_PREVIEWS"
    }
}
