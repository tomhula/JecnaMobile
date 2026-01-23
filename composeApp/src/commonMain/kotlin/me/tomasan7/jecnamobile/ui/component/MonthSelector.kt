package me.tomasan7.jecnamobile.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import jecnamobile.composeapp.generated.resources.Res
import jecnamobile.composeapp.generated.resources.month
import kotlinx.datetime.Month
import me.tomasan7.jecnamobile.util.getMonthName
import org.jetbrains.compose.resources.stringResource

@Composable
fun MonthSelector(
    selectedMonth: Month,
    modifier: Modifier = Modifier,
    onChange: (Month) -> Unit
)
{
    OutlinedDropDownSelector(
        label = stringResource(Res.string.month),
        options = Month.entries,
        optionStringMap = { getMonthName(it!!) },
        selectedValue = selectedMonth,
        modifier = modifier,
        onChange = onChange
    )
}
