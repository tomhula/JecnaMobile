package me.tomasan7.jecnamobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import me.tomasan7.jecnamobile.gradenotifications.GradeCheckerWorker
import javax.inject.Inject

@HiltAndroidApp
class JecnaMobileApplication : Application(), Configuration.Provider
{
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate()
    {
        super.onCreate()
        createNotificationChannels()
        GradeCheckerWorker.scheduleWorkerIfNotificationsEnabled(this)
    }

    private fun createNotificationChannels()
    {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(createGradeNotificationChannel())
    }

    private fun createGradeNotificationChannel(): NotificationChannel
    {
        val channelName = getString(R.string.notification_channel_grade_name)
        val channelDescription = getString(R.string.notification_channel_grade_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT

        val notificationChannel = NotificationChannel(NotificationChannelIds.GRADE, channelName, importance)
        notificationChannel.description = channelDescription

        return notificationChannel
    }

    companion object
    {
        const val NETWORK_AVAILABLE_ACTION = "me.tomasan7.jecnamobile.NETWORK_AVAILABLE"
        const val SUCCESSFUL_LOGIN_ACTION = "me.tomasan7.jecnamobile.SUCCESSFULL_LOGIN"
        const val SUCCESSFUL_LOGIN_FIRST_EXTRA = "first"
        const val GRADE_CHECKER_WORKER_ID = "me.tomasan7.jecnamobile.gradenotifications.GradeCheckerWorker"

        object NotificationChannelIds
        {
            const val GRADE = "me.tomasan7.jecnamobile.grade_notification_channel"
        }
    }
}
