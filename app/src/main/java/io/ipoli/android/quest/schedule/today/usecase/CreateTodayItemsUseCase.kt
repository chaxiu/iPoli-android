package io.ipoli.android.quest.schedule.today.usecase

import io.ipoli.android.Constants
import io.ipoli.android.common.UseCase
import io.ipoli.android.quest.Quest

class CreateTodayItemsUseCase :
    UseCase<CreateTodayItemsUseCase.Params, CreateTodayItemsUseCase.Result> {

    override fun execute(parameters: Params): Result {
        val quests = parameters.quests

        val (complete, incomplete) = quests.partition { it.isCompleted }

        val (scheduled, unscheduled) = incomplete.partition { it.isScheduled }

        val (morning, other) = scheduled.partition { it.startTime!! < Constants.AFTERNOON_TIME_START }
        val (afternoon, evening) = other.partition { it.startTime!! < Constants.EVENING_TIME_START }

        return Result(
            incomplete = createSectionWithQuests(TodayItem.UnscheduledSection, unscheduled) +
                createSectionWithQuests(TodayItem.MorningSection, morning) +
                createSectionWithQuests(TodayItem.AfternoonSection, afternoon) +
                createSectionWithQuests(TodayItem.EveningSection, evening)
            , complete = complete
        )
    }

    private fun createSectionWithQuests(
        sectionItem: TodayItem,
        quests: List<Quest>
    ): List<TodayItem> {
        if (quests.isEmpty()) {
            return emptyList()
        }
        val items = mutableListOf(sectionItem)
        items.addAll(quests.map { TodayItem.QuestItem(it) })
        return items
    }

    data class Params(val quests: List<Quest>)

    data class Result(val incomplete: List<TodayItem>, val complete: List<Quest>)

    sealed class TodayItem {

        object UnscheduledSection : TodayItem()
        object MorningSection : TodayItem()
        object AfternoonSection : TodayItem()
        object EveningSection : TodayItem()

        data class QuestItem(val quest: Quest) : TodayItem()

    }
}