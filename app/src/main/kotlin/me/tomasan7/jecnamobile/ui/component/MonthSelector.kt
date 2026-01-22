package me.tomasan7.jecnamobile.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.datetime.Month
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.util.getMonthName

@Composable
fun MonthSelector(
    selectedMonth: Month,
    modifier: Modifier = Modifier,
    onChange: (Month) -> Unit
)
{
    OutlinedDropDownSelector(
        label = stringResource(R.string.month),
        options = Month.entries,
        optionStringMap = { getMonthName(it!!) },
        selectedValue = selectedMonth,
        modifier = modifier,
        onChange = onChange
    )
}
