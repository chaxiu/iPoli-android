package io.ipoli.android.player.usecase

import io.ipoli.android.achievement.usecase.UnlockAchievementsUseCase
import io.ipoli.android.achievement.usecase.UpdatePlayerStatsUseCase
import io.ipoli.android.common.Reward
import io.ipoli.android.common.UseCase
import io.ipoli.android.player.LevelDownScheduler
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.persistence.PlayerRepository

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 29.11.17.
 */
open class RemoveRewardFromPlayerUseCase(
    private val playerRepository: PlayerRepository,
    private val levelDownScheduler: LevelDownScheduler,
    private val unlockAchievementsUseCase: UnlockAchievementsUseCase
) : UseCase<Reward, Player> {
    override fun execute(parameters: Reward): Player {
        val player = playerRepository.find()
        requireNotNull(player)
        val pet = player!!.pet
        val newPet = if (pet.isDead) pet else pet.removeReward(parameters)
        val newPlayer = player
            .removeExperience(parameters.experience)
            .removeCoins(parameters.coins)
            .copy(pet = newPet)
        val p = playerRepository.save(newPlayer)
        if (player.level != newPlayer.level) {
            levelDownScheduler.schedule()
        }
        unlockAchievementsUseCase.execute(
            UnlockAchievementsUseCase.Params(
                player = p,
                eventType = UpdatePlayerStatsUseCase.Params.EventType.ExperienceDecreased(parameters.experience.toLong())
            )
        )
        return p
    }

}