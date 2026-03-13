package me.tomasan7.jecnamobile.widgets.timetable

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ColorFilter
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
import androidx.glance.appwidget.lazy.itemsIndexed
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
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import io.github.tomhula.jecnaapi.data.timetable.Lesson
import io.github.tomhula.jecnaapi.data.timetable.LessonPeriod
import io.github.tomhula.jecnaapi.data.timetable.Timetable
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.timetable.ChangeEntry
import me.tomasan7.jecnamobile.timetable.DailySchedule
import me.tomasan7.jecnamobile.timetable.SubstitutionData

import me.tomasan7.jecnamobile.widgets.base.BaseWidgetStateDefinition
import me.tomasan7.jecnamobile.widgets.base.BaseWidgetStateSerializer
import me.tomasan7.jecnamobile.widgets.shared.SharedTimetableWidgetState

private const val LOG_TAG = "TimetableWidget"

private fun Context.getStringRes(@StringRes resId: Int, vararg formatArgs: Any): String = getString(resId, *formatArgs)

object TimetableWidgetStateSerializer : BaseWidgetStateSerializer<SharedTimetableWidgetState>(
    kSerializer = SharedTimetableWidgetState.serializer(),
    logTag = LOG_TAG
) {
    override val defaultValue = SharedTimetableWidgetState()
}

object TimetableWidgetStateDefinition : BaseWidgetStateDefinition<SharedTimetableWidgetState>(
    filePrefix = "timetable_widget_",
    serializer = TimetableWidgetStateSerializer
)

internal class TimetableWidget : GlanceAppWidget() {
    override val stateDefinition = TimetableWidgetStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val state = currentState<SharedTimetableWidgetState>()
            GlanceTheme { TimetableWidgetContent(context = context, state = state) }
        }
    }
}

@Composable
private fun TimetableWidgetContent(context: Context, state: SharedTimetableWidgetState) {
    val colors = GlanceTheme.colors
    val now = Clock.System.now();
    val today = now.toLocalDateTime(TimeZone.currentSystemDefault())

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.background)
            .padding(12.dp)
    ) {
        when {
            state.isLoading -> LoadingContent(context, colors)
            state.error != null -> ErrorContent(context, colors)
            state.timetablePage?.timetable != null -> DailyTimetableContent(
                context = context,
                timetable = state.timetablePage.timetable,
                substitutions = state.substitutions,
                lastUpdated = state.lastUpdated,
                today = today,
                colors = colors
            )
            else -> EmptyContent(context, colors)
        }
    }
}


@Composable
private fun DailyTimetableContent(
    context: Context,
    timetable: Timetable,
    substitutions: SubstitutionData?,
    lastUpdated: Long,
    today: LocalDateTime,
    colors: ColorProviders
) {
    val displayInfo = getWidgetDisplayInfo(context, timetable, today)
    val dailySchedule = substitutions?.data?.find { it.date == displayInfo.targetDate.toString() }

    Column(modifier = GlanceModifier.fillMaxSize()) {
        Header(
            context = context,
            title = displayInfo.title,
            substitutions = substitutions,
            colors = colors
        )

        if (displayInfo.visibleSpots.isEmpty()) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    context.getStringRes(R.string.widget_timetable_no_classes),
                    style = TextStyle(color = colors.onBackground)
                )
            }
        } else {
            LessonList(
                context = context,
                lessonSpots = displayInfo.visibleSpots,
                lessonPeriods = timetable.lessonPeriods,
                dailySchedule = dailySchedule,
                colors = colors
            )
        }

        if (lastUpdated > 0) {
            LastUpdatedInfo(context = context, lastUpdated = lastUpdated, colors = colors)
        }
    }
}

@Composable
private fun Header(
    context: Context,
    title: String,
    substitutions: SubstitutionData?,
    colors: ColorProviders
) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = 12.dp, start = 4.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = TextStyle(
                    color = colors.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            RefreshButton(context = context, colors = colors)
        }

        if (substitutions != null) {
            val intervalText = if (substitutions.currentUpdateSchedule < 60) {
                context.resources.getQuantityString(
                    R.plurals.substitution_update_interval_minutes,
                    substitutions.currentUpdateSchedule,
                    substitutions.currentUpdateSchedule
                )
            } else {
                val hours = substitutions.currentUpdateSchedule / 60
                context.resources.getQuantityString(
                    R.plurals.subtitution_update_interval_hours,
                    substitutions.currentUpdateSchedule,
                    hours
                )
            }
            Text(
                text = context.getStringRes(
                    R.string.subtitution_info,
                    substitutions.lastUpdated,
                    intervalText
                ),
                style = TextStyle(
                    color = colors.onBackground,
                    fontSize = 12.sp
                ),
                modifier = GlanceModifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun RefreshButton(context: Context, colors: ColorProviders) {
    Box(
        modifier = GlanceModifier
            .size(32.dp)
            .cornerRadius(8.dp)
            .clickable(actionRunCallback<RefreshTimetableAction>()),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_refresh),
            contentDescription = context.getStringRes(R.string.widget_timetable_refresh_description),
            modifier = GlanceModifier.size(20.dp),
            colorFilter = ColorFilter.tint(colors.onBackground)
        )
    }
}

@Composable
private fun LessonList(
    context: Context,
    lessonSpots: List<List<Lesson>>,
    lessonPeriods: List<LessonPeriod>,
    dailySchedule: DailySchedule?,
    colors: ColorProviders
) {
    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {

        lessonSpots.forEachIndexed { index, lessonSpot ->

            item {
                val period = lessonPeriods.getOrNull(index)
                val change = dailySchedule?.changes?.getOrNull(index)

                LessonRow(
                    context = context,
                    index = index,
                    period = period,
                    lessonSpot = lessonSpot,
                    change = change,
                    colors = colors
                )
            }

            if (index < lessonSpots.lastIndex) {
                item {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.outline)
                    ) {}
                }
            }
        }
    }
}

@Composable
private fun LessonRow(
    context: Context,
    index: Int,
    period: LessonPeriod?,
    lessonSpot: List<Lesson>?,
    change: ChangeEntry?,
    colors: ColorProviders
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PeriodColumn(index = index, period = period, colors = colors)

        Box(modifier = GlanceModifier.defaultWeight()) {
            when {
                change != null -> {
                    SubstitutionCard(context, change, colors)
                }

                !lessonSpot.isNullOrEmpty() -> {
                    Column {
                        lessonSpot.forEach { lesson ->
                            LessonCard(lesson, colors)
                        }
                    }
                }

                else -> {
                    FreePeriodCard(context, colors)
                }
            }
        }
    }
}

@Composable
private fun PeriodColumn(
    index: Int,
    period: LessonPeriod?,
    colors: ColorProviders
) {
    Column(
        modifier = GlanceModifier.width(44.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${index + 1}",
            style = TextStyle(
                color = colors.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        )
        if (period != null) {
            val times = period.toString().split(" - ")
            val text = if (times.size == 2) times[0] else period.toString()
            Text(
                text = text,
                style = TextStyle(color = colors.onBackground, fontSize = 10.sp)
            )
            if (times.size == 2) {
                Text(
                    text = times[1],
                    style = TextStyle(color = colors.onBackground, fontSize = 10.sp)
                )
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
private fun LessonCard(lesson: Lesson, colors: ColorProviders) {
    val secondaryInfo = listOfNotNull(
        lesson.teacherName?.short ?: lesson.teacherName?.full,
        lesson.group,
        lesson.clazz
    ).joinToString(" • ")

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(10.dp)
            .background(colors.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = lesson.subjectName.full,
                style = TextStyle(
                    color = colors.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                maxLines = 1
            )
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
private fun FreePeriodCard(context: Context, colors: ColorProviders) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(10.dp)
            .background(colors.surface)
            .padding(12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = context.getStringRes(R.string.widget_timetable_free_period),
            style = TextStyle(color = colors.onSurface, fontSize = 13.sp)
        )
    }
}

@Composable
private fun LastUpdatedInfo(context: Context, lastUpdated: Long, colors: ColorProviders) {
    val updatedDateTime = Instant.fromEpochMilliseconds(lastUpdated)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val month = updatedDateTime.date.month
    val dateStr = "${updatedDateTime.date.day}.$month " +
            "${updatedDateTime.hour}:${updatedDateTime.minute.toString().padStart(2, '0')}"

    Text(
        text = context.getStringRes(R.string.widget_timetable_last_updated, dateStr),
        style = TextStyle(color = colors.onBackground, fontSize = 10.sp),
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    )
}


@Composable
private fun LoadingContent(context: Context, colors: ColorProviders) {
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = colors.onBackground
        )
    }
}

@Composable
private fun ErrorContent(context: Context, colors: ColorProviders) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionRunCallback<RefreshTimetableAction>())
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = context.getStringRes(R.string.widget_timetable_error),
                style = TextStyle(color = colors.error, fontWeight = FontWeight.Bold)
            )
            Text(
                text = context.getStringRes(R.string.widget_timetable_tap_to_refresh),
                style = TextStyle(color = colors.onBackground, fontSize = 12.sp)
            )
        }
    }
}

@Composable
private fun EmptyContent(context: Context, colors: ColorProviders) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionRunCallback<RefreshTimetableAction>()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = context.getStringRes(R.string.widget_timetable_loading_timetable),
            style = TextStyle(color = colors.onBackground)
        )
    }
}

internal class TimetableWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimetableWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<TimetableWidgetWorker>().build())
    }
}

internal class RefreshTimetableAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, TimetableWidgetStateDefinition, glanceId) { state ->
            state.copy(isLoading = true, isManualRefresh = true)
        }
        TimetableWidget().update(context, glanceId)
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<TimetableWidgetWorker>().build())
    }
}
