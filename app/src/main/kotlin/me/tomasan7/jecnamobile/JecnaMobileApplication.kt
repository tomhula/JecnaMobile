package me.tomasan7.jecnamobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import me.tomasan7.jecnamobile.di.appModule
import me.tomasan7.jecnamobile.di.cacheRepositoriesModule
import me.tomasan7.jecnamobile.di.repositoriesModule
import me.tomasan7.jecnamobile.di.viewModelsModule
import me.tomasan7.jecnamobile.gradenotifications.GradeCheckerWorker
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.GlobalContext.startKoin

class JecnaMobileApplication : Application()
{
    override fun onCreate()
    {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@JecnaMobileApplication)
            workManagerFactory()

            modules(appModule, repositoriesModule, cacheRepositoriesModule, viewModelsModule)
        }
        
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
