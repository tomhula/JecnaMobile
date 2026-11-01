package me.tomasan7.jecnamobile.widgets.grades

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
import io.github.tomhula.jecnaapi.data.grade.GradesPage
import io.github.tomhula.jecnaapi.util.SchoolYear
import io.github.tomhula.jecnaapi.util.SchoolYearHalf
import me.tomasan7.jecnamobile.caching.CacheRepository
import me.tomasan7.jecnamobile.caching.SchoolYearHalfParams
import me.tomasan7.jecnamobile.login.AuthRepository
import me.tomasan7.jecnamobile.widgets.grades.average.GradesWidget
import me.tomasan7.jecnamobile.widgets.grades.average.AverageGradesWidgetState
import me.tomasan7.jecnamobile.widgets.grades.average.GradesWidgetStateDefinition
import me.tomasan7.jecnamobile.widgets.grades.recent.RecentGradesWidget
import me.tomasan7.jecnamobile.widgets.grades.recent.RecentGradesWidgetState
import me.tomasan7.jecnamobile.widgets.grades.recent.RecentGradesWidgetStateDefinition

private const val LOG_TAG = "GradesWidgetWorkerShared"

@HiltWorker
internal class GradesWidgetWorkerShared @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val jecnaClient: JecnaClient,
    private val authRepository: AuthRepository,
    private val gradesCacheRepository: CacheRepository<GradesPage, SchoolYearHalfParams>
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            if ((jecnaClient as WebJecnaClient).autoLoginAuth == null) {
                jecnaClient.autoLoginAuth = authRepository.get()
            }

            val currentSchoolYear = SchoolYear.current()
            val currentSchoolYearHalf = SchoolYearHalf.current()
            val params = SchoolYearHalfParams(currentSchoolYear, currentSchoolYearHalf)

            val cachedData = gradesCacheRepository.getCache(params)

            if (cachedData != null) {
                updateWidgetState(
                    gradesPage = cachedData.data,
                    lastUpdated = cachedData.timestamp.toEpochMilliseconds(),
                    isLoading = true,
                    isManualRefresh = false,
                    error = null
                )
            }

            val gradesPage = try {
                gradesCacheRepository.getRealAndCache(params)
            } catch (networkException: Exception) {
                Log.w(LOG_TAG, "Network fetch failed.", networkException)
                networkException.printStackTrace()

                if (cachedData != null) {
                    updateWidgetState(
                        gradesPage = cachedData.data,
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
                gradesPage = gradesPage,
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
                gradesPage = null,
                lastUpdated = 0L,
                isLoading = false,
                isManualRefresh = false,
                error = errorMessage
            )

            Result.failure()
        }
    }

    private suspend fun updateWidgetState(
        gradesPage: GradesPage?,
        lastUpdated: Long,
        isLoading: Boolean,
        isManualRefresh: Boolean,
        error: String?
    ) {
        try {
            val manager = GlanceAppWidgetManager(context)

            val gradesWidgetGlanceIds = manager.getGlanceIds(GradesWidget::class.java)
            val recentGradesWidgetGlanceIds = manager.getGlanceIds(RecentGradesWidget::class.java)

            gradesWidgetGlanceIds.forEach { glanceId ->
                updateAppWidgetState(
                    context = context,
                    definition = GradesWidgetStateDefinition,
                    glanceId = glanceId
                ) {
                    AverageGradesWidgetState(
                        gradesPage = gradesPage,
                        lastUpdated = lastUpdated,
                        isLoading = isLoading,
                        isManualRefresh = isManualRefresh,
                        error = error
                    )
                }
            }

            recentGradesWidgetGlanceIds.forEach { glanceId ->
                updateAppWidgetState(
                    context = context,
                    definition = RecentGradesWidgetStateDefinition,
                    glanceId = glanceId
                ) {
                    RecentGradesWidgetState(
                        gradesPage = gradesPage,
                        lastUpdated = lastUpdated,
                        isLoading = isLoading,
                        isManualRefresh = isManualRefresh,
                        error = error
                    )
                }
            }

            GradesWidget().run {
                gradesWidgetGlanceIds.forEach { update(context, it) }
            }
            RecentGradesWidget().run {
                recentGradesWidgetGlanceIds.forEach { update(context, it) }
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
                OneTimeWorkRequestBuilder<GradesWidgetWorkerShared>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setConstraints(constraints)
                    .build()
            )
        }
    }
}
