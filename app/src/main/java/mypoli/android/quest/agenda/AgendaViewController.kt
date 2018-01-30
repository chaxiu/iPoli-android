package mypoli.android.quest.agenda

import android.content.res.ColorStateList
import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import kotlinx.android.synthetic.main.controller_agenda.view.*
import kotlinx.android.synthetic.main.item_agenda_quest.view.*
import mypoli.android.R
import mypoli.android.common.redux.android.ReduxViewController
import mypoli.android.common.view.EndlessRecyclerViewScrollListener
import mypoli.android.common.view.colorRes
import timber.log.Timber

/**
 * Created by Polina Zhelyazkova <polina@ipoli.io>
 * on 1/26/18.
 */
class AgendaViewController(args: Bundle? = null) :
    ReduxViewController<AgendaAction, AgendaViewState, AgendaPresenter>(args) {


    override val presenter get() = AgendaPresenter()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.controller_agenda, container, false)
        val layoutManager =
            LinearLayoutManager(container.context, LinearLayoutManager.VERTICAL, false)
        view.agendaList.layoutManager = layoutManager
        view.agendaList.adapter = AgendaAdapter()
        return view
    }

    private lateinit var scrollListener: EndlessRecyclerViewScrollListener

    override fun render(state: AgendaViewState, view: View) {
        when (state.type) {
            AgendaState.StateType.DATA_CHANGED -> {
                (view.agendaList.adapter as AgendaAdapter).updateAll(state.agendaItems)
                if(state.scrollToPosition >= 0) {
                    view.agendaList.scrollToPosition(state.scrollToPosition)
                }
                scrollListener = EndlessRecyclerViewScrollListener(
                    view.agendaList.layoutManager as LinearLayoutManager,
                    { side, position ->
                        view.agendaList.removeOnScrollListener(scrollListener)
                        if (side == EndlessRecyclerViewScrollListener.Side.TOP) {
                            dispatch(AgendaAction.LoadBefore(position))
                        } else {
                            dispatch(AgendaAction.LoadAfter(position))
                        }
                        Timber.d("AAA Scroll $side position $position")
                    },
                    3
                )
                view.agendaList.addOnScrollListener(scrollListener)

            }
        }
    }

    interface AgendaViewModel

    data class QuestViewModel(
        val name: String,
        val startTime: String,
        @ColorRes val color: Int,
        val icon: IIcon
    ) : AgendaViewModel

    data class DateHeaderViewModel(val text: String) : AgendaViewModel
    data class MonthDividerViewModel(
        @DrawableRes val image: Int, val text: String
    ) : AgendaViewModel

    data class WeekHeaderViewModel(val text: String) : AgendaViewModel

    enum class ItemType {
        QUEST, DATE_HEADER, MONTH_DIVIDER, WEEK_HEADER
    }

    inner class AgendaAdapter(private var viewModels: List<AgendaViewModel> = listOf()) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val vm = viewModels[holder.adapterPosition]
            val itemView = holder.itemView

            val type = ItemType.values()[getItemViewType(position)]
            when (type) {
                ItemType.QUEST -> bindQuestViewModel(itemView, vm as QuestViewModel)
                ItemType.DATE_HEADER -> bindDateHeaderViewModel(itemView, vm as DateHeaderViewModel)
                ItemType.MONTH_DIVIDER -> bindMonthDividerViewModel(
                    itemView,
                    vm as MonthDividerViewModel
                )
                ItemType.WEEK_HEADER -> bindWeekHeaderViewModel(
                    itemView,
                    vm as WeekHeaderViewModel
                )
            }
        }

        private fun bindWeekHeaderViewModel(
            view: View,
            viewModel: WeekHeaderViewModel
        ) {
            (view as TextView).text = viewModel.text
        }

        private fun bindMonthDividerViewModel(
            view: View,
            viewModel: MonthDividerViewModel
        ) {
            (view as TextView).text = viewModel.text
        }

        private fun bindDateHeaderViewModel(
            view: View,
            viewModel: DateHeaderViewModel
        ) {
            (view as TextView).text = viewModel.text
        }

        private fun bindQuestViewModel(
            view: View,
            vm: QuestViewModel
        ) {
            view.questName.text = vm.name
            view.questIcon.backgroundTintList =
                ColorStateList.valueOf(colorRes(vm.color))
            view.questIcon.setImageDrawable(
                IconicsDrawable(view.context)
                    .icon(vm.icon)
                    .colorRes(R.color.md_white)
                    .sizeDp(24)
            )
            view.questStartTime.text = vm.startTime
        }

        override fun getItemCount() = viewModels.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            when (viewType) {
                ItemType.QUEST.ordinal -> QuestViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_agenda_quest,
                        parent,
                        false
                    )
                )
                ItemType.DATE_HEADER.ordinal -> DateHeaderViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_agenda_date_header,
                        parent,
                        false
                    )
                )
                ItemType.MONTH_DIVIDER.ordinal -> MonthDividerViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_agenda_month_divider,
                        parent,
                        false
                    )
                )
                ItemType.WEEK_HEADER.ordinal -> WeekHeaderViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_agenda_week_header,
                        parent,
                        false
                    )
                )
                else -> null
            }

        inner class QuestViewHolder(view: View) : RecyclerView.ViewHolder(view)
        inner class DateHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view)
        inner class MonthDividerViewHolder(view: View) : RecyclerView.ViewHolder(view)
        inner class WeekHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemViewType(position: Int) =
            when (viewModels[position]) {
                is QuestViewModel -> ItemType.QUEST.ordinal
                is DateHeaderViewModel -> ItemType.DATE_HEADER.ordinal
                is MonthDividerViewModel -> ItemType.MONTH_DIVIDER.ordinal
                is WeekHeaderViewModel -> ItemType.WEEK_HEADER.ordinal
                else -> super.getItemViewType(position)

            }

        fun updateAll(viewModels: List<AgendaViewModel>) {
            this.viewModels = viewModels
            notifyDataSetChanged()
        }
    }


}