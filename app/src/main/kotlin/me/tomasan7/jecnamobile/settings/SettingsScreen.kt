package me.tomasan7.jecnamobile.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.destinations.AbsencesSubScreenDestination
import me.tomasan7.jecnamobile.destinations.AttendancesSubScreenDestination
import me.tomasan7.jecnamobile.destinations.CanteenSubScreenDestination
import me.tomasan7.jecnamobile.destinations.GradesSubScreenDestination
import me.tomasan7.jecnamobile.destinations.NewsSubScreenDestination
import me.tomasan7.jecnamobile.destinations.TeachersSubScreenDestination
import me.tomasan7.jecnamobile.destinations.TimetableSubScreenDestination
import me.tomasan7.jecnamobile.mainscreen.SubScreensNavGraph
import me.tomasan7.jecnamobile.ui.component.FilledDropDownSelector
import me.tomasan7.jecnamobile.ui.component.RadioGroup
import me.tomasan7.jecnamobile.ui.theme.jm_label
import me.tomasan7.jecnamobile.util.settingsAsState


@Destination<SubScreensNavGraph>
@Composable
fun SettingsScreen(
    navigator: DestinationsNavigator,
    viewModel: SettingsViewModel = hiltViewModel()
)
{
    Scaffold(
        topBar = { TopAppBar(navigator::popBackStack) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Settings(viewModel)
        }
    }
}

@Composable
private fun Settings(viewModel: SettingsViewModel)
{
    val names = mapOf(
        NewsSubScreenDestination.route to stringResource(R.string.sidebar_news),
        GradesSubScreenDestination.route to stringResource(R.string.sidebar_grades),
        TimetableSubScreenDestination.route to stringResource(R.string.sidebar_timetable),
        CanteenSubScreenDestination.route to stringResource(R.string.sidebar_canteen),
        AttendancesSubScreenDestination.route to stringResource(R.string.sidebar_attendances),
        AbsencesSubScreenDestination.route to stringResource(R.string.sidebar_absences),
        TeachersSubScreenDestination.route to stringResource(R.string.sidebar_teachers),
    )

    val settings by settingsAsState()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Option(
            title = stringResource(id = R.string.settings_theme_title)
        ) {
            val themeOptionStringsArray = stringArrayResource(R.array.settings_theme_options)

            RadioGroup(
                options = Settings.Theme.values().toList(),
                optionStringMap = { themeOptionStringsArray[it.ordinal] },
                selectedOption = settings.theme,
                onSelectionChange = { viewModel.setTheme(theme = it) }
            )
        }

        Option(
            title = stringResource(id = R.string.settings_open_subscreen_title),
            description = stringResource(id = R.string.settings_open_subscreen_description)
        ) {
            FilledDropDownSelector(
                options = names.keys.toList(),
                optionStringMap = { names[it]!! },
                selectedValue = settings.openSubScreenRoute,
                onChange = { viewModel.setOpenSubScreen(it) }
            )
        }
    }
}

@Composable
private fun Option(
    title: String,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit
)
{
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium
        )

        if (description != null)
            Text(
                text = description,
                color = jm_label,
                style = MaterialTheme.typography.bodyMedium
            )

        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBar(
    onBackClick: () -> Unit = {},
)
{
    CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.sidebar_settings)) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
            }
        }
    )
}
