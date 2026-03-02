package me.tomasan7.jecnamobile.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.mainscreen.SubScreenDestination
import me.tomasan7.jecnamobile.ui.component.FilledDropDownSelector
import me.tomasan7.jecnamobile.util.settingsAsStateAwaitFirst

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsGeneralScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val settings by settingsAsStateAwaitFirst()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    val names = mapOf(
        SubScreenDestination.News to stringResource(R.string.sidebar_news),
        SubScreenDestination.Grades to stringResource(R.string.sidebar_grades),
        SubScreenDestination.Timetable to stringResource(R.string.sidebar_timetable),
        SubScreenDestination.Canteen to stringResource(R.string.sidebar_canteen),
        SubScreenDestination.Attendances to stringResource(R.string.sidebar_attendances),
        SubScreenDestination.Absences to stringResource(R.string.sidebar_absences),
        SubScreenDestination.Teachers to stringResource(R.string.sidebar_teachers),
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings_category_general)) },
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
                .padding(bottom = 24.dp)
        ) {
            SettingsSection(title = stringResource(id = R.string.settings_open_subscreen_title)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.settings_open_subscreen_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    FilledDropDownSelector(
                        options = names.keys.toList(),
                        optionStringMap = { names[it]!! },
                        selectedValue = settings.defaultDestination,
                        onChange = { dest -> viewModel.updateSettings { it.copy(defaultDestination = dest) } },
                        modifier = Modifier.fillMaxWidth(),
                        textFieldModifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
