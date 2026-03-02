package me.tomasan7.jecnamobile.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.unit.dp
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.util.settingsAsStateAwaitFirst

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSubstitutionScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val settings by settingsAsStateAwaitFirst()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings_substitution_server_title)) },
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
            SettingsSection(title = null) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_substitution_server_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    var url by remember { mutableStateOf(settings.substitutionServerUrl) }
                    LaunchedEffect(url) { 
                        viewModel.updateSettings { it.copy(substitutionServerUrl = url) }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateSettings { it.copy(substitutionTimetableEnabled = !it.substitutionTimetableEnabled) } }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_substitution_server_enable),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = settings.substitutionTimetableEnabled,
                            onCheckedChange = { checked -> viewModel.updateSettings { it.copy(substitutionTimetableEnabled = checked) } }
                        )
                    }
                    
                    if (settings.substitutionTimetableEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))

                        val defaultUrl = Settings.DEFAULT_SUBSTITUTION_SERVER_URL
                        val rollBackButton = if (url != defaultUrl) {
                            @Composable {
                                IconButton(onClick = { url = defaultUrl }) {
                                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.settings_substitution_server_reset))
                                } 
                            }
                        } else null

                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            singleLine = true,
                            label = { Text(stringResource(R.string.settings_substitution_server_url)) },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = rollBackButton,
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        val baseText = stringResource(R.string.settings_substitution_server_info)
                        val labelColor = MaterialTheme.colorScheme.primary
                        
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
                                                textDecoration = TextDecoration.Underline,
                                                fontWeight = FontWeight.Medium
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
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.settings_restart_required),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}
