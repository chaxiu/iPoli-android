package io.ipoli.android.theme

import android.content.res.ColorStateList
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.view.PagerAdapter
import android.view.*
import io.ipoli.android.MainActivity
import io.ipoli.android.R
import io.ipoli.android.common.ViewUtils
import io.ipoli.android.common.mvi.MviViewController
import io.ipoli.android.common.view.AndroidTheme
import io.ipoli.android.common.view.showBackButton
import io.ipoli.android.common.view.stringRes
import io.ipoli.android.common.view.visible
import io.ipoli.android.player.Theme
import io.ipoli.android.quest.calendar.dayview.view.widget.CalendarDayView
import io.ipoli.android.theme.ThemeStoreViewState.StateType.*
import kotlinx.android.synthetic.main.calendar_hour_cell.view.*
import kotlinx.android.synthetic.main.calendar_time_line.view.*
import kotlinx.android.synthetic.main.controller_theme_store.view.*
import kotlinx.android.synthetic.main.item_theme_store.view.*
import space.traversal.kapsule.required

/**
 * Created by Venelin Valkov <venelin@ipoli.io>
 * on 12/12/17.
 */
class ThemeStoreViewController(args: Bundle? = null) :
    MviViewController<ThemeStoreViewState, ThemeStoreViewController, ThemeStorePresenter, ThemeStoreIntent>(args) {

    var currentPosition = -1

    private val presenter by required { themeStorePresenter }

    override fun createPresenter() = presenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        setHasOptionsMenu(true)

        val view = inflater.inflate(R.layout.controller_theme_store, container, false)

        (activity as MainActivity).supportActionBar?.title = stringRes(R.string.themes)

        view.themePager.clipToPadding = false
        view.themePager.pageMargin = ViewUtils.dpToPx(32f, view.context).toInt()
        return view
    }

    override fun onSaveViewState(view: View, outState: Bundle) {
        outState.putInt("themePosition", view.themePager.currentItem)
        super.onSaveViewState(view, outState)
    }

    override fun onRestoreViewState(view: View, savedViewState: Bundle) {
        currentPosition = savedViewState.getInt("themePosition")
    }

    override fun onAttach(view: View) {
        showBackButton()
        super.onAttach(view)
        send(LoadDataIntent)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.actionThemes).isVisible = false
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            router.popCurrentController()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun render(state: ThemeStoreViewState, view: View) {
        when (state.type) {
            DATA_LOADED -> {
                val themeAdapter = ThemePagerAdapter(state.viewModels)
                view.themePager.adapter = themeAdapter
            }

            PLAYER_CHANGED -> {
                (view.themePager.adapter as ThemePagerAdapter).updateAll(state.viewModels)
                if(currentPosition >= 0) {
                    view.themePager.currentItem = currentPosition
                }
            }

            THEME_CHANGED -> {
                
            }
        }
    }

    inner class ThemePagerAdapter(private var viewModels: List<ThemeViewModel>) : PagerAdapter() {

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val inflater = LayoutInflater.from(container.context)
            val view = inflater.inflate(R.layout.item_theme_store, container, false)
            val vm = viewModels[position]

            val theme = AndroidTheme.valueOf(vm.theme.name)

            val attrs = intArrayOf(R.attr.colorPrimary, R.attr.colorPrimaryDark, R.attr.colorAccent).sortedArray()
            val a = activity!!.theme.obtainStyledAttributes(
                theme.style,
                attrs)

            val primaryColor = a.getResourceId(a.getIndex(attrs.indexOf(R.attr.colorPrimary)), 0)
            val primaryDarkColor = a.getResourceId(a.getIndex(attrs.indexOf(R.attr.colorPrimaryDark)), 0)
            val accentColor = a.getColor(a.getIndex(attrs.indexOf(R.attr.colorAccent)), 0)

            view.themeToolbar.setBackgroundResource(primaryColor)
            view.themeNavigationBar.setBackgroundResource(primaryColor)
            view.themeStatusBar.setBackgroundResource(primaryDarkColor)
            view.themeFab.backgroundTintList = ColorStateList.valueOf(accentColor)

            a.recycle()

            view.themeName.setText(theme.title)
            view.themePrice.text = vm.theme.price.toString()

            val action = view.themeAction
            val current = view.themeCurrent

            when {
                vm.isCurrent -> {
                    action.visible = false
                    current.visible = true
                }
                vm.isBought -> {
                    action.visible = true
                    current.visible = false
                    action.text = stringRes(R.string.store_theme_in_inventory)
                    action.setOnClickListener {
                        send(ChangeThemeIntent(vm.theme))
                    }
                }
                else -> {
                    action.visible = true
                    current.visible = false
                    action.text = stringRes(R.string.store_buy_theme)

                    action.setOnClickListener {
                        send(BuyThemeIntent(vm.theme))
                        val pm = PreferenceManager.getDefaultSharedPreferences(activity!!)
                        pm.edit().putString("currentTheme", theme.name).commit()
//                        router.popCurrentController()
                        activity!!.recreate()

                    }
                }
            }

            view.themeCalendar.setHourAdapter(object : CalendarDayView.HourCellAdapter {
                override fun bind(view: View, hour: Int) {
                    if (hour > 0) {
                        view.timeLabel.text = hour.toString() + ":00"
                    }
                }
            })

            view.themeCalendar.timeLine.setBackgroundColor(accentColor)
            view.themeCalendar.timeLineIndicator.backgroundTintList = ColorStateList.valueOf(accentColor)

            view.themeCalendar.scrollToNow()

            container.addView(view)
            return view
        }

        override fun destroyItem(container: ViewGroup, position: Int, view: Any) {
            container.removeView(view as View)
        }

        override fun isViewFromObject(view: View, `object`: Any) = view == `object`

        override fun getCount() = viewModels.size

        override fun getItemPosition(`object`: Any) = PagerAdapter.POSITION_NONE

        fun updateAll(viewModels: List<ThemeViewModel>) {
            this.viewModels = viewModels
            notifyDataSetChanged()
        }
    }

}

data class ThemeViewModel(
    val theme: Theme,
    val isBought: Boolean = false,
    val isCurrent: Boolean = false
)