package io.ipoli.android

import android.annotation.SuppressLint
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import com.crashlytics.android.Crashlytics
import com.evernote.android.job.JobManager
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.leakcanary.LeakCanary
import io.fabric.sdk.android.Fabric
import io.ipoli.android.common.di.*
import io.ipoli.android.common.job.myPoliJobCreator
import space.traversal.kapsule.transitive
import timber.log.Timber


/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 7/7/17.
 */

class myPoliApp : Application() {

    private lateinit var module: Module

    companion object {
//        lateinit var refWatcher: RefWatcher

        lateinit var instance: myPoliApp

        fun module(context: Context) =
            (context.applicationContext as myPoliApp).module
    }

    @SuppressLint("NewApi")
    override fun onCreate() {
        super.onCreate()

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }

        AndroidThreeTen.init(this)

        if (!BuildConfig.DEBUG) {

            Fabric.with(
                Fabric.Builder(this)
                    .kits(Crashlytics())
                    .debuggable(BuildConfig.DEBUG)
                    .build()
            )


        } else {
            Timber.plant(Timber.DebugTree())
            //            BlockCanary.install(this, object : BlockCanaryContext() {
//                override fun provideBlockThreshold(): Int {
//                    return 500
//                }
//            }).startDate()

            //            refWatcher = LeakCanary.install(this)
        }

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val pId = preferences.getString(Constants.KEY_PLAYER_ID, null)
        pId?.let {
            // Check for iPoli Player
            if (it.contains("-")) {
                preferences.edit().remove(Constants.KEY_PLAYER_ID).commit()
            }
        }

        JobManager.create(this).addJobCreator(myPoliJobCreator())

        module = Module(
            androidModule = MainAndroidModule(this),
            repositoryModule = AndroidRepositoryModule(this),
            useCaseModule = MainUseCaseModule(),
            presenterModule = AndroidPresenterModule(),
            stateStoreModule = AndroidStateStoreModule()
        ).transitive()

        instance = this

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channels = notificationManager.notificationChannels
            if (channels.firstOrNull { it.id == Constants.REMINDERS_NOTIFICATION_CHANNEL_ID } == null) {
                notificationManager.createNotificationChannel(createReminderChannel())
            }
            if (channels.firstOrNull { it.id == Constants.PLAN_DAY_NOTIFICATION_CHANNEL_ID } == null) {
                notificationManager.createNotificationChannel(createPlanDayChannel())
            }
        }

//        TinyDancer.create().show(this)
    }

    @SuppressLint("NewApi")
    private fun createPlanDayChannel(): NotificationChannel {
        val channel = NotificationChannel(
            Constants.PLAN_DAY_NOTIFICATION_CHANNEL_ID,
            Constants.PLAN_DAY_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = "Notifications to plan your day"
        channel.enableLights(true)
        channel.enableVibration(true)
        channel.setSound(
            Uri.parse("android.resource://" + packageName + "/" + R.raw.notification),
            Notification.AUDIO_ATTRIBUTES_DEFAULT
        )
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        return channel
    }

    @SuppressLint("NewApi")
    private fun createReminderChannel(): NotificationChannel {
        val channel = NotificationChannel(
            Constants.REMINDERS_NOTIFICATION_CHANNEL_ID,
            Constants.REMINDERS_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = "Reminder notifications"
        channel.enableLights(true)
        channel.enableVibration(true)
        channel.setSound(
            Uri.parse("android.resource://" + packageName + "/" + R.raw.notification),
            Notification.AUDIO_ATTRIBUTES_DEFAULT
        )
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        return channel
    }
}