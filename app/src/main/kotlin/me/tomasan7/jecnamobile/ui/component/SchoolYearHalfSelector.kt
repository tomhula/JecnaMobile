package me.tomasan7.jecnamobile.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.tomhula.jecnaapi.util.SchoolYearHalf
import me.tomasan7.jecnamobile.R

@Composable
fun SchoolYearHalfSelector(
    selectedSchoolYearHalf: SchoolYearHalf,
    modifier: Modifier = Modifier,
    onChange: (SchoolYearHalf) -> Unit
)
{
    OutlinedDropDownSelector(
        label = stringResource(R.string.school_year_half),
        options = SchoolYearHalf.values().toList(),
        optionStringMap = { when (it!!)
        {
            SchoolYearHalf.FIRST -> stringResource(R.string.school_year_half_1)
            SchoolYearHalf.SECOND -> stringResource(R.string.school_year_half_2)
        }},
        selectedValue = selectedSchoolYearHalf,
        modifier = modifier,
        onChange = onChange
    )
}
