package me.tomasan7.jecnamobile.settings

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import me.tomasan7.jecnamobile.navigation.AppDestination


fun <T : Any> settingsNavEntry(
    key: T,
    onNavigate: (AppDestination.Settings) -> Unit,
    onBackClick: () -> Unit
): NavEntry<T> {
    return when (key as AppDestination.Settings) {
        is AppDestination.Settings.Main -> NavEntry(key) {
            SettingsMainScreen(
                onNavigate = onNavigate,
                onBackClick = onBackClick
            )
        }
        is AppDestination.Settings.General -> NavEntry(key) {
            SettingsGeneralScreen(
                viewModel = hiltViewModel(),
                onBackClick = onBackClick
            )
        }
        is AppDestination.Settings.Notifications -> NavEntry(key) {
            SettingsNotificationsScreen(
                viewModel = hiltViewModel(),
                onBackClick = onBackClick
            )
        }
        is AppDestination.Settings.Appearance -> NavEntry(key) {
            SettingsAppearanceScreen(
                viewModel = hiltViewModel(),
                onBackClick = onBackClick
            )
        }
        is AppDestination.Settings.Substitution -> NavEntry(key) {
            SettingsSubstitutionScreen(
                viewModel = hiltViewModel(),
                onBackClick = onBackClick
            )
        }
        is AppDestination.Settings.Canteen -> NavEntry(key) {
            SettingsCanteenScreen(
                viewModel = hiltViewModel(),
                onBackClick = onBackClick
            )
        }
        is AppDestination.Settings.About -> NavEntry(key) {
            SettingsAboutScreen(
                onBackClick = onBackClick
            )
        }
        is AppDestination.Settings.Drawer -> NavEntry(key) {
            SettingsDrawerScreen(
                viewModel = hiltViewModel(),
                onBackClick = onBackClick
            )
        }
    }
}
