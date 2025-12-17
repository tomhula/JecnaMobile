package me.tomasan7.jecnamobile.student.locker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.tomhula.jecnaapi.data.student.Locker
import me.tomasan7.jecnamobile.R

@Composable
fun LockerDialog(
    onDismiss: () -> Unit,
    viewModel: LockerDialogViewModel = hiltViewModel()
) {
    val locker = viewModel.locker
    val loading = viewModel.loading
    val error = viewModel.error

    DisposableEffect(Unit) {
        viewModel.loadLocker()
        onDispose { }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.locker_title))
        },
        text = {
            when {
                loading -> {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Text(text = error)
                }
                locker != null -> {
                    LockerContent(locker)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.close))
            }
        }
    )
}

@Composable
private fun LockerContent(locker: Locker) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        LockerField(label = stringResource(R.string.locker_number), value = locker.number)
        LockerField(label = stringResource(R.string.locker_description), value = locker.description)
        LockerField(label = stringResource(R.string.locker_assigned_from), value = locker.assignedFrom.toString())
        LockerField(label = stringResource(R.string.locker_assigned_until), value = locker.assignedUntil.toString())
    }
}

@Composable
private fun LockerField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

