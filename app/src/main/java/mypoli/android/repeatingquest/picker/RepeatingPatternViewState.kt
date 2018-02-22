package mypoli.android.repeatingquest.picker

import mypoli.android.common.AppState
import mypoli.android.common.BaseViewStateReducer
import mypoli.android.common.mvi.ViewState
import mypoli.android.common.redux.Action
import mypoli.android.repeatingquest.entity.RepeatingPattern
import mypoli.android.repeatingquest.picker.RepeatingPatternViewState.FrequencyType.*
import mypoli.android.repeatingquest.picker.RepeatingPatternViewState.StateType.*
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate

/**
 * Created by Polina Zhelyazkova <polina@ipoli.io>
 * on 2/21/18.
 */
sealed class RepeatingPatternAction : Action {
    data class LoadData(val repeatingPattern: RepeatingPattern?) : RepeatingPatternAction()
    data class ChangeFrequency(val index: Int) : RepeatingPatternAction()
    data class ToggleWeekDay(val weekDay: DayOfWeek) : RepeatingPatternAction()
    data class ChangeWeekDayCount(val index: Int) : RepeatingPatternAction()
    data class ToggleMonthDay(val day: Int) : RepeatingPatternAction()
    data class ChangeMonthDayCount(val index: Int) : RepeatingPatternAction()
    data class ChangeDayOfYear(val date: LocalDate) : RepeatingPatternAction()
    data class ChangeStartDate(val date: LocalDate) : RepeatingPatternAction()
    data class ChangeEndDate(val date: LocalDate) : RepeatingPatternAction()
}


object RepeatingPatternReducer : BaseViewStateReducer<RepeatingPatternViewState>() {

    override val stateKey = key<RepeatingPatternViewState>()

    override fun reduce(
        state: AppState,
        subState: RepeatingPatternViewState,
        action: Action
    ): RepeatingPatternViewState {
        return when (action) {
            is RepeatingPatternAction.LoadData -> {
                val pattern = action.repeatingPattern
                val frequencyType =
                    if (pattern != null) frequencyTypeFor(pattern)
                    else subState.frequencyType

                val selectedWeekDays =
                    if (pattern != null) selectedWeekDaysFor(pattern) else subState.selectedWeekDays
                val weekDaysCount = Math.max(1, selectedWeekDays.size)
                val weekDaysCountIndex = subState.weekCountValues.indexOfFirst { weekDaysCount == it }

                val selectedMonthDays =
                    if (pattern != null) selectedMonthDaysFor(pattern) else subState.selectedMonthDays
                val monthDaysCount = Math.max(1, selectedMonthDays.size)
                val monthDaysCountIndex = subState.monthCountValues.indexOfFirst { monthDaysCount == it }

                val startDate = pattern?.start ?: subState.startDate
                val endDate = pattern?.end ?: subState.endDate

                subState.copy(
                    type = DATA_LOADED,
                    frequencyType = frequencyType,
                    frequencyIndex = frequencyIndexFor(frequencyType),
                    weekDaysCountIndex = weekDaysCountIndex,
                    monthDaysCountIndex = monthDaysCountIndex,
                    selectedWeekDays = selectedWeekDays,
                    selectedMonthDays = selectedMonthDays,
                    isFlexible = pattern is RepeatingPattern.Flexible,
                    startDate = startDate,
                    endDate = endDate,
                    pickerEndDate = endDate ?: subState.pickerEndDate
                )
            }

            is RepeatingPatternAction.ChangeFrequency -> {
                val frequencyType = frequencyTypeForIndex(action.index)
                subState.copy(
                    type = FREQUENCY_CHANGED,
                    frequencyType = frequencyType,
                    frequencyIndex = frequencyIndexFor(frequencyType),
                    isFlexible = isFlexible(frequencyType, subState)
                )
            }

            is RepeatingPatternAction.ToggleWeekDay -> {
                val weekDay = action.weekDay
                val selectedWeekDays = if (subState.selectedWeekDays.contains(weekDay)) {
                    subState.selectedWeekDays.minus(weekDay)
                } else {
                    subState.selectedWeekDays.plus(weekDay)
                }

                subState.copy(
                    type = WEEK_DAYS_CHANGED,
                    selectedWeekDays = selectedWeekDays,
                    isFlexible = subState.weekCountValues[subState.weekDaysCountIndex] != selectedWeekDays.size
                )
            }

            is RepeatingPatternAction.ChangeWeekDayCount -> {
                subState.copy(
                    type = COUNT_CHANGED,
                    weekDaysCountIndex = action.index,
                    isFlexible = subState.weekCountValues[action.index] != subState.selectedWeekDays.size
                )
            }

            is RepeatingPatternAction.ToggleMonthDay -> {
                val day = action.day
                val selectedMonthDays = if (subState.selectedMonthDays.contains(day)) {
                    subState.selectedMonthDays.minus(day)
                } else {
                    subState.selectedMonthDays.plus(day)
                }
                subState.copy(
                    type = MONTH_DAYS_CHANGED,
                    selectedMonthDays = selectedMonthDays,
                    isFlexible = subState.monthCountValues[subState.monthDaysCountIndex] != selectedMonthDays.size
                )
            }

            is RepeatingPatternAction.ChangeMonthDayCount -> {
                subState.copy(
                    type = COUNT_CHANGED,
                    monthDaysCountIndex = action.index,
                    isFlexible = subState.monthCountValues[action.index] != subState.selectedMonthDays.size
                )
            }

            is RepeatingPatternAction.ChangeDayOfYear -> {
                subState.copy(
                    type = YEAR_DAY_CHANGED,
                    dayOfYear = action.date
                )
            }

            is RepeatingPatternAction.ChangeStartDate -> {
                subState.copy(
                    type = START_DATE_CHANGED,
                    startDate = action.date,
                    pickerEndDate = if (subState.endDate == null)
                        action.date.plusDays(1)
                    else subState.pickerEndDate
                )
            }

            is RepeatingPatternAction.ChangeEndDate -> {
                subState.copy(
                    type = END_DATE_CHANGED,
                    endDate = action.date,
                    pickerEndDate = action.date
                )
            }

            else -> {
                subState
            }
        }
    }

    private fun isFlexible(
        frequencyType: RepeatingPatternViewState.FrequencyType,
        state: RepeatingPatternViewState
    ) =
        when (frequencyType) {
            DAILY -> false
            YEARLY -> false
            WEEKLY -> {
                val daysCount = state.weekCountValues[state.weekDaysCountIndex]
                daysCount != state.selectedWeekDays.size
            }
            MONTHLY -> {
                val daysCount = state.monthCountValues[state.monthDaysCountIndex]
                daysCount != state.selectedMonthDays.size
            }
        }

    private fun frequencyTypeForIndex(index: Int) =
        when (index) {
            1 -> RepeatingPatternViewState.FrequencyType.WEEKLY
            2 -> RepeatingPatternViewState.FrequencyType.MONTHLY
            3 -> RepeatingPatternViewState.FrequencyType.YEARLY
            else -> RepeatingPatternViewState.FrequencyType.DAILY
        }

    private fun frequencyIndexFor(frequencyType: RepeatingPatternViewState.FrequencyType): Int =
        when (frequencyType) {
            RepeatingPatternViewState.FrequencyType.DAILY -> 0
            RepeatingPatternViewState.FrequencyType.WEEKLY -> 1
            RepeatingPatternViewState.FrequencyType.MONTHLY -> 2
            RepeatingPatternViewState.FrequencyType.YEARLY -> 3
        }

    private fun frequencyTypeFor(pattern: RepeatingPattern) =
        when (pattern) {
            RepeatingPattern.Daily -> RepeatingPatternViewState.FrequencyType.DAILY
            is RepeatingPattern.Weekly -> RepeatingPatternViewState.FrequencyType.WEEKLY
            is RepeatingPattern.Flexible.Weekly -> RepeatingPatternViewState.FrequencyType.WEEKLY
            is RepeatingPattern.Monthly -> RepeatingPatternViewState.FrequencyType.MONTHLY
            is RepeatingPattern.Flexible.Monthly -> RepeatingPatternViewState.FrequencyType.MONTHLY
            is RepeatingPattern.Yearly -> RepeatingPatternViewState.FrequencyType.YEARLY
        }


    private fun selectedWeekDaysFor(pattern: RepeatingPattern): Set<DayOfWeek> =
        when (pattern) {
            is RepeatingPattern.Weekly -> pattern.daysOfWeek
            is RepeatingPattern.Flexible.Weekly -> pattern.preferredDays
            else -> setOf()
        }

    private fun selectedMonthDaysFor(pattern: RepeatingPattern): Set<Int> =
        when (pattern) {
            is RepeatingPattern.Monthly -> pattern.daysOfMonth
            is RepeatingPattern.Flexible.Monthly -> pattern.preferredDays
            else -> setOf()
        }

    override fun defaultState() =
        RepeatingPatternViewState(
            LOADING,
            DAILY,
            frequencyIndex = 0,
            weekDaysCountIndex = 0,
            monthDaysCountIndex = 0,
            selectedWeekDays = setOf(),
            selectedMonthDays = setOf(),
            weekCountValues = (1..6).toList(),
            monthCountValues = (1..31).toList(),
            isFlexible = false,
            dayOfYear = LocalDate.now(),
            startDate = LocalDate.now(),
            endDate = null,
            pickerEndDate = LocalDate.now()
        )

}

data class RepeatingPatternViewState(
    val type: RepeatingPatternViewState.StateType,
    val frequencyType: FrequencyType,
    val frequencyIndex: Int,
    val weekDaysCountIndex: Int,
    val monthDaysCountIndex: Int,
    val selectedWeekDays: Set<DayOfWeek>,
    val selectedMonthDays: Set<Int>,
    val weekCountValues: List<Int>,
    val monthCountValues: List<Int>,
    val isFlexible: Boolean,
    val dayOfYear: LocalDate,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val pickerEndDate: LocalDate
    ) : ViewState {
    enum class StateType {
        LOADING,
        DATA_LOADED,
        FREQUENCY_CHANGED,
        WEEK_DAYS_CHANGED,
        COUNT_CHANGED,
        MONTH_DAYS_CHANGED,
        YEAR_DAY_CHANGED,
        START_DATE_CHANGED,
        END_DATE_CHANGED
    }

    enum class FrequencyType {
        DAILY,
        WEEKLY,
        MONTHLY,
        YEARLY
    }
}