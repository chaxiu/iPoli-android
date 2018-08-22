package io.ipoli.android.habit.sideeffect

import io.ipoli.android.common.AppSideEffectHandler
import io.ipoli.android.common.AppState
import io.ipoli.android.common.DataLoadedAction
import io.ipoli.android.common.redux.Action
import io.ipoli.android.habit.data.Habit
import io.ipoli.android.habit.edit.EditHabitAction
import io.ipoli.android.habit.edit.EditHabitViewState
import io.ipoli.android.habit.list.HabitListAction
import io.ipoli.android.habit.usecase.*
import space.traversal.kapsule.required

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 6/16/18.
 */
object HabitSideEffectHandler : AppSideEffectHandler() {

    private val saveHabitUseCase by required { saveHabitUseCase }
    private val completeHabitUseCase by required { completeHabitUseCase }
    private val undoCompleteHabitUseCase by required { undoCompleteHabitUseCase }
    private val removeHabitUseCase by required { removeHabitUseCase }
    private val createHabitItemsUseCase by required { createHabitItemsUseCase }

    override suspend fun doExecute(action: Action, state: AppState) {

        when (action) {

            EditHabitAction.Save -> {
                val s = state.stateFor(EditHabitViewState::class.java)
                saveHabitUseCase.execute(
                    SaveHabitUseCase.Params(
                        id = s.id,
                        name = s.name,
                        color = s.color,
                        icon = s.icon,
                        tags = s.habitTags,
                        days = s.days,
                        timesADay = s.timesADay,
                        isGood = s.isGood,
                        challengeId = s.challenge?.id,
                        note = s.note
                    )
                )
            }

            is EditHabitAction.Remove ->
                removeHabitUseCase.execute(RemoveHabitUseCase.Params(action.habitId))

            is HabitListAction.Load ->
                state.dataState.habits?.let {
                    dispatchNewHabitItems(it)
                }

            is DataLoadedAction.HabitsChanged ->
                dispatchNewHabitItems(action.habits)

            is HabitListAction.CompleteHabit ->
                completeHabitUseCase.execute(CompleteHabitUseCase.Params(habitId = action.habitId))

            is HabitListAction.UndoCompleteHabit ->
                undoCompleteHabitUseCase.execute(UndoCompleteHabitUseCase.Params(habitId = action.habitId))

            else -> {
            }
        }
    }

    private fun dispatchNewHabitItems(habits: List<Habit>) {
        val habitItems = createHabitItemsUseCase.execute(
            CreateHabitItemsUseCase.Params(
                habits
            )
        )
        dispatch(DataLoadedAction.HabitItemsChanged(habitItems))
    }

    override fun canHandle(action: Action) =
        action is EditHabitAction
            || action is HabitListAction || action is DataLoadedAction.HabitsChanged
}