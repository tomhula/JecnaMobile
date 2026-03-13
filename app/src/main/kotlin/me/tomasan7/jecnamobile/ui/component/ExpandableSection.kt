package me.tomasan7.jecnamobile.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

sealed interface ExpandableSectionPadding 
{
    val start: Dp
    val top: Dp
    val end: Dp
    val bottom: Dp

    data object Default : ExpandableSectionPadding
    {
        override val start = 16.dp
        override val top = 0.dp
        override val end = 16.dp
        override val bottom = 16.dp
    }

    data object Text : ExpandableSectionPadding
    {
        override val start = 44.dp
        override val top = 0.dp
        override val end = 16.dp
        override val bottom = 8.dp
    }
    
    data class Custom(
        override val start: Dp,
        override val top: Dp,
        override val end: Dp,
        override val bottom: Dp
    ) : ExpandableSectionPadding
}

@Composable
fun ExpandableSection(
    title: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = {
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    },
    trailingHeaderContent: (@Composable RowScope.() -> Unit)? = null,
    initiallyExpanded: Boolean = false,
    expandable: Boolean = true,
    color: Color = MaterialTheme.colorScheme.surface,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    contentPadding: ExpandableSectionPadding = ExpandableSectionPadding.Default,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(!expandable || initiallyExpanded) }

    Surface(
        modifier = modifier,
        color = color,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = { expanded = !expanded },
                        role = Role.Button,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        enabled = expandable
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (icon != null) {
                        icon()
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )

                    if (trailingHeaderContent != null) {
                        trailingHeaderContent()
                    }
                }

                if (expandable)
                {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = titleColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = contentPadding.start,
                            top = contentPadding.top,
                            end = contentPadding.end,
                            bottom = contentPadding.bottom
                        ),
                    content = content
                )
            }
        }
    }
}

@Composable
fun ExpandableSection(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector,
    trailingHeaderContent: (@Composable RowScope.() -> Unit)? = null,
    initiallyExpanded: Boolean = false,
    expandable: Boolean = true,
    color: Color = MaterialTheme.colorScheme.surface,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    contentPadding: ExpandableSectionPadding = ExpandableSectionPadding.Default,
    content: @Composable ColumnScope.() -> Unit
) = ExpandableSection(
    title = title,
    modifier = modifier,
    icon = {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = iconColor
        )
    },
    trailingHeaderContent = trailingHeaderContent,
    initiallyExpanded = initiallyExpanded,
    expandable = expandable,
    color = color,
    titleColor = titleColor,
    contentPadding = contentPadding,
    content = content
)
