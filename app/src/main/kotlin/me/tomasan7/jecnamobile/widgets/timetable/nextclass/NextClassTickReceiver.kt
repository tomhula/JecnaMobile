package me.tomasan7.jecnamobile.widgets.timetable.nextclass

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NextClassTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TICK) {
            CoroutineScope(Dispatchers.IO).launch {
                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(NextClassWidget::class.java)
                glanceIds.forEach { glanceId ->
                    NextClassWidget().update(context, glanceId)
                }
                reschedule(context)
            }
        }
    }

    private fun reschedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NextClassTickReceiver::class.java).apply {
            action = ACTION_TICK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 60_000L,
            60_000L,
            pendingIntent
        )
    }

    companion object {
        const val ACTION_TICK = "me.tomasan7.jecnamobile.widgets.nextclass.ACTION_TICK"
    }
}
