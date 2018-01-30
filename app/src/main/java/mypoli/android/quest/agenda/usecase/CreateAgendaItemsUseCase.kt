package mypoli.android.quest.agenda.usecase

import mypoli.android.common.UseCase
import mypoli.android.common.datetime.DateUtils
import mypoli.android.quest.Quest
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 01/29/2018.
 */
class CreateAgendaItemsUseCase :
    UseCase<CreateAgendaItemsUseCase.Params, List<CreateAgendaItemsUseCase.AgendaItem>> {

    override fun execute(parameters: Params): List<CreateAgendaItemsUseCase.AgendaItem> {

        val scheduledQuests = parameters.scheduledQuests.groupBy { it.scheduledDate }

        val beforeItems =
            createItems(
                parameters.date.minusDays(1),
                scheduledQuests,
                parameters.itemsBefore,
                { it.minusDays(1) },
                parameters.firstDayOfWeek
            )
        val afterItems =
            createItems(
                parameters.date,
                scheduledQuests,
                parameters.itemsAfter,
                { it.plusDays(1) },
                parameters.firstDayOfWeek
            )
        return beforeItems + afterItems
    }

    private fun createItems(
        startDate: LocalDate,
        scheduledQuests: Map<LocalDate, List<Quest>>,
        itemsToFill: Int,
        nextDate: (LocalDate) -> LocalDate,
        firstDayOfWeek: DayOfWeek
    ): MutableList<AgendaItem> {
        val items = mutableListOf<AgendaItem>()
        var currentDate = startDate
        while (items.size < itemsToFill) {

            val dateItems = mutableListOf<AgendaItem>()

            addWeekItem(currentDate, firstDayOfWeek, dateItems)
            addMonthItem(currentDate, dateItems)
            addDateAndQuestItems(scheduledQuests, currentDate, dateItems)

            val newDate = nextDate(currentDate)

            if (newDate > currentDate) items.addAll(dateItems)
            else items.addAll(0, dateItems)

            currentDate = newDate
        }

        return items
    }

    private fun addDateAndQuestItems(
        scheduledQuests: Map<LocalDate, List<Quest>>,
        currentDate: LocalDate,
        dateItems: MutableList<AgendaItem>
    ) {
        if (scheduledQuests.containsKey(currentDate)) {
            dateItems.add(AgendaItem.Date(currentDate))
            scheduledQuests[currentDate]!!.forEach { q -> dateItems.add(AgendaItem.QuestItem(q)) }
        }
    }

    private fun addMonthItem(
        currentDate: LocalDate,
        dateItems: MutableList<AgendaItem>
    ) {
        if (currentDate.dayOfMonth == 1) {
            dateItems.add(
                AgendaItem.Month(
                    YearMonth.of(
                        currentDate.year,
                        currentDate.monthValue
                    )
                )
            )
        }
    }

    private fun addWeekItem(
        currentDate: LocalDate,
        firstDayOfWeek: DayOfWeek,
        dateItems: MutableList<AgendaItem>
    ) {
        if (currentDate.dayOfWeek == firstDayOfWeek) {
            dateItems.add(AgendaItem.Week(currentDate, currentDate.plusDays(6)))
        }
    }

    data class Params(
        val date: LocalDate,
        val scheduledQuests: List<Quest>,
        val itemsAfter: Int,
        val itemsBefore: Int,
        val firstDayOfWeek: DayOfWeek = DateUtils.firstDayOfWeek
    )

    sealed class AgendaItem {
        data class QuestItem(val quest: Quest) : AgendaItem()
        data class Date(val date: LocalDate) : AgendaItem()
        data class Week(val start: LocalDate, val end: LocalDate) : AgendaItem()
        data class Month(val month: YearMonth) : AgendaItem()
    }
}