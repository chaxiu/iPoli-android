package io.ipoli.android.player.profile

import android.arch.paging.PagedList
import io.ipoli.android.achievement.usecase.CreateAchievementItemsUseCase
import io.ipoli.android.challenge.entity.Challenge
import io.ipoli.android.challenge.entity.SharingPreference
import io.ipoli.android.common.AppState
import io.ipoli.android.common.BaseViewStateReducer
import io.ipoli.android.common.DataLoadedAction
import io.ipoli.android.common.datetime.Duration
import io.ipoli.android.common.datetime.Minute
import io.ipoli.android.common.redux.Action
import io.ipoli.android.common.redux.BaseViewState
import io.ipoli.android.friends.feed.PostViewModel
import io.ipoli.android.friends.feed.data.Post
import io.ipoli.android.friends.persistence.Friend
import io.ipoli.android.pet.Pet
import io.ipoli.android.player.data.Avatar
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.profile.ProfileViewState.StateType.*

sealed class ProfileAction : Action {
    data class Save(val displayName: String, val bio: String) : ProfileAction() {
        override fun toMap() = mapOf("displayName" to displayName)
    }

    data class Load(val friendId: String?) : ProfileAction() {
        override fun toMap() = mapOf("friendId" to friendId)
    }

    data class LoadFriendChallenges(val friendId: String) : ProfileAction() {
        override fun toMap() = mapOf("friendId" to friendId)
    }


    object StartEdit : ProfileAction()
    object StopEdit : ProfileAction()
    object LoadPlayerChallenges : ProfileAction()
    object LoadFriends : ProfileAction()
    data class LoadPosts(val mapToViewModel: (Post) -> PostViewModel, val friendId: String?) :
        ProfileAction()

    data class SaveDescription(val postId: String, val description: String?) : ProfileAction() {
        override fun toMap() = mapOf("postId" to postId, "description" to description)
    }

    data class ShowReactionPopupForPost(val postId: String) : ProfileAction()

    data class ReactToPost(
        val reaction: Post.ReactionType,
        val friendId: String?
    ) : ProfileAction() {
        override fun toMap() = mapOf(
            "reaction" to reaction.name,
            "friendId" to friendId
        )
    }

    object ShowRequireLogin : ProfileAction()
    object UnloadPosts : ProfileAction()
    object LoadInfo : ProfileAction()
    object NoInternetConnection : ProfileAction()
    object ErrorLoadingInitialPosts : ProfileAction()
    object EmptyPosts : ProfileAction()
    object PostsLoaded : ProfileAction()
    object ErrorLoadingPosts : ProfileAction()
    object ErrorLoadingFriends : ProfileAction()
}

class ProfileReducer(reducerKey: String) : BaseViewStateReducer<ProfileViewState>() {

    override fun reduce(
        state: AppState,
        subState: ProfileViewState,
        action: Action
    ) =
        when (action) {

            is ProfileAction.Load -> {
                if (action.friendId != null) {
                    subState.copy(
                        type = LOADING,
                        friendId = action.friendId
                    )
                } else {
                    val p = state.dataState.player
                    if (p != null)
                        createStateFromPlayer(p, subState)
                    else
                        subState
                }
            }

            is DataLoadedAction.PlayerChanged -> {
                val p = action.player
                createStateFromPlayer(p, subState)
            }

            is DataLoadedAction.ProfileDataChanged -> {

                val ns = createStateFromPlayer(action.player, subState)

                val newState = ns.copy(
                    dailyChallengeStreak = action.streak,
                    last7DaysAverageProductiveDuration = action.averageProductiveDuration,
                    unlockedAchievements = action.unlockedAchievements
                )
                if (hasProfileDataLoaded(newState)) {
                    newState.copy(
                        type = ProfileViewState.StateType.PROFILE_DATA_LOADED
                    )
                } else {
                    newState
                }
            }

            is ProfileAction.LoadInfo ->
                if (subState.username != null)
                    subState.copy(
                        type = PROFILE_INFO_LOADED
                    )
                else
                    subState

            is ProfileAction.StartEdit ->
                subState.copy(
                    type = EDIT
                )

            ProfileAction.StopEdit ->
                subState.copy(
                    type = ProfileViewState.StateType.EDIT_STOPPED
                )

            is ProfileAction.LoadPlayerChallenges -> {
                val player = state.dataState.player
                when {
                    player == null -> subState.copy(
                        type = ProfileViewState.StateType.CHALLENGE_LIST_LOADING
                    )
                    player.authProvider == null -> subState.copy(
                        type = SHOW_REQUIRE_LOGIN
                    )
                    state.dataState.challenges == null -> subState.copy(
                        type = ProfileViewState.StateType.CHALLENGE_LIST_LOADING
                    )
                    else -> subState.copy(
                        type = CHALLENGE_LIST_DATA_CHANGED,
                        challenges = state.dataState.challenges.filter { it.sharingPreference == SharingPreference.FRIENDS }
                    )
                }

            }

            is DataLoadedAction.FriendChallengesChanged ->
                subState.copy(
                    type = CHALLENGE_LIST_DATA_CHANGED,
                    challenges = action.challenges
                )

            is DataLoadedAction.FriendsChanged ->
                subState.copy(
                    type = FRIENDS_LIST_CHANGED,
                    friends = action.friends
                )

            is ProfileAction.ShowRequireLogin ->
                subState.copy(
                    type = SHOW_REQUIRE_LOGIN
                )

            is DataLoadedAction.PostsChanged ->
                subState.copy(
                    type = POSTS_CHANGED,
                    posts = action.posts
                )

            is ProfileAction.ShowReactionPopupForPost ->
                subState.copy(
                    type = REACTION_POPUP_SHOWN,
                    currentPostId = action.postId
                )

            is ProfileAction.ErrorLoadingInitialPosts,
            is ProfileAction.ErrorLoadingPosts,
            is ProfileAction.ErrorLoadingFriends,
            is ProfileAction.NoInternetConnection -> {
                subState.copy(
                    type = NO_INTERNET_CONNECTION
                )
            }

            is ProfileAction.PostsLoaded ->
                subState.copy(
                    type = NON_EMPTY_POSTS
                )

            is ProfileAction.EmptyPosts ->
                subState.copy(
                    type = EMPTY_POSTS
                )

            else -> subState
        }

    private fun hasProfileDataLoaded(state: ProfileViewState) =
        state.pet != null && state.last7DaysAverageProductiveDuration != null && state.dailyChallengeStreak >= 0

    private fun createStateFromPlayer(
        player: Player,
        subState: ProfileViewState
    ): ProfileViewState {
        val newState = subState.copy(
            avatar = player.avatar,
            displayName = player.displayName,
            username = player.username,
            titleIndex = player.level / 10,
            createdAgo = player.createdAt.toEpochMilli(),
            bio = player.bio,
            level = player.level,
            levelXpProgress = player.experienceProgressForLevel,
            levelXpMaxProgress = player.experienceForNextLevel,
            coins = player.coins,
            gems = player.gems,
            pet = player.pet
        )
        return if (hasProfileDataLoaded(newState)) {
            newState.copy(
                type = PROFILE_DATA_LOADED
            )
        } else {
            newState
        }
    }

    override fun defaultState() =
        ProfileViewState(
            type = LOADING,
            avatar = Avatar.AVATAR_00,
            displayName = null,
            username = null,
            bio = null,
            titleIndex = -1,
            createdAgo = -1,
            pet = null,
            level = -1,
            levelXpProgress = -1,
            levelXpMaxProgress = -1,
            coins = -1,
            gems = -1,
            dailyChallengeStreak = -1,
            last7DaysAverageProductiveDuration = null,
            unlockedAchievements = emptyList(),
            challenges = null,
            friends = null,
            posts = null,
            currentPostId = null,
            friendId = null
        )

    override val stateKey = reducerKey

    companion object {
        const val PROFILE_KEY = "ProfileViewState"
        const val FRIEND_KEY = "FriendViewState"
    }
}

data class ProfileViewState(
    val type: StateType,
    val avatar: Avatar,
    val displayName: String?,
    val username: String?,
    val titleIndex: Int,
    val createdAgo: Long,
    val bio: String?,
    val pet: Pet?,
    val level: Int,
    val levelXpProgress: Int,
    val levelXpMaxProgress: Int,
    val coins: Int,
    val gems: Int,
    val dailyChallengeStreak: Int,
    val last7DaysAverageProductiveDuration: Duration<Minute>?,
    val unlockedAchievements: List<CreateAchievementItemsUseCase.AchievementItem>,
    val challenges: List<Challenge>?,
    val friends: List<Friend>?,
    val posts: PagedList<PostViewModel>?,
    val currentPostId: String?,
    val friendId: String?
) : BaseViewState() {

    enum class StateType {
        LOADING,
        PROFILE_DATA_LOADED,
        EDIT,
        EDIT_STOPPED,
        CHALLENGE_LIST_DATA_CHANGED,
        CHALLENGE_LIST_LOADING,
        FRIENDS_LIST_CHANGED,
        SHOW_REQUIRE_LOGIN,
        POSTS_CHANGED,
        REACTION_POPUP_SHOWN,
        PROFILE_INFO_LOADED,
        NO_INTERNET_CONNECTION,
        NON_EMPTY_POSTS,
        EMPTY_POSTS
    }
}