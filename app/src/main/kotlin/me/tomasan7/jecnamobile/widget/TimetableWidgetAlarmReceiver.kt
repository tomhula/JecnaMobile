package me.tomasan7.jecnamobile.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class TimetableWidgetAlarmReceiver : BroadcastReceiver()
{
    override fun onReceive(context: Context, intent: Intent)
    {
        Log.d(TAG, "Lesson transition alarm fired. Updating widgets...")
        TimetableWidgetUpdater.updateAllWidgets(context)
    }

    companion object
    {
        private const val TAG = "TimetableWidgetAlarm"
    }
}
