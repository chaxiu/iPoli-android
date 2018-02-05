package mypoli.android.player

import com.evernote.android.job.Job
import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import mypoli.android.common.di.Module
import mypoli.android.common.view.asThemedWrapper
import mypoli.android.player.view.LevelUpPopup
import space.traversal.kapsule.Injects

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 11/15/17.
 */
class LevelUpJob : Job(), Injects<Module> {

    override fun onRunJob(params: Params): Result {

        val newLevel = params.extras.getInt(KEY_NEW_LEVEL, -1)
        require(newLevel > 0, { "LevelUpJob received incorrect level param: $newLevel" })
        val c = context.asThemedWrapper()
        launch(UI) {
            LevelUpPopup(newLevel).show(c)
        }

        return Result.SUCCESS
    }

    companion object {
        val TAG = "job_level_up_tag"

        val KEY_NEW_LEVEL = "NEW_LEVEL"
    }
}

interface LevelUpScheduler {
    fun schedule(newLevel: Int)
}

class AndroidLevelUpScheduler : LevelUpScheduler {

    override fun schedule(newLevel: Int) {

        val params = PersistableBundleCompat()
        params.putInt(LevelUpJob.KEY_NEW_LEVEL, newLevel)

        JobRequest.Builder(LevelUpJob.TAG)
            .setExtras(params)
            .startNow()
            .build()
            .schedule()
    }
}