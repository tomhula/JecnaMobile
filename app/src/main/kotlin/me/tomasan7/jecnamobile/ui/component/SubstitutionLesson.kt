package me.tomasan7.jecnamobile.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.ui.ElevationLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubstitutionLesson(
    text: String,
    modifier: Modifier = Modifier,
    onShowOriginal: () -> Unit,
)
{
    val dialogState = rememberObjectDialogState<String>()
    val shape = RoundedCornerShape(5.dp)

    Surface(
        modifier = modifier,
        tonalElevation = ElevationLevel.level2,
        shadowElevation = ElevationLevel.level1,
        shape = shape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        onClick = { dialogState.show(text) }
    ) {
        Box(Modifier.padding(4.dp), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 4
            )
        }
    }

    ObjectDialog(
        state = dialogState,
        onDismissRequest = { dialogState.hide() },
        content = { fullText ->
            DialogContainer(
                title = {
                    Box(Modifier.padding(8.dp)) {
                        Text(stringResource(R.string.substitution_dialog_title), fontWeight = FontWeight.Bold)
                    }
                },
                buttons = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = {
                            onShowOriginal()
                            dialogState.hide()
                        }) {
                            Text(stringResource(R.string.substitution_show_original))
                        }
                        TextButton(onClick = { dialogState.hide() }) {
                            Text(stringResource(R.string.close))
                        }
                    }
                }
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    DialogRow(
                        label = stringResource(R.string.substitution_dialog_text_label),
                        value = fullText,
                    )
                }
            }
        }
    )
}
