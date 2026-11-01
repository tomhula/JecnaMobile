package me.tomasan7.jecnamobile.widgets.base

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.color.ColorProviders
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.tomasan7.jecnamobile.R

@Composable
fun WidgetHeader(
    context: Context,
    title: String,
    lastUpdated: Long,
    isLoading: Boolean,
    colors: ColorProviders,
    refreshActionClass: Class<out ActionCallback>,
    stringResId: Int = R.string.widget_timetable_last_updated,
    titleSize: Int = 16,
    refreshButtonSize: Int = 28,
    progressIndicatorSize: Int = 18,
    bottomPadding: Int = 8
) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = bottomPadding.dp)
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
                    fontSize = titleSize.sp
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())

            if (isLoading) {
                Box(
                    modifier = GlanceModifier.size(refreshButtonSize.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = GlanceModifier.size(progressIndicatorSize.dp),
                        color = colors.onBackground
                    )
                }
            } else {
                RefreshButton(
                    context = context,
                    colors = colors,
                    actionClass = refreshActionClass,
                    size = refreshButtonSize
                )
            }
        }

        if (lastUpdated != 0L) {
            Text(
                text = formatLastUpdated(context, lastUpdated, stringResId),
                style = TextStyle(
                    color = colors.onBackground,
                    fontSize = 11.sp
                ),
                modifier = GlanceModifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun SimpleWidgetHeader(
    context: Context,
    title: String,
    isLoading: Boolean,
    colors: ColorProviders,
    refreshActionClass: Class<out ActionCallback>,
    titleSize: Int = 14,
    refreshButtonSize: Int = 28,
    progressIndicatorSize: Int = 16,
    bottomPadding: Int = 4
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = bottomPadding.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = TextStyle(
                color = colors.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = titleSize.sp
            )
        )
        Spacer(modifier = GlanceModifier.defaultWeight())

        if (isLoading) {
            Box(
                modifier = GlanceModifier.size(refreshButtonSize.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = GlanceModifier.size(progressIndicatorSize.dp),
                    color = colors.onBackground
                )
            }
        } else {
            RefreshButton(
                context = context,
                colors = colors,
                actionClass = refreshActionClass,
                size = refreshButtonSize,
                iconSize = progressIndicatorSize
            )
        }
    }
}

@Composable
fun RefreshButton(
    context: Context,
    colors: ColorProviders,
    actionClass: Class<out ActionCallback>,
    size: Int = 28,
    iconSize: Int = 18,
    descriptionResId: Int = R.string.widget_timetable_refresh_description
) {
    Box(
        modifier = GlanceModifier
            .size(size.dp)
            .cornerRadius(6.dp)
            .clickable(actionRunCallback(actionClass)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_refresh),
            contentDescription = context.getString(descriptionResId),
            modifier = GlanceModifier.size(iconSize.dp),
            colorFilter = ColorFilter.tint(colors.onBackground)
        )
    }
}

private fun formatLastUpdated(context: Context, timestamp: Long, stringResId: Int): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val timeStr = "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
    return context.getString(stringResId, timeStr)
}

@Composable
fun LoadingContent(colors: ColorProviders) {
    Box(modifier = GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = colors.onBackground
        )
    }
}

@Composable
fun ErrorContent(
    context: Context,
    colors: ColorProviders,
    refreshActionClass: Class<out ActionCallback>,
    errorResId: Int = R.string.widget_timetable_error,
    tapToRefreshResId: Int = R.string.widget_timetable_tap_to_refresh
) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionRunCallback(refreshActionClass))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = context.getString(errorResId),
                style = TextStyle(color = colors.error, fontWeight = FontWeight.Bold)
            )
            Text(
                text = context.getString(tapToRefreshResId),
                style = TextStyle(color = colors.onBackground, fontSize = 12.sp)
            )
        }
    }
}

@Composable
fun EmptyContent(
    context: Context,
    colors: ColorProviders,
    refreshActionClass: Class<out ActionCallback>,
    emptyResId: Int = R.string.widget_timetable_loading_timetable
) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionRunCallback(refreshActionClass)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = context.getString(emptyResId),
            style = TextStyle(color = colors.onBackground)
        )
    }
}
