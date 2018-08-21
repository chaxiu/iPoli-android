package io.ipoli.android.quest.schedule.today

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.design.widget.AppBarLayout
import android.support.v4.widget.TextViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.mikepenz.iconics.typeface.IIcon
import io.ipoli.android.MainActivity
import io.ipoli.android.R
import io.ipoli.android.common.ViewUtils
import io.ipoli.android.common.redux.android.ReduxViewController
import io.ipoli.android.common.view.*
import io.ipoli.android.common.view.recyclerview.MultiViewRecyclerViewAdapter
import io.ipoli.android.common.view.recyclerview.RecyclerViewViewModel
import kotlinx.android.synthetic.main.controller_today.view.*
import kotlinx.android.synthetic.main.item_agenda_quest.view.*
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

        view.backdropTransparentColor.background.alpha = (255f * 0.8).toInt()

        return view
    }

    override fun onCreateLoadAction() = TodayAction.Load(LocalDate.now())

    override fun render(state: TodayViewState, view: View) {
        (view.todayItems.adapter as TodayItemAdapter).updateAll(
            listOf(
                TodayItemViewModel.Section("Morning"),
                TodayItemViewModel.QuestViewModel(
                    id = "1234",
                    name = "Do the laundry",
                    tags = emptyList(),
                    startTime = "9:30 - 10:00",
                    color = AndroidColor.BLUE.color500,
                    icon = AndroidIcon.WASHING_MACHINE.icon,
                    isRepeating = false,
                    isFromChallenge = false
                ),
                TodayItemViewModel.QuestViewModel(
                    id = "12345",
                    name = "Design the Today screen",
                    tags = emptyList(),
                    startTime = "10:15 - 12:00",
                    color = AndroidColor.RED.color500,
                    icon = AndroidIcon.BRIEFCASE.icon,
                    isRepeating = false,
                    isFromChallenge = true
                ),
                TodayItemViewModel.Section("Afternoon"),
                TodayItemViewModel.QuestViewModel(
                    id = "123456",
                    name = "Lunch with Jim",
                    tags = emptyList(),
                    startTime = "12:30 - 14:00",
                    color = AndroidColor.ORANGE.color500,
                    icon = AndroidIcon.RESTAURANT.icon,
                    isRepeating = false,
                    isFromChallenge = false
                ),
                TodayItemViewModel.Section("Evening"),
                TodayItemViewModel.QuestViewModel(
                    id = "1234567",
                    name = "Watch Deadpool 2",
                    tags = emptyList(),
                    startTime = "20:30 - 23:00",
                    color = AndroidColor.PURPLE.color500,
                    icon = AndroidIcon.CAMERA.icon,
                    isRepeating = false,
                    isFromChallenge = false
                )
            )
        )
    }

    data class TagViewModel(val name: String, @ColorRes val color: Int)

    sealed class TodayItemViewModel(override val id: String) : RecyclerViewViewModel {
        data class Section(val text: String) : TodayItemViewModel(text)

        data class QuestViewModel(
            override val id: String,
            val name: String,
            val tags: List<TagViewModel>,
            val startTime: String,
            @ColorRes val color: Int,
            val icon: IIcon,
            val isRepeating: Boolean,
            val isFromChallenge: Boolean
        ) : TodayItemViewModel(id)
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

            registerBinder<TodayItemViewModel.QuestViewModel>(
                ViewType.QUEST.ordinal,
                R.layout.item_agenda_quest
            ) { vm, view, _ ->
                view.questName.text = vm.name

                view.questIcon.backgroundTintList =
                    ColorStateList.valueOf(colorRes(vm.color))
                view.questIcon.setImageDrawable(listItemIcon(vm.icon))

                if (vm.tags.isNotEmpty()) {
                    view.questTagName.visible()
                    renderTag(view, vm.tags.first())
                } else {
                    view.questTagName.gone()
                }

                view.questStartTime.text = vm.startTime

                view.questRepeatIndicator.visibility =
                    if (vm.isRepeating) View.VISIBLE else View.GONE
                view.questChallengeIndicator.visibility =
                    if (vm.isFromChallenge) View.VISIBLE else View.GONE
            }
        }

        private fun renderTag(view: View, tag: TagViewModel) {
            view.questTagName.text = tag.name
            TextViewCompat.setTextAppearance(
                view.questTagName,
                R.style.TextAppearance_AppCompat_Caption
            )

            val indicator = view.questTagName.compoundDrawablesRelative[0] as GradientDrawable
            indicator.mutate()
            val size = ViewUtils.dpToPx(8f, view.context).toInt()
            indicator.setSize(size, size)
            indicator.setColor(colorRes(tag.color))
            view.questTagName.setCompoundDrawablesRelativeWithIntrinsicBounds(
                indicator,
                null,
                null,
                null
            )
        }

    }

}