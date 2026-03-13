package me.tomasan7.jecnamobile.widgets.nextclass

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import io.github.tomhula.jecnaapi.data.timetable.Lesson
import io.github.tomhula.jecnaapi.data.timetable.LessonPeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.timetable.ChangeEntry
import me.tomasan7.jecnamobile.util.getWeekDayNameKey
import me.tomasan7.jecnamobile.widgets.base.BaseWidgetStateDefinition
import me.tomasan7.jecnamobile.widgets.base.BaseWidgetStateSerializer
import me.tomasan7.jecnamobile.widgets.shared.SharedTimetableWidgetState
import me.tomasan7.jecnamobile.widgets.timetable.TimetableWidgetWorker
import kotlin.time.Clock

private const val LOG_TAG = "NextClassWidget"

private fun Context.getStringRes(@StringRes resId: Int, vararg formatArgs: Any): String = getString(resId, *formatArgs)

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

@Composable
private fun NextClassWidgetContent(context: Context, state: SharedTimetableWidgetState) {
    val colors = GlanceTheme.colors
    val now = Clock.System.now()
    val today = now.toLocalDateTime(TimeZone.currentSystemDefault())

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.background)
            .padding(12.dp)
            .clickable(actionRunCallback<RefreshNextClassAction>())
    ) {
        when {
            state.isLoading && state.timetablePage == null -> LoadingContent(context, colors)
            state.error != null && state.timetablePage == null -> ErrorContent(context, colors)
            state.timetablePage?.timetable != null -> {
                val dailySchedule = state.substitutions?.data?.find { it.date == today.date.toString() }
                
                val (displayInfo, nextTickTime) = calculateNextClassInfo(
                    timetable = state.timetablePage.timetable,
                    today = today,
                    dailySchedule = dailySchedule
                )
                
                // Schedule next exact update
                scheduleNextTick(context, nextTickTime)

                NextClassDisplay(context, displayInfo, colors)
            }
            else -> EmptyContent(context, colors)
        }
    }
}

@Composable
private fun NextClassDisplay(
    context: Context,
    info: NextClassDisplayInfo,
    colors: ColorProviders
) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        if (info.isSchoolOut) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = context.getStringRes(R.string.widget_nextclass_schools_out),
                    style = TextStyle(
                        color = colors.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
            }
        } else {
            if (info.currentLessons.isNotEmpty()) {
                val minutesText = info.minutesToNextState?.let { "$it min" } ?: ""
                val header = "${context.getStringRes(R.string.widget_nextclass_now)} (ends in $minutesText)"
                
                LessonSection(
                    context = context,
                    header = header,
                    lessonInfos = info.currentLessons,
                    colors = colors
                )
                Spacer(modifier = GlanceModifier.padding(bottom = 8.dp))
            } else if (info.isBreak) {
                val minutesText = info.minutesToNextState?.let { "$it min" } ?: ""
                
                Text(
                    text = "${context.getStringRes(R.string.widget_nextclass_break)} (ends in $minutesText)",
                    style = TextStyle(
                        color = colors.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    modifier = GlanceModifier.padding(bottom = 4.dp)
                )
                Spacer(modifier = GlanceModifier.padding(bottom = 8.dp))
            }
            
            if (info.nextLessons.isNotEmpty()) {
                val isTomorrow = info.targetDate != Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                val headerPrefix = if (isTomorrow) {
                    val dayName = context.getStringRes(getWeekDayNameKey(info.targetDate?.dayOfWeek ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.dayOfWeek))
                    dayName
                } else {
                    context.getStringRes(R.string.widget_nextclass_next)
                }
                
                val header = if (info.currentLessons.isEmpty() && info.isBreak && info.minutesToNextState != null) {
                    "$headerPrefix (starts in ${info.minutesToNextState} min)"
                } else {
                    headerPrefix
                }

                LessonSection(
                    context = context,
                    header = header,
                    lessonInfos = info.nextLessons,
                    colors = colors
                )
            }
        }
    }
}

@Composable
private fun LessonSection(
    context: Context,
    header: String,
    lessonInfos: List<LessonWithPeriodAndChange>,
    colors: ColorProviders
) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Text(
            text = header,
            style = TextStyle(
                color = colors.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            ),
            modifier = GlanceModifier.padding(bottom = 4.dp)
        )
        
        lessonInfos.forEachIndexed { index, lessonInfo ->
            if (lessonInfo.change != null) {
                 SubstitutionCard(context, lessonInfo.change, colors)
            } else {
                 LessonCard(lessonInfo.lesson, lessonInfo.period, colors)
            }
            if (index < lessonInfos.size - 1) {
                Spacer(modifier = GlanceModifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
private fun SubstitutionCard(context: Context, change: ChangeEntry, colors: ColorProviders) {
    val text = if (change.willBeSpecified == true) {
        change.text + "\n" + context.getStringRes(R.string.substitution_will_be_specified)
    } else {
        change.text
    }

    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(10.dp)
            .background(colors.tertiaryContainer)
            .padding(12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = colors.onTertiaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            ),
            maxLines = 3
        )
    }
}

@Composable
private fun LessonCard(lesson: Lesson, period: LessonPeriod, colors: ColorProviders) {
    val secondaryInfo = listOfNotNull(
        lesson.teacherName?.short ?: lesson.teacherName?.full,
        lesson.group,
        lesson.clazz
    ).joinToString(" • ")
    
    val timeString = "${period.from} - ${period.to}"

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(10.dp)
            .background(colors.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                 Text(
                    text = lesson.subjectName.full,
                    style = TextStyle(
                        color = colors.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = timeString,
                    style = TextStyle(color = colors.onSurface, fontSize = 11.sp),
                    maxLines = 1
                )
            }
            if (secondaryInfo.isNotEmpty()) {
                Spacer(modifier = GlanceModifier.padding(top = 4.dp))
                Text(
                    text = secondaryInfo,
                    style = TextStyle(color = colors.onSurface, fontSize = 12.sp),
                    maxLines = 1
                )
            }
        }

        if (lesson.classroom != null) {
            Spacer(modifier = GlanceModifier.width(8.dp))
            Box(
                modifier = GlanceModifier
                    .cornerRadius(6.dp)
                    .background(colors.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = lesson.classroom!!,
                    style = TextStyle(
                        color = colors.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(context: Context, colors: ColorProviders) {
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = colors.onBackground)
    }
}

@Composable
private fun ErrorContent(context: Context, colors: ColorProviders) {
    Box(
        modifier = GlanceModifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = context.getStringRes(R.string.widget_timetable_error),
                style = TextStyle(color = colors.error, fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun EmptyContent(context: Context, colors: ColorProviders) {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = context.getStringRes(R.string.widget_timetable_loading_timetable),
            style = TextStyle(color = colors.onBackground)
        )
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
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<TimetableWidgetWorker>().build())
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
