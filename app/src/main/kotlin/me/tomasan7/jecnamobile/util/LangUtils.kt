package me.tomasan7.jecnamobile.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import me.tomasan7.jecnamobile.R
import java.text.Normalizer
import java.util.TreeMap

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

@Composable
fun getSchoolYearHalfName(schoolYearHalf: Boolean) =
    stringResource(if (schoolYearHalf) R.string.school_year_half_1 else R.string.school_year_half_2)

private val ROMAN_MAP: TreeMap<Int, String> = TreeMap<Int, String>().apply {
    put(1000, "M")
    put(900, "CM")
    put(500, "D")
    put(400, "CD")
    put(100, "C")
    put(90, "XC")
    put(50, "L")
    put(40, "XL")
    put(10, "X")
    put(9, "IX")
    put(5, "V")
    put(4, "IV")
    put(1, "I")
}

fun Int.toRoman(): String
{
    val l = ROMAN_MAP.floorKey(this)

    return if (this == l)
        ROMAN_MAP[this]!!
    else
        ROMAN_MAP[l!!] + (this - l).toRoman()
}

fun String.removeAccent() = Normalizer.normalize(this, Normalizer.Form.NFKD).replace(Regex("""\p{M}"""), "")
