package io.ipoli.android.habit.usecase

import io.ipoli.android.common.SimpleReward
import io.ipoli.android.common.UseCase
import io.ipoli.android.habit.HabitReward
import io.ipoli.android.habit.data.CompletedEntry
import io.ipoli.android.habit.data.Habit
import io.ipoli.android.habit.persistence.HabitRepository
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.persistence.PlayerRepository
import io.ipoli.android.player.usecase.RemoveRewardFromPlayerUseCase
import io.ipoli.android.player.usecase.RewardPlayerUseCase
import io.ipoli.android.quest.Color
import io.ipoli.android.quest.Icon
import io.ipoli.android.quest.Quest
import io.ipoli.android.tag.Tag
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDateTime

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 6/17/18.
 */
class SaveHabitUseCase(
    private val habitRepository: HabitRepository,
    private val playerRepository: PlayerRepository,
    private val rewardPlayerUseCase: RewardPlayerUseCase,
    private val removeRewardFromPlayerUseCase: RemoveRewardFromPlayerUseCase
) : UseCase<SaveHabitUseCase.Params, Habit> {

    override fun execute(parameters: Params): Habit {
        val habit = if (parameters.id.isBlank()) {
            Habit(
                name = parameters.name,
                color = parameters.color,
                icon = parameters.icon,
                tags = parameters.tags ?: emptyList(),
                days = parameters.days,
                timesADay = parameters.timesADay,
                isGood = parameters.isGood,
                challengeId = parameters.challengeId
            )
        } else {
            var h = habitRepository.findById(parameters.id)!!

            if (h.timesADay != parameters.timesADay) {
                h = handleRewardIfTimesADayUpdated(h, parameters.timesADay, parameters.player)
            }

            h.copy(
                name = parameters.name,
                color = parameters.color,
                icon = parameters.icon,
                tags = parameters.tags ?: emptyList(),
                days = parameters.days,
                timesADay = parameters.timesADay,
                isGood = parameters.isGood,
                challengeId = parameters.challengeId
            )
        }

        return habitRepository.save(habit)
    }

    private fun handleRewardIfTimesADayUpdated(
        habit: Habit,
        timesADay: Int,
        player: Player?
    ): Habit {
        val dateTime = LocalDateTime.now()
        val p = player ?: playerRepository.find()!!
        val date = p.currentDate(dateTime)
        val resetTime = p.preferences.resetDayTime

        if (habit.completedCountForDate(dateTime, resetTime) > timesADay) {
            val history = habit.history.toMutableMap()

            val pet = p.pet

            if (!history.containsKey(date)) {
                history[date] = CompletedEntry()
            }

            history[date]!!.coins?.let {
                val reward = HabitReward().generate(pet.coinBonus, pet.experienceBonus)
                history[date] = history[date]!!.copy(
                    coins = reward.coins,
                    experience = reward.experience
                )
            }
            rewardPlayerUseCase.execute(
                SimpleReward(
                    coins = history[date]!!.coins!!,
                    experience = history[date]!!.experience!!,
                    bounty = Quest.Bounty.None
                )
            )

            return habit.copy(
                history = history
            )
        }

        if (habit.shouldBeDoneOn(dateTime, resetTime) && habit.isCompletedFor(dateTime, resetTime)) {
            val history = habit.history
            removeRewardFromPlayerUseCase.execute(
                SimpleReward(
                    coins = history[date]!!.coins!!,
                    experience = history[date]!!.experience!!,
                    bounty = Quest.Bounty.None
                )
            )
        }

        return habit
    }

    data class Params(
        val id: String = "",
        val name: String,
        val color: Color,
        val icon: Icon,
        val tags: List<Tag>? = null,
        val days: Set<DayOfWeek>,
        val timesADay: Int,
        val isGood: Boolean,
        val challengeId: String? = null,
        val player: Player? = null
    )
}