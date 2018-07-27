package io.ipoli.android.habit.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import io.ipoli.android.R
import io.ipoli.android.common.text.CalendarFormatter
import io.ipoli.android.myPoliApp
import org.threeten.bp.LocalDate
import org.threeten.bp.format.TextStyle
import java.util.*

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 7/27/18.
 */
class HabitWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {

        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {

        val calendarFormatter = CalendarFormatter(myPoliApp.instance)

        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.getDisplayName(
            TextStyle.FULL, Locale.getDefault()
        )
        val date = calendarFormatter.dateWithoutYear(today)

        appWidgetIds.forEach {

            val rv = RemoteViews(context.packageName, R.layout.widget_habits)

            rv.setTextViewText(R.id.widgetDayOfWeek, dayOfWeek)
            rv.setTextViewText(R.id.widgetDate, date)

//            rv.setOnClickPendingIntent(R.id.widgetAgendaHeader, createStartAppIntent(context))
//            rv.setOnClickPendingIntent(R.id.widgetAgendaPet, createShowPetIntent(context))
//            rv.setOnClickPendingIntent(R.id.widgetAgendaAdd, createQuickAddIntent(context))
//
            rv.setRemoteAdapter(
                R.id.widgetHabitList,
                createHabitListIntent(context, it)
            )
//
//            rv.setPendingIntentTemplate(
//                R.id.widgetAgendaList,
//                createQuestClickIntent(context, it)
//            )

            rv.setEmptyView(R.id.widgetHabitList, R.id.widgetHabitEmpty)

            appWidgetManager.notifyAppWidgetViewDataChanged(it, R.id.widgetDayOfWeek)
            appWidgetManager.notifyAppWidgetViewDataChanged(it, R.id.widgetDate)

            appWidgetManager.notifyAppWidgetViewDataChanged(it, R.id.widgetAgendaList)
            appWidgetManager.updateAppWidget(it, rv)
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds)

    }

    private fun createHabitListIntent(context: Context, widgetId: Int) =
        Intent(context, HabitWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
}