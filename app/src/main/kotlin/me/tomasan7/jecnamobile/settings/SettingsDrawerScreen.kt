package me.tomasan7.jecnamobile.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.mainscreen.SubScreenDestination
import me.tomasan7.jecnamobile.ui.ElevationLevel
import me.tomasan7.jecnamobile.util.settingsAsStateAwaitFirst
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDrawerScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val settings by settingsAsStateAwaitFirst()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var drawerPages by remember(settings.drawerPages) {
        mutableStateOf(settings.drawerPages.toList())
    }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        drawerPages = drawerPages.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        viewModel.updateSettings { it.copy(drawerPages = drawerPages) }
    }

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
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                text = stringResource(R.string.settings_drawer_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = drawerPages,
                    key = { it.destinationName }
                ) { page: Settings.DrawerPage ->
                    val destination = try {
                        SubScreenDestination.valueOf(page.destinationName)
                    } catch (e: IllegalArgumentException) {
                        null
                    }

                    if (destination != null) {
                        ReorderableItem(
                            reorderableState,
                            key = page.destinationName
                        ) { isDragging ->
                            val elevation by animateDpAsState(
                                if (isDragging) 8.dp else 0.dp,
                                label = "elevation"
                            )

                            DrawerPageItem(
                                page = page,
                                destination = destination,
                                elevation = elevation,
                                reorderableItemScope = this,
                                onVisibilityChange = { isVisible: Boolean ->
                                    val index = drawerPages.indexOfFirst { it.destinationName == page.destinationName }
                                    if (index >= 0) {
                                        drawerPages = drawerPages.toMutableList().apply {
                                            this[index] = page.copy(isVisible = isVisible)
                                        }
                                        viewModel.updateSettings { it.copy(drawerPages = drawerPages) }
                                    }
                                }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DrawerPageItem(
    page: Settings.DrawerPage,
    destination: SubScreenDestination,
    elevation: androidx.compose.ui.unit.Dp,
    reorderableItemScope: ReorderableCollectionItemScope,
    onVisibilityChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = ElevationLevel.level1,
        shadowElevation = ElevationLevel.level1,
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
                    .let { modifier ->
                        with(reorderableItemScope) {
                            modifier.draggableHandle()
                        }
                    }
            )

            Icon(
                imageVector = destination.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = stringResource(destination.label),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = page.isVisible,
                onCheckedChange = onVisibilityChange
            )
        }
    }
}
