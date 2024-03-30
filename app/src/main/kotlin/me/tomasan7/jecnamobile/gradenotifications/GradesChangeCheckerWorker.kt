package me.tomasan7.jecnamobile.gradenotifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.tomasan7.jecnaapi.JecnaClient
import me.tomasan7.jecnaapi.data.grade.GradesPage
import me.tomasan7.jecnaapi.data.grade.Subject
import me.tomasan7.jecnaapi.util.SchoolYear
import me.tomasan7.jecnaapi.util.SchoolYearHalf
import me.tomasan7.jecnamobile.JecnaMobileApplication
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.gradenotifications.change.GradesChange
import me.tomasan7.jecnamobile.gradenotifications.change.GradesChangeChecker
import me.tomasan7.jecnamobile.grades.CacheGradesRepository
import me.tomasan7.jecnamobile.login.AuthRepository
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

@HiltWorker
class GradeCheckerWorker @AssistedInject constructor(
    @Assisted
    private val appContext: Context,
    @Assisted
    private val params: WorkerParameters,
    private val jecnaClient: JecnaClient,
    private val authRepository: AuthRepository,
    private val cacheGradesRepository: CacheGradesRepository,
    private val gradeChangeChecker: GradesChangeChecker
) : CoroutineWorker(appContext, params)
{
    private val notificationManagerCompat = NotificationManagerCompat.from(appContext)

    override suspend fun doWork(): Result
    {
        Log.i(LOG_TAG, "Checking for grade changes...")

        if (jecnaClient.autoLoginAuth == null)
            jecnaClient.autoLoginAuth = authRepository.get()

        val cachedGradesPage = cacheGradesRepository.getCachedGrades()?.data
        val realGradesPage = try
        {
            cacheGradesRepository.getRealGrades()
        }
        catch (e: Exception)
        {
            e.printStackTrace()
            return Result.failure()
        }

        Log.d(LOG_TAG, "Successfully fetched real grades")

        /* Exit if there is no cache, but after refreshing it */
        if (cachedGradesPage == null)
        {
            Log.d(LOG_TAG, "No cache found. Exiting...")
            return Result.success()
        }
        /* Non current school year and school year half are not relevant */
        else if (cachedGradesPage.selectedSchoolYear != SchoolYear.current() || cachedGradesPage.selectedSchoolYearHalf != SchoolYearHalf.current())
        {
            Log.d(LOG_TAG, "Not current school year or school year half. Exiting...")
            return Result.success()
        }

        val changes = findChanges(cachedGradesPage, realGradesPage)

        for ((subject, subjectChanges) in changes)
            for (change in subjectChanges)
                sendGradeChangeNotification(subject, change)

        return Result.success()
    }

    private fun findChanges(
        oldGradesPage: GradesPage,
        newGradesPage: GradesPage
    ): Map<Subject, Set<GradesChange>>
    {
        val result = mutableMapOf<Subject, Set<GradesChange>>()

        for (oldSubject in oldGradesPage.subjects)
            for (subjectPart in oldSubject.grades.subjectParts)
            {
                val oldSubjectGrades = oldSubject.grades[subjectPart]!!
                val newSubjectGrades = newGradesPage[oldSubject.name]?.grades?.get(subjectPart) ?: continue
                val changes = gradeChangeChecker.checkForChanges(oldSubjectGrades, newSubjectGrades)

                if (changes.isNotEmpty())
                    result[oldSubject] = changes
            }

        return result
    }

    private fun sendGradeChangeNotification(subject: Subject, change: GradesChange)
    {
        val title: String
        val text: String

        when (change)
        {
            is GradesChange.NewGrade -> with(change) {
                title = appContext.getString(R.string.notification_grade_new_title, subject.name.full)
                text = appContext.getString(R.string.notification_grade_new_text, newGrade.valueChar.toString(), newGrade.description)
            }
            is GradesChange.GradeChange -> with(change) {
                if (oldGrade.value != newGrade.value)
                {
                    title = appContext.getString(R.string.notification_grade_value_change_title, subject.name.full)
                    text = appContext.getString(R.string.notification_grade_value_change_text, oldGrade.valueChar.toString(), newGrade.valueChar.toString(), newGrade.description)
                }
                else
                {
                    title = appContext.getString(R.string.notification_grade_size_change_title, subject.name.full)
                    text = appContext.getString(R.string.notification_grade_size_change_text, oldGrade.small.sizeString(), newGrade.small.sizeString(), newGrade.description)
                }
            }
            is GradesChange.GradeRemoved -> with(change) {
                title = appContext.getString(R.string.notification_grade_remove_title, subject.name.full)
                text = appContext.getString(R.string.notification_grade_remove_text, removedGrade.description)
            }
        }

        Log.d(LOG_TAG, "Sending grade notification: $title - $text")

        sendGradeNotification(title, text, subject.hashCode() + change.hashCode())
    }

    private fun Boolean.sizeString() = if (this) appContext.getString(R.string.notification_grade_size_small) else appContext.getString(R.string.notification_grade_size_big)

    private fun sendGradeNotification(title: String, text: String, id: Int)
    {
        val builder = NotificationCompat.Builder(appContext, JecnaMobileApplication.Companion.NotificationChannelIds.GRADE)
            .setSmallIcon(R.drawable.ic_launcher_jecna_foreground)
            .setContentTitle(title)
            .setContentText(text)

        if (checkForNotificationPermission())
            notificationManagerCompat.notify(id, builder.build())
    }

    private fun checkForNotificationPermission(): Boolean
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
            return true
        return ActivityCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    companion object
    {
        private const val LOG_TAG = "GradeCheckerWorker"

        fun scheduleWorker(context: Context)
        {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<GradeCheckerWorker>(1.hours.toJavaDuration(), 30.minutes.toJavaDuration())
                .setConstraints(constraints)
                .build()

            Log.i(LOG_TAG, "Scheduling grade checker worker...")

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(JecnaMobileApplication.GRADE_CHECKER_WORKER_ID, ExistingPeriodicWorkPolicy.UPDATE, workRequest)
        }

        /* TODO: REMOVE: DEBUG ONLY */
        fun scheduleImmediate(context: Context)
        {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workRequest = OneTimeWorkRequestBuilder<GradeCheckerWorker>()
                .setConstraints(constraints)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            Log.i(LOG_TAG, "Scheduling testing one time grade checker worker...")
            WorkManager.getInstance(context)
                .enqueue(workRequest)
        }
    }
}
