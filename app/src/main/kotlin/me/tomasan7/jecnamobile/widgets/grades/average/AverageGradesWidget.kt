package me.tomasan7.jecnamobile.widgets.grades.average

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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.tomhula.jecnaapi.data.grade.Subject
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.widgets.base.BaseWidgetStateDefinition
import me.tomasan7.jecnamobile.widgets.base.BaseWidgetStateSerializer
import me.tomasan7.jecnamobile.widgets.grades.GradesWidgetWorkerShared
import java.text.DecimalFormat
import java.math.RoundingMode
import kotlin.time.Instant

private const val LOG_TAG = "GradesWidget"

private fun Context.getStringRes(@StringRes resId: Int, vararg formatArgs: Any): String = getString(resId, *formatArgs)

object GradesWidgetStateSerializer : BaseWidgetStateSerializer<AverageGradesWidgetState>(
    kSerializer = AverageGradesWidgetState.serializer(),
    logTag = LOG_TAG
) {
    override val defaultValue = AverageGradesWidgetState()
}

object GradesWidgetStateDefinition : BaseWidgetStateDefinition<AverageGradesWidgetState>(
    filePrefix = "grades_widget_",
    serializer = GradesWidgetStateSerializer
)

internal class GradesWidget : GlanceAppWidget() {
    override val stateDefinition = GradesWidgetStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val state = currentState<AverageGradesWidgetState>()
            GlanceTheme { GradesWidgetContent(context = context, state = state) }
        }
    }
}

@Composable
private fun GradesWidgetContent(context: Context, state: AverageGradesWidgetState) {
    val colors = GlanceTheme.colors

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.background)
            .padding(12.dp)
    ) {
        when {
            state.gradesPage != null -> GradesContent(
                context = context,
                state = state,
                colors = colors
            )
            state.isLoading -> LoadingContent(colors)
            state.error != null -> ErrorContent(context, colors)
            else -> EmptyContent(context, colors)
        }
    }
}

@Composable
private fun GradesContent(
    context: Context,
    state: AverageGradesWidgetState,
    colors: ColorProviders
) {
    val gradesPage = state.gradesPage!!
    val subjects = gradesPage.subjects

    Column(modifier = GlanceModifier.fillMaxSize()) {
        Header(
            context = context,
            title = context.getStringRes(R.string.widget_grades_title),
            lastUpdated = state.lastUpdated,
            isLoading = state.isLoading,
            colors = colors
        )

        val subjectsWithGrades = subjects
            .filter { it.grades.isNotEmpty() }
            .sortedBy { it.name.full }
            .take(6)

        if (subjectsWithGrades.isEmpty()) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = context.getStringRes(R.string.widget_grades_no_grades),
                    style = TextStyle(color = colors.onBackground)
                )
            }
        } else {
            SubjectAveragesList(
                context = context,
                subjects = subjectsWithGrades,
                colors = colors
            )
        }
    }
}

@Composable
private fun Header(
    context: Context,
    title: String,
    lastUpdated: Long,
    isLoading: Boolean,
    colors: ColorProviders
) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
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
                    fontSize = 16.sp
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())

            if (isLoading) {
                Box(
                    modifier = GlanceModifier.size(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = GlanceModifier.size(18.dp),
                        color = colors.onBackground
                    )
                }
            } else {
                RefreshButton(context = context, colors = colors)
            }
        }

        Text(
            text = formatLastUpdated(context, lastUpdated),
            style = TextStyle(
                color = colors.onBackground,
                fontSize = 11.sp
            ),
            modifier = GlanceModifier.padding(top = 2.dp)
        )
    }
}

private fun formatLastUpdated(context: Context, timestamp: Long): String {
    if (timestamp == 0L) return ""
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val timeStr = "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
    return context.getString(R.string.widget_grades_last_updated, timeStr)
}

@Composable
private fun SubjectAveragesList(
    context: Context,
    subjects: List<Subject>,
    colors: ColorProviders
) {
    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
        subjects.forEachIndexed { index, subject ->
            item {
                SubjectAverageRow(
                    context = context,
                    subject = subject,
                    colors = colors
                )
            }

            if (index < subjects.lastIndex) {
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
private fun SubjectAverageRow(
    context: Context,
    subject: Subject,
    colors: ColorProviders
) {
    val average = subject.grades.average()
    val avgString = if (average != null) {
        DecimalFormat("#.##").apply {
            roundingMode = RoundingMode.HALF_UP
        }.format(average)
    } else {
        "-"
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = subject.name.full,
            style = TextStyle(
                color = colors.onBackground,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )

        Box(
            modifier = GlanceModifier
                .cornerRadius(8.dp)
                .background(colors.secondaryContainer)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = avgString,
                style = TextStyle(
                    color = colors.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
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
            .clickable(actionRunCallback<RefreshGradesAction>()),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_refresh),
            contentDescription = context.getStringRes(R.string.widget_grades_refresh_description),
            modifier = GlanceModifier.size(18.dp),
            colorFilter = ColorFilter.tint(colors.onBackground)
        )
    }
}

@Composable
private fun LoadingContent(colors: ColorProviders) {
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
            .clickable(actionRunCallback<RefreshGradesAction>())
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = context.getStringRes(R.string.widget_grades_error),
                style = TextStyle(color = colors.error, fontWeight = FontWeight.Bold)
            )
            Text(
                text = context.getStringRes(R.string.widget_grades_tap_to_refresh),
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
            .clickable(actionRunCallback<RefreshGradesAction>()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = context.getStringRes(R.string.widget_grades_loading),
            style = TextStyle(color = colors.onBackground)
        )
    }
}

internal class GradesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GradesWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        GradesWidgetWorkerShared.Companion.schedule(context)
    }
}

internal class RefreshGradesAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, GradesWidgetStateDefinition, glanceId) { state ->
            state.copy(isLoading = true, isManualRefresh = true)
        }
        GradesWidget().update(context, glanceId)
        GradesWidgetWorkerShared.Companion.schedule(context)
    }
}
