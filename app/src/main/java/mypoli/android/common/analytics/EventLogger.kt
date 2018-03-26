package mypoli.android.common.analytics

import android.app.Activity
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 03/26/2018.
 */
interface EventLogger {
    fun logEvent(name: String, params: Bundle = Bundle())
    fun logCurrentScreen(activity: Activity, screenName: String, screenClass: Class<*>)
    fun setPlayerId(playerId: String)
}

class FirebaseEventLogger(private val firebaseAnalytics: FirebaseAnalytics) : EventLogger {

    override fun logEvent(name: String, params: Bundle) =
        firebaseAnalytics.logEvent(name, params)

    override fun logCurrentScreen(activity: Activity, screenName: String, screenClass: Class<*>) =
        firebaseAnalytics.setCurrentScreen(activity, screenName, screenClass.name)

    override fun setPlayerId(playerId: String) =
        firebaseAnalytics.setUserId(playerId)
}