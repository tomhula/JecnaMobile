package me.tomasan7.jecnamobile.substitutions.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.chrynan.parcelable.core.putExtra
import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.WebJecnaClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import me.tomasan7.jecnamobile.EXTRA_NAVIGATE_TO
import me.tomasan7.jecnamobile.JecnaMobileApplication
import me.tomasan7.jecnamobile.MainActivity
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.di.SubstitutionsCacheRepository
import me.tomasan7.jecnamobile.login.AuthRepository
import me.tomasan7.jecnamobile.navigation.AppDestination
import me.tomasan7.jecnamobile.substitutions.notifications.change.SubstitutionChange
import me.tomasan7.jecnamobile.substitutions.notifications.change.SubstitutionChangeChecker
import me.tomasan7.jecnamobile.timetable.TimetableRepository
import me.tomasan7.jecnamobile.util.settingsDataStore
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class SubstitutionCheckerWorker(
    private val appContext: Context,
    private val params: WorkerParameters,
    private val jecnaClient: JecnaClient,
    private val authRepository: AuthRepository,
    private val timetableRepository: TimetableRepository,
    private val substitutionsCacheRepository: SubstitutionsCacheRepository,
    private val substitutionChangeChecker: SubstitutionChangeChecker
) : CoroutineWorker(appContext, params)
{
    private val notificationManagerCompat = NotificationManagerCompat.from(appContext)

    override suspend fun doWork(): Result
    {
        if (!notificationsEnabled(appContext))
        {
            Log.i(LOG_TAG, "Substitution notifications are not enabled. Exiting...")
            return Result.success()
        }

        Log.i(LOG_TAG, "Checking for substitution changes in student's class...")

        if ((jecnaClient as WebJecnaClient).autoLoginAuth == null)
            jecnaClient.autoLoginAuth = authRepository.get()

        val className = try
        {
            timetableRepository.getStudentClassName() ?: jecnaClient.getStudentProfile().className
        }
        catch (e: Exception)
        {
            Log.e(LOG_TAG, "Failed to resolve student class name", e)
            return Result.failure()
        }

        if (className.isNullOrBlank())
        {
            Log.w(LOG_TAG, "No class name found for student. Exiting...")
            return Result.success()
        }

        Log.d(LOG_TAG, "Checking substitutions for class: $className")

        val cachedSubstitutions = substitutionsCacheRepository.getCache(className)?.data

        val realSubstitutions = try
        {
            substitutionsCacheRepository.getRealAndCache(className)
        }
        catch (e: Exception)
        {
            Log.e(LOG_TAG, "Failed to fetch real substitutions for class $className", e)
            return Result.failure()
        }

        Log.d(LOG_TAG, "Successfully fetched substitutions for class $className")

        // Exit on first run when prime cache is established
        if (cachedSubstitutions == null)
        {
            Log.d(LOG_TAG, "No previous cache found. Saved initial cache. Exiting...")
            return Result.success()
        }

        val changes = substitutionChangeChecker.checkForChanges(cachedSubstitutions, realSubstitutions)

        if (changes.isNotEmpty())
        {
            Log.i(LOG_TAG, "Found ${changes.size} changes for class $className. Sending notification...")
            sendSubstitutionNotification(className, changes)
        }
        else
        {
            Log.d(LOG_TAG, "No new changes found for class $className.")
        }

        return Result.success()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun sendSubstitutionNotification(className: String, changes: List<SubstitutionChange>)
    {
        if (changes.isEmpty()) return

        val title = appContext.getString(R.string.notification_substitution_title, className)
        val text: String
        val bigText = StringBuilder()

        if (changes.size == 1)
        {
            val change = changes.first()
            text = formatChangeSummary(change)
            bigText.append(text)
        }
        else
        {
            text = appContext.getString(R.string.notification_substitution_multiple_changes, changes.size)
            for ((index, change) in changes.withIndex())
            {
                if (index > 0) bigText.append("\n")
                bigText.append("• ").append(formatChangeSummary(change))
            }
        }

        postNotification(title, text, bigText.toString())
    }

    private fun formatChangeSummary(change: SubstitutionChange): String
    {
        return when (change)
        {
            is SubstitutionChange.LessonChange ->
            {
                val dateStr = formatDate(change.date)
                appContext.getString(R.string.notification_substitution_lesson_change, dateStr, change.hour, change.newChange.text.replace("\n", " "))
            }
            is SubstitutionChange.LessonChangeRemoved ->
            {
                val dateStr = formatDate(change.date)
                appContext.getString(R.string.notification_substitution_lesson_cancelled, dateStr, change.hour, change.removedChange.text.replace("\n", " "))
            }
            is SubstitutionChange.TakesPlaceChange ->
            {
                val dateStr = formatDate(change.date)
                appContext.getString(R.string.notification_substitution_takes_place, dateStr, change.newTakesPlace.replace("\n", " "))
            }
            is SubstitutionChange.NewAnnouncement ->
            {
                val content = change.announcement.textContent ?: change.announcement.author
                "Oznámení: ${content.replace("\n", " ")}"
            }
        }
    }

    private fun formatDate(date: String): String
    {
        val localDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return date
        val today = LocalDate.now()
        return when (localDate)
        {
            today -> appContext.getString(R.string.today)
            today.plusDays(1) -> appContext.getString(R.string.tomorrow)
            else -> "${localDate.dayOfMonth}. ${localDate.monthValue}."
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun postNotification(title: String, text: String, bigText: String)
    {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(
                name = EXTRA_NAVIGATE_TO,
                value = AppDestination.Substitution,
            )
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            appContext,
            SUBSTITUTION_NOTIFICATION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(appContext, JecnaMobileApplication.Companion.NotificationChannelIds.SUBSTITUTION)
            .setSmallIcon(R.mipmap.ic_launcher_jecna)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (notificationsEnabled(appContext))
            notificationManagerCompat.notify(SUBSTITUTION_NOTIFICATION_ID, builder.build())
    }

    companion object
    {
        private const val LOG_TAG = "SubstitutionCheckerWorker"
        private const val SUBSTITUTION_NOTIFICATION_ID = 2001
        private const val SUBSTITUTION_NOTIFICATION_REQUEST_CODE = 101

        /**
         * Checks if substitution notifications are enabled in settings and if app has permission.
         * If so, schedules periodic worker every 15 minutes; otherwise cancels it.
         */
        fun scheduleWorkerIfNotificationsEnabled(context: Context)
        {
            if (!notificationsEnabled(context))
            {
                Log.i(LOG_TAG, "Substitution notifications not enabled. Cancelling worker...")
                WorkManager.getInstance(context).cancelUniqueWork(JecnaMobileApplication.SUBSTITUTION_CHECKER_WORKER_ID)
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<SubstitutionCheckerWorker>(
                15.minutes.toJavaDuration(),
                5.minutes.toJavaDuration()
            )
                .setConstraints(constraints)
                .build()

            Log.i(LOG_TAG, "Scheduling substitution checker worker (15 min)...")

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    JecnaMobileApplication.SUBSTITUTION_CHECKER_WORKER_ID,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
        }

        private fun notificationsEnabled(context: Context): Boolean
        {
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            else
                true

            val settingsEnabled = runCatching {
                runBlocking {
                    val settings = context.settingsDataStore.data.first()
                    val masterEnabled = settings.notificationsEnabled ?: settings.hasSeenWelcomeScreen
                    masterEnabled && settings.substitutionNotificationsEnabled
                }
            }.getOrElse { false }

            return hasPermission && settingsEnabled
        }
    }
}
