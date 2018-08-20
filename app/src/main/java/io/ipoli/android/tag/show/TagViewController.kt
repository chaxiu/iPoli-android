package io.ipoli.android.tag.show

import android.content.res.ColorStateList
import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.design.widget.AppBarLayout
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.SpannableString
import android.text.style.StrikethroughSpan
import android.view.*
import android.widget.TextView
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler
import com.bluelinelabs.conductor.changehandler.VerticalChangeHandler
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.ionicons_typeface_library.Ionicons
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic
import io.ipoli.android.MainActivity
import io.ipoli.android.R
import io.ipoli.android.common.redux.android.ReduxViewController
import io.ipoli.android.common.text.QuestStartTimeFormatter
import io.ipoli.android.common.view.*
import io.ipoli.android.common.view.recyclerview.MultiViewRecyclerViewAdapter
import io.ipoli.android.common.view.recyclerview.RecyclerViewViewModel
import io.ipoli.android.common.view.recyclerview.SimpleSwipeCallback
import io.ipoli.android.quest.CompletedQuestViewController
import io.ipoli.android.tag.usecase.CreateTagItemsUseCase
import kotlinx.android.synthetic.main.controller_tag.view.*
import kotlinx.android.synthetic.main.item_agenda_quest.view.*
import kotlinx.android.synthetic.main.view_empty_list.view.*

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 04/04/2018.
 */
class TagViewController(args: Bundle? = null) :
    ReduxViewController<TagAction, TagViewState, TagReducer>(args = args) {

    override val reducer = TagReducer

    private var tagId = ""
    private var isFavorite: Boolean = false

    private constructor(tagId: String) : this() {
        this.tagId = tagId
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        applyStatusBarColors = false
        val view = inflater.inflate(R.layout.controller_tag, container, false)
        setToolbar(view.toolbar)
        view.collapsingToolbarContainer.isTitleEnabled = false

        setupAppBar(view)

        initEmptyView(view)

        view.tagQuests.layoutManager = LinearLayoutManager(activity!!)
        view.tagQuests.adapter = ItemAdapter()

        val swipeHandler = object : SimpleSwipeCallback(
            view.context,
            R.drawable.ic_done_white_24dp,
            R.color.md_green_500,
            R.drawable.ic_close_white_24dp,
            R.color.md_amber_500
        ) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.END) {
                    dispatch(TagAction.CompleteQuest(questId(viewHolder)))
                } else if (direction == ItemTouchHelper.START) {
                    dispatch(TagAction.UndoCompleteQuest(questId(viewHolder)))
                }
            }

            private fun questId(holder: RecyclerView.ViewHolder): String {
                val adapter = view.tagQuests.adapter as ItemAdapter
                return if (holder.itemViewType == ViewType.QUEST.value) {
                    val item = adapter.getItemAt<ItemViewModel.QuestItem>(holder.adapterPosition)
                    item.id
                } else {
                    val item =
                        adapter.getItemAt<ItemViewModel.CompletedQuestItem>(holder.adapterPosition)
                    item.id
                }

            }

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) = when {
                viewHolder.itemViewType == ViewType.QUEST.value -> ItemTouchHelper.END
                viewHolder.itemViewType == ViewType.COMPLETED_QUEST.value -> ItemTouchHelper.START
                else -> 0
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(view.tagQuests)

        return view
    }

    private fun initEmptyView(view: View) {
        view.emptyAnimation.setAnimation("empty_tag_quests.json")
        view.emptyTitle.setText(R.string.empty_tag_quest_list_title)
        view.emptyText.setText(R.string.empty_tag_quest_list_text)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        showBackButton()
    }

    override fun onCreateLoadAction() = TagAction.Load(tagId)

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.tag_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val favoriteItem = menu.findItem(R.id.actionFavorite)
        favoriteItem.icon = ContextCompat.getDrawable(
            view!!.context,
            if (isFavorite) R.drawable.ic_favorite_white_24dp
            else R.drawable.ic_favorite_outline_white_24dp
        )
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home ->
                return router.handleBack()

            R.id.actionEdit -> {
                navigateFromRoot().toEditTag(
                    tagId = tagId,
                    changeHandler = HorizontalChangeHandler()
                )
            }

            R.id.actionFavorite -> {
                if (isFavorite) {
                    dispatch(TagAction.Unfavorite(tagId))
                } else {
                    dispatch(TagAction.Favorite(tagId))
                }
            }

            R.id.actionRemove -> {
                dispatch(TagAction.Remove(tagId))
                router.handleBack()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupAppBar(view: View) {
        view.appbar.addOnOffsetChangedListener(object :
            AppBarStateChangeListener() {
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
    }

    override fun render(state: TagViewState, view: View) {
        when (state.type) {
            TagViewState.StateType.TAG_CHANGED -> {
                colorLayout(state.color.androidColor, view)
                renderName(state.name, view)
                renderIcon(state.iicon, view)
                isFavorite = state.isFavorite
                activity!!.invalidateOptionsMenu()
            }

            TagViewState.StateType.TAG_ITEMS_CHANGED -> {
                (view.tagQuests.adapter as ItemAdapter).updateAll(state.itemViewModels)


                if (state.questCount == 0) {
                    view.questCountText.gone()
                    view.progressText.gone()
                    view.questsProgress.gone()

                    view.emptyContainer.visible()
                    view.emptyAnimation.playAnimation()
                } else {
                    view.emptyContainer.invisible()
                    view.emptyAnimation.pauseAnimation()

                    view.questCountText.visible()
                    view.progressText.visible()
                    view.questsProgress.visible()

                    view.questCountText.text = state.questCountText
                    view.progressText.text = state.progressText

                    view.questsProgress.animateProgressFromCurrentValue(state.progressPercent)
                }
            }

            else -> {
            }
        }
    }

    private fun renderIcon(iicon: IIcon, view: View) {
        view.tagIcon.setImageDrawable(
            IconicsDrawable(view.context)
                .icon(iicon)
                .color(colorRes(R.color.md_white))
                .paddingDp(8)
                .sizeDp(64)
        )
    }

    private fun renderName(
        name: String,
        view: View
    ) {
        toolbarTitle = name
        view.tagName.text = name
    }

    private fun colorLayout(
        color: AndroidColor,
        view: View
    ) {
        view.appbar.setBackgroundColor(colorRes(color.color500))
        view.toolbar.setBackgroundColor(colorRes(color.color500))
        view.collapsingToolbarContainer.setContentScrimColor(colorRes(color.color500))
        activity?.window?.navigationBarColor = colorRes(color.color500)
        activity?.window?.statusBarColor = colorRes(color.color700)
    }

    sealed class ItemViewModel(override val id: String) : RecyclerViewViewModel {

        data class SectionItem(val text: String) : ItemViewModel(text)

        data class QuestItem(
            override val id: String,
            val name: String,
            val startTime: String,
            @ColorRes val color: Int,
            val icon: IIcon,
            val isRepeating: Boolean,
            val isFromChallenge: Boolean
        ) : ItemViewModel(id)

        data class CompletedQuestItem(
            override val id: String,
            val name: String,
            val startTime: String,
            @ColorRes val color: Int,
            val icon: IIcon,
            val isRepeating: Boolean,
            val isFromChallenge: Boolean
        ) : ItemViewModel(id)
    }

    enum class ViewType(val value: Int) {
        SECTION(0),
        QUEST(1),
        COMPLETED_QUEST(2)
    }

    inner class ItemAdapter : MultiViewRecyclerViewAdapter<ItemViewModel>() {

        override fun onRegisterItemBinders() {

            registerBinder<ItemViewModel.SectionItem>(
                ViewType.SECTION.value,
                R.layout.item_list_section
            ) { vm, view, _ ->
                (view as TextView).text = vm.text
            }

            registerBinder<ItemViewModel.QuestItem>(
                ViewType.QUEST.value,
                R.layout.item_agenda_quest
            ) { vm, view, _ ->
                view.questName.text = vm.name
                view.questTagName.gone()

                view.questIcon.backgroundTintList =
                    ColorStateList.valueOf(colorRes(vm.color))
                view.questIcon.setImageDrawable(listItemIcon(vm.icon))

                view.questStartTime.text = vm.startTime

                view.questRepeatIndicator.visibility =
                    if (vm.isRepeating) View.VISIBLE else View.GONE
                view.questChallengeIndicator.visibility =
                    if (vm.isFromChallenge) View.VISIBLE else View.GONE

                view.onDebounceClick {
                    navigateFromRoot().toQuest(vm.id)
                }
            }

            registerBinder<ItemViewModel.CompletedQuestItem>(
                ViewType.COMPLETED_QUEST.value,
                R.layout.item_agenda_quest
            ) { vm, view, _ ->
                val span = SpannableString(vm.name)
                span.setSpan(StrikethroughSpan(), 0, vm.name.length, 0)

                view.questName.text = span

                view.questTagName.gone()

                view.questIcon.backgroundTintList =
                    ColorStateList.valueOf(colorRes(vm.color))
                view.questIcon.setImageDrawable(listItemIcon(vm.icon))

                view.questStartTime.text = vm.startTime

                view.questRepeatIndicator.visibility =
                    if (vm.isRepeating) View.VISIBLE else View.GONE
                view.questChallengeIndicator.visibility =
                    if (vm.isFromChallenge) View.VISIBLE else View.GONE

                view.onDebounceClick {
                    val handler = FadeChangeHandler()
                    rootRouter.pushController(
                        RouterTransaction
                            .with(CompletedQuestViewController(vm.id))
                            .pushChangeHandler(handler)
                            .popChangeHandler(handler)
                    )
                }
            }
        }

    }

    private val TagViewState.iicon: IIcon
        get() = icon?.androidIcon?.icon ?: MaterialDesignIconic.Icon.gmi_label

    private val TagViewState.questCountText: String
        get() = if (questCount == 1) {
            "1 Quest"
        } else {
            "$questCount Quests"
        }

    private val TagViewState.progressText: String
        get() = stringRes(R.string.percentage_done, progressPercent)

    private val TagViewState.itemViewModels: List<ItemViewModel>
        get() = items.map {
            when (it) {
                is CreateTagItemsUseCase.TagItem.QuestItem -> {

                    val q = it.quest

                    val color = if (q.isCompleted)
                        R.color.md_grey_500
                    else
                        q.color.androidColor.color500

                    if (q.isCompleted) {
                        ItemViewModel.CompletedQuestItem(
                            id = q.id,
                            name = q.name,
                            startTime = QuestStartTimeFormatter.formatWithDuration(
                                q,
                                activity!!,
                                shouldUse24HourFormat
                            ),
                            color = color,
                            icon = q.icon?.androidIcon?.icon
                                ?: Ionicons.Icon.ion_android_clipboard,
                            isRepeating = q.isFromRepeatingQuest,
                            isFromChallenge = q.isFromChallenge
                        )
                    } else {

                        ItemViewModel.QuestItem(
                            id = q.id,
                            name = q.name,
                            startTime = QuestStartTimeFormatter.formatWithDuration(
                                q,
                                activity!!,
                                shouldUse24HourFormat
                            ),
                            color = color,
                            icon = q.icon?.androidIcon?.icon
                                ?: Ionicons.Icon.ion_android_clipboard,
                            isRepeating = q.isFromRepeatingQuest,
                            isFromChallenge = q.isFromChallenge
                        )
                    }
                }

                is CreateTagItemsUseCase.TagItem.Unscheduled ->
                    ItemViewModel.SectionItem(stringRes(R.string.unscheduled))

                is CreateTagItemsUseCase.TagItem.Today ->
                    ItemViewModel.SectionItem(stringRes(R.string.today))

                is CreateTagItemsUseCase.TagItem.Tomorrow ->
                    ItemViewModel.SectionItem(stringRes(R.string.tomorrow))

                is CreateTagItemsUseCase.TagItem.Upcoming ->
                    ItemViewModel.SectionItem(stringRes(R.string.upcoming))

                is CreateTagItemsUseCase.TagItem.Completed ->
                    ItemViewModel.SectionItem(stringRes(R.string.completed))

                is CreateTagItemsUseCase.TagItem.Previous ->
                    ItemViewModel.SectionItem(stringRes(R.string.previous))
            }

        }

    companion object {
        fun routerTransaction(tagId: String) =
            RouterTransaction.with(TagViewController(tagId))
                .pushChangeHandler(VerticalChangeHandler())
                .popChangeHandler(VerticalChangeHandler())
    }
}