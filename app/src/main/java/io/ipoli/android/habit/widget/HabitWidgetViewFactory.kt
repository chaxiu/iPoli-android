package io.ipoli.android.habit.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
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
import org.threeten.bp.LocalDate
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

                val isCompleted = it.isCompletedFor(LocalDate.now())
                val iconColor =
                    if (isCompleted) R.color.md_white else AndroidColor.valueOf(it.color.name).color500
                val iconDrawable =
                    IconicsDrawable(context).normalIcon(icon, iconColor)

                setImageViewBitmap(R.id.habitIcon, iconDrawable.toBitmap())

//                setProgressBar(R.id.habitProgress, 2, 1, false)


                val d = context.getDrawable(R.drawable.widget_habit_progress)
                d.setColorFilter(
                    ContextCompat.getColor(context, R.color.md_purple_500),
                    PorterDuff.Mode.SRC_ATOP
                )
//                setImageViewBitmap(R.id.habitProgress, drawableToBitmap(d))
//                setImageViewBitmap(R.id.habitProgress, BitmapFactory.decodeResource(context.resources, R.drawable.widget_habit_progress))

                val b = context.getDrawable(R.drawable.completed_quest_progress_background)
                b.setColorFilter(
                    ContextCompat.getColor(context, R.color.md_purple_500),
                    PorterDuff.Mode.SRC_ATOP
                )
//                Timber.d("AAAA $b")
//                val bitmat = Bitmap.createBitmap()
//                (b as GradientDrawable).
//                setImageViewBitmap(R.id.completedHabit, drawableToBitmap(b))

                setBitmap(R.id.completedHabit, "setImageBitmap", drawableToBitmap(b))

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

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        var width = drawable.intrinsicWidth
        width = if (width > 0) width else 1
        var height = drawable.intrinsicHeight
        height = if (height > 0) height else 1

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
//        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }


    override fun getCount() = items.size

    override fun getViewTypeCount() = 1

    override fun onDestroy() {
    }

}