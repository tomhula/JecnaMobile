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
        "https://spsejecnacz.sharepoint.com/:x:/s/nastenka/EbA_RcWKRdRNlB8YU1iuWM4BnMetCQlVm8toHuuyW-TPyA?e=uu3iPR&CID=2686cea0-2d06-3304-4519-087fb9e06fd0",
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
