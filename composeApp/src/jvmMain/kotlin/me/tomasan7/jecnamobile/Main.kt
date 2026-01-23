package me.tomasan7.jecnamobile

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.singleWindowApplication
import io.github.tomhula.jecnaapi.util.SchoolYear
import kotlinx.datetime.Month
import me.tomasan7.jecnamobile.ui.component.MonthSelector
import me.tomasan7.jecnamobile.ui.component.SchoolYearSelector
import me.tomasan7.jecnamobile.ui.theme.JecnaMobileTheme
import me.tomasan7.jecnamobile.util.rememberMutableStateOf

fun main()
{
    singleWindowApplication {
        JecnaMobileTheme {
            var selectedSchoolYear by rememberMutableStateOf(SchoolYear.current())
            var selectedMonth by rememberMutableStateOf(Month(1))
            Row {
                SchoolYearSelector(
                    selectedSchoolYear = selectedSchoolYear,
                    onChange = { selectedSchoolYear = it }
                )

                MonthSelector(
                    selectedMonth = selectedMonth,
                    onChange = { selectedMonth = it }
                )
            }
        }
    }
}
