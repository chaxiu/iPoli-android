package io.ipoli.android.quest.schedule.today

import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.ipoli.android.MainActivity
import io.ipoli.android.R
import io.ipoli.android.common.redux.android.ReduxViewController
import io.ipoli.android.common.view.AppBarStateChangeListener
import io.ipoli.android.common.view.inflate
import io.ipoli.android.common.view.recyclerview.MultiViewRecyclerViewAdapter
import io.ipoli.android.common.view.recyclerview.RecyclerViewViewModel
import io.ipoli.android.common.view.setToolbar
import kotlinx.android.synthetic.main.controller_today.view.*
import org.threeten.bp.LocalDate
import space.traversal.kapsule.required

class TodayViewController(args: Bundle? = null) :
    ReduxViewController<TodayAction, TodayViewState, TodayReducer>(args = args) {

    override val reducer = TodayReducer

    private val imageLoader by required { imageLoader }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        applyStatusBarColors = false
        val view = container.inflate(R.layout.controller_today)

        setToolbar(view.toolbar)
        val collapsingToolbar = view.collapsingToolbarContainer
        collapsingToolbar.isTitleEnabled = false

        view.appbar.addOnOffsetChangedListener(object : AppBarStateChangeListener() {
            override fun onStateChanged(appBarLayout: AppBarLayout, state: State) {

                appBarLayout.post {
                    if (state == State.EXPANDED) {
                        val supportActionBar = (activity as MainActivity).supportActionBar
                        supportActionBar?.setDisplayShowTitleEnabled(false)
                    } else if (state == State.COLLAPSED) {
                        val supportActionBar = (activity as MainActivity).supportActionBar
                        supportActionBar?.setDisplayShowTitleEnabled(true)
                    }
                }

            }
        })

        imageLoader.loadMotivationalImage(
            imageUrl = "https://images.unsplash.com/photo-1500993855538-c6a99f437aa7?ixlib=rb-0.3.5&ixid=eyJhcHBfaWQiOjEyMDd9&s=72a0229d410f0e7c7701ebfc53b68a65&auto=format&fit=crop&w=1500&q=80",
            view = view.backdrop,
            onReady = {},
            onError = { _ -> }
        )

        view.todayItems.layoutManager = LinearLayoutManager(view.context)
        view.todayItems.adapter = TodayItemAdapter()

        return view
    }

    override fun onCreateLoadAction() = TodayAction.Load(LocalDate.now())

    override fun render(state: TodayViewState, view: View) {
        (view.todayItems.adapter as TodayItemAdapter).updateAll(
            listOf(
                TodayItemViewModel.Section("Morning"),
                TodayItemViewModel.Section("Afternoon"),
                TodayItemViewModel.Section("Evening")
            )
        )
    }

    sealed class TodayItemViewModel(override val id: String) : RecyclerViewViewModel {
        data class Section(val text: String) : TodayItemViewModel(text)
    }

    enum class ViewType {
        SECTION,
        QUEST,
        COMPLETED_QUEST
    }

    inner class TodayItemAdapter : MultiViewRecyclerViewAdapter<TodayItemViewModel>() {

        override fun onRegisterItemBinders() {
            registerBinder<TodayItemViewModel.Section>(
                ViewType.SECTION.ordinal,
                R.layout.item_list_section
            ) { vm, view, _ ->
                (view as TextView).text = vm.text
            }
        }

    }

}