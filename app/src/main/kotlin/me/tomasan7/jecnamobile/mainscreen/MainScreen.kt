package me.tomasan7.jecnamobile.mainscreen

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavArgs
import androidx.navigation.NavArgument
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.navigation.navGraph
import com.ramcosta.composedestinations.utils.findDestination
import com.ramcosta.composedestinations.utils.toDestinationsNavigator
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import me.tomasan7.jecnamobile.NavGraphs
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.destinations.LoginScreenDestination
import me.tomasan7.jecnamobile.destinations.MainScreenDestination
import me.tomasan7.jecnamobile.destinations.SettingsScreenDestination
import me.tomasan7.jecnamobile.navgraphs.SubScreensGraph
import me.tomasan7.jecnamobile.util.rememberMutableStateOf
import me.tomasan7.jecnamobile.util.settingsAsStateAwaitFirst

@Destination<RootGraph>
@Composable
fun MainScreen(
    navigator: DestinationsNavigator,
    viewModel: MainScreenViewModel = hiltViewModel()
)
{
    RequestNotificationsPermission()

    val settings by settingsAsStateAwaitFirst()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val destinationItems = SubScreenDestination.entries
    val linkItems = SidebarLink.entries
    var selectedItem by rememberMutableStateOf(SubScreenDestination.Timetable)
    val subScreensNavController = rememberNavController()
    val subScreensNavigator = remember(subScreensNavController) { subScreensNavController.toDestinationsNavigator() }
    val startRoute = remember { NavGraphs.subScreens.findDestination(settings.openSubScreenRoute)!! }
    // Workaround, because DestinationsNavHost does not accept a DestinationSpec as start
    val startDirection = remember(startRoute) {
        object : com.ramcosta.composedestinations.spec.Direction {
            override val route: String = startRoute.route
        }
    }
    val navDrawerController = rememberNavDrawerController(drawerState, scope)

    LaunchedEffect(subScreensNavController) {
        subScreensNavController.addOnDestinationChangedListener { _, destination, _ ->
            val newSelectedItem = destinationItems.find { it.destination.route == destination.route }
            if (newSelectedItem != null)
                selectedItem = newSelectedItem
        }
    }

    EventEffect(
        event = viewModel.navigateToLoginEvent,
        onConsumed = viewModel::onLoginEventConsumed
    ) {
        navigator.navigate(LoginScreenDestination) {
            popUpTo(LoginScreenDestination)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(all = 28.dp)
                )

                destinationItems.forEach { item ->
                    val selected = item === selectedItem
                    DestinationItem(
                        item = item,
                        selected = selected,
                        onClick = onClick@{
                            scope.launch { drawerState.close() }
                            if (selected)
                                return@onClick

                            /* https://developer.android.com/jetpack/compose/navigation#bottom-nav */
                            subScreensNavigator.navigate(item.destination) {
                                popUpTo(startRoute) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true

                            }
                            selectedItem = item
                        }
                    )
                }

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 28.dp)
                )

                linkItems.forEach { LinkItem(it) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 28.dp, end = 28.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    SidebarButtonsRow(
                        onSettingsClick = {
                            scope.launch { drawerState.close() }
                            subScreensNavigator.navigate(SettingsScreenDestination)
                        },
                        onLogoutClick = {
                            viewModel.logout()
                            navigator.navigate(LoginScreenDestination) {
                                popUpTo(MainScreenDestination) {
                                    inclusive = true
                                }
                            }
                        }
                    )
                }
            }
        },
        content = {
            DestinationsNavHost(
                navGraph = SubScreensGraph,
                start = startDirection,
                navController = subScreensNavController,
                modifier = Modifier.fillMaxSize(),
                dependenciesContainerBuilder = {
                    navGraph(SubScreensGraph) {
                        dependency(navDrawerController)
                    }
                }
            )
        }
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RequestNotificationsPermission()
{
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
    {
        val permissionState = rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(permissionState.status.isGranted) {
            if (!permissionState.status.isGranted)
                permissionState.launchPermissionRequest()
        }
    }
}

@Composable
private fun SidebarButtonsRow(
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
)
{
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
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
    item: SubScreenDestination,
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
            intent.data = Uri.parse(item.link)
            context.startActivity(intent)
        },
        badge = { Icon(Icons.Outlined.OpenInBrowser, label) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}
