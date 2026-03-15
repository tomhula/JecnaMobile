package me.tomasan7.jecnamobile.widgets.timetable

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.WebJecnaClient
import io.github.tomhula.jecnaapi.util.SchoolYear
import me.tomasan7.jecnamobile.caching.CacheRepository
import me.tomasan7.jecnamobile.caching.SchoolYearPeriodParams
import me.tomasan7.jecnamobile.login.AuthRepository
import me.tomasan7.jecnamobile.timetable.TimetableData
import me.tomasan7.jecnamobile.widgets.timetable.nextclass.NextClassWidget
import me.tomasan7.jecnamobile.widgets.timetable.nextclass.NextClassWidgetStateDefinition
import me.tomasan7.jecnamobile.widgets.timetable.full.TimetableWidget
import me.tomasan7.jecnamobile.widgets.timetable.full.TimetableWidgetStateDefinition

private const val LOG_TAG = "TimetableWidgetWorker"

@HiltWorker
internal class TimetableWidgetWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val jecnaClient: JecnaClient,
    private val authRepository: AuthRepository,
    private val timetableCacheRepository: CacheRepository<TimetableData, SchoolYearPeriodParams>
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            if ((jecnaClient as WebJecnaClient).autoLoginAuth == null) {
                jecnaClient.autoLoginAuth = authRepository.get()
            }

            val params = SchoolYearPeriodParams(SchoolYear.current(), SchoolYearPeriodParams.CURRENT_PERIOD_ID)

            val cachedData = timetableCacheRepository.getCache(params)

            if (cachedData != null) {
                updateWidgetState(
                    timetablePage = cachedData.data.page,
                    substitutions = cachedData.data.substitutions,
                    lastUpdated = cachedData.timestamp.toEpochMilliseconds(),
                    isLoading = true,
                    isManualRefresh = false,
                    error = null
                )
            }

            val timetableData = try {
                timetableCacheRepository.getRealAndCache(params)
            } catch (networkException: Exception) {
                Log.w(LOG_TAG, "Network fetch failed.", networkException)

                if (cachedData != null) {
                    updateWidgetState(
                        timetablePage = cachedData.data.page,
                        substitutions = cachedData.data.substitutions,
                        lastUpdated = cachedData.timestamp.toEpochMilliseconds(),
                        isLoading = false,
                        isManualRefresh = false,
                        error = null
                    )
                    return Result.success()
                } else {
                    throw networkException
                }
            }

            updateWidgetState(
                timetablePage = timetableData.page,
                substitutions = timetableData.substitutions,
                lastUpdated = System.currentTimeMillis(),
                isLoading = false,
                isManualRefresh = false,
                error = null
            )

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()

            val errorMessage = when (e) {
                is io.ktor.util.network.UnresolvedAddressException,
                is java.net.UnknownHostException,
                is java.net.ConnectException,
                is java.net.SocketTimeoutException,
                is io.ktor.client.plugins.HttpRequestTimeoutException -> context.getString(me.tomasan7.jecnamobile.R.string.no_internet_connection)
                else -> e.message
            }

            updateWidgetState(
                timetablePage = null,
                substitutions = null,
                lastUpdated = 0L,
                isLoading = false,
                isManualRefresh = false,
                error = errorMessage
            )

            Result.failure()
        }
    }

    private suspend fun updateWidgetState(
        timetablePage: io.github.tomhula.jecnaapi.data.timetable.TimetablePage?,
        substitutions: me.tomasan7.jecnamobile.timetable.SubstitutionData?,
        lastUpdated: Long,
        isLoading: Boolean,
        isManualRefresh: Boolean,
        error: String?
    ) {
        try {
            val manager = GlanceAppWidgetManager(context)
            
            val timetableGlanceIds = manager.getGlanceIds(TimetableWidget::class.java)
            val nextClassGlanceIds = manager.getGlanceIds(NextClassWidget::class.java)

            timetableGlanceIds.forEach { glanceId ->
                updateAppWidgetState(
                    context = context,
                    definition = TimetableWidgetStateDefinition,
                    glanceId = glanceId
                ) {
                    TimetableWidgetState(
                        timetablePage = timetablePage,
                        substitutions = substitutions,
                        lastUpdated = lastUpdated,
                        isLoading = isLoading,
                        isManualRefresh = isManualRefresh,
                        error = error
                    )
                }
            }

            nextClassGlanceIds.forEach { glanceId ->
                updateAppWidgetState(
                    context = context,
                    definition = NextClassWidgetStateDefinition,
                    glanceId = glanceId
                ) {
                    TimetableWidgetState(
                        timetablePage = timetablePage,
                        substitutions = substitutions,
                        lastUpdated = lastUpdated,
                        isLoading = isLoading,
                        isManualRefresh = isManualRefresh,
                        error = error
                    )
                }
            }

            TimetableWidget().run {
                timetableGlanceIds.forEach { update(context, it) }
            }
            NextClassWidget().run {
                nextClassGlanceIds.forEach { update(context, it) }
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "updateWidgetState: error updating widgets", e)
        }
    }

    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequestBuilder<TimetableWidgetWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setConstraints(constraints)
                    .build()
            )
        }
    }
}
