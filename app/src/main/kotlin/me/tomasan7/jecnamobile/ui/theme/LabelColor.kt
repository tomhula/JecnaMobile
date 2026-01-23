package me.tomasan7.jecnamobile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import me.tomasan7.jecnamobile.settings.isAppInDarkTheme

val jm_theme_light_label = Color.DarkGray
val jm_theme_dark_label = Color.LightGray
val jm_label: Color
    @Composable
    get() = if (isAppInDarkTheme()) jm_theme_dark_label else jm_theme_light_label
