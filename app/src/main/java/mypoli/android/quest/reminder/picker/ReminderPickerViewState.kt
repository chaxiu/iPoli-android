package mypoli.android.quest.reminder.picker

import mypoli.android.common.mvi.Intent
import mypoli.android.common.mvi.ViewState
import mypoli.android.pet.PetAvatar

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 10/5/17.
 */

sealed class ReminderPickerIntent : Intent

data class LoadReminderDataIntent(val reminder: ReminderViewModel?) : ReminderPickerIntent()
data class ChangeMessageIntent(val message: String) : ReminderPickerIntent()
data class ChangeCustomTimeIntent(val timeValue: String) : ReminderPickerIntent()
data class ChangePredefinedTimeIntent(val index: Int) : ReminderPickerIntent()
data class ChangeTimeUnitIntent(val index: Int) : ReminderPickerIntent()
object PickReminderIntent : ReminderPickerIntent()

data class ReminderPickerViewState(
    val type: StateType,
    val message: String = "",
    val predefinedValues: List<String> = listOf(),
    val predefinedIndex: Int? = null,
    val timeValue: String = "",
    val timeUnits: List<String> = listOf(),
    val timeUnitIndex: Int? = null,
    val petAvatar: PetAvatar? = null,
    val viewModel: ReminderViewModel? = null
) : ViewState {

    enum class StateType {
        LOADING,
        NEW_REMINDER,
        EDIT_REMINDER,
        CUSTOM_TIME,
        NEW_VALUES,
        TIME_VALUE_VALIDATION_ERROR,
        FINISHED
    }

    companion object {
        val Loading = ReminderPickerViewState(StateType.LOADING)
    }
}