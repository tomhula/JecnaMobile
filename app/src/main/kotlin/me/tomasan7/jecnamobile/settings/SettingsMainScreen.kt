package me.tomasan7.jecnamobile.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.navigation.AppDestination
import me.tomasan7.jecnamobile.ui.component.HorizontalDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMainScreen(
    onNavigate: (AppDestination.Settings) -> Unit,
    onBackClick: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.sidebar_settings)) },
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
                .padding(bottom = 24.dp),
        ) {
            SettingsSection(title = null) {
                SettingsCategoryItem(
                    title = stringResource(R.string.settings_category_general),
                    icon = Icons.Default.Settings,
                    onClick = { onNavigate(AppDestination.Settings.General) }
                )
                HorizontalDivider()
                SettingsCategoryItem(
                    title = stringResource(R.string.settings_category_notifications),
                    icon = Icons.Default.Notifications,
                    onClick = { onNavigate(AppDestination.Settings.Notifications) }
                )
                HorizontalDivider()
                SettingsCategoryItem(
                    title = stringResource(R.string.settings_category_appearance),
                    icon = Icons.Default.Palette,
                    onClick = { onNavigate(AppDestination.Settings.Appearance) }
                )
                HorizontalDivider()
                SettingsCategoryItem(
                    title = stringResource(R.string.settings_drawer_title),
                    icon = Icons.Default.Menu,
                    onClick = { onNavigate(AppDestination.Settings.Drawer) }
                )
                HorizontalDivider()
                SettingsCategoryItem(
                    title = stringResource(R.string.settings_substitution_server_title),
                    icon = Icons.Default.Warning,
                    onClick = { onNavigate(AppDestination.Settings.Substitution) }
                )
                HorizontalDivider()
                SettingsCategoryItem(
                    title = stringResource(R.string.sidebar_canteen),
                    icon = Icons.Default.Restaurant,
                    onClick = { onNavigate(AppDestination.Settings.Canteen) }
                )
            }

            SettingsSection(title = null) {
                SettingsCategoryItem(
                    title = stringResource(R.string.settings_category_about),
                    icon = Icons.Default.Info,
                    onClick = { onNavigate(AppDestination.Settings.About) }
                )
            }
        }
    }
}
