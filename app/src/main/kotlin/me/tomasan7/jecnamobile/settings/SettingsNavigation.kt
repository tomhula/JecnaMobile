package me.tomasan7.jecnamobile.settings

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface SettingsDestination : NavKey {
    @Serializable data object Main : SettingsDestination
    @Serializable data object General : SettingsDestination
    @Serializable data object Notifications : SettingsDestination
    @Serializable data object Appearance : SettingsDestination
    @Serializable data object Substitution : SettingsDestination
    @Serializable data object Canteen : SettingsDestination
    @Serializable data object About : SettingsDestination
}

fun <T : Any> settingsNavEntry(
    key: T,
    onNavigate: (SettingsDestination) -> Unit,
    onBackClick: () -> Unit
): NavEntry<T> {
    return when (key as SettingsDestination) {
        is SettingsDestination.Main -> NavEntry(key) {
            SettingsMainScreen(
                onNavigate = onNavigate,
                onBackClick = onBackClick
            )
        }
        is SettingsDestination.General -> NavEntry(key) {
            SettingsGeneralScreen(
                viewModel = hiltViewModel(),
                onBackClick = onBackClick
            )
        }
        is SettingsDestination.Notifications -> NavEntry(key) {
            SettingsNotificationsScreen(
                viewModel = hiltViewModel(),
                onBackClick = onBackClick
            )
        }
        is SettingsDestination.Appearance -> NavEntry(key) {
            SettingsAppearanceScreen(
                viewModel = hiltViewModel(),
                onBackClick = onBackClick
            )
        }
        is SettingsDestination.Substitution -> NavEntry(key) {
            SettingsSubstitutionScreen(
                viewModel = hiltViewModel(),
                onBackClick = onBackClick
            )
        }
        is SettingsDestination.Canteen -> NavEntry(key) {
            SettingsCanteenScreen(
                viewModel = hiltViewModel(),
                onBackClick = onBackClick
            )
        }
        is SettingsDestination.About -> NavEntry(key) {
            SettingsAboutScreen(
                onBackClick = onBackClick
            )
        }
    }
}
