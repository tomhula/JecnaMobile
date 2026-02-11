package me.tomasan7.jecnamobile.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cz.jzitnik.jecna_supl_client.ReportLocation

import androidx.compose.ui.res.stringResource
import me.tomasan7.jecnamobile.R

@Composable
fun ReportDialog(
    isLoading: Boolean,
    onDismissRequest: () -> Unit,
    onReport: (String, ReportLocation) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var location by remember { mutableStateOf(ReportLocation.TIMETABLE) }

    Dialog(onDismissRequest = onDismissRequest) {
        DialogContainer(
            title = { Text(stringResource(R.string.report_title)) },
            buttons = {
                TextButton(
                    onClick = onDismissRequest,
                    enabled = !isLoading
                ) {
                    Text(stringResource(R.string.report_cancel))
                }
                TextButton(
                    onClick = { onReport(content, location) },
                    enabled = content.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.report_submit))
                    }
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.report_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.report_class_name_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(R.string.report_content_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    enabled = !isLoading
                )

                Spacer(Modifier.height(16.dp))

                Text(stringResource(R.string.report_type_label), style = MaterialTheme.typography.labelLarge)

                ReportLocation.entries.forEach { loc ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { location = loc }
                    ) {
                        RadioButton(
                            selected = (location == loc),
                            onClick = { location = loc }
                        )
                        Text(
                            text = when (loc) {
                                ReportLocation.TIMETABLE -> stringResource(R.string.report_type_timetable)
                                ReportLocation.TEACHER_ABSENCE -> stringResource(R.string.report_type_teacher_absence)
                                ReportLocation.TAKES_PLACE -> stringResource(R.string.report_type_takes_place)
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
