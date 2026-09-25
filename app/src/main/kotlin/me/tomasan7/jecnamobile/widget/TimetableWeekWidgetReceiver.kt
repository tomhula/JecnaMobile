package me.tomasan7.jecnamobile.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.tomasan7.jecnamobile.util.settingsDataStore

class TimetableWeekWidgetReceiver : AppWidgetProvider()
{
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray)
    {
        Log.d(TAG, "onUpdate called for ${appWidgetIds.size} week widgets")
        TimetableWidgetUpdater.updateWeekWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    )
    {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        Log.d(TAG, "onAppWidgetOptionsChanged for week widget $appWidgetId")
        TimetableWidgetUpdater.updateWeekWidgets(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onReceive(context: Context, intent: Intent)
    {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH)
        {
            Log.d(TAG, "Manual refresh action received")
            TimetableWidgetUpdater.triggerManualRefresh(context)
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
        Log.d(TAG, "First week widget enabled, scheduling periodic worker...")
        TimetableWidgetWorker.schedulePeriodic(context)
        TimetableWidgetUpdater.updateAllWidgets(context)
    }

    override fun onDisabled(context: Context)
    {
        super.onDisabled(context)
        Log.d(TAG, "Last week widget disabled")
    }

    companion object
    {
        private const val TAG = "TimetableWeekWidget"
        const val ACTION_REFRESH = "me.tomasan7.jecnamobile.widget.ACTION_REFRESH_WEEK"
    }
}
