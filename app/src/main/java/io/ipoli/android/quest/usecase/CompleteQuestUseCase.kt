package io.ipoli.android.quest.usecase

import io.ipoli.android.Constants
import io.ipoli.android.challenge.entity.SharingPreference
import io.ipoli.android.challenge.persistence.ChallengeRepository
import io.ipoli.android.common.SimpleReward
import io.ipoli.android.common.UseCase
import io.ipoli.android.common.datetime.Time
import io.ipoli.android.common.rate.RatePopupScheduler
import io.ipoli.android.dailychallenge.job.DailyChallengeCompleteScheduler
import io.ipoli.android.dailychallenge.usecase.CheckForDailyChallengeCompletionUseCase
import io.ipoli.android.friends.usecase.SavePostsUseCase
import io.ipoli.android.pet.Food
import io.ipoli.android.player.persistence.PlayerRepository
import io.ipoli.android.player.usecase.RewardPlayerUseCase
import io.ipoli.android.quest.Quest
import io.ipoli.android.quest.data.persistence.QuestRepository
import io.ipoli.android.quest.job.ReminderScheduler
import io.ipoli.android.quest.job.RewardScheduler
import org.threeten.bp.LocalDate
import java.util.*

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 10/27/17.
 */
open class CompleteQuestUseCase(
    private val questRepository: QuestRepository,
    private val playerRepository: PlayerRepository,
    private val challengeRepository: ChallengeRepository,
    private val reminderScheduler: ReminderScheduler,
    private val rewardScheduler: RewardScheduler,
    private val ratePopupScheduler: RatePopupScheduler,
    private val rewardPlayerUseCase: RewardPlayerUseCase,
    private val checkForDailyChallengeCompletionUseCase: CheckForDailyChallengeCompletionUseCase,
    private val dailyChallengeCompleteScheduler: DailyChallengeCompleteScheduler,
    private val savePostsUseCase: SavePostsUseCase,
    private val randomSeed: Long? = null
) : UseCase<CompleteQuestUseCase.Params, Quest> {
    override fun execute(parameters: Params): Quest {

        val quest = when (parameters) {
            is Params.WithQuest -> parameters.quest
            is Params.WithQuestId -> {
                val questId = parameters.questId
                require(questId.isNotEmpty(), { "questId cannot be empty" })
                questRepository.findById(questId)!!
            }
        }

        val player = playerRepository.find()!!
        val pet = player.pet

        val experience = quest.experience ?: experience(pet.experienceBonus)
        val coins = quest.coins ?: coins(pet.coinBonus)
        val bounty = quest.bounty ?: bounty(pet.itemDropBonus)
        val newQuest = quest.copy(
            completedAtDate = parameters.completedDate,
            completedAtTime = parameters.completedTime,
            experience = experience,
            coins = coins,
            bounty = bounty
        )

        questRepository.save(newQuest)

        reminderScheduler.schedule()

        val reward = SimpleReward(
            newQuest.experience!!,
            newQuest.coins!!,
            if (quest.bounty == null) bounty else Quest.Bounty.None
        )
        rewardPlayerUseCase.execute(reward)

        rewardScheduler.schedule(
            reward = reward,
            type = RewardScheduler.Type.QUEST,
            entityId = quest.id
        )
        ratePopupScheduler.schedule()

        val r = checkForDailyChallengeCompletionUseCase.execute(Unit)
        if (r == CheckForDailyChallengeCompletionUseCase.Result.Complete) {
            dailyChallengeCompleteScheduler.schedule(
                experience(100f),
                coins(100f)
            )
        }

        if (newQuest.isFromChallenge) {
            val challenge = challengeRepository.findById(newQuest.challengeId!!)!!
            if (!challenge.isCompleted && challenge.sharingPreference == SharingPreference.FRIENDS) {
                savePostsUseCase.execute(
                    SavePostsUseCase.Params.QuestFromChallengeComplete(
                        quest = newQuest,
                        challenge = challenge,
                        player = player
                    )
                )
            }
        }

        return newQuest
    }

    private fun coins(coinBonusPercentage: Float): Int {
        val rewards = intArrayOf(1, 2, 3, 5)
        val bonusCoef = (100 + coinBonusPercentage) / 100
        val reward = rewards[createRandom().nextInt(rewards.size)]
        return (reward * bonusCoef).toInt()
    }

    private fun experience(xpBonusPercentage: Float): Int {
        val rewards = intArrayOf(2, 5, 7, 10, 15)
        val bonusCoef = (100 + xpBonusPercentage) / 100
        val reward = rewards[createRandom().nextInt(rewards.size)]
        return (reward * bonusCoef).toInt()
    }

    private fun bounty(bountyBonusPercentage: Float): Quest.Bounty {
        val bountyBonus = Constants.QUEST_BOUNTY_DROP_PERCENTAGE * (bountyBonusPercentage / 100)
        val totalBountyPercentage = Constants.QUEST_BOUNTY_DROP_PERCENTAGE + bountyBonus

        val random = createRandom().nextDouble()
        return if (random > totalBountyPercentage / 100) {
            Quest.Bounty.None
        } else {
            chooseBounty()
        }
    }

    private fun chooseBounty(): Quest.Bounty.Food {
        val foods = Food.values() + Food.POOP + Food.POOP + Food.BEER
        val index = createRandom().nextInt(foods.size)
        return Quest.Bounty.Food(foods[index])
    }

    private fun createRandom(): Random {
        val random = Random()
        randomSeed?.let { random.setSeed(it) }
        return random
    }

    sealed class Params(open val completedDate: LocalDate, open val completedTime: Time) {
        data class WithQuest(
            val quest: Quest,
            override val completedDate: LocalDate = LocalDate.now(),
            override val completedTime: Time = Time.now()
        ) : Params(completedDate, completedTime)

        data class WithQuestId(
            val questId: String,
            override val completedDate: LocalDate = LocalDate.now(),
            override val completedTime: Time = Time.now()
        ) : Params(completedDate, completedTime)
    }
}