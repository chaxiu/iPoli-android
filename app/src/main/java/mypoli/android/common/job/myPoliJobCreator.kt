package mypoli.android.common.job

import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator
import mypoli.android.ReminderNotificationJob
import mypoli.android.pet.LowerPetStatsJob
import mypoli.android.player.LevelUpJob
import mypoli.android.quest.QuestCompleteJob
import mypoli.android.rate.RatePopupJob

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 10/28/17.
 */
class myPoliJobCreator : JobCreator {
    override fun create(tag: String): Job? =
        when (tag) {
            ReminderNotificationJob.TAG -> ReminderNotificationJob()
            QuestCompleteJob.TAG -> QuestCompleteJob()
            LevelUpJob.TAG -> LevelUpJob()
            RatePopupJob.TAG -> RatePopupJob()
            LowerPetStatsJob.TAG -> LowerPetStatsJob()
            else -> null
        }
}