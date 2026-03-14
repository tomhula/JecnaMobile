package me.tomasan7.jecnamobile.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.mainscreen.SidebarLink
import me.tomasan7.jecnamobile.mainscreen.SubScreenDestination
import me.tomasan7.jecnamobile.ui.ElevationLevel
import me.tomasan7.jecnamobile.util.settingsAsStateAwaitFirst
import sh.calvin.reorderable.ReorderableColumn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDrawerScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val settings by settingsAsStateAwaitFirst()

    var pages by remember { mutableStateOf(settings.drawerPages) }
    var links by remember { mutableStateOf(settings.drawerLinks) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings_drawer_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.updateSettings {
                                it.copy(drawerPages = pages, drawerLinks = links)
                            }
                            onBackClick()
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save Changes")
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
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_drawer_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = stringResource(R.string.settings_drawer_pages_section),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            ReorderableColumn(
                list = pages,
                onSettle = { from, to ->
                    pages = pages.toMutableList().apply {
                        add(to, removeAt(from))
                    }
                },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) { _, page, isDragging ->
                key(page.destinationName) {
                    ReorderableItem {
                        val destination = runCatching { SubScreenDestination.valueOf(page.destinationName) }.getOrNull()

                        if (destination != null) {
                            DrawerListItemContent(
                                icon = destination.icon,
                                label = destination.label,
                                isVisible = page.isVisible,
                                isDragging = isDragging,
                                modifier = Modifier.draggableHandle(),
                                onVisibilityChange = { isVisible ->
                                    val index = pages.indexOfFirst { it.destinationName == page.destinationName }
                                    if (index >= 0) {
                                        pages = pages.toMutableList().apply {
                                            this[index] = this[index].copy(isVisible = isVisible)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.settings_drawer_links_section),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )

            ReorderableColumn(
                list = links,
                onSettle = { from, to ->
                    links = links.toMutableList().apply {
                        add(to, removeAt(from))
                    }
                },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) { _, link, isDragging ->
                key(link.linkName) {
                    ReorderableItem {
                        val sidebarLink = runCatching { SidebarLink.valueOf(link.linkName) }.getOrNull()

                        if (sidebarLink != null) {
                            DrawerListItemContent(
                                icon = sidebarLink.icon,
                                label = sidebarLink.label,
                                isVisible = link.isVisible,
                                isDragging = isDragging,
                                modifier = Modifier.draggableHandle(),
                                onVisibilityChange = { isVisible ->
                                    val index = links.indexOfFirst { it.linkName == link.linkName }
                                    if (index >= 0) {
                                        links = links.toMutableList().apply {
                                            this[index] = this[index].copy(isVisible = isVisible)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DrawerListItemContent(
    icon: ImageVector,
    label: Int,
    isVisible: Boolean,
    isDragging: Boolean,
    modifier: Modifier,
    onVisibilityChange: (Boolean) -> Unit
) {
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        label = "elevation"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = ElevationLevel.level1,
        shadowElevation = elevation,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = stringResource(R.string.settings_drawer_drag_handle),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 12.dp, end = 8.dp)
                    .then(modifier)
            )

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = isVisible,
                onCheckedChange = onVisibilityChange
            )
        }
    }
}
