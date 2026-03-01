package me.tomasan7.jecnamobile.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMainScreen(
    onNavigate: (SettingsDestination) -> Unit,
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
                    onClick = { onNavigate(SettingsDestination.General) }
                )
                Divider()
                SettingsCategoryItem(
                    title = stringResource(R.string.settings_category_appearance),
                    icon = Icons.Default.Palette,
                    onClick = { onNavigate(SettingsDestination.Appearance) }
                )
                Divider()
                SettingsCategoryItem(
                    title = stringResource(R.string.settings_substitution_server_title),
                    icon = Icons.Default.Warning,
                    onClick = { onNavigate(SettingsDestination.Substitution) }
                )
                Divider()
                SettingsCategoryItem(
                    title = stringResource(R.string.sidebar_canteen),
                    icon = Icons.Default.Restaurant,
                    onClick = { onNavigate(SettingsDestination.Canteen) }
                )
            }

            SettingsSection(title = null) {
                SettingsCategoryItem(
                    title = stringResource(R.string.settings_category_about),
                    icon = Icons.Default.Info,
                    onClick = { onNavigate(SettingsDestination.About) }
                )
            }
        }
    }
}
