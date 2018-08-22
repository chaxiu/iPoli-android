package io.ipoli.android.habit.usecase

import io.ipoli.android.common.SimpleReward
import io.ipoli.android.common.UseCase
import io.ipoli.android.common.datetime.toTime
import io.ipoli.android.habit.HabitReward
import io.ipoli.android.habit.data.CompletedEntry
import io.ipoli.android.habit.data.Habit
import io.ipoli.android.habit.persistence.HabitRepository
import io.ipoli.android.player.persistence.PlayerRepository
import io.ipoli.android.player.usecase.RemoveRewardFromPlayerUseCase
import io.ipoli.android.player.usecase.RewardPlayerUseCase
import io.ipoli.android.quest.Quest
import io.ipoli.android.quest.job.RewardScheduler
import org.threeten.bp.LocalDateTime

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 6/17/18.
 */
class CompleteHabitUseCase(
    private val habitRepository: HabitRepository,
    private val playerRepository: PlayerRepository,
    private val rewardScheduler: RewardScheduler,
    private val rewardPlayerUseCase: RewardPlayerUseCase,
    private val removeRewardFromPlayerUseCase: RemoveRewardFromPlayerUseCase,
    private val randomSeed: Long? = null
) : UseCase<CompleteHabitUseCase.Params, Habit> {

    override fun execute(parameters: Params): Habit {
        val habit = habitRepository.findById(parameters.habitId)
        requireNotNull(habit)

        val player = playerRepository.find()!!

        val dateTime = parameters.dateTime
        val date = dateTime.toLocalDate()
        val resetDayTime = player.preferences.resetDayTime

        require(habit!!.shouldBeDoneOn(dateTime, resetDayTime))
        require(!habit.isCompletedFor(dateTime, resetDayTime))

        val history = habit.history.toMutableMap()

        val completedEntry =
            if (history.containsKey(date)) history[date]!!
            else CompletedEntry()

        history[date] = completedEntry.complete(dateTime.toTime())

        val isCompleted = habit.copy(
            history = history
        ).isCompletedFor(dateTime, resetDayTime)


        val playerDate = player.currentDate(dateTime)

        if (isCompleted) {
            val pet = player.pet
            val ce = history[playerDate] ?: CompletedEntry()
            val reward = HabitReward(randomSeed).generate(pet.coinBonus, pet.experienceBonus)
            history[playerDate] = ce.copy(
                experience = ce.coins ?: reward.coins,
                coins = ce.experience ?: reward.experience
            )
        }

        val currentStreak =
            if (isCompleted && habit.isGood) habit.currentStreak + 1
            else if (!habit.isGood) 0
            else habit.currentStreak


        if (isCompleted) {
            val reward = SimpleReward(
                coins = history[playerDate]!!.coins!!,
                experience = history[playerDate]!!.experience!!,
                bounty = Quest.Bounty.None
            )
            if (habit.isGood) {
                rewardPlayerUseCase.execute(reward)
                rewardScheduler.schedule(
                    reward = reward,
                    type = RewardScheduler.Type.HABIT,
                    entityId = habit.id
                )
            } else {
                removeRewardFromPlayerUseCase.execute(reward)
                rewardScheduler.schedule(
                    reward = reward.copy(
                        coins = -reward.coins,
                        experience = -reward.experience
                    ),
                    isPositive = false,
                    type = RewardScheduler.Type.HABIT,
                    entityId = habit.id
                )
            }
        }

        return habitRepository.save(
            habit.copy(
                history = history,
                currentStreak = currentStreak,
                prevStreak = if (currentStreak != habit.currentStreak) habit.currentStreak else habit.prevStreak,
                bestStreak = Math.max(currentStreak, habit.bestStreak)
            )
        )
    }

    data class Params(val habitId: String, val dateTime: LocalDateTime = LocalDateTime.now())
}