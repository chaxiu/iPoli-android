package mypoli.android.repeatingquest.usecase

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import mypoli.android.TestUtil
import mypoli.android.TestUtil.firstDateOfWeek
import mypoli.android.TestUtil.lastDateOfWeek
import mypoli.android.quest.Quest
import mypoli.android.quest.data.persistence.QuestRepository
import mypoli.android.repeatingquest.entity.RepeatingQuest
import org.amshove.kluent.`should be empty`
import org.amshove.kluent.`should be`
import org.amshove.kluent.shouldThrow
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.threeten.bp.LocalDate

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 02/14/2018.
 */
class ScheduleRepeatingQuestUseCaseSpek : Spek({

    describe("FindQuestsForRepeatingQuest") {
        fun executeUseCase(
            quest: RepeatingQuest,
            start: LocalDate,
            end: LocalDate,
            questRepo: QuestRepository = TestUtil.questRepoMock()
        ) =
            FindQuestsForRepeatingQuest(questRepo).execute(
                FindQuestsForRepeatingQuest.Params(
                    repeatingQuest = quest,
                    start = start,
                    end = end
                )
            )

        fun mockQuestsForRepeatingQuest(quests: List<Quest> = listOf()) =
            mock<QuestRepository> {
                on {
                    findForRepeatingQuestBetween(
                        any(),
                        any(),
                        any()
                    )
                } doReturn quests
            }

        val questId = "qid"

        it("should require end after start") {
            val exec = {
                val today = LocalDate.now()
                executeUseCase(TestUtil.repeatingQuest, today, today)
            }
            exec shouldThrow IllegalArgumentException::class
        }

        describe("repeating daily") {

            it("should schedule for every day at start of week when no quests are scheduled") {

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest,
                    start = firstDateOfWeek,
                    end = lastDateOfWeek
                )
                quests.size.`should be`(7)
            }

            it("should schedule for every day at start of week when 1 quest is scheduled") {

                val repo = mockQuestsForRepeatingQuest(
                    listOf(
                        TestUtil.quest.copy(
                            id = questId,
                            originalScheduledDate = firstDateOfWeek
                        )
                    )
                )

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest,
                    start = firstDateOfWeek,
                    end = lastDateOfWeek,
                    questRepo = repo
                )
                quests.size.`should be`(7)
                quests.first().id.`should be`(questId)
            }

            it("should find only stored quests") {


                val repo = mockQuestsForRepeatingQuest(
                    List(7) { i ->
                        TestUtil.quest.copy(
                            id = questId,
                            originalScheduledDate = firstDateOfWeek.plusDays(i.toLong())
                        )
                    }
                )

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest,
                    start = firstDateOfWeek,
                    end = lastDateOfWeek,
                    questRepo = repo
                )
                quests.size.`should be`(7)
                quests.forEach { it.id.`should be`(questId) }
            }

            it("should not schedule for dates with deleted quest") {

                val repo = mockQuestsForRepeatingQuest(
                    listOf(
                        TestUtil.quest.copy(
                            id = questId,
                            originalScheduledDate = firstDateOfWeek,
                            isRemoved = true
                        )
                    )
                )

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest,
                    start = firstDateOfWeek,
                    end = lastDateOfWeek,
                    questRepo = repo
                )
                quests.size.`should be`(6)
                quests.filter { it.scheduledDate.isEqual(firstDateOfWeek) }
                    .`should be empty`()
            }
        }

        describe("repeating weekly") {

        }

        describe("repeating monthly") {

        }

        describe("repeating yearly") {

        }
    }
})