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
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.tomhula.jecnaapi.data.grade.Subject
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.widgets.base.BaseWidgetStateDefinition
import me.tomasan7.jecnamobile.widgets.base.BaseWidgetStateSerializer
import me.tomasan7.jecnamobile.widgets.base.EmptyContent
import me.tomasan7.jecnamobile.widgets.base.ErrorContent
import me.tomasan7.jecnamobile.widgets.base.LoadingContent
import me.tomasan7.jecnamobile.widgets.base.WidgetHeader
import me.tomasan7.jecnamobile.widgets.grades.GradesWidgetWorkerShared
import java.text.DecimalFormat
import java.math.RoundingMode

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
            state.error != null -> ErrorContent(
                context = context,
                colors = colors,
                refreshActionClass = RefreshGradesAction::class.java,
                errorResId = R.string.widget_grades_error,
                tapToRefreshResId = R.string.widget_grades_tap_to_refresh
            )
            else -> EmptyContent(
                context = context,
                colors = colors,
                refreshActionClass = RefreshGradesAction::class.java,
                emptyResId = R.string.widget_grades_loading
            )
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
    WidgetHeader(
        context = context,
        title = title,
        lastUpdated = lastUpdated,
        isLoading = isLoading,
        colors = colors,
        refreshActionClass = RefreshGradesAction::class.java,
        stringResId = R.string.widget_grades_last_updated
    )
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

internal class GradesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GradesWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        GradesWidgetWorkerShared.schedule(context)
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
        GradesWidgetWorkerShared.schedule(context)
    }
}
