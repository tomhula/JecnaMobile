package me.tomasan7.jecnamobile.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun DialogRow(
    label: String,
    value: String,
    onClick: () -> Unit = {}
)
{
    Surface(
        tonalElevation = 10.dp,
        shape = RoundedCornerShape(28.dp),
        onClick = onClick,
    ) {
        Column(Modifier.padding(top = 5.dp, start = 15.dp, end = 15.dp, bottom = 15.dp)) {
            Text(
                text = label,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, // TODO: change color
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = value,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
