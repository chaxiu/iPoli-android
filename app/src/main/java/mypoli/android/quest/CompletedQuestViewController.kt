package mypoli.android.quest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mikepenz.iconics.IconicsDrawable
import kotlinx.android.synthetic.main.controller_completed_quest.view.*
import mypoli.android.R
import mypoli.android.common.mvi.MviViewController
import mypoli.android.common.text.DateFormatter
import mypoli.android.quest.CompletedQuestViewState.StateType.DATA_LOADED
import space.traversal.kapsule.required

/**
 * Created by Polina Zhelyazkova <polina@ipoli.io>
 * on 1/24/18.
 */
class CompletedQuestViewController :
    MviViewController<CompletedQuestViewState, CompletedQuestViewController, CompletedQuestPresenter, CompletedQuestIntent> {

    private lateinit var questId: String

    private val presenter by required { completedQuestPresenter }

    constructor(args: Bundle? = null) : super(args)

    constructor(questId: String) : super() {
        this.questId = questId
    }

    override fun createPresenter() = presenter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.controller_completed_quest, container, false)
        return view
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        send(CompletedQuestIntent.LoadData(questId))
    }

    override fun render(state: CompletedQuestViewState, view: View) {
        when (state.type) {
            DATA_LOADED -> {

                view.questName.text = state.name

                state.icon?.let {
                    view.questName.setCompoundDrawablesWithIntrinsicBounds(
                        IconicsDrawable(view.context)
                            .icon(it.icon)
                            .colorRes(R.color.md_white)
                            .sizeDp(24),
                        null,
                        null,
                        null
                    )
                }
                view.questName.setBackgroundResource(state.color!!.color500)

                view.questDate.text = DateFormatter.format(view.context, state.completeAt)
//                view.questTime.text = state.startedAt!!

            }
        }
    }

}