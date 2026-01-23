package me.tomasan7.jecnamobile.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.tomhula.jecnaapi.util.SchoolYear
import jecnamobile.composeapp.generated.resources.Res
import jecnamobile.composeapp.generated.resources.school_year
import kotlinx.datetime.LocalDate
import me.tomasan7.jecnamobile.util.now
import org.jetbrains.compose.resources.stringResource

@Composable
fun SchoolYearSelector(
    selectedSchoolYear: SchoolYear,
    modifier: Modifier = Modifier,
    showYearAhead: Boolean = false,
    onChange: (SchoolYear) -> Unit
)
{
    /* List of four past SchoolYears including the current one. (optionally the following one) */
    val past4SchoolYears = remember {
        val currentSchoolYear = SchoolYear.fromDate(LocalDate.now())
        val rangeEnd = if (showYearAhead) currentSchoolYear + 1 else currentSchoolYear
        ((currentSchoolYear - 3)..rangeEnd).toList()
    }

    OutlinedDropDownSelector(
        label = stringResource(Res.string.school_year),
        options = past4SchoolYears,
        selectedValue = selectedSchoolYear,
        modifier = modifier,
        onChange = onChange
    )
}
