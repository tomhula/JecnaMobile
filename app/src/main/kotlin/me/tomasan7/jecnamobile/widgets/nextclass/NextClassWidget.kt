package me.tomasan7.jecnamobile.widgets.nextclass

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProviders
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import io.github.tomhula.jecnaapi.data.timetable.Lesson
import io.github.tomhula.jecnaapi.data.timetable.Timetable
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.timetable.ChangeEntry
import me.tomasan7.jecnamobile.timetable.SubstitutionData
import me.tomasan7.jecnamobile.widgets.base.BaseWidgetStateDefinition
import me.tomasan7.jecnamobile.widgets.base.BaseWidgetStateSerializer
import me.tomasan7.jecnamobile.widgets.shared.SharedTimetableWidgetState
import me.tomasan7.jecnamobile.widgets.timetable.TimetableWidgetWorker

private const val LOG_TAG = "NextClassWidget"

private fun Context.getStringRes(@StringRes resId: Int, vararg formatArgs: Any): String =
    getString(resId, *formatArgs)

object NextClassWidgetStateSerializer : BaseWidgetStateSerializer<SharedTimetableWidgetState>(
    kSerializer = SharedTimetableWidgetState.serializer(),
    logTag = LOG_TAG
) {
    override val defaultValue = SharedTimetableWidgetState()
}

object NextClassWidgetStateDefinition : BaseWidgetStateDefinition<SharedTimetableWidgetState>(
    filePrefix = "nextclass_widget_",
    serializer = NextClassWidgetStateSerializer
)

internal class NextClassWidget : GlanceAppWidget() {
    override val stateDefinition = NextClassWidgetStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val state = currentState<SharedTimetableWidgetState>()
            GlanceTheme { NextClassWidgetContent(context = context, state = state) }
        }
    }
}

internal class NextClassWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextClassWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        appWidgetIds.forEach { appWidgetId ->
            scheduleTickUpdates(context, appWidgetId)
        }
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<TimetableWidgetWorker>().build())
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { appWidgetId ->
            cancelTickUpdates(context, appWidgetId)
        }
    }

    private fun scheduleTickUpdates(context: Context, appWidgetId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NextClassTickReceiver::class.java).apply {
            action = NextClassTickReceiver.ACTION_TICK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val now = java.util.Calendar.getInstance()
        val hour = now.get(java.util.Calendar.HOUR_OF_DAY)
        val dayOfWeek = now.get(java.util.Calendar.DAY_OF_WEEK)

        val isWeekend = dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY
        val isAfterSchool = hour >= 17

        val intervalMillis = when {
            isWeekend || isAfterSchool -> 60 * 60 * 1000L  // 1 hour
            else -> 5 * 60 * 1000L  // 5 minutes during classes
        }

        val triggerTime = System.currentTimeMillis() + intervalMillis

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            intervalMillis,
            pendingIntent
        )
    }

    private fun cancelTickUpdates(context: Context, appWidgetId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NextClassTickReceiver::class.java).apply {
            action = NextClassTickReceiver.ACTION_TICK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}

internal class RefreshNextClassAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, NextClassWidgetStateDefinition, glanceId) { state ->
            state.copy(isLoading = true, isManualRefresh = true)
        }
        NextClassWidget().update(context, glanceId)
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<TimetableWidgetWorker>().build())
    }
}

@Composable
private fun NextClassWidgetContent(context: Context, state: SharedTimetableWidgetState) {
    val colors = GlanceTheme.colors

    val now = Clock.System.now()
    val today = now.toLocalDateTime(TimeZone.currentSystemDefault())

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.background)
            .padding(8.dp)
    ) {
        when {
            state.isLoading && state.timetablePage == null -> LoadingContent(colors)
            state.error != null && state.timetablePage == null -> ErrorContent(context, colors)
            state.timetablePage?.timetable != null -> NextClassDisplayContent(
                context = context,
                timetable = state.timetablePage.timetable,
                substitutions = state.substitutions,
                today = today,
                colors = colors
            )
            else -> EmptyContent(context, colors)
        }
    }
}

@Composable
private fun NextClassDisplayContent(
    context: Context,
    timetable: Timetable,
    substitutions: SubstitutionData?,
    today: LocalDateTime,
    colors: ColorProviders
) {
    val displayInfo = getNextClassDisplayInfo(context, timetable, today)
    val dailySchedule = substitutions?.data?.find { it.date == displayInfo.targetDate.toString() }

    Column(modifier = GlanceModifier.fillMaxSize()) {
        Header(context = context, title = displayInfo.title, colors = colors)

        if (displayInfo.isSchoolOut) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = context.getStringRes(R.string.widget_timetable_no_classes),
                    style = TextStyle(color = colors.onBackground, fontSize = 14.sp)
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize().padding(top = 4.dp)) {

                if (displayInfo.currentLessons.isNotEmpty()) {
                    item {
                        val endsInText = displayInfo.minutesToNextState?.let {
                            context.getStringRes(R.string.widget_nextclass_ends_in, it.toString())
                        }
                        SectionHeader(
                            title = context.getStringRes(R.string.widget_nextclass_now),
                            timeText = endsInText,
                            colors = colors,
                            isCurrent = true
                        )
                    }

                    displayInfo.currentLessons.forEachIndexed { index, lesson ->
                        item {
                            LessonCardSwitcher(
                                context = context,
                                lesson = lesson,
                                change = dailySchedule?.changes?.getOrNull(displayInfo.currentLessonIndex),
                                isCurrent = true,
                                colors = colors
                            )
                        }
                        if (index < displayInfo.currentLessons.lastIndex) {
                            item { Spacer(modifier = GlanceModifier.height(4.dp)) }
                        }
                    }
                }

                if (displayInfo.currentLessons.isNotEmpty() && displayInfo.nextLessons.isNotEmpty() && !displayInfo.isBreak) {
                    item { Spacer(modifier = GlanceModifier.height(12.dp)) }
                }

                if (displayInfo.isBreak || displayInfo.nextLessons.isNotEmpty()) {
                    item {
                        val nextMinutes = if (displayInfo.currentLessons.isEmpty()) {
                            displayInfo.minutesToNextState
                        } else {
                            displayInfo.nextPeriod?.from?.let { nextStart ->
                                ((nextStart.hour * 60 + nextStart.minute) - (today.time.hour * 60 + today.time.minute)).toLong()
                            }
                        }
                        val startsInText = nextMinutes?.let {
                            context.getStringRes(R.string.widget_nextclass_starts_in, it.toString())
                        }

                        SectionHeader(
                            title = context.getStringRes(R.string.widget_nextclass_next),
                            timeText = startsInText,
                            colors = colors,
                            isCurrent = false
                        )
                    }

                    displayInfo.nextLessons.forEachIndexed { index, lesson ->
                        item {
                            LessonCardSwitcher(
                                context = context,
                                lesson = lesson,
                                change = dailySchedule?.changes?.getOrNull(displayInfo.nextLessonIndex),
                                isCurrent = false,
                                colors = colors
                            )
                        }
                        if (index < displayInfo.nextLessons.lastIndex) {
                            item { Spacer(modifier = GlanceModifier.height(4.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(context: Context, title: String, colors: ColorProviders) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = TextStyle(
                color = colors.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        RefreshButton(context = context, colors = colors)
    }
}

@Composable
private fun SectionHeader(title: String, timeText: String?, colors: ColorProviders, isCurrent: Boolean) {
    val highlightColor = if (isCurrent) colors.primary else colors.onBackground

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = 6.dp, start = 4.dp, top = if (isCurrent) 0.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = TextStyle(
                color = highlightColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        )
        if (timeText != null) {
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = "•  $timeText",
                style = TextStyle(
                    color = highlightColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun RefreshButton(context: Context, colors: ColorProviders) {
    Box(
        modifier = GlanceModifier
            .size(28.dp)
            .cornerRadius(6.dp)
            .clickable(actionRunCallback<RefreshNextClassAction>()),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_refresh),
            contentDescription = context.getStringRes(R.string.widget_timetable_refresh_description),
            modifier = GlanceModifier.size(16.dp),
            colorFilter = ColorFilter.tint(colors.onBackground)
        )
    }
}

@Composable
private fun LessonCardSwitcher(
    context: Context,
    lesson: Lesson,
    change: ChangeEntry?,
    isCurrent: Boolean,
    colors: ColorProviders
) {
    if (change != null) {
        SubstitutionCard(context, change, colors)
    } else {
        LessonCard(lesson, isCurrent, colors)
    }
}

@Composable
private fun LessonCard(
    lesson: Lesson,
    isCurrent: Boolean,
    colors: ColorProviders
) {
    val containerBg = if (isCurrent) colors.primaryContainer else colors.surface
    val contentColor = if (isCurrent) colors.onPrimaryContainer else colors.onSurface

    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(10.dp)
            .background(containerBg)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = lesson.subjectName.full,
                    style = TextStyle(color = contentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    maxLines = 1
                )

                Spacer(modifier = GlanceModifier.height(2.dp))

                val secondaryInfo = listOfNotNull(
                    lesson.teacherName?.short ?: lesson.teacherName?.full,
                    lesson.group,
                    lesson.clazz
                ).joinToString(" • ")

                if (secondaryInfo.isNotEmpty()) {
                    Text(
                        text = secondaryInfo,
                        style = TextStyle(color = contentColor, fontSize = 12.sp),
                        maxLines = 1
                    )
                }
            }

            if (lesson.classroom != null) {
                Spacer(modifier = GlanceModifier.width(8.dp))
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(6.dp)
                        .background(if (isCurrent) colors.onPrimaryContainer else colors.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lesson.classroom!!,
                        style = TextStyle(
                            color = if (isCurrent) colors.primaryContainer else colors.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SubstitutionCard(
    context: Context,
    change: ChangeEntry,
    colors: ColorProviders
) {
    val text = if (change.willBeSpecified == true) {
        change.text + "\n" + context.getStringRes(R.string.substitution_will_be_specified)
    } else {
        change.text
    }

    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(10.dp)
            .background(colors.errorContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = TextStyle(color = colors.onErrorContainer, fontWeight = FontWeight.Bold, fontSize = 13.sp),
            maxLines = 3
        )
    }
}

@Composable
private fun LoadingContent(colors: ColorProviders) {
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = colors.onBackground, modifier = GlanceModifier.size(24.dp))
    }
}

@Composable
private fun ErrorContent(context: Context, colors: ColorProviders) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionRunCallback<RefreshNextClassAction>())
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = context.getStringRes(R.string.widget_timetable_error),
                style = TextStyle(color = colors.error, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            )
            Text(
                text = context.getStringRes(R.string.widget_timetable_tap_to_refresh),
                style = TextStyle(color = colors.onBackground, fontSize = 10.sp)
            )
        }
    }
}

@Composable
private fun EmptyContent(context: Context, colors: ColorProviders) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionRunCallback<RefreshNextClassAction>()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = context.getStringRes(R.string.widget_timetable_loading_timetable),
            style = TextStyle(color = colors.onBackground, fontSize = 12.sp)
        )
    }
}
