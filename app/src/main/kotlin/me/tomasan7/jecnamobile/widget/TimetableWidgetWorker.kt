package me.tomasan7.jecnamobile.widget

import android.content.Context
import android.util.Log
import androidx.work.*
import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.WebJecnaClient
import kotlinx.coroutines.flow.first
import kotlinx.datetime.*
import me.tomasan7.jecnamobile.caching.SchoolYearPeriodParams
import me.tomasan7.jecnamobile.di.SubstitutionsCacheRepository
import me.tomasan7.jecnamobile.login.AuthRepository
import me.tomasan7.jecnamobile.substitutions.SubstitutionScheduleHelper
import me.tomasan7.jecnamobile.timetable.TimetableCacheRepository
import me.tomasan7.jecnamobile.timetable.TimetableRepository
import me.tomasan7.jecnamobile.util.UserProfileGroupHelper
import me.tomasan7.jecnamobile.util.settingsDataStore
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class TimetableWidgetWorker(
    private val appContext: Context,
    private val params: WorkerParameters,
    private val jecnaClient: JecnaClient,
    private val authRepository: AuthRepository,
    private val timetableRepository: TimetableRepository,
    private val timetableCacheRepository: TimetableCacheRepository,
    private val substitutionsCacheRepository: SubstitutionsCacheRepository
) : CoroutineWorker(appContext, params)
{
    override suspend fun doWork(): Result
    {
        Log.i(TAG, "TimetableWidgetWorker executing timetable & substitution sync...")

        if (!authRepository.exists())
        {
            Log.d(TAG, "User is not logged in. Skipping background widget sync.")
            return Result.success()
        }

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        val className = runCatching {
            timetableRepository.getStudentClassName()
                ?: TimetableWidgetRenderer.extractClassName(
                    try {
                        val p = SchoolYearPeriodParams(io.github.tomhula.jecnaapi.util.SchoolYear.current(), SchoolYearPeriodParams.CURRENT_PERIOD_ID)
                        timetableCacheRepository.getCache(p)?.data
                    } catch (e: Exception) {
                        null
                    }
                )
        }.getOrNull()

        val cachedSubEntry = if (!className.isNullOrBlank()) substitutionsCacheRepository.getCache(className) else null
        val cachedSubs = cachedSubEntry?.data
        val ageMinutes = cachedSubEntry?.timestamp?.let { (Clock.System.now() - it).inWholeMinutes } ?: Long.MAX_VALUE
        val effectiveInterval = SubstitutionScheduleHelper.getEffectiveIntervalMinutes(appContext, now, cachedSubs?.currentUpdateSchedule)

        // 1. Overnight battery saver: In deep night (22:00 - 05:30), avoid waking the radio if checked within 180 min
        if (SubstitutionScheduleHelper.isDeepNight(now) && ageMinutes < 180L)
        {
            Log.d(TAG, "Deep night mode. Skipping network refresh to preserve 100% battery.")
            TimetableWidgetUpdater.updateAllWidgets(appContext)
            scheduleNext(appContext, effectiveInterval)
            return Result.success()
        }

        // 2. Cache validity check: If data was refreshed recently within the schedule interval, skip network call
        if (ageMinutes < effectiveInterval)
        {
            Log.d(TAG, "Substitutions cache is still fresh ($ageMinutes min < $effectiveInterval min). Skipping network call.")
            TimetableWidgetUpdater.updateAllWidgets(appContext)
            val remainingDelay = maxOf(1L, effectiveInterval - ageMinutes)
            scheduleNext(appContext, remainingDelay)
            return Result.success()
        }

        try
        {
            if ((jecnaClient as? WebJecnaClient)?.autoLoginAuth == null)
            {
                (jecnaClient as? WebJecnaClient)?.autoLoginAuth = authRepository.get()
            }

            runCatching {
                UserProfileGroupHelper.fetchAndSyncUserGroup(appContext, jecnaClient)
            }

            var substitutionsChanged = false
            var latestReportedInterval: Int? = cachedSubs?.currentUpdateSchedule

            if (!className.isNullOrBlank())
            {
                try
                {
                    val realSubs = substitutionsCacheRepository.getRealAndCache(className)
                    latestReportedInterval = realSubs.currentUpdateSchedule
                    substitutionsChanged = (cachedSubs == null || cachedSubs.lastUpdated != realSubs.lastUpdated || cachedSubs.data != realSubs.data)
                }
                catch (e: Exception)
                {
                    Log.w(TAG, "Failed to refresh substitutions for $className: ${e.message}")
                }
            }

            // Sync base timetable if missing or substitutions changed
            val timetableParams = SchoolYearPeriodParams(io.github.tomhula.jecnaapi.util.SchoolYear.current(), SchoolYearPeriodParams.CURRENT_PERIOD_ID)
            val cachedTimetable = timetableCacheRepository.getCache(timetableParams)
            if (cachedTimetable == null || substitutionsChanged)
            {
                runCatching { timetableCacheRepository.getRealAndCache(timetableParams) }
            }

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timestamp = timeFormat.format(Date())

            appContext.settingsDataStore.updateData { current ->
                current.copy(
                    widgetSettings = current.widgetSettings.copy(lastRefreshedTimestamp = timestamp)
                )
            }

            TimetableWidgetUpdater.updateAllWidgets(appContext)
            Log.i(TAG, "TimetableWidgetWorker sync finished. Substitutions updated: $substitutionsChanged.")

            // Dynamically schedule next run based on the latest schedule rules and battery state
            val nextInterval = SubstitutionScheduleHelper.getEffectiveIntervalMinutes(appContext, now, latestReportedInterval)
            scheduleNext(appContext, nextInterval)

            return Result.success()
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Error in TimetableWidgetWorker", e)
            scheduleNext(appContext, 15L)
            return Result.retry()
        }
    }

    companion object
    {
        private const val TAG = "TimetableWidgetWorker"
        const val WORK_NAME = "me.tomasan7.jecnamobile.widget.TimetableWidgetWorker"
        const val DYNAMIC_WORK_NAME = "me.tomasan7.jecnamobile.widget.TimetableWidgetWorker.Dynamic"

        /**
         * Schedules the next dynamic one-time sync with precise interval.
         */
        fun scheduleNext(context: Context, delayMinutes: Long)
        {
            val safeDelay = maxOf(1L, delayMinutes)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<TimetableWidgetWorker>()
                .setInitialDelay(safeDelay, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            Log.i(TAG, "Scheduling next dynamic timetable & substitution sync in $safeDelay minutes...")

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    DYNAMIC_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
        }

        /**
         * Schedules periodic keepalive and triggers initial dynamic sync.
         */
        fun schedulePeriodic(context: Context)
        {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<TimetableWidgetWorker>(
                15.minutes.toJavaDuration(),
                5.minutes.toJavaDuration()
            )
                .setConstraints(constraints)
                .build()

            Log.i(TAG, "Scheduling periodic 15-min keepalive for timetable widget sync...")

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )

            // Trigger immediate dynamic evaluation
            val initialDelay = SubstitutionScheduleHelper.getEffectiveIntervalMinutes(context)
            scheduleNext(context, initialDelay)
        }

        fun cancel(context: Context)
        {
            Log.i(TAG, "Cancelling periodic & dynamic timetable widget sync...")
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(DYNAMIC_WORK_NAME)
        }
    }
}
