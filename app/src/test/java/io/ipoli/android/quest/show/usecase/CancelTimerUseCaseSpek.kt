package io.ipoli.android.quest.show.usecase

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.ipoli.android.TestUtil
import io.ipoli.android.quest.Quest
import io.ipoli.android.quest.TimeRange
import io.ipoli.android.quest.data.persistence.QuestRepository
import io.ipoli.android.quest.show.job.TimerCompleteScheduler
import io.ipoli.android.quest.show.pomodoros
import org.amshove.kluent.`should be false`
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.threeten.bp.Instant

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 1/18/18.
 */
class CancelTimerUseCaseSpek : Spek({

    describe("CancelTimerUseCase") {

        fun executeUseCase(
            quest: Quest
        ): Quest {
            val questRepoMock = mock<QuestRepository> {
                on { findById(any()) } doReturn quest
                on { save(any<Quest>()) } doAnswer { invocation ->
                    invocation.getArgument(0)
                }
            }
            return CancelTimerUseCase(questRepoMock, mock(), mock())
                .execute(CancelTimerUseCase.Params(quest.id))
        }

        val simpleQuest = TestUtil.quest

        it("should cancel count down") {
            val result = executeUseCase(
                simpleQuest.copy(
                    timeRanges = listOf(
                        TimeRange(
                            TimeRange.Type.COUNTDOWN,
                            simpleQuest.duration,
                            start = Instant.now(),
                            end = null
                        )
                    )
                )
            )
            result.hasCountDownTimer.`should be false`()
        }

        it("should cancel current pomodoro") {
            val result = executeUseCase(
                simpleQuest.copy(
                    timeRanges = listOf(
                        TimeRange(
                            TimeRange.Type.POMODORO_WORK,
                            1.pomodoros(),
                            start = Instant.now(),
                            end = null
                        )
                    )
                )
            )
            result.hasPomodoroTimer.`should be false`()
        }
    }
})