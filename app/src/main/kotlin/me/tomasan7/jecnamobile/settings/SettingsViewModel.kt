package me.tomasan7.jecnamobile.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.tomasan7.jecnamobile.util.settingsDataStore


class SettingsViewModel(
    val appContext: Context
) : ViewModel()
{
    private val settingsDataStore = appContext.settingsDataStore

    fun updateSettings(transform: suspend (Settings) -> Settings) = viewModelScope.launch {
        settingsDataStore.updateData(transform)
    }
}
