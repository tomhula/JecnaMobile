package me.tomasan7.jecnamobile.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.timetable.Announcement
import java.time.format.DateTimeFormatter
import java.time.LocalDate as JavaLocalDate

@Composable
fun AnnouncementsSection(
    announcements: Map<String, List<Announcement>>,
    modifier: Modifier = Modifier,
    targetDate: LocalDate? = null
)
{
    val visibleAnnouncements = remember(targetDate, announcements) {
        if (targetDate != null)
            announcements[targetDate.toString()]
                ?.filter { it.textContent != null }
                .orEmpty()
        else
            announcements.values.flatten().filter { it.textContent != null }
    }

    if (visibleAnnouncements.isEmpty())
        return

    ExpandableSection(
        title = stringResource(R.string.announcements_title),
        modifier = modifier,
        icon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp)) },
        initiallyExpanded = true
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            visibleAnnouncements.forEach { announcement ->
                AnnouncementCard(announcement = announcement)
            }
        }
    }
}

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("d.M.yyyy")
private val DATE_ICON_SIZE = 16.dp
private const val ICON_ALPHA = 0.6f

@Composable
private fun AnnouncementCard(
    announcement: Announcement,
    modifier: Modifier = Modifier
)
{
    val dateText = remember(announcement.startDate, announcement.endDate) {
        val start = runCatching { JavaLocalDate.parse(announcement.startDate) }.getOrNull()
        val end = runCatching { JavaLocalDate.parse(announcement.endDate) }.getOrNull()
        if (start != null && end != null)
        {
            if (start != end)
                "${start.format(DATE_FORMATTER)} - ${end.format(DATE_FORMATTER)}"
            else
                start.format(DATE_FORMATTER)
        }
        else
            null
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(DATE_ICON_SIZE),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = ICON_ALPHA)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = announcement.author,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
                Spacer(Modifier.weight(1f))
                if (dateText != null)
                {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(DATE_ICON_SIZE),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = ICON_ALPHA)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = announcement.textContent!!,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}
