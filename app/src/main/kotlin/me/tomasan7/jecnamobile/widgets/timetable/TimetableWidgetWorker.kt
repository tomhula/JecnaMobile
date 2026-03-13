package me.tomasan7.jecnamobile.widgets.timetable

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
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
import me.tomasan7.jecnamobile.widgets.shared.SharedTimetableWidgetState
import me.tomasan7.jecnamobile.widgets.nextclass.NextClassWidget
import me.tomasan7.jecnamobile.widgets.nextclass.NextClassWidgetStateDefinition

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
            var isFromCache = false
            var timestamp = System.currentTimeMillis()

            val timetableData = try {
                timetableCacheRepository.getRealAndCache(params)
            } catch (networkException: Exception) {
                Log.w(LOG_TAG, "Network fetch failed, attempting to read from cache.", networkException)

                val cachedData = timetableCacheRepository.getCache(params)

                if (cachedData != null) {
                    isFromCache = true
                    timestamp = cachedData.timestamp.toEpochMilliseconds()
                    cachedData.data
                } else {
                    throw networkException
                }
            }

            updateWidgetState { currentState ->
                currentState.copy(
                    timetablePage = timetableData.page,
                    substitutions = timetableData.substitutions,
                    lastUpdated = timestamp,
                    isLoading = false,
                    isManualRefresh = false,
                    error = if (currentState.isManualRefresh && isFromCache) "Offline: Showing cached data" else null
                )
            }

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

            updateWidgetState { currentState ->
                if (currentState.isManualRefresh) {
                    currentState.copy(
                        isLoading = false,
                        isManualRefresh = false,
                        error = errorMessage
                    )
                } else {
                    currentState.copy(
                        isLoading = false,
                        isManualRefresh = false
                    )
                }
            }

            Result.failure()
        }
    }

    private suspend fun updateWidgetState(updateBlock: (SharedTimetableWidgetState) -> SharedTimetableWidgetState) {
        try {
            val manager = GlanceAppWidgetManager(context)
            
            val timetableGlanceIds = manager.getGlanceIds(TimetableWidget::class.java)
            val nextClassGlanceIds = manager.getGlanceIds(NextClassWidget::class.java)
            val allGlanceIds = timetableGlanceIds.map { it to TimetableWidgetStateDefinition } +
                              nextClassGlanceIds.map { it to NextClassWidgetStateDefinition }

            allGlanceIds.forEach { (glanceId, stateDefinition) ->
                updateAppWidgetState(
                    context = context,
                    definition = stateDefinition,
                    glanceId = glanceId
                ) { currentState ->
                    updateBlock(currentState)
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
}
