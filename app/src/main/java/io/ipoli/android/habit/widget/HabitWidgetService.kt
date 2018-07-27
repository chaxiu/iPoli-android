package io.ipoli.android.habit.widget

import android.content.Intent
import android.widget.RemoteViewsService

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 7/27/18.
 */
class HabitWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent?) =
        HabitWidgetViewsFactory(applicationContext)
}