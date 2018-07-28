package io.ipoli.android.habit.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import io.ipoli.android.R

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

        appWidgetIds.forEach {

            val rv = RemoteViews(context.packageName, R.layout.widget_habits)

            rv.setRemoteAdapter(
                R.id.widgetHabitList,
                createHabitListIntent(context, it)
            )
            rv.setEmptyView(R.id.widgetHabitList, R.id.widgetHabitEmpty)

            appWidgetManager.notifyAppWidgetViewDataChanged(it, R.id.widgetHabitList)
            appWidgetManager.updateAppWidget(it, rv)
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds)

    }

    private fun createHabitListIntent(context: Context, widgetId: Int) =
        Intent(context, HabitWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
}