package io.ipoli.android.habit.usecase

import io.ipoli.android.common.SimpleReward
import io.ipoli.android.common.UseCase
import io.ipoli.android.habit.data.Habit
import io.ipoli.android.habit.persistence.HabitRepository
import io.ipoli.android.player.persistence.PlayerRepository
import io.ipoli.android.player.usecase.RemoveRewardFromPlayerUseCase
import io.ipoli.android.quest.Quest
import org.threeten.bp.LocalDateTime

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 6/17/18.
 */
class UndoCompleteHabitUseCase(
    private val habitRepository: HabitRepository,
    private val playerRepository: PlayerRepository,
    private val removeRewardFromPlayerUseCase: RemoveRewardFromPlayerUseCase
) : UseCase<UndoCompleteHabitUseCase.Params, Habit> {

    override fun execute(parameters: Params): Habit {
        val habit = habitRepository.findById(parameters.habitId)
        requireNotNull(habit)

        val player = playerRepository.find()!!

        val dateTime = parameters.dateTime
        val resetDayTime = player.preferences.resetDayTime

        val history = habit!!.history.toMutableMap()

        if(habit.completedCountForDate(dateTime, resetDayTime) == 0) {
            return habit
        }

        val wasCompleted = habit.isCompletedFor(dateTime, resetDayTime)

        val (startDate, endDate) = player.datesSpan(dateTime)

        val ced = if (endDate != null && history[endDate] != null) endDate else startDate

        if(history[ced]!!.completedAtTimes.isNotEmpty()) {
            history[ced] = history[ced]!!.undoLastComplete()
        } else {
            history[startDate] = history[startDate]!!.undoLastComplete()
        }

        if (wasCompleted && habit.isGood) {
            removeRewardFromPlayerUseCase.execute(
                SimpleReward(
                    coins = history[ced]!!.coins!!,
                    experience = history[ced]!!.experience!!,
                    bounty = Quest.Bounty.None
                )
            )
        }

        val currentStreak = habit.currentStreak
        val bestStreak = habit.bestStreak

        val newStreak = Math.max(
            if (wasCompleted && habit.isGood) currentStreak - 1
            else if (!habit.isGood) habit.prevStreak
            else currentStreak,
            0
        )

        return habitRepository.save(
            habit.copy(
                history = history,
                currentStreak = newStreak,
                prevStreak = if (newStreak != currentStreak && currentStreak != 0) currentStreak else habit.prevStreak,
                bestStreak = if (habit.isGood) {
                    if (wasCompleted && bestStreak == currentStreak) Math.max(
                        bestStreak - 1,
                        0
                    ) else bestStreak
                } else {
                    if (habit.prevStreak > bestStreak) {
                        habit.prevStreak
                    } else {
                        bestStreak
                    }
                }

            )
        )
    }

    data class Params(val habitId: String, val dateTime: LocalDateTime = LocalDateTime.now())
}