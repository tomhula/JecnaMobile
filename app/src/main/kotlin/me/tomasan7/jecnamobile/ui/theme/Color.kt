package me.tomasan7.jecnamobile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import me.tomasan7.jecnamobile.settings.isAppInDarkTheme

val md_theme_light_primary = Color(0xFF006875)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFF9DEFFF)
val md_theme_light_onPrimaryContainer = Color(0xFF001F24)
val md_theme_light_secondary = Color(0xFF4A6267)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFCDE7ED)
val md_theme_light_onSecondaryContainer = Color(0xFF051F23)
val md_theme_light_tertiary = Color(0xFF535D7E)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFDAE1FF)
val md_theme_light_onTertiaryContainer = Color(0xFF0F1A37)
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_background = Color(0xFFFBFCFD)
val md_theme_light_onBackground = Color(0xFF191C1D)
val md_theme_light_surface = Color(0xFFFBFCFD)
val md_theme_light_onSurface = Color(0xFF191C1D)
val md_theme_light_surfaceVariant = Color(0xFFDBE4E6)
val md_theme_light_onSurfaceVariant = Color(0xFF3F484A)
val md_theme_light_outline = Color(0xFF6F797B)
val md_theme_light_inverseOnSurface = Color(0xFFEFF1F2)
val md_theme_light_inverseSurface = Color(0xFF2E3132)
val md_theme_light_inversePrimary = Color(0xFF50D7ED)
val md_theme_light_shadow = Color(0xFF000000)

val md_theme_dark_primary = Color(0xFF50D7ED)
val md_theme_dark_onPrimary = Color(0xFF00363D)
val md_theme_dark_primaryContainer = Color(0xFF004F59)
val md_theme_dark_onPrimaryContainer = Color(0xFF9DEFFF)
val md_theme_dark_secondary = Color(0xFFB1CBD0)
val md_theme_dark_onSecondary = Color(0xFF1C3438)
val md_theme_dark_secondaryContainer = Color(0xFF334B4F)
val md_theme_dark_onSecondaryContainer = Color(0xFFCDE7ED)
val md_theme_dark_tertiary = Color(0xFFBBC5EA)
val md_theme_dark_onTertiary = Color(0xFF252F4D)
val md_theme_dark_tertiaryContainer = Color(0xFF3C4665)
val md_theme_dark_onTertiaryContainer = Color(0xFFDAE1FF)
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
val md_theme_dark_background = Color(0xFF191C1D)
val md_theme_dark_onBackground = Color(0xFFE1E3E3)
val md_theme_dark_surface = Color(0xFF191C1D)
val md_theme_dark_onSurface = Color(0xFFE1E3E3)
val md_theme_dark_surfaceVariant = Color(0xFF3F484A)
val md_theme_dark_onSurfaceVariant = Color(0xFFBFC8CA)
val md_theme_dark_outline = Color(0xFF899294)
val md_theme_dark_inverseOnSurface = Color(0xFF191C1D)
val md_theme_dark_inverseSurface = Color(0xFFE1E3E3)
val md_theme_dark_inversePrimary = Color(0xFF006875)
val md_theme_dark_shadow = Color(0xFF000000)


val seed = Color(0xFF00A0B3)

val jm_theme_light_label = Color.DarkGray
val jm_theme_dark_label = Color.LightGray
val jm_label: Color
    @Composable
    get() = if (isAppInDarkTheme()) jm_theme_dark_label else jm_theme_light_label

val jm_canteen_ordered = Color(0x326DC00F)
val jm_canteen_disabled = Color(0x33FD0320)
val jm_canteen_ordered_disabled = Color(0x33C0930C)

val jm_late_attendance = Color(0xFFDA1C1C)

val grade_0 = Color(0xFFA3A7AD)
val grade_1 = Color(0xFF00E03C)
val grade_2 = Color(0xFF008A25)
val grade_3 = Color(0xFFBEDB00)
val grade_4 = Color(0xFFCFA200)
val grade_5 = Color(0xFFA63232)

val grade_excused = Color(0xFF757474)
val grade_grades_warning = Color(0xFFFF8A8A)
val grade_absence_warning = Color(0xFFFF8A8A)

val teacher_search_query_highlight = Color(0xFFCDDC39)

val canteen_dish_description_difference = Color(0xFF889415)
