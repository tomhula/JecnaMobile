package me.tomasan7.jecnamobile.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import me.tomasan7.jecnamobile.R
import java.text.Normalizer
import java.util.*

@Composable
fun getWeekDayName(dayOfWeek: DayOfWeek) = when (dayOfWeek)
{
    DayOfWeek.MONDAY    -> stringResource(R.string.monday)
    DayOfWeek.TUESDAY   -> stringResource(R.string.tuesday)
    DayOfWeek.WEDNESDAY -> stringResource(R.string.wednesday)
    DayOfWeek.THURSDAY  -> stringResource(R.string.thursday)
    DayOfWeek.FRIDAY    -> stringResource(R.string.friday)
    DayOfWeek.SATURDAY  -> stringResource(R.string.saturday)
    DayOfWeek.SUNDAY    -> stringResource(R.string.sunday)
}

@Composable
fun getMonthName(month: Month) = when (month)
{
    Month.JANUARY   -> stringResource(R.string.january)
    Month.FEBRUARY  -> stringResource(R.string.february)
    Month.MARCH     -> stringResource(R.string.march)
    Month.APRIL     -> stringResource(R.string.april)
    Month.MAY       -> stringResource(R.string.may)
    Month.JUNE      -> stringResource(R.string.june)
    Month.JULY      -> stringResource(R.string.july)
    Month.AUGUST    -> stringResource(R.string.august)
    Month.SEPTEMBER -> stringResource(R.string.september)
    Month.OCTOBER   -> stringResource(R.string.october)
    Month.NOVEMBER  -> stringResource(R.string.november)
    Month.DECEMBER  -> stringResource(R.string.december)
}

fun String.removeAccent() = Normalizer.normalize(this, Normalizer.Form.NFKD).replace(Regex("""\p{M}"""), "")
