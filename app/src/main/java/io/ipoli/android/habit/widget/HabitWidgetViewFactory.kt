package io.ipoli.android.habit.widget

import android.content.Context
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import io.ipoli.android.R
import io.ipoli.android.common.di.BackgroundModule
import io.ipoli.android.habit.data.Habit
import io.ipoli.android.myPoliApp
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 7/27/18.
 */
class HabitWidgetViewsFactory(private val context: Context) :
    RemoteViewsService.RemoteViewsFactory, Injects<BackgroundModule> {

    private val habitRepository by required { habitRepository }

    private var items = CopyOnWriteArrayList<Habit>()

    override fun onCreate() {
        inject(myPoliApp.backgroundModule(context))
    }

    override fun getLoadingView() = null

    override fun getItemId(position: Int): Long = position.toLong()

    override fun onDataSetChanged() {
        items.clear()
        items.addAll(habitRepository.findAll())
    }

    override fun hasStableIds() = true

    override fun getViewAt(position: Int): RemoteViews {
        return items[position].let {
            RemoteViews(context.packageName, R.layout.item_widget_habit).apply {

            }
        }
    }

    override fun getCount() = items.size

    override fun getViewTypeCount() = 1

    override fun onDestroy() {
    }

}