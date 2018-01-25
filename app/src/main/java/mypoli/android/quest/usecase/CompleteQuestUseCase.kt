package mypoli.android.quest.usecase

import mypoli.android.Constants
import mypoli.android.common.SimpleReward
import mypoli.android.common.UseCase
import mypoli.android.common.datetime.Time
import mypoli.android.pet.Food
import mypoli.android.player.persistence.PlayerRepository
import mypoli.android.player.usecase.RewardPlayerUseCase
import mypoli.android.quest.Quest
import mypoli.android.quest.data.persistence.QuestRepository
import mypoli.android.quest.job.QuestCompleteScheduler
import mypoli.android.quest.job.ReminderScheduler
import mypoli.android.rate.RatePopupScheduler
import org.threeten.bp.LocalDate
import java.util.*

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 10/27/17.
 */
open class CompleteQuestUseCase(
    private val questRepository: QuestRepository,
    private val playerRepository: PlayerRepository,
    private val reminderScheduler: ReminderScheduler,
    private val questCompleteScheduler: QuestCompleteScheduler,
    private val ratePopupScheduler: RatePopupScheduler,
    private val rewardPlayerUseCase: RewardPlayerUseCase,
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

        val pet = playerRepository.find()!!.pet


        val experience = quest.experience ?: experience(pet.experienceBonus)
        val coins = quest.coins ?: coins(pet.coinBonus)
        val bounty = quest.bounty ?: bounty(pet.bountyBonus)
        val newQuest = quest.copy(
            completedAtDate = LocalDate.now(),
            completedAtTime = Time.now(),
            experience = experience,
            coins = coins,
            bounty = bounty
        )

        questRepository.save(newQuest)

        val quests = questRepository.findNextQuestsToRemind()
        if (quests.isNotEmpty()) {
            reminderScheduler.schedule(quests.first().reminder!!.toMillis())
        }

        val reward = SimpleReward(
            newQuest.experience!!,
            newQuest.coins!!,
            if (quest.bounty == null) bounty else Quest.Bounty.None
        )
        rewardPlayerUseCase.execute(reward)

        questCompleteScheduler.schedule(reward)
        ratePopupScheduler.schedule()
        return newQuest
    }

    private fun coins(coinBonusPercentage: Float): Int {
        val rewards = intArrayOf(2, 5, 7, 10)
        val bonusCoef = (100 + coinBonusPercentage) / 100
        val reward = rewards[createRandom().nextInt(rewards.size)]
        return (reward * bonusCoef).toInt()
    }

    private fun experience(xpBonusPercentage: Float): Int {
        val rewards = intArrayOf(5, 10, 15, 20, 30)
        val bonusCoef = (100 + xpBonusPercentage) / 100
        val reward = rewards[createRandom().nextInt(rewards.size)]
        return (reward * bonusCoef).toInt()
    }

    private fun bounty(bountyBonusPercentage: Float): Quest.Bounty {
        val bountyBonus = Constants.QUEST_BOUNTY_DROP_PERCENTAGE * (bountyBonusPercentage / 100)
        val totalBountyPercentage = Constants.QUEST_BOUNTY_DROP_PERCENTAGE + bountyBonus

        val random = createRandom().nextDouble()
        if (random > totalBountyPercentage / 100) {
            return Quest.Bounty.None
        } else {
            return chooseBounty()
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

    sealed class Params {
        data class WithQuest(val quest: Quest) : Params()
        data class WithQuestId(val questId: String) : Params()
    }
}