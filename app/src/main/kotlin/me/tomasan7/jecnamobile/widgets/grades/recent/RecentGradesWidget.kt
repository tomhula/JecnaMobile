package me.tomasan7.jecnamobile.widgets.grades.recent

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
import androidx.glance.appwidget.lazy.items
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
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.tomhula.jecnaapi.data.grade.Grade
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.widgets.base.BaseWidgetStateDefinition
import me.tomasan7.jecnamobile.widgets.base.BaseWidgetStateSerializer
import me.tomasan7.jecnamobile.widgets.base.EmptyContent
import me.tomasan7.jecnamobile.widgets.base.ErrorContent
import me.tomasan7.jecnamobile.widgets.base.LoadingContent
import me.tomasan7.jecnamobile.widgets.base.RefreshButton
import me.tomasan7.jecnamobile.widgets.base.WidgetHeader
import me.tomasan7.jecnamobile.widgets.grades.GradesWidgetWorkerShared

private const val LOG_TAG = "RecentGradesWidget"

private fun Context.getStringRes(@StringRes resId: Int, vararg formatArgs: Any): String = getString(resId, *formatArgs)

object RecentGradesWidgetStateSerializer : BaseWidgetStateSerializer<RecentGradesWidgetState>(
    kSerializer = RecentGradesWidgetState.serializer(),
    logTag = LOG_TAG
) {
    override val defaultValue = RecentGradesWidgetState()
}

object RecentGradesWidgetStateDefinition : BaseWidgetStateDefinition<RecentGradesWidgetState>(
    filePrefix = "recent_grades_widget_",
    serializer = RecentGradesWidgetStateSerializer
)

internal class RecentGradesWidget : GlanceAppWidget() {
    override val stateDefinition = RecentGradesWidgetStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val state = currentState<RecentGradesWidgetState>()
            GlanceTheme { RecentGradesWidgetContent(context = context, state = state) }
        }
    }
}

@Composable
private fun RecentGradesWidgetContent(context: Context, state: RecentGradesWidgetState) {
    val colors = GlanceTheme.colors

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.background)
            .padding(12.dp)
    ) {
        when {
            state.gradesPage != null -> RecentGradesContent(
                context = context,
                state = state,
                colors = colors
            )
            state.isLoading -> LoadingContent(colors)
            state.error != null -> ErrorContent(
                context = context,
                colors = colors,
                refreshActionClass = RefreshRecentGradesAction::class.java,
                errorResId = R.string.widget_grades_error,
                tapToRefreshResId = R.string.widget_grades_tap_to_refresh
            )
            else -> EmptyContent(
                context = context,
                colors = colors,
                refreshActionClass = RefreshRecentGradesAction::class.java,
                emptyResId = R.string.widget_grades_loading
            )
        }
    }
}

@Composable
private fun RecentGradesContent(
    context: Context,
    state: RecentGradesWidgetState,
    colors: ColorProviders
) {
    val gradesPage = state.gradesPage!!
    val subjects = gradesPage.subjects

    Column(modifier = GlanceModifier.fillMaxSize()) {
        Header(
            context = context,
            title = context.getStringRes(R.string.widget_recent_grades_title),
            lastUpdated = state.lastUpdated,
            isLoading = state.isLoading,
            colors = colors
        )

        val allGrades = subjects.flatMap { subject ->
            subject.grades.subjectParts.flatMap { subject.grades[it]!! }
                .map { grade -> grade to subject.name.full }
        }.sortedByDescending { it.first.receiveDate }.take(10)

        if (allGrades.isEmpty()) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = context.getStringRes(R.string.widget_grades_no_grades),
                    style = TextStyle(color = colors.onBackground)
                )
            }
        } else {
            RecentGradesList(
                context = context,
                grades = allGrades,
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
        refreshActionClass = RefreshRecentGradesAction::class.java,
        stringResId = R.string.widget_grades_last_updated
    )
}

@Composable
private fun RecentGradesList(
    context: Context,
    grades: List<Pair<Grade, String>>,
    colors: ColorProviders
) {
    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
        items(grades) { (grade, subjectName) ->
            RecentGradeRow(
                context = context,
                grade = grade,
                subjectName = subjectName,
                colors = colors
            )
        }
    }
}

@Composable
private fun RecentGradeRow(
    context: Context,
    grade: Grade,
    subjectName: String,
    colors: ColorProviders
) {
    val gradeColor = colors.primaryContainer

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .cornerRadius(6.dp)
                .background(gradeColor)
                .size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = grade.valueChar.toString(),
                style = TextStyle(
                    color = colors.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
        }

        Spacer(modifier = GlanceModifier.width(10.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = subjectName,
                style = TextStyle(
                    color = colors.onBackground,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                ),
                maxLines = 1
            )
            val description = grade.description
            if (description != null) {
                Text(
                    text = description,
                    style = TextStyle(
                        color = colors.onBackground,
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )
            }
        }

        grade.receiveDate?.let { date ->
            Text(
                text = formatDate(date),
                style = TextStyle(
                    color = colors.onBackground,
                    fontSize = 11.sp
                )
            )
        }
    }
}

private fun formatDate(date: LocalDate): String {
    return "${date.day}.${date.month.number}."
}

internal class RecentGradesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RecentGradesWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        GradesWidgetWorkerShared.Companion.schedule(context)
    }
}

internal class RefreshRecentGradesAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, RecentGradesWidgetStateDefinition, glanceId) { state ->
            state.copy(isLoading = true, isManualRefresh = true)
        }
        RecentGradesWidget().update(context, glanceId)
        GradesWidgetWorkerShared.Companion.schedule(context)
    }
}
