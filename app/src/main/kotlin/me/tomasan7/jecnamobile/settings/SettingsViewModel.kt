package me.tomasan7.jecnamobile.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import me.tomasan7.jecnamobile.util.settingsDataStore
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext
    val appContext: Context
) : ViewModel()
{
    private val settingsDataStore = appContext.settingsDataStore

    fun updateSettings(transform: suspend (Settings) -> Settings) = viewModelScope.launch {
        settingsDataStore.updateData(transform)
    }
}
