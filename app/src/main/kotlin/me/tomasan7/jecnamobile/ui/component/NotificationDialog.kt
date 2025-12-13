package me.tomasan7.jecnamobile.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.data.notification.Notification
import io.github.tomhula.jecnaapi.data.notification.NotificationReference
import io.github.tomhula.jecnaapi.data.schoolStaff.TeacherReference
import me.tomasan7.jecnamobile.R
import java.time.format.DateTimeFormatter

@Composable
fun NotificationDialog(
    onDismissRequest: () -> Unit,
    onTeacherClick: (TeacherReference) -> Unit = {},
    notificationReference: NotificationReference,
    jecnaClient: JecnaClient
)
{
    val jecnaDateFormatter = DateTimeFormatter.ofPattern("d.M.yyyy")
    val notificationState = produceState<Notification?>(initialValue = null, notificationReference) {
        value = jecnaClient.getNotification(notificationReference)
    }

    Dialog(onDismissRequest = onDismissRequest) {
        DialogContainer(
            title = { Text(text = notificationState.value?.exactType ?: stringResource(R.string.notification_dialog_title)) },
            buttons = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(R.string.close))
                }
            }
        ) {
            val notification = notificationState.value

            if (notification == null)
            {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            else
            {
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
}