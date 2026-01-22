package me.tomasan7.jecnamobile.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun <T> RadioGroup(
    options: List<T>,
    selectedOption: T,
    onSelectionChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionStringMap: (T) -> String = { it.toString() }
)
{
    /* Copied from https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#RadioButton(kotlin.Boolean,kotlin.Function0,androidx.compose.ui.Modifier,kotlin.Boolean,androidx.compose.material3.RadioButtonColors,androidx.compose.foundation.interaction.MutableInteractionSource) */
    Column(modifier.selectableGroup()) {
        options.forEach { option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(5.dp))
                    .height(56.dp)
                    .selectable(
                        selected = option == selectedOption,
                        onClick = { onSelectionChange(option) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = option == selectedOption,
                    onClick = null
                )
                Text(
                    text = optionStringMap(option),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}
