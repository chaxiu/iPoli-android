package mypoli.android.timer.job

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.support.v4.app.NotificationCompat
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import mypoli.android.Constants
import mypoli.android.MainActivity
import mypoli.android.R
import mypoli.android.common.datetime.Interval
import mypoli.android.common.datetime.Minute
import mypoli.android.common.di.ControllerModule
import mypoli.android.common.di.SimpleModule
import mypoli.android.myPoliApp
import mypoli.android.pet.AndroidPetAvatar
import mypoli.android.quest.TimeRange
import mypoli.android.quest.receiver.CompleteQuestReceiver
import space.traversal.kapsule.Injects
import space.traversal.kapsule.Kapsule
import java.util.*


/**
 * Created by Venelin Valkov <venelin@ipoli.io>
 * on 01/22/2018.
 */
class TimerCompleteNotificationJob : Job(), Injects<ControllerModule> {

    @SuppressLint("NewApi")
    override fun onRunJob(params: Job.Params): Job.Result {

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val kap = Kapsule<SimpleModule>()
        val questRepository by kap.required { questRepository }
        val findPetUseCase by kap.required { findPetUseCase }
        kap.inject(myPoliApp.simpleModule(context))

        val questId = params.extras.getString("questId", "")
        require(questId.isNotEmpty())
        val quest = questRepository.findById(questId)
        val pet = findPetUseCase.execute(Unit)

        val petAvatar = AndroidPetAvatar.valueOf(pet.avatar.name)

        val notificationBuilder =
            NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_small)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, petAvatar.headImage))

        val (name, message) = if (quest!!.actualStart != null) {

            addMarkDoneAction(questId, notificationBuilder)
            Pair(
                "Quest complete",
                "${quest.name} is all done!"
            )

        } else {
            val timeRange = quest.pomodoroTimeRanges.last()
            if (timeRange.type == TimeRange.Type.WORK) {
                Pair(
                    "Pomodoro complete",
                    "Your pomodoro is added to ${quest.name}. Ready for a break?"
                )
            } else {
                if (quest.hasCompletedAllTimeRanges()) {
                    addMarkDoneAction(questId, notificationBuilder)
                    Pair(
                        "Break complete",
                        "${quest.name} is all done!"
                    )
                } else {
                    Pair(
                        "Break complete",
                        "Ready to work on ${quest.name}?"
                    )
                }
            }
        }

        val contentIntent = Intent(context, MainActivity::class.java)
        contentIntent.action = MainActivity.ACTION_SHOW_TIMER
        contentIntent.putExtra(Constants.QUEST_ID_EXTRA_KEY, questId)

        val contentPI = PendingIntent.getActivity(
            context,
            Random().nextInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        notificationBuilder.setContentIntent(contentPI)

        val notification = notificationBuilder
            .setContentTitle(name)
            .setContentText(message)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .build()

        val notificationId = Random().nextInt()
        notificationManager.notify(notificationId, notification)

        return Job.Result.SUCCESS
    }

    private fun addMarkDoneAction(
        questId: String,
        notificationBuilder: NotificationCompat.Builder
    ) {
        val intent = Intent(context, CompleteQuestReceiver::class.java)
        intent.putExtra(Constants.QUEST_ID_EXTRA_KEY, questId)

        val pi = getBroadcastPendingIntent(context, intent, Random().nextInt())

        notificationBuilder.addAction(
            R.drawable.ic_done_black_24dp,
            "Mark Done",
            pi
        )
    }

    fun getBroadcastPendingIntent(
        context: Context,
        intent: Intent,
        requestCode: Int
    ) =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

    companion object {
        val TAG = "job_timer_complete_notification_tag"
    }
}

interface TimerCompleteScheduler {
    fun schedule(questId: String, after: Interval<Minute>)
}

class AndroidJobTimerCompleteScheduler : TimerCompleteScheduler {

    override fun schedule(questId: String, after: Interval<Minute>) {
        JobManager.instance().cancelAllForTag(TimerCompleteNotificationJob.TAG)

        val bundle = PersistableBundleCompat()
        bundle.putString("questId", questId)
        JobRequest.Builder(TimerCompleteNotificationJob.TAG)
            .setExtras(bundle)
//            .setExact(after.asMilliseconds.longValue)
            .setExact(1000)
            .build()
            .schedule()
    }
}