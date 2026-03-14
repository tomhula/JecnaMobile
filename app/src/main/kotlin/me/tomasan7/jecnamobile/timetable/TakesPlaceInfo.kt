package me.tomasan7.jecnamobile.timetable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.tomasan7.jecnamobile.ui.component.ExpandableSection
import me.tomasan7.jecnamobile.ui.component.ExpandableSectionPadding

@Composable
fun TakesPlaceInfo(label: String, text: String, expandable: Boolean = true) {
    ExpandableSection(
        title = label,
        modifier = Modifier.fillMaxWidth(),
        icon = Icons.Default.Info,
        expandable = expandable,
        color = MaterialTheme.colorScheme.secondaryContainer,
        titleColor = MaterialTheme.colorScheme.onSecondaryContainer,
        iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        contentPadding = ExpandableSectionPadding.Text
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
