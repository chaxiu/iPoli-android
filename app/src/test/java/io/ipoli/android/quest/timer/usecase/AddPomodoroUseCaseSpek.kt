package io.ipoli.android.quest.timer.usecase

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.ipoli.android.common.datetime.Time
import io.ipoli.android.common.datetime.plusMinutes
import io.ipoli.android.quest.Color
import io.ipoli.android.quest.Quest
import io.ipoli.android.quest.Reminder
import io.ipoli.android.quest.TimeRange
import io.ipoli.android.quest.data.persistence.QuestRepository
import io.ipoli.android.quest.timer.longBreaks
import io.ipoli.android.quest.timer.pomodoros
import io.ipoli.android.quest.timer.shortBreaks
import org.amshove.kluent.`should be equal to`
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 1/19/18.
 */
class AddPomodoroUseCaseSpek : Spek({

    describe("AddPomodoroUseCase") {

        fun executeUseCase(
            quest: Quest
        ): Quest {
            val questRepoMock = mock<QuestRepository> {
                on { findById(any()) } doReturn quest
                on { save(any<Quest>()) } doAnswer { invocation ->
                    invocation.getArgument(0)
                }
            }
            return AddPomodoroUseCase(
                questRepoMock,
                SplitDurationForPomodoroTimerUseCase()
            )
                .execute(AddPomodoroUseCase.Params(quest.id))
        }

        val simpleQuest = Quest(
            name = "",
            color = Color.BLUE,
            scheduledDate = LocalDate.now(),
            duration = 30,
            reminder = Reminder("", Time.now(), LocalDate.now())
        )

        val now = Instant.now()

        it("should add duration of 1 pomodoro with short break") {
            val result = executeUseCase(
                simpleQuest.copy(
                    duration = 1.pomodoros() + 1.shortBreaks()
                )
            )
            result.duration.`should be equal to`(2.pomodoros() + 2.shortBreaks())
        }

        it("should add pomodoro duration to actual duration") {
            val result = executeUseCase(
                simpleQuest.copy(
                    duration = 10,
                    timeRanges = listOf(
                        TimeRange(
                            TimeRange.Type.POMODORO_WORK,
                            1.pomodoros(),
                            now,
                            now.plusMinutes(1.pomodoros().toLong())
                        ),
                        TimeRange(
                            TimeRange.Type.POMODORO_SHORT_BREAK,
                            1.shortBreaks(),
                            now,
                            now.plusMinutes(1.shortBreaks().toLong())
                        )
                    )
                )
            )
            result.duration.`should be equal to`(2.pomodoros() + 2.shortBreaks())
        }

        it("should add pomodoro duration to short duration quest") {
            val result = executeUseCase(
                simpleQuest.copy(
                    duration = 10,
                    timeRanges = listOf()
                )
            )
            result.duration.`should be equal to`(1.pomodoros() + 1.shortBreaks() + 10)
        }

        it("should add pomodoro duration with long break") {
            val timeRanges = mutableListOf<TimeRange>()

            for (i: Int in 1..4) {
                if (i % 2 == 1) {
                    timeRanges.add(
                        TimeRange(
                            TimeRange.Type.POMODORO_WORK,
                            1.pomodoros(),
                            now,
                            now.plusMinutes(1.pomodoros().toLong())
                        )
                    )
                } else {
                    timeRanges.add(
                        TimeRange(
                            TimeRange.Type.POMODORO_SHORT_BREAK,
                            1.shortBreaks(),
                            now,
                            now.plusMinutes(1.shortBreaks().toLong())
                        )
                    )
                }
            }

            val result = executeUseCase(
                simpleQuest.copy(
                    duration = 3.pomodoros() + 3.shortBreaks(),
                    timeRanges = timeRanges
                )
            )
            result.duration.`should be equal to`(4.pomodoros() + 3.shortBreaks() + 1.longBreaks())
        }
    }
})