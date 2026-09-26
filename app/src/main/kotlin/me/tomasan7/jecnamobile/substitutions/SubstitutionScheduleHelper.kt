package me.tomasan7.jecnamobile.substitutions

import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import kotlin.time.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Dynamic scheduler and interval calculator for substitutions (mimořádný rozvrh).
 * Harmonizes with the server-side update schedule rules from jzitnik/jecnarozvrh:
 * - 07:30 – 16:00 (weekdays): 5 min (school hours, high frequency for immediate substitution updates)
 * - 06:00 – 07:30 (weekdays): 15 min (morning preparation before school)
 * - 05:00 – 06:00 (weekdays): 30 min (early morning)
 * - 16:00 – 19:00 (weekdays): 60 min (after school)
 * - 19:00 – 05:00 (night): 180 min (night dormancy)
 * - 03:00 – 04:00: 60 min
 * - Sunday 16:00 – 21:00: 60 min (Monday schedule preparation)
 * - Sunday before 16:00 & Saturday: 180 min
 *
 * Includes battery optimization:
 * - Backs off to >= 30 minutes when power saver is active or battery is <= 15%.
 * - Detects deep night hours to avoid unnecessary radio wakeups.
 */
object SubstitutionScheduleHelper
{
    fun getTargetIntervalMinutes(
        now: kotlinx.datetime.LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        serverReportedInterval: Int? = null
    ): Long
    {
        val isSchoolDay = now.dayOfWeek != DayOfWeek.SATURDAY && now.dayOfWeek != DayOfWeek.SUNDAY
        val timeInMinutes = now.hour * 60 + now.minute

        val scheduleInterval: Long = when
        {
            isSchoolDay -> when
            {
                timeInMinutes in (7 * 60 + 30) until (16 * 60) -> 5L
                timeInMinutes in (6 * 60) until (7 * 60 + 30) -> 15L
                timeInMinutes in (5 * 60) until (6 * 60) -> 30L
                timeInMinutes in (16 * 60) until (19 * 60) -> 60L
                timeInMinutes in (3 * 60) until (4 * 60) -> 60L
                else -> 180L
            }
            now.dayOfWeek == DayOfWeek.SUNDAY -> when
            {
                timeInMinutes in (16 * 60) until (21 * 60) -> 60L
                else -> 180L
            }
            else -> 180L // Saturday
        }

        return if (serverReportedInterval != null && serverReportedInterval in 5..180)
        {
            minOf(scheduleInterval, serverReportedInterval.toLong())
        }
        else
        {
            scheduleInterval
        }
    }

    /**
     * Checks if the device is currently in a battery-constrained state
     * (power save mode enabled or battery level below 15%).
     */
    fun isBatteryConstrained(context: Context): Boolean
    {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (powerManager?.isPowerSaveMode == true) return true

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
        return batteryPct in 1..15
    }

    /**
     * Returns the effective interval in minutes after applying battery optimizations.
     * When battery is constrained, minimum interval is backed off to 30-60 minutes to protect device battery.
     */
    fun getEffectiveIntervalMinutes(
        context: Context,
        now: kotlinx.datetime.LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        serverReportedInterval: Int? = null
    ): Long
    {
        val target = getTargetIntervalMinutes(now, serverReportedInterval)
        return if (isBatteryConstrained(context))
        {
            maxOf(target, 30L)
        }
        else
        {
            target
        }
    }

    /**
     * Returns true if current time is deep night (22:00 to 05:30),
     * where background network activity should be minimized or postponed until morning.
     */
    fun isDeepNight(now: kotlinx.datetime.LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())): Boolean
    {
        val timeInMinutes = now.hour * 60 + now.minute
        return timeInMinutes >= (22 * 60) || timeInMinutes < (5 * 60 + 30)
    }
}
