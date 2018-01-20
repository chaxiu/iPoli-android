package mypoli.android.quest.usecase

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import mypoli.android.Constants
import mypoli.android.common.datetime.Time
import mypoli.android.quest.*
import mypoli.android.quest.data.persistence.QuestRepository
import mypoli.android.timer.pomodoros
import mypoli.android.timer.shortBreaks
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be null`
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should not be null`
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate

/**
 * Created by Polina Zhelyazkova <polina@ipoli.io>
 * on 1/18/18.
 */
class SaveQuestActualDurationUseCaseSpek : Spek({

    describe("SaveQuestActualDurationUseCase") {

        fun executeUseCase(
            quest: Quest,
            isPomodoro: Boolean,
            time: Instant = Instant.now()
        ): Quest {
            val questRepoMock = mock<QuestRepository> {
                on { findById(any()) } doReturn quest
                on { save(any()) } doAnswer { invocation ->
                    invocation.getArgument(0)
                }
            }
            return SaveQuestActualDurationUseCase(
                questRepoMock,
                SplitDurationForPomodoroTimerUseCase()
            )
                .execute(SaveQuestActualDurationUseCase.Params(quest.id, isPomodoro, time))
        }

        val simpleQuest = Quest(
            name = "",
            color = Color.BLUE,
            category = Category("Wellness", Color.BLUE),
            scheduledDate = LocalDate.now(),
            duration = 30,
            reminder = Reminder("", Time.now(), LocalDate.now())
        )

        val now = Instant.now()

        it("should save actual start") {
            val result = executeUseCase(simpleQuest, false, now)
            result.actualStart.`should be`(now)
        }

        it("should add the first time range") {
            val result = executeUseCase(simpleQuest, true)
            result.pomodoroTimeRanges.size `should be equal to` (1)
            val range = result.pomodoroTimeRanges.first()
            range.start.`should not be null`()
            range.end.`should be null`()
        }

        it("should end the last time range") {
            val quest = simpleQuest.copy(
                duration = 1.pomodoros() + 1.shortBreaks(),
                pomodoroTimeRanges = listOf(
                    TimeRange(
                        TimeRange.Type.WORK,
                        Constants.DEFAULT_POMODORO_WORK_DURATION,
                        now,
                        now
                    ),
                    TimeRange(
                        TimeRange.Type.BREAK,
                        Constants.DEFAULT_POMODORO_BREAK_DURATION,
                        now,
                        null
                    )
                )
            )
            val result = executeUseCase(quest, true)
            result.pomodoroTimeRanges.size `should be equal to` (2)
            result.pomodoroTimeRanges.last().end.`should not be null`()
        }

        it("should end the last time range with smaller duration") {
            val quest = simpleQuest.copy(
                duration = 10,
                pomodoroTimeRanges = listOf(
                    TimeRange(
                        TimeRange.Type.WORK,
                        Constants.DEFAULT_POMODORO_WORK_DURATION,
                        now,
                        now
                    ),
                    TimeRange(
                        TimeRange.Type.BREAK,
                        Constants.DEFAULT_POMODORO_BREAK_DURATION,
                        now,
                        null
                    )
                )
            )
            val result = executeUseCase(quest, true)
            result.pomodoroTimeRanges.size `should be equal to` (2)
            result.pomodoroTimeRanges.last().end.`should not be null`()
        }

        it("should end the last and add new time range") {
            val quest = simpleQuest.copy(
                duration = 2.pomodoros() + 1.shortBreaks(),
                pomodoroTimeRanges = listOf(
                    TimeRange(
                        TimeRange.Type.WORK,
                        Constants.DEFAULT_POMODORO_WORK_DURATION,
                        now,
                        now
                    ),
                    TimeRange(
                        TimeRange.Type.BREAK,
                        Constants.DEFAULT_POMODORO_BREAK_DURATION,
                        now,
                        null
                    )
                )
            )
            val result = executeUseCase(quest, true)
            result.pomodoroTimeRanges.size `should be equal to` (3)
            result.pomodoroTimeRanges[1].end.`should not be null`()
            val range = result.pomodoroTimeRanges.last()
            range.start.`should not be null`()
            range.end.`should be null`()
        }

    }
})