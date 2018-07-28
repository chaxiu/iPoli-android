package io.ipoli.android.common.middleware

import io.ipoli.android.common.AppState
import io.ipoli.android.common.NamespaceAction
import io.ipoli.android.common.UiAction
import io.ipoli.android.common.di.BackgroundModule
import io.ipoli.android.common.redux.Action
import io.ipoli.android.common.redux.AsyncMiddleware
import io.ipoli.android.common.redux.Dispatcher
import io.ipoli.android.common.text.toSnakeCase
import io.ipoli.android.MyPoliApp
import io.ipoli.android.quest.show.QuestAction
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 03/25/2018.
 */
object LogEventsMiddleWare : AsyncMiddleware<AppState>, Injects<BackgroundModule> {

    private val eventLogger by required { eventLogger }

    override fun onCreate() {
        inject(MyPoliApp.backgroundModule(MyPoliApp.instance))
    }

    override fun onExecute(
        state: AppState,
        dispatcher: Dispatcher,
        action: Action
    ) {

        val a = (action as? NamespaceAction)?.source ?: action

        if (a === QuestAction.Tick || a is UiAction) {
            return
        }

        val params = action.toMap()
            .plus("current_state_data" to state.toString())

        eventLogger.logEvent(createEventName(a), params)
    }

    private fun createEventName(action: Action): String {
        return action.javaClass.canonicalName
            .replace("Action", "")
            .split(".")
            .filter { it[0].isUpperCase() }
            .joinToString("_")
            { it.toSnakeCase() }
    }

}