package io.ipoli.android.common.view

import android.support.annotation.*
import android.support.v4.content.ContextCompat
import android.util.TypedValue
import com.bluelinelabs.conductor.Controller
import io.ipoli.android.MainActivity

/**
 * Created by Venelin Valkov <venelin@ipoli.io>
 * on 10/7/17.
 */
fun Controller.stringRes(@StringRes stringRes: Int): String =
    resources!!.getString(stringRes)

fun Controller.stringRes(@StringRes stringRes: Int, vararg formatArgs: Any): String =
    resources!!.getString(stringRes, *formatArgs)

fun Controller.stringsRes(@ArrayRes stringArrayRes: Int): List<String> =
    resources!!.getStringArray(stringArrayRes).toList()

fun Controller.colorRes(@ColorRes colorRes: Int): Int =
    ContextCompat.getColor(activity!!, colorRes)

fun Controller.intRes(@IntegerRes res: Int): Int =
    resources!!.getInteger(res)

val Controller.shortAnimTime: Long
    get() = resources!!.getInteger(android.R.integer.config_shortAnimTime).toLong()

val Controller.mediumAnimTime: Long
    get() = resources!!.getInteger(android.R.integer.config_mediumAnimTime).toLong()

val Controller.longAnimTime: Long
    get() = resources!!.getInteger(android.R.integer.config_longAnimTime).toLong()

fun Controller.showBackButton() {
    (activity!! as MainActivity).showBackButton()
}

fun Controller.hideBackButton() {
    (activity!! as MainActivity).hideBackButton()
}

fun Controller.attr(@AttrRes attributeRes: Int) =
    TypedValue().let {
        activity!!.theme.resolveAttribute(attributeRes, it, true)
        it.data
    }