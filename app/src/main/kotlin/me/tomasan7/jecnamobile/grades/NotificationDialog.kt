package me.tomasan7.jecnamobile.grades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.tomhula.jecnaapi.data.notification.Notification
import io.github.tomhula.jecnaapi.data.schoolStaff.TeacherReference
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.ui.component.DialogContainer
import me.tomasan7.jecnamobile.ui.component.DialogRow

private val jecnaDateFormatter = LocalDate.Format {
    day(padding = Padding.NONE)
    char('.')
    monthNumber(padding = Padding.NONE)
    year()
}

@Composable
fun NotificationDialog(
    onDismissRequest: () -> Unit,
    onTeacherClick: (TeacherReference) -> Unit = {},
    notification: Notification
)
{
    Dialog(onDismissRequest = onDismissRequest) {
        DialogContainer(
            title = {
                Text(text = notification.exactType)
            },
            buttons = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(R.string.close))
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogRow(
                    label = stringResource(R.string.notification_dialog_date),
                    value = notification.date.format(jecnaDateFormatter)
                )
                notification.issuedBy?.let {
                    DialogRow(
                        label = stringResource(R.string.notification_dialog_issued_by),
                        value = it.fullName,
                        onClick = { onTeacherClick(TeacherReference(it.fullName, it.tag)) }
                    )
                }
                notification.caseNumber?.let {
                    DialogRow(
                        label = stringResource(R.string.notification_dialog_case_number),
                        value = it
                    )
                }
                DialogRow(
                    label = stringResource(R.string.notification_dialog_message),
                    value = notification.message
                )
            }
        }
    }
}
