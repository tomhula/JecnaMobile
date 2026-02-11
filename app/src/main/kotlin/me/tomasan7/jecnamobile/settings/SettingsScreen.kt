package me.tomasan7.jecnamobile.settings

import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.mainscreen.SubScreenDestination
import me.tomasan7.jecnamobile.ui.component.FilledDropDownSelector
import me.tomasan7.jecnamobile.ui.component.RadioGroup
import me.tomasan7.jecnamobile.ui.theme.jm_label
import me.tomasan7.jecnamobile.util.settingsAsState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
)
{
    Scaffold(
        topBar = { TopAppBar(onBackClick = onBackClick) }
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
        SubScreenDestination.News to stringResource(R.string.sidebar_news),
        SubScreenDestination.Grades to stringResource(R.string.sidebar_grades),
        SubScreenDestination.Timetable to stringResource(R.string.sidebar_timetable),
        SubScreenDestination.Canteen to stringResource(R.string.sidebar_canteen),
        SubScreenDestination.Attendances to stringResource(R.string.sidebar_attendances),
        SubScreenDestination.Absences to stringResource(R.string.sidebar_absences),
        SubScreenDestination.Teachers to stringResource(R.string.sidebar_teachers),
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
                options = Settings.Theme.entries,
                selectedOption = settings.theme,
                onSelectionChange = { viewModel.setTheme(theme = it) }
            ) { themeOptionStringsArray[it.ordinal] }
        }

        Option(
            title = stringResource(id = R.string.settings_open_subscreen_title),
            description = stringResource(id = R.string.settings_open_subscreen_description)
        ) {
            FilledDropDownSelector(
                options = names.keys.toList(),
                optionStringMap = { names[it]!! },
                selectedValue = settings.defaultDestination,
                onChange = { viewModel.setOpenSubScreen(it) }
            )
        }

        Option(
            title = stringResource(R.string.settings_substitution_server_title),
            description = stringResource(R.string.settings_substitution_server_description)
        ) {
            // If we directly use settings.substitutionServerUrl the text field will be laggy and will reset users cursor
            var url by remember { mutableStateOf(settings.substitutionServerUrl) }
            LaunchedEffect(url) { 
                viewModel.setSubstitutionServerUrl(url) 
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setSubstitutionTimetableEnabled(!settings.substitutionTimetableEnabled) }
            ) {
                Text(
                    text = stringResource(R.string.settings_substitution_server_enable),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = settings.substitutionTimetableEnabled,
                    onCheckedChange = { viewModel.setSubstitutionTimetableEnabled(it) }
                )
            }
            
            val defaultUrl = Settings.DEFAULT_SUBSTITUTION_SERVER_URL
            val rollBackButton = if (url != defaultUrl)
            {
                @Composable {
                    IconButton(onClick = {
                        url = defaultUrl
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.settings_substitution_server_reset))
                    } 
                }
            }
            else 
                null

            OutlinedTextField(
                value = url,
                onValueChange = { 
                    url = it
                },
                singleLine = true,
                label = { Text(stringResource(R.string.settings_substitution_server_url)) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = rollBackButton
            )
            
            val baseText = stringResource(R.string.settings_substitution_server_info)
            val labelColor = jm_label
            
            val annotated = remember(baseText, labelColor) { 
                buildAnnotatedString {
                    append(baseText)
                    append(" ")

                    withLink(
                        LinkAnnotation.Url(
                            url = "https://jecnarozvrh.jzitnik.dev",
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = labelColor,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        )
                    ) {
                        append("jecnarozvrh.jzitnik.dev")
                    }

                    append(".")
                }
            }

            Text(
                text = annotated,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = stringResource(R.string.settings_restart_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
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
