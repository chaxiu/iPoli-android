package io.ipoli.android.habit.usecase

import com.nhaarman.mockito_kotlin.mock
import io.ipoli.android.TestUtil
import io.ipoli.android.common.SimpleReward
import io.ipoli.android.common.datetime.Time
import io.ipoli.android.habit.data.CompletedEntry
import io.ipoli.android.habit.data.Habit
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.usecase.RemoveRewardFromPlayerUseCase
import io.ipoli.android.player.usecase.RewardPlayerUseCase
import io.ipoli.android.quest.Quest
import org.amshove.kluent.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 6/17/18.
 */
class CompleteHabitUseCaseSpek : Spek({

    describe("CompleteHabitUseCase") {

        val completedEntry = CompletedEntry()

        fun executeUseCase(
            habit: Habit,
            date: LocalDateTime = LocalDateTime.now(),
            rewardPlayerUseCase: RewardPlayerUseCase = mock(),
            removeRewardFromPlayerUseCase: RemoveRewardFromPlayerUseCase = mock(),
            player: Player = TestUtil.player()

        ) =
            CompleteHabitUseCase(
                TestUtil.habitRepoMock(
                    habit
                ),
                TestUtil.playerRepoMock(player),
                mock(),
                rewardPlayerUseCase,
                removeRewardFromPlayerUseCase
            ).execute(
                CompleteHabitUseCase.Params(habit.id, date)
            )

        it("should add entry in history") {
            val habit = executeUseCase(
                TestUtil.habit
            )
            habit.history.size.`should be`(1)
            habit.history.keys.`should contain`(LocalDate.now())
        }

        it("should check if should be done today") {
            val exec = {
                executeUseCase(
                    TestUtil.habit.copy(
                        days = setOf(DayOfWeek.SUNDAY)
                    ),
                    LocalDateTime.now().with(DayOfWeek.MONDAY)
                )
            }
            exec shouldThrow IllegalArgumentException::class
        }

        it("should check if already complete today") {
            val exec = {
                executeUseCase(
                    TestUtil.habit.copy(
                        days = setOf(DayOfWeek.SUNDAY),
                        history = mapOf(
                            LocalDate.now() to
                                completedEntry.copy(completedAtTimes = listOf(Time.now()))
                        )
                    ),
                    LocalDateTime.now()
                )
            }
            exec shouldThrow IllegalArgumentException::class
        }

        it("should add entry in existing history") {
            val today = LocalDate.now()
            val habit = executeUseCase(
                TestUtil.habit.copy(
                    history = mapOf(
                        today.minusDays(1) to completedEntry
                    )
                ),
                LocalDateTime.now()
            )
            habit.history.size.`should be`(2)
            habit.history.keys.`should contain all`(listOf(today.minusDays(1), today))
            habit.history[today]!!.completedCount.`should be`(1)
        }

        it("should add entry with 2 times a day") {
            val today = LocalDate.now()
            val habit = executeUseCase(
                TestUtil.habit.copy(
                    timesADay = 2,
                    history = mapOf(
                        today to completedEntry.copy(completedAtTimes = listOf(Time.now()))
                    )
                ),
                LocalDateTime.now()
            )
            habit.history[today]!!.completedCount.`should be`(2)
        }

        it("should not add entry when all times a day are completed") {
            val today = LocalDate.now()
            val habit = executeUseCase(
                TestUtil.habit.copy(
                    timesADay = 1,
                    history = mapOf(
                        today to completedEntry
                    )
                ),
                LocalDateTime.now()
            )
            habit.history[today]!!.completedCount.`should be`(1)
        }

        it("should increase current streak") {
            val habit = executeUseCase(
                TestUtil.habit.copy(
                    currentStreak = 0
                )
            )
            habit.currentStreak.`should be`(1)
        }

        it("should restart current streak for negative") {
            val habit = executeUseCase(
                TestUtil.habit.copy(
                    isGood = false,
                    currentStreak = 3,
                    prevStreak = 2
                )
            )
            habit.currentStreak.`should be`(0)
            habit.prevStreak.`should be`(3)
        }

        it("should not increase current streak with 2 times a day") {
            val habit = executeUseCase(
                TestUtil.habit.copy(
                    timesADay = 2,
                    currentStreak = 0
                )
            )
            habit.currentStreak.`should be`(0)
        }

        it("should increase current streak with 2 times a day") {
            val today = LocalDate.now()
            val habit = executeUseCase(
                TestUtil.habit.copy(
                    timesADay = 2,
                    history = mapOf(
                        today to completedEntry.copy(completedAtTimes = listOf(Time.now()))
                    ),
                    prevStreak = 1,
                    currentStreak = 2
                ),
                LocalDateTime.now()
            )
            habit.currentStreak.`should be`(3)
            habit.prevStreak.`should be`(2)
        }

        it("should not increase best streak") {
            val habit = executeUseCase(
                TestUtil.habit.copy(
                    currentStreak = 0,
                    bestStreak = 2
                )
            )
            habit.bestStreak.`should be`(2)
        }

        it("should increase best streak") {
            val habit = executeUseCase(
                TestUtil.habit.copy(
                    currentStreak = 2,
                    bestStreak = 2
                )
            )
            habit.bestStreak.`should be`(3)
        }

        describe("Rewards") {

            it("should give rewards") {
                val rewardPlayerPlayerUseCaseMock = mock<RewardPlayerUseCase>()
                val today = LocalDate.now()
                val habit = executeUseCase(
                    TestUtil.habit.copy(
                        isGood = true
                    ),
                    LocalDateTime.now(),
                    rewardPlayerPlayerUseCaseMock
                )
                val ce = habit.history[today]!!
                ce.coins!!.`should be greater than`(0)
                ce.experience!!.`should be greater than`(0)

                Verify on rewardPlayerPlayerUseCaseMock that rewardPlayerPlayerUseCaseMock.execute(
                    SimpleReward(ce.experience!!, ce.coins!!, Quest.Bounty.None)
                ) was called
            }

            it("should not give rewards if not completed") {
                val rewardPlayerPlayerUseCaseMock = mock<RewardPlayerUseCase>()
                val today = LocalDate.now()
                val habit = executeUseCase(
                    TestUtil.habit.copy(
                        isGood = true,
                        timesADay = 2
                    ),
                    LocalDateTime.now(),
                    rewardPlayerPlayerUseCaseMock
                )
                val ce = habit.history[today]!!
                ce.coins.`should be null`()
                ce.experience.`should be null`()
            }

            it("should assign rewards if negative") {
                val rewardPlayerPlayerUseCaseMock = mock<RewardPlayerUseCase>()
                val removeRewardFromPlayerUseCaseMock = mock<RemoveRewardFromPlayerUseCase>()
                val today = LocalDate.now()
                val habit = executeUseCase(
                    TestUtil.habit.copy(
                        isGood = false
                    ),
                    LocalDateTime.now(),
                    rewardPlayerPlayerUseCaseMock,
                    removeRewardFromPlayerUseCaseMock
                )
                val ce = habit.history[today]!!
                ce.coins!!.`should be greater than`(0)
                ce.experience!!.`should be greater than`(0)

                val reward = SimpleReward(ce.experience!!, ce.coins!!, Quest.Bounty.None)

                `Verify not called` on rewardPlayerPlayerUseCaseMock that rewardPlayerPlayerUseCaseMock.execute(
                    reward
                )

                Verify on removeRewardFromPlayerUseCaseMock that removeRewardFromPlayerUseCaseMock.execute(
                    reward
                ) was called
            }

            it("should not give reward when completed before reset day time") {
                val habit = TestUtil.habit.copy(
                    timesADay = 2,
                    isGood = true,
                    history = mapOf(
                        LocalDate.now() to CompletedEntry(
                            completedAtTimes = listOf(
                                Time.at(12, 15)
                            )
                        )
                    )
                )

                val p = TestUtil.player().copy(
                    preferences = TestUtil.player().preferences.copy(
                        resetDayTime = Time.at(12, 30)
                    )
                )

                val newHabit = executeUseCase(
                    habit = habit,
                    date = LocalDateTime.of(LocalDate.now(), LocalTime.of(12, 46)),
                    player = p
                )

                newHabit.history.size.`should be`(1)
                val ce = newHabit.history.values.first()
                ce.completedAtTimes.size.`should be`(2)
                ce.coins.`should be null`()
                ce.experience.`should be null`()
            }

            it("should give reward when completed before and after reset day time") {
                val habit = TestUtil.habit.copy(
                    timesADay = 2,
                    isGood = true,
                    history = mapOf(
                        LocalDate.now() to CompletedEntry(
                            completedAtTimes = listOf(
                                Time.at(12, 15),
                                Time.at(12, 40)
                            )
                        )
                    )
                )

                val p = TestUtil.player().copy(
                    preferences = TestUtil.player().preferences.copy(
                        resetDayTime = Time.at(12, 30)
                    )
                )

                val newHabit = executeUseCase(
                    habit = habit,
                    date = LocalDateTime.of(LocalDate.now(), LocalTime.of(12, 45)),
                    player = p
                )

                newHabit.history.size.`should be`(2)
                val ce = newHabit.history.values.last()
                ce.completedAtTimes.size.`should be`(0)
                ce.coins.`should not be null`()
                ce.experience.`should not be null`()
            }
        }
    }
})