package me.tomasan7.jecnamobile.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.tomasan7.jecnamobile.ui.ElevationLevel

/**
 * Material 3 compliant dialog container.
 */
@Composable
fun DialogContainer(
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    buttons: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
)
{
    Surface(
        modifier = modifier,
        tonalElevation = ElevationLevel.level3,
        shadowElevation = ElevationLevel.level3,
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            if (title != null)
            {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.titleLarge,
                    content = title
                )

                VerticalSpacer(16.dp)
            }

            content()

            if (buttons != null)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .fillMaxWidth(),
                    content = buttons
                )
        }
    }
}
