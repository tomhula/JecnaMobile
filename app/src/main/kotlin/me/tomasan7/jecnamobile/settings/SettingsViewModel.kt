package me.tomasan7.jecnamobile.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import me.tomasan7.jecnamobile.mainscreen.SubScreenDestination
import me.tomasan7.jecnamobile.util.settingsDataStore
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext
    val appContext: Context
) : ViewModel()
{
    private val settingsDataStore = appContext.settingsDataStore

    fun setTheme(theme: Settings.Theme) = viewModelScope.launch {
        settingsDataStore.updateData {
            it.copy(theme = theme)
        }
    }

    fun setOpenSubScreen(subScreenDestination: SubScreenDestination) = viewModelScope.launch {
        settingsDataStore.updateData {
            it.copy(defaultDestination = subScreenDestination)
        }
    }

    fun setSubstitutionServerUrl(url: String) = viewModelScope.launch {
        settingsDataStore.updateData {
            it.copy(substitutionServerUrl = url)
        }
    }

    fun setSubstitutionTimetableEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsDataStore.updateData {
            it.copy(substitutionTimetableEnabled = enabled)
        }
    }
}
