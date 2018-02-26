package mypoli.android.common

import mypoli.android.common.redux.Action
import mypoli.android.common.redux.Reducer
import mypoli.android.common.redux.State
import mypoli.android.player.Player
import mypoli.android.quest.Quest
import mypoli.android.quest.schedule.agenda.usecase.CreateAgendaItemsUseCase
import mypoli.android.quest.usecase.Schedule
import mypoli.android.repeatingquest.entity.RepeatingQuest
import org.threeten.bp.LocalDate

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 01/24/2018.
 */

sealed class DataLoadedAction : Action {
    data class PlayerChanged(val player: Player) : DataLoadedAction()
    data class TodayQuestsChanged(val quests: List<Quest>) : DataLoadedAction()
    data class RepeatingQuestsChanged(val repeatingQuests: List<RepeatingQuest>) :
        DataLoadedAction()

    data class AgendaItemsChanged(
        val start: LocalDate,
        val end: LocalDate,
        val agendaItems: List<CreateAgendaItemsUseCase.AgendaItem>,
        val currentAgendaItemDate: LocalDate?
    ) : DataLoadedAction()

    data class ScheduledQuestsChanged(val scheduledQuests: Map<LocalDate, Schedule>) :
        DataLoadedAction()
}

data class AppDataState(
    val today: LocalDate,
    val player: Player?,
    val todayQuests: List<Quest>,
    val scheduledQuests: Map<LocalDate, Schedule>,
    val repeatingQuests: List<RepeatingQuest>
) : State

object AppDataReducer : Reducer<AppState, AppDataState> {

    override val stateKey = AppDataState::class.java

    override fun reduce(state: AppState, subState: AppDataState, action: Action) =
        when (action) {

            is DataLoadedAction.PlayerChanged -> {
                subState.copy(
                    player = action.player
                )
            }

            is DataLoadedAction.ScheduledQuestsChanged -> {
                subState.copy(
                    scheduledQuests = action.scheduledQuests
                )
            }

            is DataLoadedAction.TodayQuestsChanged -> {
                subState.copy(
                    todayQuests = action.quests
                )
            }

            is DataLoadedAction.RepeatingQuestsChanged -> {
                subState.copy(
                    repeatingQuests = action.repeatingQuests
                )
            }
            else -> subState
        }

    override fun defaultState(): AppDataState {
        return AppDataState(
            today = LocalDate.now(),
            player = null,
            todayQuests = listOf(),
            scheduledQuests = mapOf(),
            repeatingQuests = listOf()
        )
    }

}