package io.ipoli.android.quest.schedule.today

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.design.widget.AppBarLayout
import android.support.v4.widget.TextViewCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import com.bluelinelabs.conductor.changehandler.VerticalChangeHandler
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.ionicons_typeface_library.Ionicons
import io.ipoli.android.MainActivity
import io.ipoli.android.R
import io.ipoli.android.common.ViewUtils
import io.ipoli.android.common.redux.android.ReduxViewController
import io.ipoli.android.common.text.QuestStartTimeFormatter
import io.ipoli.android.common.view.*
import io.ipoli.android.common.view.recyclerview.BaseRecyclerViewAdapter
import io.ipoli.android.common.view.recyclerview.MultiViewRecyclerViewAdapter
import io.ipoli.android.common.view.recyclerview.RecyclerViewViewModel
import io.ipoli.android.common.view.recyclerview.SimpleViewHolder
import io.ipoli.android.quest.schedule.today.usecase.CreateTodayItemsUseCase
import kotlinx.android.synthetic.main.controller_today.view.*
import kotlinx.android.synthetic.main.item_agenda_quest.view.*
import kotlinx.android.synthetic.main.item_habit_list.view.*
import org.threeten.bp.LocalDate
import space.traversal.kapsule.required
import timber.log.Timber

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
            onReady = {
                Timber.d("AAA ready")
            },
            onError = { _ -> }
        )

        view.questItems.layoutManager = LinearLayoutManager(view.context)
        view.questItems.isNestedScrollingEnabled = false
        view.questItems.adapter = TodayItemAdapter()

        val gridLayoutManager = GridLayoutManager(view.context, 2)
        view.habitItems.layoutManager = gridLayoutManager

        val adapter = HabitListAdapter()
        view.habitItems.adapter = adapter

        view.completedQuests.layoutManager = LinearLayoutManager(view.context)
        view.completedQuests.isNestedScrollingEnabled = false
        view.completedQuests.adapter = CompletedQuestAdapter()

        view.backdropTransparentColor.background.alpha = (255f * 0.8).toInt()

        return view
    }

    override fun onCreateLoadAction() = TodayAction.Load(LocalDate.now())

    override fun render(state: TodayViewState, view: View) {

        when (state.type) {
            TodayViewState.StateType.HABITS_CHANGED -> {
                (view.habitItems.adapter as HabitListAdapter).updateAll(state.habitItemViewModels)
            }

            TodayViewState.StateType.QUESTS_CHANGED -> {
                (view.questItems.adapter as TodayItemAdapter).updateAll(state.questItemViewModels)
                (view.completedQuests.adapter as CompletedQuestAdapter).updateAll(state.completedQuestViewModels)
            }

            else -> {
            }
        }
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

    enum class QuestViewType {
        SECTION,
        QUEST,
        COMPLETED_QUEST
    }

    inner class TodayItemAdapter : MultiViewRecyclerViewAdapter<TodayItemViewModel>() {

        override fun onRegisterItemBinders() {

            registerBinder<TodayItemViewModel.Section>(
                QuestViewType.SECTION.ordinal,
                R.layout.item_list_section
            ) { vm, view, _ ->
                (view as TextView).text = vm.text
            }

            registerBinder<TodayItemViewModel.QuestViewModel>(
                QuestViewType.QUEST.ordinal,
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

    data class HabitViewModel(
        override val id: String,
        val name: String,
        val icon: IIcon,
        @ColorRes val color: Int,
        @ColorRes val secondaryColor: Int,
        val streak: Int,
        val isBestStreak: Boolean,
        val timesADay: Int,
        val progress: Int,
        val maxProgress: Int,
        val isCompleted: Boolean,
        val isGood: Boolean
    ) : RecyclerViewViewModel

    inner class HabitListAdapter :
        BaseRecyclerViewAdapter<HabitViewModel>(R.layout.item_habit_list) {
        override fun onBindViewModel(vm: HabitViewModel, view: View, holder: SimpleViewHolder) {
            renderName(view, vm.name, vm.isGood)
            renderIcon(view, vm.icon, if (vm.isCompleted) R.color.md_white else vm.color)
            renderStreak(
                view = view,
                streak = vm.streak,
                isBestStreak = vm.isBestStreak,
                color = if (vm.isCompleted) R.color.md_white else vm.color,
                textColor = if (vm.isCompleted) R.color.md_white else R.color.md_dark_text_87
            )
            renderCompletedBackground(view, vm.color)

            view.habitProgress.setProgressStartColor(colorRes(vm.color))
            view.habitProgress.setProgressEndColor(colorRes(vm.color))
            view.habitProgress.setProgressBackgroundColor(colorRes(vm.secondaryColor))
            view.habitProgress.setProgressFormatter(null)
            renderProgress(view, vm.progress, vm.maxProgress)

            if (vm.timesADay > 1) {
                view.habitTimesADayProgress.visible()
                view.habitTimesADayProgress.setProgressStartColor(colorRes(R.color.md_white))
                view.habitTimesADayProgress.setProgressEndColor(colorRes(R.color.md_white))
                view.habitTimesADayProgress.setProgressFormatter(null)
                renderTimesADayProgress(view, vm.progress, vm.maxProgress)
            } else {
                view.habitTimesADayProgress.gone()
            }

            val habitCompleteBackground = view.habitCompletedBackground
            if (vm.isCompleted) {
                view.habitProgress.invisible()
                habitCompleteBackground.visible()
                view.habitCompletedBackground.setOnLongClickListener {
                    navigateFromRoot().toEditHabit(vm.id, VerticalChangeHandler())
                    return@setOnLongClickListener true
                }
                view.habitProgress.setOnLongClickListener(null)
            } else {
                view.habitProgress.visible()
                habitCompleteBackground.invisible()
                view.habitProgress.setOnLongClickListener {
                    navigateFromRoot().toEditHabit(vm.id, VerticalChangeHandler())
                    return@setOnLongClickListener true
                }
                view.habitCompletedBackground.setOnLongClickListener(null)
            }

            view.habitProgress.onDebounceClick {
                val isLastProgress = vm.maxProgress - vm.progress == 1
                if (isLastProgress) {
                    startCompleteAnimation(view, vm)
                } else {
//                        dispatch(
//                            if (vm.isGood) HabitListAction.CompleteHabit(vm.id)
//                            else HabitListAction.UndoCompleteHabit(vm.id)
//                        )
                }
            }

            view.habitCompletedBackground.onDebounceClick {
                startUndoCompleteAnimation(view, vm)
            }
        }

        private fun renderCompletedBackground(
            view: View,
            color: Int
        ): View? {
            val habitCompleteBackground = view.habitCompletedBackground
            val b = habitCompleteBackground.background as GradientDrawable
            b.setColor(colorRes(color))
            return habitCompleteBackground
        }

        private fun renderStreak(
            view: View,
            streak: Int,
            isBestStreak: Boolean,
            textColor: Int,
            color: Int
        ) {
            view.habitStreak.text = streak.toString()
            view.habitStreak.setTextColor(colorRes(textColor))
            if (isBestStreak) {
                view.habitBestProgressIndicator.visible()
                view.habitBestProgressIndicator.setImageDrawable(
                    IconicsDrawable(view.context).normalIcon(
                        GoogleMaterial.Icon.gmd_star,
                        color
                    )
                )
            } else {
                view.habitBestProgressIndicator.gone()
            }
        }

        private fun renderIcon(
            view: View,
            icon: IIcon,
            color: Int
        ) {
            view.habitIcon.setImageDrawable(
                IconicsDrawable(view.context).normalIcon(icon, color)
            )
        }

        private fun renderName(
            view: View,
            name: String,
            isGood: Boolean
        ) {

            view.habitName.text = if (isGood) name else "\u2205 $name"
        }

        private fun renderProgress(
            view: View,
            progress: Int,
            maxProgress: Int
        ) {
            view.habitProgress.max = maxProgress
            view.habitProgress.progress = progress
        }

        private fun renderTimesADayProgress(
            view: View,
            progress: Int,
            maxProgress: Int
        ) {
            view.habitTimesADayProgress.max = maxProgress
            view.habitTimesADayProgress.setLineCount(maxProgress)
            view.habitTimesADayProgress.progress = progress
        }

        private fun startUndoCompleteAnimation(
            view: View,
            vm: HabitViewModel
        ) {
            val hcb = view.habitCompletedBackground
            val half = hcb.width / 2
            val completeAnim = ViewAnimationUtils.createCircularReveal(
                hcb,
                half, half,
                half.toFloat(), 0f
            )
            completeAnim.duration = shortAnimTime
            completeAnim.interpolator = AccelerateDecelerateInterpolator()
            completeAnim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    view.habitProgress.visible()
                }

                override fun onAnimationEnd(animation: Animator?) {
                    hcb.invisible()
                    view.habitIcon.setImageDrawable(
                        IconicsDrawable(view.context).normalIcon(vm.icon, vm.color)
                    )
                    view.habitStreak.setTextColor(colorRes(R.color.md_dark_text_87))
                    renderProgress(view, vm.progress - 1, vm.maxProgress)
                    renderTimesADayProgress(view, vm.progress - 1, vm.maxProgress)

//                    dispatch(
//                        if (vm.isGood) HabitListAction.UndoCompleteHabit(vm.id)
//                        else HabitListAction.CompleteHabit(vm.id)
//                    )
                }
            })
            completeAnim.start()
        }

        private fun startCompleteAnimation(
            view: View,
            vm: HabitViewModel
        ) {
            val hcb = view.habitCompletedBackground
            val half = hcb.width / 2
            val completeAnim = ViewAnimationUtils.createCircularReveal(
                hcb,
                half, half,
                0f, half.toFloat()
            )
            completeAnim.duration = shortAnimTime
            completeAnim.interpolator = AccelerateDecelerateInterpolator()
            completeAnim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    hcb.visible()
                }

                override fun onAnimationEnd(animation: Animator?) {
                    view.habitIcon.setImageDrawable(
                        IconicsDrawable(view.context).normalIcon(vm.icon, R.color.md_white)
                    )
                    view.habitStreak.setTextColor(colorRes(R.color.md_white))
//                    dispatch(
//                        if (vm.isGood) HabitListAction.CompleteHabit(vm.id)
//                        else HabitListAction.UndoCompleteHabit(vm.id)
//                    )
                }
            })
            completeAnim.start()
        }
    }

    data class CompletedQuestViewModel(
        override val id: String,
        val name: String,
        val tags: List<TagViewModel>,
        val startTime: String,
        @ColorRes val color: Int,
        val icon: IIcon,
        val isRepeating: Boolean,
        val isFromChallenge: Boolean
    ) : RecyclerViewViewModel

    inner class CompletedQuestAdapter :
        BaseRecyclerViewAdapter<CompletedQuestViewModel>(R.layout.item_agenda_quest) {

        override fun onBindViewModel(
            vm: CompletedQuestViewModel,
            view: View,
            holder: SimpleViewHolder
        ) {
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

    private val TodayViewState.questItemViewModels: List<TodayItemViewModel>
        get() =
            quests!!.incomplete.map {
                when (it) {
                    is CreateTodayItemsUseCase.TodayItem.UnscheduledSection ->
                        TodayItemViewModel.Section(stringRes(R.string.unscheduled))

                    is CreateTodayItemsUseCase.TodayItem.MorningSection ->
                        TodayItemViewModel.Section(stringRes(R.string.morning))

                    is CreateTodayItemsUseCase.TodayItem.AfternoonSection ->
                        TodayItemViewModel.Section(stringRes(R.string.afternoon))

                    is CreateTodayItemsUseCase.TodayItem.EveningSection ->
                        TodayItemViewModel.Section(stringRes(R.string.evening))

                    is CreateTodayItemsUseCase.TodayItem.QuestItem -> {
                        val quest = it.quest
                        TodayItemViewModel.QuestViewModel(
                            id = quest.id,
                            name = quest.name,
                            tags = quest.tags.map { t ->
                                TagViewModel(
                                    t.name,
                                    AndroidColor.valueOf(t.color.name).color500
                                )
                            },
                            startTime = QuestStartTimeFormatter.formatWithDuration(
                                quest,
                                activity!!,
                                shouldUse24HourFormat
                            ),
                            color = quest.color.androidColor.color500,
                            icon = quest.icon?.let { ic -> AndroidIcon.valueOf(ic.name).icon }
                                ?: Ionicons.Icon.ion_android_clipboard,
                            isRepeating = quest.isFromRepeatingQuest,
                            isFromChallenge = quest.isFromChallenge
                        )
                    }
                }
            }

    private val TodayViewState.habitItemViewModels: List<HabitViewModel>
        get() =
            todayHabitItems!!.map {
                val habit = it.habit
                HabitViewModel(
                    id = habit.id,
                    name = habit.name,
                    color = habit.color.androidColor.color500,
                    secondaryColor = habit.color.androidColor.color100,
                    icon = habit.icon.androidIcon.icon,
                    timesADay = habit.timesADay,
                    isCompleted = it.isCompleted,
                    isGood = habit.isGood,
                    streak = habit.currentStreak,
                    isBestStreak = it.isBestStreak,
                    progress = it.completedCount,
                    maxProgress = habit.timesADay
                )
            }

    private val TodayViewState.completedQuestViewModels: List<CompletedQuestViewModel>
        get() =
            quests!!.complete.map {
                CompletedQuestViewModel(
                    id = it.id,
                    name = it.name,
                    tags = it.tags.map { t ->
                        TagViewModel(
                            t.name,
                            AndroidColor.valueOf(t.color.name).color500
                        )
                    },
                    startTime = QuestStartTimeFormatter.formatWithDuration(
                        it,
                        activity!!,
                        shouldUse24HourFormat
                    ),
                    color = R.color.md_grey_500,
                    icon = it.icon?.let { ic -> AndroidIcon.valueOf(ic.name).icon }
                        ?: Ionicons.Icon.ion_android_clipboard,
                    isRepeating = it.isFromRepeatingQuest,
                    isFromChallenge = it.isFromChallenge
                )
            }
}