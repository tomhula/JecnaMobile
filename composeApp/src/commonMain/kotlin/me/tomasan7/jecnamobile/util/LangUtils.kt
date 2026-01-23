package me.tomasan7.jecnamobile.util

import androidx.compose.runtime.Composable
import jecnamobile.composeapp.generated.resources.Res
import jecnamobile.composeapp.generated.resources.april
import jecnamobile.composeapp.generated.resources.august
import jecnamobile.composeapp.generated.resources.december
import jecnamobile.composeapp.generated.resources.february
import jecnamobile.composeapp.generated.resources.friday
import jecnamobile.composeapp.generated.resources.january
import jecnamobile.composeapp.generated.resources.july
import jecnamobile.composeapp.generated.resources.june
import jecnamobile.composeapp.generated.resources.march
import jecnamobile.composeapp.generated.resources.may
import jecnamobile.composeapp.generated.resources.monday
import jecnamobile.composeapp.generated.resources.november
import jecnamobile.composeapp.generated.resources.october
import jecnamobile.composeapp.generated.resources.saturday
import jecnamobile.composeapp.generated.resources.september
import jecnamobile.composeapp.generated.resources.sunday
import jecnamobile.composeapp.generated.resources.thursday
import jecnamobile.composeapp.generated.resources.tuesday
import jecnamobile.composeapp.generated.resources.wednesday
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import org.jetbrains.compose.resources.stringResource
import java.text.Normalizer

@Composable
fun getWeekDayName(dayOfWeek: DayOfWeek) = when (dayOfWeek)
{
    DayOfWeek.MONDAY    -> stringResource(Res.string.monday)
    DayOfWeek.TUESDAY   -> stringResource(Res.string.tuesday)
    DayOfWeek.WEDNESDAY -> stringResource(Res.string.wednesday)
    DayOfWeek.THURSDAY  -> stringResource(Res.string.thursday)
    DayOfWeek.FRIDAY    -> stringResource(Res.string.friday)
    DayOfWeek.SATURDAY  -> stringResource(Res.string.saturday)
    DayOfWeek.SUNDAY    -> stringResource(Res.string.sunday)
}

@Composable
fun getMonthName(month: Month) = when (month)
{
    Month.JANUARY   -> stringResource(Res.string.january)
    Month.FEBRUARY  -> stringResource(Res.string.february)
    Month.MARCH     -> stringResource(Res.string.march)
    Month.APRIL     -> stringResource(Res.string.april)
    Month.MAY       -> stringResource(Res.string.may)
    Month.JUNE      -> stringResource(Res.string.june)
    Month.JULY      -> stringResource(Res.string.july)
    Month.AUGUST    -> stringResource(Res.string.august)
    Month.SEPTEMBER -> stringResource(Res.string.september)
    Month.OCTOBER   -> stringResource(Res.string.october)
    Month.NOVEMBER  -> stringResource(Res.string.november)
    Month.DECEMBER  -> stringResource(Res.string.december)
}

fun String.removeAccent() = Normalizer.normalize(this, Normalizer.Form.NFKD).replace(Regex("""\p{M}"""), "")
