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
import io.ipoli.android.quest.Color
import io.ipoli.android.quest.Icon
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
 * on 8/21/18.
 */
class SaveHabitUseCaseSpek : Spek({

    describe("SaveHabitUseCase") {

        fun executeUseCase(
            params: SaveHabitUseCase.Params,
            habit: Habit,
            rewardPlayerUseCase: RewardPlayerUseCase = mock(),
            removeRewardFromPlayerUseCase: RemoveRewardFromPlayerUseCase = mock(),
            player: Player = TestUtil.player()
        ) =
            SaveHabitUseCase(
                TestUtil.habitRepoMock(
                    habit
                ),
                TestUtil.playerRepoMock(player),
                rewardPlayerUseCase,
                removeRewardFromPlayerUseCase
            ).execute(
                params
            )

        it("should remove reward from player") {
            val removeRewardFromPlayerUseCaseMock = mock<RemoveRewardFromPlayerUseCase>()
            val h = executeUseCase(
                params = SaveHabitUseCase.Params(
                    id = "AAA",
                    timesADay = 2,
                    name = "",
                    color = Color.LIME,
                    icon = Icon.DROP,
                    days = DayOfWeek.values().toSet(),
                    isGood = true,
                    dateTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(12, 46))
                ),
                habit = TestUtil.habit.copy(
                    id = "AAA",
                    days = DayOfWeek.values().toSet(),
                    timesADay = 1,
                    history = mapOf(
                        LocalDate.now() to
                            CompletedEntry().copy(
                                completedAtTimes = listOf(Time.at(12, 45))
                            ),
                        LocalDate.now().plusDays(1) to
                            CompletedEntry().copy(
                                coins = 1,
                                experience = 10
                            )
                    )
                ),
                player = TestUtil.player().copy(
                    preferences = TestUtil.player().preferences.copy(
                        resetDayTime = Time.at(12, 30)
                    )
                ),
                removeRewardFromPlayerUseCase = removeRewardFromPlayerUseCaseMock
            )

            val expectedReward =
                SimpleReward(10, 1, Quest.Bounty.None)
            Verify on removeRewardFromPlayerUseCaseMock that removeRewardFromPlayerUseCaseMock.execute(
                expectedReward
            ) was called
        }
    }

})