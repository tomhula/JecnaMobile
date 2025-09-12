package me.tomasan7.jecnamobile.mainscreen

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.ui.graphics.vector.ImageVector
import me.tomasan7.jecnaapi.web.jecna.JecnaWebClient
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.icons.Jecna
import me.tomasan7.jecnamobile.icons.Moodle

enum class SidebarLink(
    val link: String,
    @StringRes
    val label: Int,
    val icon: ImageVector
)
{
    SubstitutionTimetable(
        "https://spsejecnacz.sharepoint.com/:x:/s/nastenka/ESy19K245Y9BouR5ksciMvgBu3Pn_9EaT0fpP8R6MrkEmg?e=9Vgl8M",
        R.string.sidebar_link_substitution_timetable,
        Icons.Outlined.TableChart
    ),
    Moodle(
        "https://moodle.spsejecna.cz",
        R.string.sidebar_link_moodle,
        Icons.Moodle
    ),
    JecnaWeb(
        JecnaWebClient.ENDPOINT,
        R.string.sidebar_link_jecna_web,
        Icons.Jecna
    )
}
