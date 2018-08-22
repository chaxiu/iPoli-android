package io.ipoli.android.onboarding.scenes

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.support.v4.widget.TintableCompoundButton
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.florent37.tutoshowcase.TutoShowcase
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.IconicsDrawable
import io.ipoli.android.Constants
import io.ipoli.android.R
import io.ipoli.android.common.ViewUtils
import io.ipoli.android.common.datetime.Time
import io.ipoli.android.common.redux.android.BaseViewController
import io.ipoli.android.common.view.*
import io.ipoli.android.common.view.anim.TypewriterTextAnimator
import io.ipoli.android.onboarding.OnboardAction
import io.ipoli.android.onboarding.OnboardReducer
import io.ipoli.android.onboarding.OnboardViewState
import io.ipoli.android.pet.AndroidPetAvatar
import io.ipoli.android.quest.schedule.calendar.dayview.view.widget.CalendarDayView
import io.ipoli.android.quest.schedule.calendar.dayview.view.widget.CalendarEvent
import io.ipoli.android.quest.schedule.calendar.dayview.view.widget.ScheduledEventsAdapter
import kotlinx.android.synthetic.main.calendar_hour_cell.view.*
import kotlinx.android.synthetic.main.controller_onboard_first_quest.view.*
import kotlinx.android.synthetic.main.item_calendar_quest.view.*
import kotlinx.android.synthetic.main.popup_reward.view.*
import kotlinx.android.synthetic.main.view_default_toolbar.view.*

class FirstQuestViewController(args: Bundle? = null) :
    BaseViewController<OnboardAction, OnboardViewState>(
        args
    ) {

    override val stateKey = OnboardReducer.stateKey

    private val animations = mutableListOf<Animator>()

    private lateinit var petAvatar: AndroidPetAvatar

    private var showcase: TutoShowcase? = null

    private val nameWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable) {
            if (s.length > 2) {
                view?.saveQuest?.visible()
            } else {
                view?.saveQuest?.gone()
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        val view = container.inflate(R.layout.controller_onboard_first_quest)

        view.saveQuest.setImageDrawable(
            IconicsDrawable(activity!!)
                .icon(GoogleMaterial.Icon.gmd_send)
                .color(attrData(R.attr.colorAccent))
                .sizeDp(24)
                .respectFontBounds(true)
        )
        view.saveQuest.onDebounceClick {
            view.calendar.scrollToNow()
            view.addQuestContainer.gone()
            view.addContainerBackground.gone()
            ViewUtils.hideKeyboard(view)

            val eventsAdapter =
                OnboardQuestAdapter(
                    activity!!,
                    mutableListOf(
                        OnboardQuestViewModel(
                            "",
                            view.firstQuestName.text.toString(),
                            60,
                            Time.now().toMinuteOfDay(),
                            shouldUse24HourFormat
                        )
                    )
                )
            view.calendar.setScheduledEventsAdapter(eventsAdapter)

            view.calendar.postDelayed({
                showcase = showcaseRect(
                    layout = R.layout.view_onboard_complete_quest,
                    view = R.id.calendarQuestContainer,
                    containerView = R.id.completeQuestContainer,
                    onClick = {
                        it.dismiss()
                        showcase = null
                        onQuestComplete(view)
                    })
            }, 300)
        }

        view.firstQuestName.addTextChangedListener(nameWatcher)

        setToolbar(view.toolbar)
        toolbarTitle = stringRes(R.string.onboard_first_quest_title)

        view.calendar.setHourAdapter(object :
            CalendarDayView.HourCellAdapter {
            override fun bind(view: View, hour: Int) {
                if (hour > 0) {
                    view.timeLabel.text = Time.atHours(hour).toString(shouldUse24HourFormat)
                }
            }
        })
        view.calendar.hideTimeline()

        return view
    }

    private fun onQuestComplete(view: View) {
        view.checkBox.isChecked = true

        FirstQuestCompletePopup(
            petAvatar.headImage,
            Constants.DEFAULT_PLAYER_XP.toInt(),
            Constants.DEFAULT_PLAYER_COINS
        ).show(view.context)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        exitFullScreen()
    }

    override fun onCreateLoadAction() = OnboardAction.LoadFirstQuest

    private fun onQuestCompleteAnimationEnd(popup: Popup, contentView: View) {
        showcase = showcaseRect(
            layout = R.layout.view_onboard_bounty,
            view = contentView,
            containerView = R.id.bountyContainer
        )
        contentView.setOnClickListener {
            showcase?.dismiss()
            showcase = null
            popup.hide()
            view?.postDelayed({
                dispatch(OnboardAction.ShowNext)
            }, 300)
        }
    }

    private fun onAddQuest(view: View) {
        view.addQuest.gone()
        view.addQuestContainer.visible()
        view.addContainerBackground.visible()
        view.post {
            view.firstQuestName.requestFocus()
            ViewUtils.showKeyboard(view.context, view.firstQuestName)
        }
    }

    override fun render(state: OnboardViewState, view: View) {
        if (state.type == OnboardViewState.StateType.FIRST_QUEST_DATA_LOADED) {
            petAvatar = AndroidPetAvatar.valueOf(state.pet.name)
            showcase = showcaseCircle(
                layout = R.layout.view_onboard_calendar,
                view = R.id.addQuest,
                containerView = R.id.onboardCalendarContainer,
                onClick = {
                    it.dismiss()
                    showcase = null
                    onAddQuest(view)
                })
        }
    }

    override fun onDetach(view: View) {
        showcase?.dismiss()
        showcase = null
        for (a in animations) {
            a.cancel()
        }
        animations.clear()
        super.onDetach(view)
    }

    data class OnboardQuestViewModel(
        override val id: String,
        val name: String,
        override val duration: Int,
        override val startMinute: Int,
        val use24HourFormat: Boolean
    ) : CalendarEvent {

        val startTime: String get() = Time.of(startMinute).toString(use24HourFormat)

        val endTime: String get() = Time.of(startMinute).plus(duration).toString(use24HourFormat)
    }

    inner class OnboardQuestAdapter(
        context: Context,
        events: MutableList<OnboardQuestViewModel>
    ) :
        ScheduledEventsAdapter<OnboardQuestViewModel>(
            context,
            R.layout.item_calendar_quest,
            events
        ) {
        override fun bindView(view: View, position: Int) {
            val vm = getItem(position)
            view.checkBox.visible()
            view.backgroundView.setBackgroundColor(colorRes(R.color.md_green_500))

            view.questName.text = vm.name
            view.questName.setTextColor(colorRes(R.color.md_white))

            view.questSchedule.text = "${vm.startTime} - ${vm.endTime}"
            view.questSchedule.setTextColor(colorRes(R.color.md_light_text_70))

            view.questIcon.visible = true
            view.questIcon.setImageDrawable(
                IconicsDrawable(context).normalIcon(
                    CommunityMaterial.Icon.cmd_duck,
                    R.color.md_green_200
                )
            )

            view.questColorIndicator.setBackgroundResource(R.color.md_green_900)

            (view.checkBox as TintableCompoundButton).supportButtonTintList =
                ContextCompat.getColorStateList(
                    context,
                    R.color.md_green_200
                )
            view.completedBackgroundView.invisible()
            view.repeatIndicator.gone()
            view.challengeIndicator.gone()

            view.checkBox.setOnCheckedChangeListener { cb, checked ->
                if (checked) {
                    (view.checkBox as TintableCompoundButton).supportButtonTintList =
                        ContextCompat.getColorStateList(
                            context,
                            R.color.md_grey_700
                        )
                    val anim = RevealAnimator()
                        .create(view.completedBackgroundView, cb)
                    anim.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator?) {
                            view.questColorIndicator.setBackgroundResource(R.color.md_grey_400)
                            view.completedBackgroundView.visibility = View.VISIBLE

                            view.questIcon.setImageDrawable(
                                IconicsDrawable(context).normalIcon(
                                    CommunityMaterial.Icon.cmd_duck,
                                    R.color.md_dark_text_38
                                )
                            )
                        }

                        override fun onAnimationEnd(animation: Animator?) {

                        }

                    })
                    anim.start()
                }
            }
        }

        override fun adaptViewForHeight(adapterView: View, height: Float) {
        }

        override fun rescheduleEvent(position: Int, startTime: Time, duration: Int) {
        }

    }

    inner class FirstQuestCompletePopup(
        @DrawableRes private val petImage: Int,
        private val earnedXP: Int,
        private val earnedCoins: Int
    ) : Popup(
        position = Position.BOTTOM,
        isAutoHide = true,
        overlayBackground = null
    ) {

        override fun createView(inflater: LayoutInflater): View =
            inflater.inflate(R.layout.popup_onboard_reward, null)

        override fun onViewShown(contentView: View) {
            super.onViewShown(contentView)

            contentView.pet.setImageResource(petImage)
            startTypingAnimation(contentView)
        }

        private fun startTypingAnimation(contentView: View) {
            val title = contentView.message
            val message = "You`re a natural"
            val typewriterAnim =
                TypewriterTextAnimator.of(
                    title,
                    message,
                    typeSpeed = 20
                )
            typewriterAnim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    startEarnedRewardAnimation(contentView)
                }
            })
            typewriterAnim.start()
        }

        private fun startEarnedRewardAnimation(contentView: View) {

            val xpAnim = ValueAnimator.ofInt(0, earnedXP)
            xpAnim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    contentView.earnedXP.visible = true
                }
            })
            xpAnim.addUpdateListener {
                contentView.earnedXP.text = "${it.animatedValue}"
            }

            val coinsAnim = ValueAnimator.ofInt(0, earnedCoins)

            coinsAnim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    contentView.earnedCoins.visible = true
                }
            })

            coinsAnim.addUpdateListener {
                contentView.earnedCoins.text = "${it.animatedValue}"
            }

            val anim = AnimatorSet()
            anim.duration = shortAnimTime
            anim.playSequentially(xpAnim, coinsAnim)

            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onQuestCompleteAnimationEnd(this@FirstQuestCompletePopup, contentView)
                }
            })
            animations.add(anim)
            anim.start()
        }

    }
}