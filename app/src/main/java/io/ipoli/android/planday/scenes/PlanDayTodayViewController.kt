package io.ipoli.android.planday.scenes

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.v4.widget.TextViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.*
import android.widget.TextView
import com.bluelinelabs.conductor.RouterTransaction
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.ionicons_typeface_library.Ionicons
import io.ipoli.android.Constants
import io.ipoli.android.R
import io.ipoli.android.common.ViewUtils
import io.ipoli.android.common.home.HomeViewController
import io.ipoli.android.common.redux.android.BaseViewController
import io.ipoli.android.common.text.DurationFormatter
import io.ipoli.android.common.text.QuestStartTimeFormatter
import io.ipoli.android.common.view.*
import io.ipoli.android.common.view.recyclerview.BaseRecyclerViewAdapter
import io.ipoli.android.common.view.recyclerview.RecyclerViewViewModel
import io.ipoli.android.common.view.recyclerview.SimpleSwipeCallback
import io.ipoli.android.common.view.recyclerview.SimpleViewHolder
import io.ipoli.android.planday.PlanDayAction
import io.ipoli.android.planday.PlanDayReducer
import io.ipoli.android.planday.PlanDayViewState
import io.ipoli.android.planday.RescheduleDialogController
import io.ipoli.android.quest.schedule.addquest.AddQuestAnimationHelper
import kotlinx.android.synthetic.main.animation_empty_list.view.*
import kotlinx.android.synthetic.main.controller_plan_day_today.view.*
import kotlinx.android.synthetic.main.item_plan_today_quest.view.*
import kotlinx.android.synthetic.main.item_plan_today_suggestion.view.*
import kotlinx.android.synthetic.main.view_default_toolbar.view.*
import kotlinx.android.synthetic.main.view_loader.view.*
import org.threeten.bp.LocalDate

class PlanDayTodayViewController(args: Bundle? = null) :
    BaseViewController<PlanDayAction, PlanDayViewState>(args) {

    override val stateKey = PlanDayReducer.stateKey

    private lateinit var addQuestAnimationHelper: AddQuestAnimationHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        val view = container.inflate(R.layout.controller_plan_day_today)
        setToolbar(view.toolbar)
        toolbarTitle = stringRes(R.string.plan_my_day)
        view.todayQuests.layoutManager =
            LinearLayoutManager(
                container.context,
                LinearLayoutManager.VERTICAL,
                false
            )
        view.todayQuests.adapter = QuestAdapter()

        view.suggestionQuests.layoutManager =
            LinearLayoutManager(
                container.context,
                LinearLayoutManager.VERTICAL,
                false
            )
        view.suggestionQuests.adapter = SuggestionAdapter()

        initSwipe(view)
        initAddQuest(view)
        initEmptyView(view)
        return view
    }

    private fun initSwipe(view: View) {
        val swipeHandler = object : SimpleSwipeCallback(
            view.context,
            R.drawable.ic_event_white_24dp,
            R.color.md_blue_500,
            R.drawable.ic_delete_white_24dp,
            R.color.md_red_500
        ) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val questId = questId(viewHolder)
                if (direction == ItemTouchHelper.END) {
                    RescheduleDialogController(
                        includeToday = false,
                        listener = { date ->
                            dispatch(PlanDayAction.RescheduleQuest(questId, date))
                        },
                        cancelListener = {
                            view.todayQuests.adapter.notifyItemChanged(viewHolder.adapterPosition)
                        }
                    ).show(router)
                } else {
                    dispatch(PlanDayAction.RemoveQuest(questId))
                    PetMessagePopup(
                        stringRes(R.string.remove_quest_undo_message),
                        { dispatch(PlanDayAction.UndoRemoveQuest(questId)) },
                        stringRes(R.string.undo)
                    ).show(view.context)
                }
            }

            private fun questId(holder: RecyclerView.ViewHolder): String {
                val adapter = view.todayQuests.adapter as QuestAdapter
                val item = adapter.getItemAt(holder.adapterPosition)
                return item.id
            }

            override fun getSwipeDirs(
                recyclerView: RecyclerView?,
                viewHolder: RecyclerView.ViewHolder?
            ) = ItemTouchHelper.START or ItemTouchHelper.END
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(view.todayQuests)
    }

    private fun initAddQuest(view: View) {
        addQuestAnimationHelper = AddQuestAnimationHelper(
            controller = this,
            addContainer = view.addContainer,
            fab = view.addQuest,
            background = view.addContainerBackground
        )

        view.addContainerBackground.setOnClickListener {
            addContainerRouter(view).popCurrentController()
            ViewUtils.hideKeyboard(view)
            addQuestAnimationHelper.closeAddContainer()
        }

        view.addQuest.setOnClickListener {
            addQuestAnimationHelper.openAddContainer(LocalDate.now())
        }
    }


    private fun addContainerRouter(view: View) =
        getChildRouter(view.addContainer, "add-quest")

    private fun initEmptyView(view: View) {
        view.emptyAnimation.setAnimation("empty_plan_day.json")
        view.emptyTitle.setText(R.string.empty_plan_today_list_title)
        view.emptyText.setText(R.string.empty_plan_today_list_text)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        showBackButton()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.plan_day_today_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            dispatch(PlanDayAction.Back)
            return true
        }
        if (item.itemId == R.id.actionDone) {
            activity?.let {
                val message = stringsRes(R.array.plan_day_done).shuffled().first()
                PetMessagePopup(message)
                    .show(it)
            }
            dispatch(PlanDayAction.Done)
            rootRouter.setRoot(
                RouterTransaction.with(HomeViewController())
            )
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateLoadAction() = PlanDayAction.LoadToday

    override fun render(state: PlanDayViewState, view: View) {
        if (state.type == PlanDayViewState.StateType.TODAY_DATA_LOADED) {
            view.loader.gone()
            (view.todayQuests.adapter as QuestAdapter).updateAll(state.todayViewModels)
            if (state.todayQuests!!.isEmpty()) {
                view.dailyQuestsTitle.gone()
                view.timelineIndicator.gone()
                view.emptyContainer.visible()
                view.emptyAnimation.playAnimation()
            } else {
                view.dailyQuestsTitle.visible()
                view.timelineIndicator.visible()
                view.emptyContainer.gone()
                view.emptyAnimation.pauseAnimation()
            }

            (view.suggestionQuests.adapter as SuggestionAdapter).updateAll(state.suggestionViewModels)
            if (state.suggestedQuests!!.isEmpty()) {
                view.suggestionQuestsContainer.gone()
            } else {
                view.suggestionQuestsContainer.visible()
            }
        }
    }


    private fun renderTag(tagNameView: TextView, tag: TagViewModel) {
        tagNameView.visible()
        tagNameView.text = tag.name
        TextViewCompat.setTextAppearance(
            tagNameView,
            R.style.TextAppearance_AppCompat_Caption
        )

        val indicator = tagNameView.compoundDrawablesRelative[0] as GradientDrawable
        indicator.mutate()
        val size = ViewUtils.dpToPx(8f, tagNameView.context).toInt()
        indicator.setSize(size, size)
        indicator.setColor(colorRes(tag.color))
        tagNameView.setCompoundDrawablesRelativeWithIntrinsicBounds(
            indicator,
            null,
            null,
            null
        )
    }

    data class TagViewModel(val name: String, @ColorRes val color: Int)

    data class QuestItem(
        override val id: String,
        val name: String,
        val startTime: String,
        val duration: String,
        @ColorRes val color: Int,
        val tags: List<TagViewModel>,
        val icon: IIcon,
        val isRepeating: Boolean,
        val isFromChallenge: Boolean
    ) : RecyclerViewViewModel

    data class SuggestionItem(
        override val id: String,
        val name: String,
        val startTime: String,
        val startTimeIcon: IIcon,
        @ColorRes val color: Int,
        val tags: List<TagViewModel>,
        val icon: IIcon,
        val isRepeating: Boolean,
        val isFromChallenge: Boolean
    ) : RecyclerViewViewModel

    inner class QuestAdapter :
        BaseRecyclerViewAdapter<QuestItem>(R.layout.item_plan_today_quest) {
        override fun onBindViewModel(vm: QuestItem, view: View, holder: SimpleViewHolder) {
            view.questName.text = vm.name

            view.questIcon.backgroundTintList =
                ColorStateList.valueOf(colorRes(vm.color))

            view.questIcon.setImageDrawable(listItemIcon(vm.icon))

            val width = view.questStartTime.paint.measureText("12:00 am")

            view.questStartTime.text = vm.startTime
            if (vm.startTime.isEmpty()) {
                view.questStartTime.invisible()
                view.questNoStartTime.visible()
            } else {
                view.questStartTime.visible()
                view.questNoStartTime.gone()
            }
            view.questStartTime.layoutParams.width = width.toInt()

            view.questDuration.text = vm.duration

            if (vm.tags.isNotEmpty()) {
                renderTag(view.questTagName, vm.tags.first())
            } else {
                view.questTagName.gone()
            }

            view.questRepeatIndicator.visibility =
                if (vm.isRepeating) View.VISIBLE else View.GONE
            view.questChallengeIndicator.visibility =
                if (vm.isFromChallenge) View.VISIBLE else View.GONE

        }
    }

    inner class SuggestionAdapter :
        BaseRecyclerViewAdapter<SuggestionItem>(R.layout.item_plan_today_suggestion) {
        override fun onBindViewModel(vm: SuggestionItem, view: View, holder: SimpleViewHolder) {
            view.suggestionName.text = vm.name

            view.suggestionIcon.backgroundTintList =
                ColorStateList.valueOf(colorRes(vm.color))

            view.suggestionIcon.setImageDrawable(listItemIcon(vm.icon))

            view.suggestionStartTime.text = vm.startTime

            view.suggestionStartTime.setCompoundDrawablesRelativeWithIntrinsicBounds(
                IconicsDrawable(view.context)
                    .icon(vm.startTimeIcon)
                    .sizeDp(16)
                    .alpha(Constants.MEDIUM_ALPHA)
                    .colorRes(R.color.md_black)
                    .respectFontBounds(true),
                null, null, null
            )

            if (vm.tags.isNotEmpty()) {
                renderTag(view.suggestionTagName, vm.tags.first())
            } else {
                view.suggestionTagName.gone()
            }

            view.suggestionRepeatIndicator.visibility =
                if (vm.isRepeating) View.VISIBLE else View.GONE
            view.suggestionChallengeIndicator.visibility =
                if (vm.isFromChallenge) View.VISIBLE else View.GONE

            view.suggestionAccept.onDebounceClick {
                dispatch(PlanDayAction.AcceptSuggestion(vm.id))
                showShortToast(R.string.suggestion_accepted)
            }
        }
    }

    private val PlanDayViewState.suggestionViewModels: List<SuggestionItem>
        get() =
            suggestedQuests!!.map {
                SuggestionItem(
                    id = it.id,
                    name = it.name,
                    tags = it.tags.map {
                        TagViewModel(
                            it.name,
                            AndroidColor.valueOf(it.color.name).color500
                        )
                    },
                    startTime = QuestStartTimeFormatter.formatWithDuration(it, activity!!, shouldUse24HourFormat),
                    startTimeIcon =
                    if (it.isScheduled)
                        GoogleMaterial.Icon.gmd_access_time
                    else
                        GoogleMaterial.Icon.gmd_timer,
                    color = it.color.androidColor.color500,
                    icon = it.icon?.androidIcon?.icon
                        ?: Ionicons.Icon.ion_android_clipboard,
                    isRepeating = it.isFromRepeatingQuest,
                    isFromChallenge = it.isFromChallenge
                )
            }


    private val PlanDayViewState.todayViewModels: List<QuestItem>
        get() =
            todayQuests!!.map {
                QuestItem(
                    id = it.id,
                    name = it.name,
                    tags = it.tags.map {
                        TagViewModel(
                            it.name,
                            AndroidColor.valueOf(it.color.name).color500
                        )
                    },
                    startTime = QuestStartTimeFormatter.format(it, shouldUse24HourFormat),
                    duration = DurationFormatter.formatShort(it.duration),
                    color = it.color.androidColor.color500,
                    icon = it.icon?.androidIcon?.icon
                        ?: Ionicons.Icon.ion_android_clipboard,
                    isRepeating = it.isFromRepeatingQuest,
                    isFromChallenge = it.isFromChallenge
                )
            }
}