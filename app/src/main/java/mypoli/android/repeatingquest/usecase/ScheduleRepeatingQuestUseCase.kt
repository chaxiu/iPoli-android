package mypoli.android.repeatingquest.usecase

import mypoli.android.common.UseCase
import mypoli.android.quest.Quest
import mypoli.android.quest.data.persistence.QuestRepository
import mypoli.android.repeatingquest.entity.RepeatingQuest
import org.threeten.bp.LocalDate

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 02/14/2018.
 */
class ScheduleRepeatingQuestUseCase(private val questRepository: QuestRepository) :
    UseCase<ScheduleRepeatingQuestUseCase.Params, List<Quest>> {

    override fun execute(parameters: Params): List<Quest> {

        val (rq, start, end) = parameters

        require(end.isAfter(start))

        return listOf()
    }

    /**
     * @start inclusive
     * @end exlusive
     */
    data class Params(
        val repeatingQuest: RepeatingQuest,
        val start: LocalDate,
        val end: LocalDate
    )
}