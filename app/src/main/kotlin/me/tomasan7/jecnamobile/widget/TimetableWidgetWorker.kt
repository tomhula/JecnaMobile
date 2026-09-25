package me.tomasan7.jecnamobile.widget

import android.content.Context
import android.util.Log
import androidx.work.*
import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.WebJecnaClient
import kotlinx.coroutines.flow.first
import kotlinx.datetime.*
import me.tomasan7.jecnamobile.JecnaMobileApplication
import me.tomasan7.jecnamobile.caching.SchoolYearPeriodParams
import me.tomasan7.jecnamobile.di.SubstitutionsCacheRepository
import me.tomasan7.jecnamobile.login.AuthRepository
import me.tomasan7.jecnamobile.timetable.TimetableCacheRepository
import me.tomasan7.jecnamobile.timetable.TimetableRepository
import me.tomasan7.jecnamobile.util.settingsDataStore
import java.text.SimpleDateFormat
import java.util.*
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
        Log.i(TAG, "TimetableWidgetWorker running periodic sync...")

        if (!authRepository.exists())
        {
            Log.d(TAG, "User is not logged in. Skipping background widget sync.")
            return Result.success()
        }

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val isSchoolDay = now.dayOfWeek != DayOfWeek.SATURDAY && now.dayOfWeek != DayOfWeek.SUNDAY
        val isSchoolHours = now.hour in 6..16

        // Ultra-low battery optimization: skip network calls on weekends and outside 06:30 - 16:30
        if (!isSchoolDay || !isSchoolHours)
        {
            Log.d(TAG, "Outside school hours. Skipping network refresh to save battery.")
            TimetableWidgetUpdater.updateAllWidgets(appContext)
            return Result.success()
        }

        try
        {
            if ((jecnaClient as? WebJecnaClient)?.autoLoginAuth == null)
            {
                (jecnaClient as? WebJecnaClient)?.autoLoginAuth = authRepository.get()
            }

            val className = timetableRepository.getStudentClassName()
                ?: TimetableWidgetRenderer.extractClassName(
                    try {
                        val params = SchoolYearPeriodParams(io.github.tomhula.jecnaapi.util.SchoolYear.current(), SchoolYearPeriodParams.CURRENT_PERIOD_ID)
                        timetableCacheRepository.getCache(params)?.data
                    } catch (e: Exception) {
                        null
                    }
                )
            if (!className.isNullOrBlank())
            {
                substitutionsCacheRepository.getRealAndCache(className)
            }
            val params = SchoolYearPeriodParams(io.github.tomhula.jecnaapi.util.SchoolYear.current(), SchoolYearPeriodParams.CURRENT_PERIOD_ID)
            timetableCacheRepository.getRealAndCache(params)

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timestamp = timeFormat.format(Date())

            appContext.settingsDataStore.updateData { current ->
                current.copy(
                    widgetSettings = current.widgetSettings.copy(lastRefreshedTimestamp = timestamp)
                )
            }

            TimetableWidgetUpdater.updateAllWidgets(appContext)
            Log.i(TAG, "TimetableWidgetWorker finished successfully. Widgets updated.")
            return Result.success()
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Error in TimetableWidgetWorker", e)
            return Result.retry()
        }
    }

    companion object
    {
        private const val TAG = "TimetableWidgetWorker"
        const val WORK_NAME = "me.tomasan7.jecnamobile.widget.TimetableWidgetWorker"

        fun schedulePeriodic(context: Context)
        {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<TimetableWidgetWorker>(
                30.minutes.toJavaDuration(),
                10.minutes.toJavaDuration()
            )
                .setConstraints(constraints)
                .build()

            Log.i(TAG, "Scheduling periodic 30-min timetable widget sync...")

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
        }

        fun cancel(context: Context)
        {
            Log.i(TAG, "Cancelling periodic timetable widget sync...")
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
