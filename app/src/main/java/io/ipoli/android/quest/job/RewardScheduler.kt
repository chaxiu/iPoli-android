package io.ipoli.android.quest.job

import android.content.Context
import io.ipoli.android.MyPoliApp
import io.ipoli.android.achievement.usecase.UnlockAchievementsUseCase
import io.ipoli.android.achievement.usecase.UpdatePlayerStatsUseCase
import io.ipoli.android.common.Reward
import io.ipoli.android.common.di.BackgroundModule
import io.ipoli.android.common.view.asThemedWrapper
import io.ipoli.android.habit.usecase.UndoCompleteHabitUseCase
import io.ipoli.android.pet.AndroidPetAvatar
import io.ipoli.android.quest.Quest
import io.ipoli.android.quest.view.RewardPopup
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import space.traversal.kapsule.Kapsule

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 11/15/17.
 */

interface RewardScheduler {
    fun schedule(reward: Reward, isPositive: Boolean = true, type: Type, entityId: String)

    enum class Type { QUEST, HABIT }
}

class AndroidJobRewardScheduler(private val context: Context) : RewardScheduler {
    override fun schedule(
        reward: Reward,
        isPositive: Boolean,
        type: RewardScheduler.Type,
        entityId: String
    ) {

        val c = context.asThemedWrapper()

        val kap = Kapsule<BackgroundModule>()
        val playerRepository by kap.required { playerRepository }
        val undoCompletedQuestUseCase by kap.required { undoCompletedQuestUseCase }
        val undoCompleteHabitUseCase by kap.required { undoCompleteHabitUseCase }
        val unlockAchievementsUseCase by kap.required { unlockAchievementsUseCase }
        kap.inject(MyPoliApp.backgroundModule(context))

        val bounty = reward.bounty

        val petAvatar = playerRepository.find()!!.pet.avatar
        val petHeadImage = AndroidPetAvatar.valueOf(petAvatar.name).headImage
        launch(UI) {
            RewardPopup(
                petHeadImage = petHeadImage,
                earnedXP = reward.experience,
                earnedCoins = reward.coins,
                bounty = if (bounty is Quest.Bounty.Food) {
                    bounty.food
                } else {
                    null
                },
                undoListener = {
                    launch(CommonPool) {
                        when (type) {
                            RewardScheduler.Type.QUEST -> {
                                undoCompletedQuestUseCase.execute(entityId)
                                unlockAchievementsUseCase.execute(
                                    UnlockAchievementsUseCase.Params(
                                        player = playerRepository.find()!!,
                                        eventType = UpdatePlayerStatsUseCase.Params.EventType.QuestUncompleted
                                    )
                                )
                            }
                            RewardScheduler.Type.HABIT ->
                                undoCompleteHabitUseCase.execute(
                                    UndoCompleteHabitUseCase.Params(
                                        entityId
                                    )
                                )
                        }
                    }
                },
                isPositive = isPositive
            ).show(c)
        }
    }

}