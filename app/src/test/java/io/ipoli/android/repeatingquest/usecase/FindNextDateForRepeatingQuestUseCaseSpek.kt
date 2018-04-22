package io.ipoli.android.repeatingquest.usecase

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.ipoli.android.TestUtil
import io.ipoli.android.quest.RepeatingQuest
import io.ipoli.android.quest.data.persistence.QuestRepository
import io.ipoli.android.repeatingquest.entity.RepeatPattern
import org.amshove.kluent.`should be true`
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.temporal.TemporalAdjusters

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 2/25/18.
 */
class FindNextDateForRepeatingQuestUseCaseSpek : Spek({
    describe("FindNextDateForRepeatingQuestUseCase") {

        fun executeUseCase(
            questRepo: QuestRepository,
            repeatingQuest: RepeatingQuest,
            fromDate: LocalDate = LocalDate.now()
        ) =
            FindNextDateForRepeatingQuestUseCase(questRepo).execute(
                FindNextDateForRepeatingQuestUseCase.Params(
                    repeatingQuest, fromDate
                )
            )

        fun shouldHaveNextDate(repeatingQuest: RepeatingQuest, date: LocalDate) {
            repeatingQuest.nextDate!!.isEqual(date).`should be true`()
        }

        it("should find scheduled for today") {
            val today = LocalDate.now()
            val quest = TestUtil.quest.copy(
                scheduledDate = today,
                originalScheduledDate = today
            )
            val questRepoMock = mock<QuestRepository> {
                on { findNextScheduledNotCompletedForRepeatingQuest(any(), any()) } doReturn quest
            }

            val rq = executeUseCase(questRepoMock, TestUtil.repeatingQuest)
            shouldHaveNextDate(rq, today)
        }

        it("should find scheduled for tomorrow") {
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)
            val quest = TestUtil.quest.copy(
                originalScheduledDate = today,
                scheduledDate = tomorrow
            )
            val questRepoMock = mock<QuestRepository> {
                on { findNextScheduledNotCompletedForRepeatingQuest(any(), any()) } doReturn quest
                on { findOriginalScheduledForRepeatingQuestAtDate(any(), any()) } doReturn quest
            }

            val rq = executeUseCase(
                questRepoMock, TestUtil.repeatingQuest.copy(
                    repeatPattern = RepeatPattern.Daily(startDate = today)
                )
            )
            shouldHaveNextDate(rq, tomorrow)
        }

        it("should find next from daily pattern") {

            val today = LocalDate.now()
            val questRepoMock = mock<QuestRepository>()

            val rq = executeUseCase(questRepoMock, TestUtil.repeatingQuest)
            shouldHaveNextDate(rq, today)
        }

        it("should find Friday for weekly pattern MON, WED, FRI when today is TUE and WED is removed & FRI is not scheduled") {
            val today = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.TUESDAY))
            val questRepoMock = mock<QuestRepository> {
                on {
                    findOriginalScheduledForRepeatingQuestAtDate(
                        "",
                        today.with(DayOfWeek.WEDNESDAY)
                    )
                } doReturn TestUtil.quest.copy(
                    originalScheduledDate = today.with(DayOfWeek.WEDNESDAY),
                    isRemoved = true
                )
            }

            val rq = executeUseCase(
                questRepoMock, TestUtil.repeatingQuest.copy(
                    repeatPattern = RepeatPattern.Weekly(
                        daysOfWeek = setOf(
                            DayOfWeek.WEDNESDAY,
                            DayOfWeek.FRIDAY
                        ),
                        startDate = today
                    )
                ),
                fromDate = today
            )
            shouldHaveNextDate(rq, today.with(DayOfWeek.FRIDAY))
        }


        it("should find Friday for weekly pattern MON, WED, FRI when today is TUE and WED is scheduled for SAT & FRI is not scheduled") {
            val today = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.TUESDAY))

            val quest = TestUtil.quest.copy(
                originalScheduledDate = today.with(DayOfWeek.WEDNESDAY),
                scheduledDate = today.with(DayOfWeek.SATURDAY)
            )

            val questRepoMock = mock<QuestRepository> {

                on {
                    findOriginalScheduledForRepeatingQuestAtDate(
                        "",
                        today.with(DayOfWeek.WEDNESDAY)
                    )
                } doReturn quest

                on { findNextScheduledNotCompletedForRepeatingQuest(any(), any()) } doReturn quest
            }

            val rq = executeUseCase(
                questRepoMock, TestUtil.repeatingQuest.copy(
                    repeatPattern = RepeatPattern.Weekly(
                        daysOfWeek = setOf(
                            DayOfWeek.WEDNESDAY,
                            DayOfWeek.FRIDAY
                        ),
                        startDate = today
                    )
                ),
                fromDate = today
            )
            shouldHaveNextDate(rq, today.with(DayOfWeek.FRIDAY))
        }

    }
})