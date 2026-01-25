package me.tomasan7.jecnamobile.mainscreen

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
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
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import de.palm.composestateevents.EventEffect
import io.github.tomhula.jecnaapi.data.room.RoomReference
import io.github.tomhula.jecnaapi.data.schoolStaff.TeacherReference
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.absence.AbsencesSubScreen
import me.tomasan7.jecnamobile.attendances.AttendancesSubScreen
import me.tomasan7.jecnamobile.canteen.CanteenSubScreen
import me.tomasan7.jecnamobile.grades.GradesSubScreen
import me.tomasan7.jecnamobile.news.NewsSubScreen
import me.tomasan7.jecnamobile.rooms.RoomsSubScreen
import me.tomasan7.jecnamobile.rooms.room.RoomScreen
import me.tomasan7.jecnamobile.settings.SettingsScreen
import me.tomasan7.jecnamobile.student.StudentProfileScreen
import me.tomasan7.jecnamobile.teachers.TeachersSubScreen
import me.tomasan7.jecnamobile.teachers.teacher.TeacherScreen
import me.tomasan7.jecnamobile.timetable.TimetableSubScreen
import me.tomasan7.jecnamobile.util.settingsAsStateAwaitFirst

@Composable
fun MainScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: MainScreenViewModel = hiltViewModel()
)
{
    RequestNotificationsPermission()

    val settings by settingsAsStateAwaitFirst()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val destinationItems = SubScreenDestination.entries
    val linkItems = SidebarLink.entries
    val navBackStack = rememberNavBackStack(settings.defaultDestination)
    // val navBackStack = remember { mutableStateListOf<Any>(settings.defaultDestination) }
    val navDrawerController = rememberNavDrawerController(drawerState, scope)

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
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(all = 28.dp)
                )

                destinationItems.forEach { item ->
                    val selected = item === navBackStack.lastOrNull()
                    DestinationItem(
                        item = item,
                        selected = selected,
                        onClick = onClick@{
                            scope.launch { drawerState.close() }
                            if (selected)
                                return@onClick

                            navBackStack.clear()
                            navBackStack.add(item)
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
                        onProfileClick = {
                            scope.launch { drawerState.close() }
                            navBackStack.add(StudentProfileDestination)
                        },
                        onSettingsClick = {
                            scope.launch { drawerState.close() }
                            navBackStack.add(SettingsScreenDestination)
                        },
                        onLogoutClick = {
                            viewModel.logout()
                            onNavigateToLogin()
                        }
                    )
                }
            }
        },
        content = {
            NavDisplay(
                backStack = navBackStack,
                onBack = { navBackStack.pop() },
                entryProvider = { key ->
                    when (key)
                    {
                        is SubScreenDestination -> when (key)
                        {
                            SubScreenDestination.News -> NavEntry(key) {
                                NewsSubScreen(navDrawerController)
                            }
                            SubScreenDestination.Grades -> NavEntry(key) {
                                GradesSubScreen(
                                    navDrawerController = navDrawerController,
                                    onTeacherClick = { navBackStack.add(TeacherScreenDestination(it)) }
                                )
                            }
                            SubScreenDestination.Timetable -> NavEntry(key) {
                                TimetableSubScreen(
                                    navDrawerController = navDrawerController,
                                    onTeacherClick = { navBackStack.add(TeacherScreenDestination(it)) },
                                    onRoomClick = { navBackStack.add(RoomScreenDestination(it)) }
                                )
                            }
                            SubScreenDestination.Canteen -> NavEntry(key) {
                                CanteenSubScreen(navDrawerController)
                            }
                            SubScreenDestination.Attendances -> NavEntry(key) {
                                AttendancesSubScreen(navDrawerController)
                            }
                            SubScreenDestination.Absences -> NavEntry(key) {
                                AbsencesSubScreen(navDrawerController)
                            }
                            SubScreenDestination.Teachers -> NavEntry(key) {
                                TeachersSubScreen(
                                    navDrawerController = navDrawerController,
                                    onTeacherClick = { navBackStack.add(TeacherScreenDestination(it)) }
                                )
                            }
                            SubScreenDestination.Rooms -> NavEntry(key) {
                                RoomsSubScreen(
                                    navDrawerController = navDrawerController,
                                    onRoomClick = { navBackStack.add(RoomScreenDestination(it)) }
                                )
                            }
                        }
                        is TeacherScreenDestination -> NavEntry(key) {
                            TeacherScreen(
                                teacherReference = key.reference,
                                onBackClick = { navBackStack.pop() },
                                onRoomClick = { navBackStack.add(RoomScreenDestination(it)) }
                            )
                        }
                        is RoomScreenDestination -> NavEntry(key) {
                            RoomScreen(
                                roomReference = key.reference,
                                onBackClick = { navBackStack.pop() },
                                onTeacherClick = { navBackStack.add(TeacherScreenDestination(it)) }
                            )
                        }
                        is StudentProfileDestination -> NavEntry(key) {
                            StudentProfileScreen(
                                onBackClick = { navBackStack.pop() }
                            )
                        }
                        is SettingsScreenDestination -> NavEntry(key) {
                            SettingsScreen(
                                onBackClick = { navBackStack.pop() }
                            )
                        }
                        else -> NavEntry(key) { Text("Unknown route") }
                    }
                }
            )
        }
    )
}

private fun <T> MutableList<T>.pop() = if (size > 1) removeAt(lastIndex) else Unit

@Serializable
private data class TeacherScreenDestination(val reference: TeacherReference) : NavKey
@Serializable
private data class RoomScreenDestination(val reference: RoomReference) : NavKey
@Serializable
private data object StudentProfileDestination: NavKey
@Serializable
private data object SettingsScreenDestination: NavKey

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
            intent.data = item.link.toUri()
            context.startActivity(intent)
        },
        badge = { Icon(Icons.Outlined.OpenInBrowser, label) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}
