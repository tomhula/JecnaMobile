package me.tomasan7.jecnamobile.widgets.nextclass

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.runBlocking
import me.tomasan7.jecnamobile.widgets.timetable.TimetableWidgetWorker

class NextClassTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TICK) {
            runBlocking {
                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(NextClassWidget::class.java)
                
                glanceIds.forEach { glanceId ->
                    NextClassWidget().update(context, glanceId)
                }
            }
        }
    }

    companion object {
        const val ACTION_TICK = "me.tomasan7.jecnamobile.widgets.nextclass.ACTION_TICK"
    }
}
