package me.tomasan7.jecnamobile.mainscreen

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.rememberNavBackStack
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.navigation.AppDestination
import me.tomasan7.jecnamobile.navigation.LocalNavDrawerHandle
import me.tomasan7.jecnamobile.navigation.MainNavDisplay
import me.tomasan7.jecnamobile.navigation.NavDrawerDestination
import me.tomasan7.jecnamobile.navigation.rememberNavDrawerHandle
import me.tomasan7.jecnamobile.util.settingsAsStateAwaitFirst

@Composable
fun MainScreen(
    onNavigateToLogin: () -> Unit,
    initialNavigateTo: AppDestination? = null,
    viewModel: MainScreenViewModel = hiltViewModel()
)
{
    val settings by settingsAsStateAwaitFirst()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val destinationItems = remember(settings.substitutionTimetableEnabled, settings.drawerPages) {
        val visiblePages = settings.drawerPages
            .filter { it.isVisible }
            .mapNotNull { page -> NavDrawerDestination.entries.find { it.name == page.destinationName } }
        
        if (settings.substitutionTimetableEnabled) {
            visiblePages
        } else {
            visiblePages.filter { it != NavDrawerDestination.Substitution }
        }
    }
    val linkItems = remember(settings.drawerLinks) {
        settings.drawerLinks
            .filter { it.isVisible }
            .mapNotNull { link -> SidebarLink.entries.find { it.name == link.linkName } }
    }
    
    val initialDestination = initialNavigateTo ?: settings.defaultDestination.destination
    val navBackStack: NavBackStack<AppDestination>  = rememberNavBackStack(initialDestination) as NavBackStack<AppDestination>

    val navDrawerHandle = rememberNavDrawerHandle(drawerState, scope)

    EventEffect(
        event = viewModel.navigateToLoginEvent,
        onConsumed = viewModel::onLoginEventConsumed
    ) {
        onNavigateToLogin()
    }
    
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(viewModel)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(viewModel)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(all = 28.dp)
                    )

                    destinationItems.forEach { item ->
                        /* Don't use instance equality check (===), the ::class is not a singleton and returns a different instance each time. */
                        val selected = item.destination::class == navBackStack.lastOrNull()?.let { it::class }
                        DestinationItem(
                            item = item,
                            selected = selected,
                            onClick = onClick@{
                                scope.launch { drawerState.close() }
                                if (selected)
                                    return@onClick

                                navBackStack.clear()
                                navBackStack.add(item.destination)
                            }
                        )
                    }

                    if (destinationItems.isNotEmpty() && linkItems.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 28.dp)
                        )
                    }

                    linkItems.forEach { LinkItem(it) }
                }

                Column(
                    modifier = Modifier
                        .padding(start = 28.dp, end = 28.dp, bottom = 16.dp),
                ) {
                    SidebarButtonsRow(
                        onProfileClick = {
                            scope.launch { drawerState.close() }
                            navBackStack.add(AppDestination.StudentProfile)
                        },
                        onSettingsClick = {
                            scope.launch { drawerState.close() }
                            navBackStack.add(AppDestination.Settings.Main)
                        },
                        onLogoutClick = {
                            viewModel.logout()
                            onNavigateToLogin()
                        }
                    )
                }
            }
        }
    ) {
        CompositionLocalProvider(LocalNavDrawerHandle provides navDrawerHandle) {
            MainNavDisplay(
                navBackStack = navBackStack,
                onBack = { navBackStack.pop() }
            )
        }
    }
}

private fun <T> MutableList<T>.pop() = if (size > 1) removeAt(lastIndex) else Unit

@Composable
private fun SidebarButtonsRow(
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
)
{
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        IconButton(onClick = onProfileClick) {
            Icon(Icons.Outlined.AccountCircle, contentDescription = null)
        }

        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Outlined.Settings, contentDescription = null)
        }

        IconButton(onClick = onLogoutClick) {
            Icon(Icons.AutoMirrored.Outlined.ExitToApp, contentDescription = null)
        }
    }
}

@Composable
fun DestinationItem(
    item: NavDrawerDestination,
    selected: Boolean,
    onClick: () -> Unit
)
{
    NavigationDrawerItem(
        icon = { Icon(if (selected) item.iconSelected else item.icon, contentDescription = null) },
        label = { Text(stringResource(item.label)) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

@Composable
fun LinkItem(item: SidebarLink)
{
    val context = LocalContext.current
    val label = stringResource(item.label)

    NavigationDrawerItem(
        icon = { Icon(item.icon, contentDescription = null) },
        label = { Text(label) },
        selected = false,
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = item.link.toUri()
            context.startActivity(intent)
        },
        badge = { Icon(Icons.Outlined.OpenInBrowser, label) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}
