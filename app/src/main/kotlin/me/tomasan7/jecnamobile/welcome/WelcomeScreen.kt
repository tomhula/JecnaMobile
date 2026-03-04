package me.tomasan7.jecnamobile.welcome

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.gradenotifications.GradeCheckerWorker
import me.tomasan7.jecnamobile.util.settingsDataStore

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WelcomeScreen(
    onWelcomeComplete: () -> Unit
)
{
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    fun markWelcomeSeenAndContinue()
    {
        coroutineScope.launch {
            context.settingsDataStore.updateData { it.copy(hasSeenWelcomeScreen = true) }
            onWelcomeComplete()
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
    {
        val notificationPermissionState = rememberPermissionState(
            Manifest.permission.POST_NOTIFICATIONS
        ) { isGranted ->
            if (isGranted)
            {
                GradeCheckerWorker.scheduleWorkerIfNotificationsEnabled(context)
            }
            markWelcomeSeenAndContinue()
        }

        WelcomeContent(
            onRequestPermission = {
                if (!notificationPermissionState.status.isGranted)
                {
                    notificationPermissionState.launchPermissionRequest()
                }
                else
                {
                    markWelcomeSeenAndContinue()
                }
            },
            onSkip = {
                markWelcomeSeenAndContinue()
            }
        )
    }
    else
    {
        // For older Android versions, permission is granted at install time.
        WelcomeContent(
            onRequestPermission = {
                GradeCheckerWorker.scheduleWorkerIfNotificationsEnabled(context)
                markWelcomeSeenAndContinue()
            },
            onSkip = {
                markWelcomeSeenAndContinue()
            }
        )
    }
}

@Composable
private fun WelcomeContent(
    onRequestPermission: () -> Unit,
    onSkip: () -> Unit
)
{
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.NotificationsActive,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.welcome_notifications_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.welcome_notifications_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text(
                text = stringResource(R.string.welcome_notifications_button_turn_on),
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text(
                text = stringResource(R.string.welcome_notifications_button_skip),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
