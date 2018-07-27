package io.ipoli.android.habit.widget

import android.content.Context
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.mikepenz.iconics.IconicsDrawable
import io.ipoli.android.R
import io.ipoli.android.common.di.BackgroundModule
import io.ipoli.android.common.view.AndroidColor
import io.ipoli.android.common.view.AndroidIcon
import io.ipoli.android.common.view.normalIcon
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
                setTextViewText(R.id.habitName, it.name)
                val icon = it.icon.let { AndroidIcon.valueOf(it.name).icon }

                val iconColor = AndroidColor.valueOf(it.color.name).color500
                val iconDrawable =
                    IconicsDrawable(context).normalIcon(icon, iconColor)

                setImageViewBitmap(R.id.habitIcon, iconDrawable.toBitmap())

                setProgressBar(R.id.habitProgress, 2, 1, false)


//                val progressDrawable = drawable.getDrawable(1)
//                progressDrawable.setColorFilter(
//                    iconColor,
//                    PorterDuff.Mode.SRC_ATOP
//                )
//
//                this.

            }
        }
    }


    override fun getCount() = items.size

    override fun getViewTypeCount() = 1

    override fun onDestroy() {
    }

}