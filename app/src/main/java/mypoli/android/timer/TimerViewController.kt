package mypoli.android.timer

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.ionicons_typeface_library.Ionicons
import kotlinx.android.synthetic.main.controller_timer.view.*
import kotlinx.android.synthetic.main.item_timer_progress.view.*
import mypoli.android.R
import mypoli.android.common.mvi.MviViewController
import mypoli.android.common.view.*
import space.traversal.kapsule.required

/**
 * Created by Venelin Valkov <venelin@ipoli.io>
 * on 6.01.18.
 */
class TimerViewController : MviViewController<TimerViewState, TimerViewController, TimerPresenter, TimerIntent> {

    private lateinit var questId: String

    private lateinit var handler: Handler

    private val presenter by required { timerPresenter }

    constructor(args: Bundle? = null) : super(args)

    constructor(questId: String) : super() {
        this.questId = questId
    }

    override fun createPresenter() = presenter

    private lateinit var updateTimer: () -> Unit

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        val view = inflater.inflate(R.layout.controller_timer, container, false)


        val icon = IconicsDrawable(view.context)
            .icon(Ionicons.Icon.ion_play)
            .color(attr(R.attr.colorAccent))
            .sizeDp(22)

        view.startStop.setImageDrawable(icon)

//        startTimer(view)


//        view.timerProgressContainer.addView(createProgress(inflater, view))
//        view.timerProgressContainer.addView(createProgress(inflater, view))

        return view
    }

    private fun startTimer(view: View) {
        handler = Handler(Looper.getMainLooper())
        updateTimer = {
            view.timerProgress.progress = view.timerProgress.progress + 1
            handler.postDelayed(updateTimer, 1000)
        }

        handler.postDelayed(updateTimer, 1000)
    }

    private fun createProgressView(view: View) =
        LayoutInflater.from(view.context).inflate(R.layout.item_timer_progress, view.timerProgressContainer, false)

    override fun onAttach(view: View) {
        super.onAttach(view)
        enterFullScreen()
        send(TimerIntent.LoadData(questId))
    }

    override fun onDetach(view: View) {
        exitFullScreen()
        super.onDetach(view)
    }

    override fun render(state: TimerViewState, view: View) {

        view.questName.text = state.questName

        when (state.type) {
            TimerViewState.StateType.SHOW_POMODORO -> {
                state.pomodoroProgress.forEach {
                    addProgressIndicator(view, it)
                }
            }
        }
    }

    private fun addProgressIndicator(view: View, progress: PomodoroProgress) {
        val progressView = createProgressView(view)
        val progressDrawable = resources!!.getDrawable(R.drawable.timer_progress_item, view.context.theme) as GradientDrawable

        when (progress) {
            PomodoroProgress.INCOMPLETE_WORK -> {
                progressDrawable.setColor(colorRes(R.color.md_grey_300))
            }

            PomodoroProgress.COMPLETE_WORK -> {
                progressDrawable.setColor(attr(R.attr.colorAccent))
            }

            PomodoroProgress.INCOMPLETE_SHORT_BREAK -> {
                progressDrawable.setColor(colorRes(R.color.md_grey_300))
                progressView.setScale(0.5f)
            }

            PomodoroProgress.COMPLETE_SHORT_BREAK -> {
                progressDrawable.setColor(attr(R.attr.colorAccent))
                progressView.setScale(0.5f)
            }

            PomodoroProgress.INCOMPLETE_LONG_BREAK -> {
                progressDrawable.setColor(colorRes(R.color.md_grey_300))
                progressView.setScale(0.75f)
            }

            PomodoroProgress.COMPLETE_LONG_BREAK -> {
                progressDrawable.setColor(attr(R.attr.colorAccent))
                progressView.setScale(0.75f)
            }
        }
        progressView.timerItemProgress.background = progressDrawable
        view.timerProgressContainer.addView(progressView)
    }
}

enum class PomodoroProgress() {
    INCOMPLETE_SHORT_BREAK,
    COMPLETE_SHORT_BREAK,
    INCOMPLETE_LONG_BREAK,
    COMPLETE_LONG_BREAK,
    INCOMPLETE_WORK,
    COMPLETE_WORK
}