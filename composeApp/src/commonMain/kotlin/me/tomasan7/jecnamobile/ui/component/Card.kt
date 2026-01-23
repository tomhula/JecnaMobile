package me.tomasan7.jecnamobile.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.tomasan7.jecnamobile.ui.ElevationLevel

@Composable
fun Card(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    tonalElevation: Dp = ElevationLevel.level1,
    shadowElevation: Dp = ElevationLevel.level1,
    content: @Composable () -> Unit
)
{
    Surface(
        modifier = modifier,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp)
        ) {
            title()

            Spacer(Modifier.height(15.dp))

            content()
        }
    }
}
