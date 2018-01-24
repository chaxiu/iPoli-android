package mypoli.android.common.redux

import mypoli.android.common.redux.MiddleWare.Result.Continue

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 01/20/2018.
 */

interface Action

interface AsyncAction : Action {
    suspend fun execute(dispatcher: Dispatcher)
}

interface State

interface PartialState

interface Reducer<S : State, in A : Action> {

    fun reduce(state: S, action: A): S

    fun defaultState(): S
}

interface PartialReducer<in S : State, out P : PartialState, in A : Action> {

    fun reduce(state: S, action: A): P

    fun defaultState(): P
}

interface Dispatcher {
    fun <A : Action> dispatch(action: A)
}

class StateStore<out S : State>(
    private val reducer: Reducer<S, Action>,
    middleware: List<MiddleWare<S>> = listOf()
) : Dispatcher {

    interface StateChangeSubscriber<in S, T> {

        interface StateTransformer<in S, out T> {

            fun transformInitial(state: S): T {
                return transform(state)
            }

            fun transform(state: S): T
        }

        val transformer: StateTransformer<S, T>

        fun changeIfNew(oldState: S, newState: S) {
            val old = transformer.transform(oldState)
            val new = transformer.transform(newState)
            if (old != new) onStateChanged(new)
        }

        fun onStateChanged(newState: T)
    }

    interface SimpleStateChangeSubscriber<S> : StateChangeSubscriber<S, S> {
        override val transformer
            get() = object : StateChangeSubscriber.StateTransformer<S, S> {
                override fun transform(state: S) = state
            }
    }

    private var stateChangeSubscribers: List<StateChangeSubscriber<S, *>> = listOf()
    private var state = reducer.defaultState()
    private val middleWare = CompositeMiddleware<S>(middleware)

    override fun <A : Action> dispatch(action: A) {
        if (!middleWare.canHandle(action)) {
            changeState(action)
            return
        }
        val res = middleWare.execute(state, this, action)
        if (res == Continue) changeState(action)
    }

    private fun changeState(action: Action) {
        val newState = reducer.reduce(state, action)
        val oldState = state
        state = newState
        notifyStateChanged(oldState, newState)
    }

    private fun notifyStateChanged(oldState: S, newState: S) {
        stateChangeSubscribers.forEach {
            it.changeIfNew(oldState, newState)
        }
    }

    fun <T> subscribe(subscriber: StateChangeSubscriber<S, T>) {
        stateChangeSubscribers += subscriber
        subscriber.onStateChanged(subscriber.transformer.transformInitial(state))
    }

    fun <T> unsubscribe(subscriber: StateChangeSubscriber<S, T>) {
        stateChangeSubscribers -= subscriber
    }
}