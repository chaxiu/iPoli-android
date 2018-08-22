package io.ipoli.android.common.job

import android.content.Context
import io.ipoli.android.MyPoliApp
import io.ipoli.android.achievement.usecase.UpdatePlayerStatsUseCase
import io.ipoli.android.common.di.BackgroundModule
import io.ipoli.android.habit.usecase.UpdateHabitStreaksUseCase
import io.ipoli.android.pet.usecase.LowerPetStatsUseCase
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import org.threeten.bp.LocalDateTime
import space.traversal.kapsule.Kapsule

class ResetDayJob : FixedDailyJob(ResetDayJob.TAG) {

    override fun doRunJob(params: Params): Result {

        val kap = Kapsule<BackgroundModule>()
        val playerRepository by kap.required { playerRepository }
        val updateHabitStreaksUseCase by kap.required { updateHabitStreaksUseCase }
        val lowerPetStatsUseCase by kap.required { lowerPetStatsUseCase }
        val updatePlayerStatsUseCase by kap.required { updatePlayerStatsUseCase }
        kap.inject(MyPoliApp.backgroundModule(context))

        val player = playerRepository.find()!!

        updateHabitStreaksUseCase.execute(
            UpdateHabitStreaksUseCase.Params(
                today = LocalDateTime.now(),
                resetDayTime = player.preferences.resetDayTime
            )
        )

        val oldPet = player.pet

        val newPet = lowerPetStatsUseCase.execute(LowerPetStatsUseCase.Params())

        if (oldPet.isDead != newPet.isDead) {
            updatePlayerStatsUseCase.execute(
                UpdatePlayerStatsUseCase.Params(
                    player = playerRepository.find()!!,
                    eventType = UpdatePlayerStatsUseCase.Params.EventType.PetDied
                )
            )
        }

        return Result.SUCCESS
    }

    companion object {
        const val TAG = "job_reset_day_tag"
    }
}

interface ResetDayScheduler {
    fun schedule()
}

class AndroidResetDayScheduler(private val context: Context) : ResetDayScheduler {

    override fun schedule() {
        launch(CommonPool) {

            val kap = Kapsule<BackgroundModule>()
            val playerRepository by kap.required { playerRepository }
            kap.inject(MyPoliApp.backgroundModule(this@AndroidResetDayScheduler.context))

            val p = playerRepository.find()

            requireNotNull(p)

            val t = p!!.preferences.resetDayTime
            FixedDailyJobScheduler.schedule(ResetDayJob.TAG, t)
        }
    }

}