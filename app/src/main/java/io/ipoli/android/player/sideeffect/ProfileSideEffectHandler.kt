package io.ipoli.android.player.sideeffect

import android.arch.paging.ItemKeyedDataSource
import android.arch.paging.PagedList
import com.google.firebase.auth.FirebaseAuth
import io.ipoli.android.achievement.usecase.CreateAchievementItemsUseCase
import io.ipoli.android.common.AppSideEffectHandler
import io.ipoli.android.common.AppState
import io.ipoli.android.common.DataLoadedAction
import io.ipoli.android.common.ErrorLogger
import io.ipoli.android.common.datetime.instant
import io.ipoli.android.common.redux.Action
import io.ipoli.android.friends.feed.PostViewModel
import io.ipoli.android.friends.feed.data.Post
import io.ipoli.android.friends.feed.persistence.PostRepository
import io.ipoli.android.friends.usecase.SavePostReactionUseCase
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.profile.ProfileAction
import io.ipoli.android.player.profile.ProfileReducer
import io.ipoli.android.player.profile.ProfileViewState
import io.ipoli.android.player.usecase.FindAverageFocusedDurationForPeriodUseCase
import io.ipoli.android.player.usecase.SaveProfileUseCase
import kotlinx.coroutines.experimental.channels.Channel
import space.traversal.kapsule.required
import java.util.concurrent.Executor

object ProfileSideEffectHandler : AppSideEffectHandler() {

    private val findAverageFocusedDurationForPeriodUseCase by required { findAverageFocusedDurationForPeriodUseCase }
    private val saveProfileUseCase by required { saveProfileUseCase }
    private val createAchievementItemsUseCase by required { createAchievementItemsUseCase }
    private val friendRepository by required { friendRepository }
    private val playerRepository by required { playerRepository }
    private val postRepository by required { postRepository }
    private val challengeRepository by required { challengeRepository }
    private val executor by required { executorService }
    private val savePostReactionUseCase by required { savePostReactionUseCase }
    private val internetConnectionChecker by required { internetConnectionChecker }

    private var postsChangedChannel: Channel<Unit>? = null

    override suspend fun doExecute(action: Action, state: AppState) {
        when (action) {
            is ProfileAction.Load ->
                if (action.friendId != null) {
                    updateFriendData(action.friendId)
                } else {
                    state.dataState.player?.let {
                        updateProfileData(it)
                    }
                }

            is DataLoadedAction.PlayerChanged ->
                if (state.hasState(ProfileReducer.PROFILE_KEY))
                    updateProfileData(action.player)

            is ProfileAction.Save ->
                saveProfileUseCase.execute(
                    SaveProfileUseCase.Params(
                        displayName = action.displayName,
                        bio = action.bio
                    )
                )

            is ProfileAction.LoadFriends -> {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    dispatch(ProfileAction.ShowRequireLogin)
                    return
                }
                if (!internetConnectionChecker.isConnected()) {
                    dispatch(ProfileAction.NoInternetConnection)
                    return
                }
                try {
                    val friends = friendRepository.findAll()
                    dispatch(DataLoadedAction.FriendsChanged(friends))
                } catch (e: Throwable) {
                    ErrorLogger.log(e)
                    dispatch(ProfileAction.ErrorLoadingFriends)
                }
            }

            is ProfileAction.LoadPosts -> {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    dispatch(ProfileAction.ShowRequireLogin)
                    return
                }

                if (!internetConnectionChecker.isConnected()) {
                    dispatch(ProfileAction.NoInternetConnection)
                    return
                }

                val playerId = action.friendId ?: FirebaseAuth.getInstance().currentUser!!.uid

                listenForChanges(
                    oldChannel = postsChangedChannel,
                    channelCreator = {
                        postsChangedChannel = postRepository.listenForPlayerChange(playerId)
                        postsChangedChannel!!
                    },
                    onResult = { _ ->
                        val dataSource =
                            PostDataSource(
                                playerId,
                                postRepository,
                                action.mapToViewModel
                            )
                        val pagedList = PagedList.Builder(
                            dataSource,
                            PagedList.Config.Builder()
                                .setPageSize(10)
                                .setPrefetchDistance(10)
                                .setEnablePlaceholders(false)
                                .build()
                        )
                            .setFetchExecutor(executor)
                            .setNotifyExecutor(CurrentThreadExecutor())
                            .build()
                        dispatch(DataLoadedAction.PostsChanged(pagedList))
                    }
                )

            }

            is ProfileAction.UnloadPosts -> {
                postsChangedChannel?.close()
            }

            is ProfileAction.SaveDescription -> {
                postRepository.saveDescription(action.postId, action.description)
            }

            is ProfileAction.ReactToPost -> {
                val s: ProfileViewState = state.stateFor(
                    if (action.friendId == null) ProfileReducer.PROFILE_KEY
                    else ProfileReducer.FRIEND_KEY
                )

                savePostReactionUseCase.execute(
                    SavePostReactionUseCase.Params(
                        postPlayerId = action.friendId
                            ?: FirebaseAuth.getInstance().currentUser!!.uid,
                        postId = s.currentPostId!!,
                        reactionType = action.reaction
                    )
                )
            }

            is ProfileAction.LoadFriendChallenges -> {
                dispatch(
                    DataLoadedAction.FriendChallengesChanged(
                        challengeRepository.findForFriend(
                            action.friendId
                        )
                    )
                )
            }

        }
    }

    private class CurrentThreadExecutor : Executor {

        override fun execute(command: Runnable) {
            command.run()
        }
    }

    private fun updateProfileData(player: Player) {
        val ai = createAchievementItemsUseCase.execute(
            CreateAchievementItemsUseCase.Params(player)
        )
            .filter {
                it is CreateAchievementItemsUseCase.AchievementListItem.UnlockedItem || hasUnlockedAtLeast1Level(
                    it
                )
            }
            .map {
                when (it) {
                    is CreateAchievementItemsUseCase.AchievementListItem.UnlockedItem ->
                        it.achievementItem

                    is CreateAchievementItemsUseCase.AchievementListItem.LockedItem ->
                        it.achievementItem

                    else -> throw IllegalArgumentException("Unknown achievement type $it")
                }
            }
        dispatch(
            DataLoadedAction.ProfileDataChanged(
                player,
                ai,
                player.statistics.dailyChallengeCompleteStreak.count.toInt(),
                findAverageFocusedDurationForPeriodUseCase.execute(
                    FindAverageFocusedDurationForPeriodUseCase.Params(dayPeriod = 7)
                )
            )
        )
    }

    private fun updateFriendData(friendId: String) {
        val friend = playerRepository.findFriend(friendId)

        val ai = createAchievementItemsUseCase.execute(
            CreateAchievementItemsUseCase.Params(friend)
        )
            .filter {
                it is CreateAchievementItemsUseCase.AchievementListItem.UnlockedItem || hasUnlockedAtLeast1Level(
                    it
                )
            }
            .map {
                when (it) {
                    is CreateAchievementItemsUseCase.AchievementListItem.UnlockedItem ->
                        it.achievementItem

                    is CreateAchievementItemsUseCase.AchievementListItem.LockedItem ->
                        it.achievementItem

                    else -> throw IllegalArgumentException("Unknown achievement type $it")
                }
            }

        dispatch(
            DataLoadedAction.ProfileDataChanged(
                friend,
                ai,
                friend.statistics.dailyChallengeCompleteStreak.count.toInt(),
                findAverageFocusedDurationForPeriodUseCase.execute(
                    FindAverageFocusedDurationForPeriodUseCase.Params(
                        dayPeriod = 7,
                        friendId = friendId
                    )
                )
            )
        )
    }

    private fun hasUnlockedAtLeast1Level(it: CreateAchievementItemsUseCase.AchievementListItem) =
        it is CreateAchievementItemsUseCase.AchievementListItem.LockedItem && it.achievementItem.isMultiLevel && it.achievementItem.currentLevel >= 1

    override fun canHandle(action: Action) =
        action is ProfileAction || action is DataLoadedAction.PlayerChanged

    private class PostDataSource(
        private val playerId: String,
        private val postRepository: PostRepository,
        private val mapToViewModel: (Post) -> PostViewModel
    ) :
        ItemKeyedDataSource<Long, PostViewModel>() {
        override fun loadBefore(
            params: LoadParams<Long>,
            callback: LoadCallback<PostViewModel>
        ) {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun loadInitial(
            params: LoadInitialParams<Long>,
            callback: LoadInitialCallback<PostViewModel>
        ) {
            try {
                loadInitialPosts(params, callback)
            } catch (e: Throwable) {
                ErrorLogger.log(e)
                dispatch(ProfileAction.ErrorLoadingInitialPosts)
            }
        }

        private fun loadInitialPosts(
            params: LoadInitialParams<Long>,
            callback: LoadInitialCallback<PostViewModel>
        ) {
            val posts = postRepository.findForPlayer(playerId, params.requestedLoadSize)
            callback.onResult(posts.map(mapToViewModel), 0, posts.size)

            if (posts.isEmpty()) {
                dispatch(ProfileAction.EmptyPosts)
            } else {
                dispatch(ProfileAction.PostsLoaded)
            }
        }

        override fun loadAfter(
            params: LoadParams<Long>,
            callback: LoadCallback<PostViewModel>
        ) {
            try {
                val posts = postRepository.findForPlayer(
                    playerId,
                    params.requestedLoadSize,
                    params.key.instant
                )
                callback.onResult(posts.map(mapToViewModel))
            } catch (e: Throwable) {
                ErrorLogger.log(e)
                dispatch(ProfileAction.ErrorLoadingPosts)
            }
        }

        override fun getKey(item: PostViewModel) = item.createdAt

    }
}