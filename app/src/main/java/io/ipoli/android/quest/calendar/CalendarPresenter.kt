package io.ipoli.android.quest.calendar

import io.ipoli.android.common.mvi.BaseMviPresenter
import io.ipoli.android.common.mvi.ViewStateRenderer
import io.ipoli.android.common.text.CalendarFormatter
import io.ipoli.android.quest.calendar.CalendarViewState.DatePickerState.*
import io.ipoli.android.quest.calendar.CalendarViewState.StateType.*
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Created by Venelin Valkov <venelin@ipoli.io>
 * on 10/21/17.
 */
class CalendarPresenter(
    private val calendarFormatter: CalendarFormatter,
    coroutineContext: CoroutineContext
) : BaseMviPresenter<ViewStateRenderer<CalendarViewState>, CalendarViewState, CalendarIntent>(
    CalendarViewState(
        type = LOADING,
        currentDate = LocalDate.now(),
        datePickerState = INVISIBLE,
        adapterPosition = MID_POSITION
    ),
    coroutineContext
) {

    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM")

    override fun reduceState(intent: CalendarIntent, state: CalendarViewState): CalendarViewState =
        when (intent) {
            is LoadDataIntent -> {
                val date = intent.currentDate
                val (dayText, dateText) = formatDayAndDate(date)
                state.copy(
                    type = DATA_LOADED,
                    adapterPosition = MID_POSITION,
                    currentDate = intent.currentDate,
                    dayText = dayText,
                    monthText = monthFormatter.format(date),
                    dateText = dateText
                )
            }
            is ExpandToolbarIntent -> {
                when (state.datePickerState) {
                    INVISIBLE -> state.copy(type = DATE_PICKER_CHANGED, datePickerState = SHOW_WEEK)
                    else -> state.copy(type = DATE_PICKER_CHANGED, datePickerState = INVISIBLE)
                }
            }
            is ExpandToolbarWeekIntent -> {
                when (state.datePickerState) {
                    SHOW_WEEK -> state.copy(type = DATE_PICKER_CHANGED, datePickerState = SHOW_MONTH)
                    else -> state.copy(type = DATE_PICKER_CHANGED, datePickerState = SHOW_WEEK)
                }
            }
            is CalendarChangeDateIntent -> {
                val newDate = LocalDate.of(intent.year, intent.month, intent.day)
                val (dayText, dateText) = formatDayAndDate(newDate)
                state.copy(
                    type = CALENDAR_DATE_CHANGED,
                    currentDate = newDate,
                    dayText = dayText,
                    monthText = monthFormatter.format(newDate),
                    dateText = dateText,
                    adapterPosition = MID_POSITION
                )
            }
            is SwipeChangeDateIntent -> {
                val newDate = state.currentDate.plusDays((intent.position - state.adapterPosition).toLong())
                val (dayText, dateText) = formatDayAndDate(newDate)
                state.copy(
                    type = SWIPE_DATE_CHANGED,
                    currentDate = newDate,
                    dayText = dayText,
                    monthText = monthFormatter.format(newDate),
                    dateText = dateText,
                    adapterPosition = intent.position
                )
            }
            is ChangeMonthIntent -> {
                val newDate = LocalDate.of(intent.year, intent.month, 1)
                state.copy(
                    type = DEFAULT,
                    monthText = monthFormatter.format(newDate)
                )
            }
        }

    private fun formatDayAndDate(date: LocalDate): Pair<String, String> {
        val dayText = calendarFormatter.day(date)
        val dateText = calendarFormatter.date(date)
        return Pair(dayText, dateText)
    }

    companion object {
        const val MID_POSITION = 49
    }
}