package io.ipoli.android.quest.schedule.today

import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.ipoli.android.MainActivity
import io.ipoli.android.R
import io.ipoli.android.common.redux.android.ReduxViewController
import io.ipoli.android.common.view.AppBarStateChangeListener
import io.ipoli.android.common.view.inflate
import io.ipoli.android.common.view.setToolbar
import kotlinx.android.synthetic.main.controller_today.view.*
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

        return view
    }

    override fun render(state: TodayViewState, view: View) {

    }

}