package me.tomasan7.jecnamobile.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.gradenotifications.GradeCheckerWorker
import me.tomasan7.jecnamobile.substitutions.notifications.SubstitutionCheckerWorker
import me.tomasan7.jecnamobile.util.settingsAsStateAwaitFirst
import me.tomasan7.jecnamobile.util.settingsDataStore

private enum class NotificationTarget {
    GRADE,
    SUBSTITUTION
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsNotificationsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settings by settingsAsStateAwaitFirst()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        permissionState.status.isGranted
    } else {
        true
    }

    val gradeNotificationsEnabled = (settings.notificationsEnabled ?: settings.hasSeenWelcomeScreen) && hasPermission
    val substitutionNotificationsEnabled = settings.substitutionNotificationsEnabled && hasPermission

    var pendingNotificationTarget by remember { mutableStateOf<NotificationTarget?>(null) }

    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS) { isGranted ->
            if (isGranted) {
                coroutineScope.launch {
                    when (pendingNotificationTarget) {
                        NotificationTarget.GRADE -> {
                            context.settingsDataStore.updateData { it.copy(notificationsEnabled = true) }
                            GradeCheckerWorker.scheduleWorkerIfNotificationsEnabled(context)
                        }
                        NotificationTarget.SUBSTITUTION -> {
                            context.settingsDataStore.updateData { it.copy(substitutionNotificationsEnabled = true) }
                            SubstitutionCheckerWorker.scheduleWorkerIfNotificationsEnabled(context)
                        }
                        null -> {
                            GradeCheckerWorker.scheduleWorkerIfNotificationsEnabled(context)
                            SubstitutionCheckerWorker.scheduleWorkerIfNotificationsEnabled(context)
                        }
                    }
                    pendingNotificationTarget = null
                }
            } else {
                pendingNotificationTarget = null
            }
        }
    } else null

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings_category_notifications)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            SettingsSection(title = stringResource(R.string.settings_notifications_title)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_notifications_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (gradeNotificationsEnabled) {
                                stringResource(R.string.settings_notifications_enabled)
                            } else {
                                stringResource(R.string.settings_notifications_disabled)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (gradeNotificationsEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )

                        Switch(
                            checked = gradeNotificationsEnabled,
                            onCheckedChange = { enabled ->
                                coroutineScope.launch {
                                    if (enabled) {
                                        if (hasPermission) {
                                            context.settingsDataStore.updateData { it.copy(notificationsEnabled = true) }
                                            GradeCheckerWorker.scheduleWorkerIfNotificationsEnabled(context)
                                        } else {
                                            pendingNotificationTarget = NotificationTarget.GRADE
                                            notificationPermissionState?.launchPermissionRequest()
                                        }
                                    } else {
                                        context.settingsDataStore.updateData { it.copy(notificationsEnabled = false) }
                                        GradeCheckerWorker.scheduleWorkerIfNotificationsEnabled(context)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.settings_notifications_substitution_title)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_notifications_substitution_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (substitutionNotificationsEnabled) {
                                stringResource(R.string.settings_notifications_enabled)
                            } else {
                                stringResource(R.string.settings_notifications_disabled)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (substitutionNotificationsEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )

                        Switch(
                            checked = substitutionNotificationsEnabled,
                            onCheckedChange = { enabled ->
                                coroutineScope.launch {
                                    if (enabled) {
                                        if (hasPermission) {
                                            context.settingsDataStore.updateData { it.copy(substitutionNotificationsEnabled = true) }
                                            SubstitutionCheckerWorker.scheduleWorkerIfNotificationsEnabled(context)
                                        } else {
                                            pendingNotificationTarget = NotificationTarget.SUBSTITUTION
                                            notificationPermissionState?.launchPermissionRequest()
                                        }
                                    } else {
                                        context.settingsDataStore.updateData { it.copy(substitutionNotificationsEnabled = false) }
                                        SubstitutionCheckerWorker.scheduleWorkerIfNotificationsEnabled(context)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            SettingsSection(title = null) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (!hasPermission) {
                        Text(
                            text = stringResource(R.string.settings_notifications_permission_required),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Text(
                        text = stringResource(R.string.settings_notifications_system_settings_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_notifications_system_settings))
                    }
                }
            }
        }
    }
}
