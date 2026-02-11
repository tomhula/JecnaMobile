package me.tomasan7.jecnamobile.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(modifier.height(IntrinsicSize.Min)) {
        Surface(
            tonalElevation = 20.dp,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxHeight()
                .width(150.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.CenterStart
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(5.dp))

        Surface(
            tonalElevation = 4.dp,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.CenterStart
            ) {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(
                        text = value,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    @androidx.annotation.StringRes label: Int,
    value: String,
    modifier: Modifier = Modifier
) = InfoRow(stringResource(label), value, modifier)
