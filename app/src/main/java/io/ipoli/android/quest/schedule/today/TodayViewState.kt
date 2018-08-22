package io.ipoli.android.quest.schedule.today

import io.ipoli.android.common.AppState
import io.ipoli.android.common.BaseViewStateReducer
import io.ipoli.android.common.DataLoadedAction
import io.ipoli.android.common.redux.Action
import io.ipoli.android.common.redux.BaseViewState
import io.ipoli.android.habit.usecase.CreateHabitItemsUseCase
import io.ipoli.android.quest.Quest
import org.threeten.bp.LocalDate

sealed class TodayAction : Action {
    data class Load(val today: LocalDate) : TodayAction()
}

object TodayReducer : BaseViewStateReducer<TodayViewState>() {

    override val stateKey = key<TodayViewState>()

    override fun reduce(state: AppState, subState: TodayViewState, action: Action) =
        when (action) {

            is TodayAction.Load ->
                state.dataState.todayQuests?.let {
                    subState.copy(
                        type = TodayViewState.StateType.QUESTS_CHANGED,
                        quests = it
                    )
                } ?: subState

            is DataLoadedAction.TodayQuestsChanged ->
                subState.copy(
                    type = TodayViewState.StateType.QUESTS_CHANGED,
                    quests = action.quests
                )

            is DataLoadedAction.HabitItemsChanged ->
                subState.copy(
                    type = TodayViewState.StateType.HABITS_CHANGED,
                    todayHabitItems = action.habitItems
                        .filterIsInstance(CreateHabitItemsUseCase.HabitItem.Today::class.java)
                )

            else -> subState
        }

    override fun defaultState() =
        TodayViewState(
            type = TodayViewState.StateType.LOADING,
            quests = null,
            todayHabitItems = null
        )
}

data class TodayViewState(
    val type: StateType,
    val quests: List<Quest>?,
    val todayHabitItems: List<CreateHabitItemsUseCase.HabitItem.Today>?
) :
    BaseViewState() {

    enum class StateType {
        LOADING,
        HABITS_CHANGED,
        QUESTS_CHANGED
    }
}