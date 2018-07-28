package io.ipoli.android.friends

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.ipoli.android.R
import io.ipoli.android.common.AppState
import io.ipoli.android.common.BaseViewStateReducer
import io.ipoli.android.common.DataLoadedAction
import io.ipoli.android.common.redux.Action
import io.ipoli.android.common.redux.BaseViewState
import io.ipoli.android.common.view.ReduxDialogController
import io.ipoli.android.common.view.attrResourceId
import io.ipoli.android.common.view.colorRes
import io.ipoli.android.common.view.recyclerview.BaseRecyclerViewAdapter
import io.ipoli.android.common.view.recyclerview.RecyclerViewViewModel
import io.ipoli.android.common.view.recyclerview.SimpleViewHolder
import io.ipoli.android.friends.ReactionHistoryDialogViewState.StateType.*
import io.ipoli.android.friends.feed.data.Post
import io.ipoli.android.friends.usecase.CreateReactionHistoryItemsUseCase
import io.ipoli.android.pet.AndroidPetAvatar
import io.ipoli.android.pet.PetAvatar
import io.ipoli.android.player.data.AndroidAvatar
import kotlinx.android.synthetic.main.dialog_reaction_history.view.*
import kotlinx.android.synthetic.main.item_reaction_history.view.*
import kotlinx.android.synthetic.main.view_dialog_header.view.*

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 7/13/18.
 */
sealed class ReactionHistoryDialogAction : Action {
    data class Load(val reactions: List<Post.Reaction>) : ReactionHistoryDialogAction()
}

object ReactionHistoryDialogReducer : BaseViewStateReducer<ReactionHistoryDialogViewState>() {
    override val stateKey = key<ReactionHistoryDialogViewState>()

    override fun reduce(
        state: AppState,
        subState: ReactionHistoryDialogViewState,
        action: Action
    ): ReactionHistoryDialogViewState {
        return when (action) {
            is DataLoadedAction.ReactionHistoryItemsChanged -> {
                val player = state.dataState.player
                if (player != null) {
                    subState.copy(
                        type = DATA_CHANGED,
                        playerId = player.id,
                        petAvatar = player.pet.avatar,
                        reactions = action.items
                    )
                } else {
                    subState.copy(
                        type = LOADING,
                        reactions = action.items
                    )
                }
            }

            is DataLoadedAction.PlayerChanged ->
                subState.copy(
                    type = PET_AVATAR_CHANGED,
                    petAvatar = action.player.pet.avatar
                )

            else -> subState
        }
    }

    override fun defaultState() = ReactionHistoryDialogViewState(
        type = LOADING,
        playerId = null,
        petAvatar = null,
        reactions = null
    )


}

data class ReactionHistoryDialogViewState(
    val type: StateType,
    val playerId: String?,
    val petAvatar: PetAvatar?,
    val reactions: List<CreateReactionHistoryItemsUseCase.ReactionHistoryItem>?
) : BaseViewState() {
    enum class StateType {
        LOADING,
        DATA_CHANGED,
        PET_AVATAR_CHANGED
    }
}

class ReactionHistoryDialogViewController(args: Bundle? = null) :
    ReduxDialogController<ReactionHistoryDialogAction, ReactionHistoryDialogViewState, ReactionHistoryDialogReducer>(
        args
    ) {

    private lateinit var reactions: List<Post.Reaction>

    constructor(reactions: List<Post.Reaction>) : this() {
        this.reactions = reactions
    }

    override val reducer = ReactionHistoryDialogReducer

    override fun onCreateContentView(inflater: LayoutInflater, savedViewState: Bundle?): View {
        val view = inflater.inflate(R.layout.dialog_reaction_history, null)

        view.reactionList.layoutManager = LinearLayoutManager(view.context)
        view.reactionList.adapter = ReactionHistoryAdapter()

        return view
    }

    override fun onHeaderViewCreated(headerView: View) {
        headerView.dialogHeaderTitle.setText(R.string.history)
    }

    override fun onCreateLoadAction() =
        ReactionHistoryDialogAction.Load(reactions)

    override fun onCreateDialog(
        dialogBuilder: AlertDialog.Builder,
        contentView: View,
        savedViewState: Bundle?
    ): AlertDialog = dialogBuilder
        .setNegativeButton(R.string.cancel, null)
        .create()

    override fun render(state: ReactionHistoryDialogViewState, view: View) {
        when (state.type) {
            DATA_CHANGED -> {
                changeIcon(AndroidPetAvatar.valueOf(state.petAvatar!!.name).headImage)
                (view.reactionList.adapter as ReactionHistoryAdapter).updateAll(state.viewModels)
            }

            PET_AVATAR_CHANGED -> changeIcon(AndroidPetAvatar.valueOf(state.petAvatar!!.name).headImage)

            else -> {
            }
        }

    }


    data class ReactionViewModel(
        override val id: String,
        val avatar: AndroidAvatar,
        val name: String,
        val username: String,
        val time: String,
        @DrawableRes val reactionImage: Int,
        val isCurrentPlayer: Boolean
    ) : RecyclerViewViewModel

    inner class ReactionHistoryAdapter :
        BaseRecyclerViewAdapter<ReactionViewModel>(R.layout.item_reaction_history) {
        override fun onBindViewModel(vm: ReactionViewModel, view: View, holder: SimpleViewHolder) {
            Glide.with(view.context).load(vm.avatar.image)
                .apply(RequestOptions.circleCropTransform())
                .into(view.playerAvatar)

            val gradientDrawable = view.playerAvatar.background as GradientDrawable
            gradientDrawable.mutate()
            gradientDrawable.setColor(colorRes(vm.avatar.backgroundColor))

            Glide.with(view.context).load(vm.reactionImage)
                .apply(RequestOptions.circleCropTransform())
                .into(view.reactionImage)

            (view.reactionImage.background as GradientDrawable).setColor(Color.WHITE)

            view.playerName.text = vm.name
            view.playerUsername.text = vm.username
            view.reactionTime.text = vm.time

            if (vm.isCurrentPlayer) {
                view.isClickable = false
                view.background = null
                view.setOnClickListener(null)
            } else {
                view.setBackgroundResource(attrResourceId(android.R.attr.selectableItemBackground))
                view.isClickable = true
                view.onDebounceClick {
                    navigateFromRoot().toProfile(vm.id)
                }
            }
        }
    }

    private val ReactionHistoryDialogViewState.viewModels: List<ReactionViewModel>
        get() = reactions!!.map {
            ReactionViewModel(
                id = it.friend.id,
                avatar = AndroidAvatar.valueOf(it.friend.avatar.name),
                name = it.friend.displayName,
                username = "@${it.friend.username}",
                time = DateUtils.getRelativeTimeSpanString(
                    it.reaction.createdAt.toEpochMilli(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_ALL
                ).toString(),
                reactionImage = it.reaction.reactionType.androidType.image,
                isCurrentPlayer = it.friend.id == playerId
            )
        }
}