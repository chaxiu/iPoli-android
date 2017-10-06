package io.ipoli.android.quest.calendar.dayview.ui.widget

import io.ipoli.android.common.ui.Color

/**
 * Created by Venelin Valkov <venelin@ipoli.io>
 * on 9/2/17.
 */
interface CalendarEvent {
    val duration: Int

    val startMinute: Int

    val name: String

    val backgroundColor: Color
}