package mypoli.android.common

import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import mypoli.android.challenge.category.list.ChallengeListForCategoryAction
import mypoli.android.challenge.usecase.BuyChallengeUseCase
import mypoli.android.common.DataLoadedAction.PlayerChanged
import mypoli.android.common.DataLoadedAction.TodayQuestsChanged
import mypoli.android.common.di.Module
import mypoli.android.common.redux.Action
import mypoli.android.common.redux.Dispatcher
import mypoli.android.common.redux.Saga
import mypoli.android.myPoliApp
import mypoli.android.pet.store.PetStoreAction
import mypoli.android.pet.usecase.BuyPetUseCase
import mypoli.android.quest.Quest
import mypoli.android.quest.schedule.ScheduleAction
import mypoli.android.quest.schedule.agenda.AgendaAction
import mypoli.android.quest.schedule.agenda.AgendaReducer
import mypoli.android.quest.schedule.agenda.usecase.CreateAgendaItemsUseCase
import mypoli.android.quest.schedule.agenda.usecase.FindAgendaDatesUseCase
import mypoli.android.quest.schedule.calendar.CalendarAction
import mypoli.android.quest.usecase.CompleteQuestUseCase.Params.WithQuest
import org.threeten.bp.LocalDate
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 01/27/2018.
 */
class BuyPredefinedChallengeSaga : Saga<AppState>, Injects<Module> {

    private val buyChallengeUseCase by required { buyChallengeUseCase }

    override suspend fun execute(action: Action, state: AppState, dispatcher: Dispatcher) {
        inject(myPoliApp.module(myPoliApp.instance))
        val challenge = (action as ChallengeListForCategoryAction.BuyChallenge).challenge
        val result = buyChallengeUseCase.execute(BuyChallengeUseCase.Params(challenge))
        when (result) {
            is BuyChallengeUseCase.Result.ChallengeBought -> {
                dispatcher.dispatch(ChallengeListForCategoryAction.ChallengeBought(challenge))
            }

            BuyChallengeUseCase.Result.TooExpensive -> {
                dispatcher.dispatch(
                    ChallengeListForCategoryAction.ChallengeTooExpensive(
                        challenge
                    )
                )
            }
        }
    }

    override fun canHandle(action: Action) = action is ChallengeListForCategoryAction.BuyChallenge
}

class ChangePetSaga : Saga<AppState>, Injects<Module> {

    private val changePetUseCase by required { changePetUseCase }

    override suspend fun execute(action: Action, state: AppState, dispatcher: Dispatcher) {
        inject(myPoliApp.module(myPoliApp.instance))
        changePetUseCase.execute((action as PetStoreAction.ChangePet).pet)
    }

    override fun canHandle(action: Action) = action is PetStoreAction.ChangePet

}

class BuyPetSaga : Saga<AppState>, Injects<Module> {
    private val buyPetUseCase by required { buyPetUseCase }

    override suspend fun execute(action: Action, state: AppState, dispatcher: Dispatcher) {
        inject(myPoliApp.module(myPoliApp.instance))
        val result = buyPetUseCase.execute((action as PetStoreAction.BuyPet).pet)
        when (result) {
            is BuyPetUseCase.Result.PetBought -> {
                dispatcher.dispatch(PetStoreAction.PetBought)
            }
            BuyPetUseCase.Result.TooExpensive -> {
                dispatcher.dispatch(PetStoreAction.PetTooExpensive)
            }
        }
    }

    override fun canHandle(action: Action) = action is PetStoreAction.BuyPet
}

class CompleteQuestSaga : Saga<AppState>, Injects<Module> {

    private val completeQuestUseCase by required { completeQuestUseCase }

    override suspend fun execute(action: Action, state: AppState, dispatcher: Dispatcher) {
        inject(myPoliApp.module(myPoliApp.instance))
        if (action is AgendaAction.CompleteQuest) {
            val adapterPos = action.itemPosition
            val questItem =
                state.agendaState.agendaItems[adapterPos] as CreateAgendaItemsUseCase.AgendaItem.QuestItem
            completeQuestUseCase.execute(WithQuest(questItem.quest))
        }
    }

    override fun canHandle(action: Action) = action is AgendaAction.CompleteQuest
}

class UndoCompletedQuestSaga : Saga<AppState>, Injects<Module> {

    private val undoCompletedQuestUseCase by required { undoCompletedQuestUseCase }

    override suspend fun execute(action: Action, state: AppState, dispatcher: Dispatcher) {
        inject(myPoliApp.module(myPoliApp.instance))
        if (action is AgendaAction.UndoCompleteQuest) {
            val adapterPos = action.itemPosition
            val questItem =
                state.agendaState.agendaItems[adapterPos] as CreateAgendaItemsUseCase.AgendaItem.QuestItem
            undoCompletedQuestUseCase.execute(questItem.quest.id)
        }
    }

    override fun canHandle(action: Action) = action is AgendaAction.UndoCompleteQuest
}

class AgendaSaga : Saga<AppState>, Injects<Module> {

    private val findAgendaDatesUseCase by required { findAgendaDatesUseCase }
    private val createAgendaItemsUseCase by required { createAgendaItemsUseCase }
    private val questRepository by required { questRepository }

    private var scheduledQuestsChannel: ReceiveChannel<List<Quest>>? = null

    override suspend fun execute(action: Action, state: AppState, dispatcher: Dispatcher) {
        inject(myPoliApp.module(myPoliApp.instance))

        val agendaItems = state.agendaState.agendaItems

        when (action) {
            is AgendaAction.LoadBefore -> {
                val position = action.itemPosition
                val agendaItem = agendaItems[position]
                val agendaDate = agendaItem.startDate()
                val result = findAgendaDatesUseCase.execute(
                    FindAgendaDatesUseCase.Params.Before(
                        agendaDate,
                        AgendaReducer.ITEMS_BEFORE_COUNT
                    )
                )
                var start = agendaDate.minusMonths(3)
                (result as FindAgendaDatesUseCase.Result.Before).date?.let {
                    start = it
                }
                val end =
                    agendaItems[position + AgendaReducer.ITEMS_AFTER_COUNT - 1].startDate()
                listenForAgendaItems(start, end, dispatcher, agendaDate, false)
            }
            is AgendaAction.LoadAfter -> {
                val position = action.itemPosition
                val agendaItem = agendaItems[position]
                val agendaDate = agendaItem.startDate()
                val result = findAgendaDatesUseCase.execute(
                    FindAgendaDatesUseCase.Params.After(agendaDate, AgendaReducer.ITEMS_AFTER_COUNT)
                )
                val start = agendaItems[position - AgendaReducer.ITEMS_BEFORE_COUNT].startDate()
                var end = agendaDate.plusMonths(3)
                (result as FindAgendaDatesUseCase.Result.After).date?.let {
                    end = it
                }

                listenForAgendaItems(start, end, dispatcher, agendaDate, false)
            }
            is LoadDataAction.All -> {
                val agendaDate = state.appDataState.today
                val pair = findAllAgendaDates(agendaDate)
                val start = pair.first
                val end = pair.second

                listenForAgendaItems(start, end, dispatcher, agendaDate, true)
            }
            is ScheduleAction.ScheduleChangeDate -> {
                val agendaDate = LocalDate.of(action.year, action.month, action.day)
                val pair = findAllAgendaDates(agendaDate)
                val start = pair.first
                val end = pair.second
                listenForAgendaItems(start, end, dispatcher, agendaDate, true)
            }
            is CalendarAction.SwipeChangeDate -> {
                val currentPos = state.calendarState.adapterPosition
                val newPos = action.adapterPosition
                val curDate = state.scheduleState.currentDate
                val agendaDate = if (newPos < currentPos)
                    curDate.minusDays(1)
                else
                    curDate.plusDays(1)
                val pair = findAllAgendaDates(agendaDate)
                val start = pair.first
                val end = pair.second
                listenForAgendaItems(start, end, dispatcher, agendaDate, true)
            }
        }
    }

    private fun listenForAgendaItems(
        start: LocalDate,
        end: LocalDate,
        dispatcher: Dispatcher,
        agendaDate: LocalDate,
        changeCurrentAgendaItem: Boolean
    ) {

        var isFirstData = true
        launch {
            scheduledQuestsChannel?.cancel()
            scheduledQuestsChannel = questRepository.listenForScheduledBetween(
                start,
                end
            )
            scheduledQuestsChannel!!.consumeEach {
                val agendaItems = createAgendaItemsUseCase.execute(
                    CreateAgendaItemsUseCase.Params(
                        agendaDate,
                        it,
                        AgendaReducer.ITEMS_BEFORE_COUNT,
                        AgendaReducer.ITEMS_AFTER_COUNT
                    )
                )
                dispatcher.dispatch(
                    DataLoadedAction.AgendaItemsChanged(
                        start = start,
                        end = end,
                        agendaItems = agendaItems,
                        currentAgendaItemDate = if (changeCurrentAgendaItem && isFirstData) agendaDate else null
                    )
                )
                isFirstData = false
            }
        }
    }

    private fun findAllAgendaDates(
        agendaDate: LocalDate
    ): Pair<LocalDate, LocalDate> {
        val result = findAgendaDatesUseCase.execute(
            FindAgendaDatesUseCase.Params.All(
                agendaDate,
                AgendaReducer.ITEMS_BEFORE_COUNT,
                AgendaReducer.ITEMS_AFTER_COUNT
            )
        ) as FindAgendaDatesUseCase.Result.All
        val start = result.start ?: agendaDate.minusMonths(3)
        val end = result.end ?: agendaDate.plusMonths(3)
        return Pair(start, end)
    }

    override fun canHandle(action: Action) =
        action == LoadDataAction.All
            || action is AgendaAction.LoadBefore
            || action is AgendaAction.LoadAfter
            || action is ScheduleAction.ScheduleChangeDate
            || action is CalendarAction.SwipeChangeDate

}

class LoadAllDataSaga : Saga<AppState>, Injects<Module> {

    private val playerRepository by required { playerRepository }
    private val questRepository by required { questRepository }

    override suspend fun execute(action: Action, state: AppState, dispatcher: Dispatcher) {
        inject(myPoliApp.module(myPoliApp.instance))

        if (action == LoadDataAction.All) {
            launch {
                playerRepository.listen().consumeEach {
                    dispatcher.dispatch(PlayerChanged(it!!))
                }
            }
        }

        launch {
            questRepository.listenForScheduledAt(state.appDataState.today).consumeEach {
                dispatcher.dispatch(TodayQuestsChanged(it))
            }
        }

    }

    override fun canHandle(action: Action) =
        action == LoadDataAction.All
}