package mypoli.android.timer

import mypoli.android.common.datetime.Interval
import mypoli.android.common.datetime.Second
import mypoli.android.common.mvi.Intent
import mypoli.android.common.mvi.ViewState
import mypoli.android.quest.Quest

/**
 * Created by Venelin Valkov <venelin@ipoli.io>
 * on 6.01.18.
 */

sealed class TimerIntent : Intent {
    data class LoadData(val questId: String) : TimerIntent()
    data class QuestChanged(val quest: Quest) : TimerIntent()
    object Start : TimerIntent()
    object Stop : TimerIntent()
    object Tick : TimerIntent()
}

data class TimerViewState(
    val type: StateType,
    val showTimerTypeSwitch: Boolean = false,
    val timerLabel: String = "",
    val remainingTime: Interval<Second>? = null,
    val timerType: TimerType = TimerType.COUNTDOWN,
    val questName: String = "",
    val timerProgress: Int = 0,
    val maxTimerProgress: Int = 0,
    val pomodoroProgress: List<PomodoroProgress> = listOf()
) : ViewState {

    enum class StateType {
        LOADING,
        SHOW_POMODORO,
        SHOW_COUNTDOWN,
        TIMER_STARTED,
        TIMER_STOPPED,
        RUNNING
    }

    enum class TimerType {
        COUNTDOWN,
        POMODORO
    }
}