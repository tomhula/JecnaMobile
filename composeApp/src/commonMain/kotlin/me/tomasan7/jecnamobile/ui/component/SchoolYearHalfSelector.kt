package me.tomasan7.jecnamobile.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.tomhula.jecnaapi.util.SchoolYearHalf
import jecnamobile.composeapp.generated.resources.Res
import jecnamobile.composeapp.generated.resources.school_year_half
import jecnamobile.composeapp.generated.resources.school_year_half_1
import jecnamobile.composeapp.generated.resources.school_year_half_2
import org.jetbrains.compose.resources.stringResource

@Composable
fun SchoolYearHalfSelector(
    selectedSchoolYearHalf: SchoolYearHalf,
    modifier: Modifier = Modifier,
    onChange: (SchoolYearHalf) -> Unit
)
{
    OutlinedDropDownSelector(
        label = stringResource(Res.string.school_year_half),
        options = SchoolYearHalf.entries,
        optionStringMap = { when (it!!)
        {
            SchoolYearHalf.FIRST -> stringResource(Res.string.school_year_half_1)
            SchoolYearHalf.SECOND -> stringResource(Res.string.school_year_half_2)
        }},
        selectedValue = selectedSchoolYearHalf,
        modifier = modifier,
        onChange = onChange
    )
}
