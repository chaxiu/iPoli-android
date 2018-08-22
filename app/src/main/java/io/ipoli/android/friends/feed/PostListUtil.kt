package io.ipoli.android.friends.feed

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.support.annotation.DrawableRes
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableString
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.view.MotionEvent
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.ipoli.android.R
import io.ipoli.android.achievement.Achievement
import io.ipoli.android.achievement.androidAchievement
import io.ipoli.android.common.ViewUtils
import io.ipoli.android.common.text.DurationFormatter
import io.ipoli.android.common.view.*
import io.ipoli.android.common.view.recyclerview.BaseRecyclerViewAdapter
import io.ipoli.android.common.view.recyclerview.MultiViewPagedRecyclerViewAdapter
import io.ipoli.android.common.view.recyclerview.RecyclerViewViewModel
import io.ipoli.android.common.view.recyclerview.SimpleViewHolder
import io.ipoli.android.friends.feed.data.AndroidReactionType
import io.ipoli.android.friends.feed.data.Post
import io.ipoli.android.player.data.AndroidAvatar
import kotlinx.android.synthetic.main.item_achievement.view.*
import kotlinx.android.synthetic.main.item_feed.view.*
import kotlinx.android.synthetic.main.item_popup_reaction.view.*
import kotlinx.android.synthetic.main.item_reaction.view.*

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 7/23/18.
 */
sealed class PostViewModel(
    override val id: String
) : RecyclerViewViewModel {
    abstract val playerId: String
    abstract val playerAvatar: AndroidAvatar
    abstract val playerName: String
    abstract val playerUsername: String
    abstract val playerLevel: String
    abstract val postedTime: String
    abstract val canEdit: Boolean
    abstract val playerMessage: String
    abstract val reactions: List<ReactionViewModel>
    abstract val createdAt: Long
    abstract val post: Post
    abstract val reactListener: (postId: String, playerId: String) -> Unit
    abstract val reactListListener: (reactions: List<Post.Reaction>) -> Unit
    abstract val saveListener: ((postId: String, playerMessage: String?) -> Unit)?
    abstract val postClickListener: ((postPlayerId: String) -> Unit)?

    data class SimpleViewModel(
        override val id: String,
        override val playerId: String,
        override val playerAvatar: AndroidAvatar,
        override val playerName: String,
        override val playerUsername: String,
        override val playerLevel: String,
        override val postedTime: String,
        override val canEdit: Boolean = false,
        val message: SpannableString,
        val messageIcon: Int,
        override val playerMessage: String,
        override val reactions: List<ReactionViewModel>,
        override val createdAt: Long,
        override val post: Post,
        override val reactListener: (postId: String, playerId: String) -> Unit,
        override val reactListListener: (reactions: List<Post.Reaction>) -> Unit,
        override val saveListener: ((postId: String, playerMessage: String?) -> Unit)?,
        override val postClickListener: ((postPlayerId: String) -> Unit)?
    ) : PostViewModel(id)

    data class ComplexViewModel(
        override val id: String,
        override val playerId: String,
        override val playerAvatar: AndroidAvatar,
        override val playerName: String,
        override val playerUsername: String,
        override val playerLevel: String,
        override val postedTime: String,
        @DrawableRes val image: Int,
        val title: String,
        val message: SpannableString,
        override val canEdit: Boolean = false,
        override val playerMessage: String,
        override val reactions: List<ReactionViewModel>,
        override val createdAt: Long,
        override val post: Post,
        override val reactListener: (postId: String, playerId: String) -> Unit,
        override val reactListListener: (reactions: List<Post.Reaction>) -> Unit,
        override val saveListener: ((postId: String, playerMessage: String?) -> Unit)?,
        override val postClickListener: ((postPlayerId: String) -> Unit)?
    ) : PostViewModel(id)

    data class AchievementViewModel(
        override val id: String,
        override val playerId: String,
        override val playerAvatar: AndroidAvatar,
        override val playerName: String,
        override val playerUsername: String,
        override val playerLevel: String,
        override val postedTime: String,
        @DrawableRes val image: Int,
        val showStars: Boolean,
        @DrawableRes val star1: Int,
        @DrawableRes val star2: Int,
        @DrawableRes val star3: Int,
        val title: String,
        val message: SpannableString,
        val color: Int,
        override val canEdit: Boolean = false,
        override val playerMessage: String,
        override val reactions: List<ReactionViewModel>,
        override val createdAt: Long,
        override val post: Post,
        override val reactListener: (postId: String, playerId: String) -> Unit,
        override val reactListListener: (reactions: List<Post.Reaction>) -> Unit,
        override val saveListener: ((postId: String, playerMessage: String?) -> Unit)?,
        override val postClickListener: ((postPlayerId: String) -> Unit)?
    ) : PostViewModel(id)

    data class ForChallengeViewModel(
        override val id: String,
        override val playerId: String,
        override val playerAvatar: AndroidAvatar,
        override val playerName: String,
        override val playerUsername: String,
        override val playerLevel: String,
        override val postedTime: String,
        val challengeName: String,
        override val canEdit: Boolean = false,
        override val playerMessage: String,
        val message: SpannableString,
        val messageIcon: Int,
        override val reactions: List<ReactionViewModel>,
        override val createdAt: Long,
        override val post: Post,
        override val reactListener: (postId: String, playerId: String) -> Unit,
        override val reactListListener: (reactions: List<Post.Reaction>) -> Unit,
        override val saveListener: ((postId: String, playerMessage: String?) -> Unit)?,
        override val postClickListener: ((postPlayerId: String) -> Unit)?
    ) : PostViewModel(id)
}

enum class PostType {
    SIMPLE, COMPLEX, FOR_CHALLENGE, ACHIEVEMENT
}

class PostAdapter(private val reactionPopupHandler: ReactionPopupHandler) :
    MultiViewPagedRecyclerViewAdapter<PostViewModel>() {
    override fun onRegisterItemBinders() {

        registerBinder<PostViewModel.SimpleViewModel>(
            PostType.SIMPLE.ordinal,
            R.layout.item_feed
        ) { vm, view, _ ->
            view.messageGroup.visible()
            (view.complexGroup.views() + view.feedBigPicture + view.feedAchievement).map { it.gone() }
            view.challengeGroup.gone()

            bindHeader(vm, view)
            bindPlayerMessage(vm, view)
            bindFooter(vm, view)

            view.feedMessage.text = vm.message
            view.feedMessageIcon.setImageResource(vm.messageIcon)
        }

        registerBinder<PostViewModel.ComplexViewModel>(
            PostType.COMPLEX.ordinal,
            R.layout.item_feed
        ) { vm, view, _ ->
            view.messageGroup.gone()
            view.feedBigPicture.visible()
            view.complexGroup.visible()
            view.challengeGroup.gone()
            view.feedAchievement.gone()

            bindHeader(vm, view)
            bindPlayerMessage(vm, view)
            bindFooter(vm, view)

            view.feedBigPicture.setImageResource(vm.image)
            view.feedComplexTitle.text = vm.title
            view.feedComplexMessage.text = vm.message
        }

        registerBinder<PostViewModel.AchievementViewModel>(
            PostType.ACHIEVEMENT.ordinal,
            R.layout.item_feed
        ) { vm, view, _ ->
            view.messageGroup.gone()
            view.challengeGroup.gone()
            view.complexGroup.visible()
            view.feedBigPicture.invisible()
            view.feedAchievement.visible()

            bindHeader(vm, view)
            bindPlayerMessage(vm, view)
            bindFooter(vm, view)

            view.achievementIcon.setImageResource(vm.image)
            view.achievementBackground.backgroundTintList =
                ColorStateList.valueOf(view.context.colorRes(vm.color))

            if (vm.showStars) {
                view.stars.visible()
                view.star1.setImageResource(vm.star1)
                view.star2.setImageResource(vm.star2)
                view.star3.setImageResource(vm.star3)
            } else {
                view.stars.gone()
            }
            view.feedComplexTitle.text = vm.title
            view.feedComplexMessage.text = vm.message
        }

        registerBinder<PostViewModel.ForChallengeViewModel>(
            PostType.FOR_CHALLENGE.ordinal,
            R.layout.item_feed
        ) { vm, view, _ ->
            view.messageGroup.visible()
            (view.complexGroup.views() + view.feedBigPicture + view.feedAchievement).map { it.gone() }
            view.challengeGroup.visible()

            bindHeader(vm, view)
            bindPlayerMessage(vm, view)
            bindFooter(vm, view)

            val gradientDrawable =
                view.feedChallengeIconBackground.background as GradientDrawable
            gradientDrawable.mutate()
            gradientDrawable.setColor(view.context.colorRes(R.color.md_blue_500))

            view.feedChallengeTitle.text = vm.challengeName
            view.feedMessage.text = vm.message
            view.feedMessageIcon.setImageResource(vm.messageIcon)

        }
    }

    private fun bindHeader(vm: PostViewModel, view: View) {
        view.openProfile.setOnClickListener(Debounce.clickListener {
            if (reactionPopupHandler.isPopupShown()) reactionPopupHandler.hideReactionPopup()
            else vm.postClickListener?.invoke(vm.playerId)
        })

        Glide.with(view.context).load(vm.playerAvatar.image)
            .apply(RequestOptions.circleCropTransform())
            .into(view.playerAvatar)

        val gradientDrawable = view.playerAvatar.background as GradientDrawable
        gradientDrawable.mutate()
        gradientDrawable.setColor(view.context.colorRes(vm.playerAvatar.backgroundColor))

        view.playerName.text = vm.playerName
        @SuppressLint("SetTextI18n")
        view.playerUsername.text = "@${vm.playerUsername}"
        view.playerLevel.text = vm.playerLevel

        view.postedTime.text = vm.postedTime
    }

    private fun bindFooter(vm: PostViewModel, view: View) {
        view.reactionList.layoutManager =
            LinearLayoutManager(view.context, RecyclerView.HORIZONTAL, false)
        view.reactionList.adapter = ReactionAdapter()
        if (vm.reactions.isEmpty()) {
            view.reactionList.gone()
        } else {
            view.reactionList.visible()
            (view.reactionList.adapter as ReactionAdapter).updateAll(vm.reactions)

            view.reactionList.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    vm.reactListListener(vm.post.reactions)
                    true
                } else false
            }
        }

        val popupHeight = ViewUtils.dpToPx(128f, view.context)



        view.react.setOnClickListener(
            Debounce.clickListener {
                val containerCoordinates = reactionPopupHandler.containerCoordinates()
                val containerX = containerCoordinates[0]
                val containerY = containerCoordinates[1]

                val location = IntArray(2)
                view.react.getLocationOnScreen(location)

                val x = location[0] - containerX
                val buttonY = location[1]

                val y: Float = if (buttonY - containerY >= popupHeight) {
                    buttonY - containerY - popupHeight
                } else {
                    (buttonY - containerY + it.height).toFloat()
                }

                vm.reactListener(vm.id, vm.playerId)
                reactionPopupHandler.showReactionPopup(x, y)
            })
    }


    private fun bindPlayerMessage(vm: PostViewModel, view: View) {
        if (vm.canEdit) {
            ViewUtils.hideKeyboard(view)
            view.requestFocus()

            view.playerMessage.gone()
            view.playerMessageEdit.visible()
            view.editGroup.gone()
            view.playerMessageEdit.setText(vm.playerMessage)

            view.playerMessageEdit.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    view.reactionGroup.gone()
                    view.editGroup.visible()
                } else {
                    view.reactionGroup.visible()
                    view.editGroup.gone()
                }
            }

            view.cancel.setOnClickListener(
                Debounce.clickListener {
                    ViewUtils.hideKeyboard(view.playerMessageEdit)
                    view.playerMessageEdit.clearFocus()
                    view.playerMessageEdit.setText(vm.playerMessage)
                    view.requestFocus()
                    view.editGroup.gone()
                }
            )

            view.save.setOnClickListener(Debounce.clickListener {
                ViewUtils.hideKeyboard(view.playerMessageEdit)
                view.requestFocus()
                val message = view.playerMessageEdit.text.toString()
                vm.saveListener?.invoke(vm.id, if (message.isBlank()) null else message)
            })

        } else {
            view.playerMessageEdit.gone()
            view.editGroup.gone()
            view.playerMessage.text = vm.playerMessage
            if (vm.playerMessage.isNotBlank()) view.playerMessage.visible()
            else view.playerMessage.gone()
        }
    }
}

data class ReactionViewModel(
    override val id: String,
    val image: Int,
    val count: Int
) : RecyclerViewViewModel

class ReactionAdapter :
    BaseRecyclerViewAdapter<ReactionViewModel>(R.layout.item_reaction) {

    override fun onBindViewModel(vm: ReactionViewModel, view: View, holder: SimpleViewHolder) {
        view.reactionImage.setImageResource(vm.image)
        view.reactionCount.text = vm.count.toString()
    }

}

data class ReactionPopupViewModel(
    override val id: String,
    val animation: String,
    val text: String,
    val reaction: Post.ReactionType,
    val reactListener: (reaction: Post.ReactionType) -> Unit
) : RecyclerViewViewModel


class ReactionPopupAdapter(private val popupHandler: ReactionPopupHandler) :
    BaseRecyclerViewAdapter<ReactionPopupViewModel>(R.layout.item_popup_reaction) {
    override fun onBindViewModel(
        vm: ReactionPopupViewModel,
        view: View,
        holder: SimpleViewHolder
    ) {
        view.reactionAnimation.setAnimation(vm.animation)
        view.reactionText.text = vm.text
        view.setOnClickListener(
            Debounce.clickListener {
                vm.reactListener(vm.reaction)
                popupHandler.hideReactionPopup()
            }
        )
    }
}

fun toPostViewModel(
    context: Context,
    post: Post,
    reactListener: (postId: String, playerId: String) -> Unit,
    reactListListener: (reactions: List<Post.Reaction>) -> Unit,
    saveListener: ((postId: String, playerMessage: String?) -> Unit)? = null,
    postClickListener: ((postPlayerId: String) -> Unit)? = null,
    canEdit: Boolean = false
): PostViewModel {
    val reactions = post.reactions.groupBy { it.reactionType }.map {
        ReactionViewModel(
            it.key.name,
            AndroidReactionType.valueOf(it.key.name).image,
            it.value.size
        )
    }
    return when (post.data) {
        is Post.Data.DailyChallengeCompleted -> {
            val message = when {
                post.data.streak == 1 -> "Completed Daily Challenge"
                post.data.streak < post.data.bestStreak -> "Completed Daily Challenge ${post.data.streak} times in a row"
                else -> "New Daily Challenge best streak: ${post.data.bestStreak} times in a row"
            }
            createSimpleViewModel(
                post,
                SpannableString.valueOf(message),
                R.drawable.ic_post_dailychallenge,
                reactions,
                reactListener,
                reactListListener,
                saveListener,
                postClickListener,
                canEdit
            )
        }

        is Post.Data.DailyChallengeFailed ->
            createSimpleViewModel(
                post,
                SpannableString.valueOf("Failed to complete the Daily Challenge"),
                R.drawable.ic_post_failed,
                reactions,
                reactListener,
                reactListListener,
                saveListener,
                postClickListener,
                canEdit
            )

        is Post.Data.QuestShared -> {
            val completedQuest = context.stringRes(R.string.feed_completed_quest)
            val message = "$completedQuest ${post.data.questName}"

            val finalMessage = if (post.data.durationTracked.intValue > 0) {
                val duration = " " + context.stringRes(
                    R.string.feed_timer, DurationFormatter.formatShort(
                        context,
                        post.data.durationTracked.intValue
                    )
                )
                context.lightenText(message + duration, completedQuest, duration)
            } else
                context.lightenText(message, completedQuest)
            createSimpleViewModel(
                post,
                finalMessage,
                R.drawable.ic_post_done, reactions,
                reactListener,
                reactListListener,
                saveListener,
                postClickListener,
                canEdit
            )
        }

        is Post.Data.QuestWithPomodoroShared -> {
            val completedQuest = context.stringRes(R.string.feed_completed_quest)
            val pomodoros = context.stringRes(R.string.feed_pomodoro_count, post.data.pomodoroCount)
            val m = "$completedQuest ${post.data.questName} $pomodoros"
            createSimpleViewModel(
                post,
                context.lightenText(m, completedQuest, pomodoros),
                R.drawable.ic_post_done,
                reactions,
                reactListener,
                reactListListener,
                saveListener,
                postClickListener,
                canEdit
            )
        }

        is Post.Data.LevelUp ->
            createSimpleViewModel(
                post,
                SpannableString.valueOf("Raised to Level ${post.data.level}"),
                R.drawable.ic_post_level,
                reactions,
                reactListener,
                reactListListener,
                saveListener,
                postClickListener,
                canEdit
            )

        is Post.Data.AchievementUnlocked -> {
            val achievement = Achievement.valueOf(post.data.achievement.name)
            val aa = achievement.androidAchievement

            val starsToShow = if (aa.levelDescriptions.size == 1) -1 else achievement.level

            val star1 =
                if (starsToShow > 0) R.drawable.achievement_star else R.drawable.achievement_star_empty
            val star2 =
                if (starsToShow > 1) R.drawable.achievement_star else R.drawable.achievement_star_empty
            val star3 =
                if (starsToShow > 2) R.drawable.achievement_star else R.drawable.achievement_star_empty

            val name = context.stringRes(aa.title)
            val message = if (starsToShow > 0) "$name (Level $starsToShow)" else name

            return PostViewModel.AchievementViewModel(
                id = post.id,
                playerId = post.playerId,
                playerAvatar = AndroidAvatar.valueOf(post.playerAvatar.name),
                playerName = post.playerDisplayName,
                playerUsername = post.playerUsername,
                playerLevel = "Level ${post.playerLevel}",
                postedTime = DateUtils.getRelativeTimeSpanString(
                    post.createdAt.toEpochMilli(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_ALL
                ).toString(),
                playerMessage = post.description ?: "",
                reactions = reactions,
                title = "Unlocked achievement",
                message = SpannableString.valueOf(message),
                color = aa.color,
                image = aa.icon,
                createdAt = post.createdAt.toEpochMilli(),
                showStars = starsToShow > 0,
                star1 = star1,
                star2 = star2,
                star3 = star3,
                post = post,
                reactListener = reactListener,
                reactListListener = reactListListener,
                saveListener = saveListener,
                postClickListener = postClickListener,
                canEdit = canEdit
            )
        }

        is Post.Data.ChallengeShared -> {
            val accepted = context.stringRes(R.string.feed_accepted_challenge)
            val m = "$accepted ${post.data.name}"
            createSimpleViewModel(
                post,
                context.lightenText(m, accepted),
                R.drawable.ic_post_challenge,
                reactions,
                reactListener,
                reactListListener,
                saveListener,
                postClickListener,
                canEdit
            )
        }

        is Post.Data.ChallengeCompleted ->
            createComplexViewModel(
                post,
                "Completed Challenge",
                SpannableString.valueOf("${post.data.name} achieved in ${post.data.duration.intValue} days"),
                R.drawable.drawer_achievement_trophy,
                reactions,
                reactListener,
                reactListListener,
                saveListener,
                postClickListener,
                canEdit
            )

        is Post.Data.QuestFromChallengeCompleted -> {
            val completedQuest = context.stringRes(R.string.feed_completed_quest)
            val message = "$completedQuest ${post.data.questName}"

            val finalMessage = if (post.data.durationTracked.intValue > 0) {
                val duration = " " + context.stringRes(
                    R.string.feed_timer, DurationFormatter.formatShort(
                        context,
                        post.data.durationTracked.intValue
                    )
                )
                context.lightenText(message + duration, completedQuest, duration)
            } else
                context.lightenText(message, completedQuest)
            createPostForChallengeViewModel(
                post,
                post.data.challengeName,
                finalMessage,
                R.drawable.ic_post_done,
                reactions,
                reactListener,
                reactListListener,
                saveListener,
                postClickListener,
                canEdit
            )
        }

        is Post.Data.QuestWithPomodoroFromChallengeCompleted -> {
            val completedQuest = context.stringRes(R.string.feed_completed_quest)
            val pomodoros = context.stringRes(R.string.feed_pomodoro_count, post.data.pomodoroCount)
            val m = "$completedQuest ${post.data.questName} $pomodoros"
            createPostForChallengeViewModel(
                post,
                post.data.challengeName,
                context.lightenText(m, completedQuest, pomodoros),
                R.drawable.ic_post_done,
                reactions,
                reactListener,
                reactListListener,
                saveListener,
                postClickListener,
                canEdit
            )
        }

        is Post.Data.QuestFromChallengeFailed -> {
            val failed = context.stringRes(R.string.feed_failed_quest)
            val m = "$failed ${post.data.questName}"
            createPostForChallengeViewModel(
                post,
                post.data.challengeName,
                context.lightenText(m, failed),
                R.drawable.ic_post_failed,
                reactions,
                reactListener,
                reactListListener,
                saveListener,
                postClickListener,
                canEdit
            )
        }

        is Post.Data.HabitFromChallengeCompleted -> {
            val message = when {
                post.data.streak == 1 -> {
                    val completedHabit = context.stringRes(R.string.feed_completed_habit)
                    val m = "$completedHabit ${post.data.habitName}"
                    context.lightenText(m, completedHabit)
                }
                post.data.streak < post.data.bestStreak -> {
                    val completedHabit = context.stringRes(R.string.feed_completed_habit)
                    val timesInARow = context.stringRes(R.string.feed_times_in_a_row)
                    val m =
                        "$completedHabit ${post.data.habitName} ${post.data.streak} $timesInARow"
                    context.lightenText(m, completedHabit, timesInARow)
                }
                else -> {
                    val newBestStreak = context.stringRes(R.string.feed_best_streak_habit)
                    val timesInARow = context.stringRes(R.string.feed_times_in_a_row)
                    val m =
                        "$newBestStreak ${post.data.habitName}: ${post.data.bestStreak} $timesInARow"
                    context.lightenText(m, newBestStreak, timesInARow)
                }
            }
            createPostForChallengeViewModel(
                post,
                post.data.challengeName,
                message,
                R.drawable.ic_post_habit,
                reactions,
                reactListener,
                reactListListener,
                saveListener,
                postClickListener,
                canEdit
            )
        }

        is Post.Data.HabitFromChallengeFailed -> {
            val message = if (post.data.isGood) {
                "Failed to complete Habit ${post.data.habitName}"
            } else {
                "Failed Habit ${post.data.habitName}"
            }
            createPostForChallengeViewModel(
                post,
                post.data.challengeName,
                SpannableString.valueOf(message),
                R.drawable.ic_post_failed,
                reactions,
                reactListener,
                reactListListener,
                saveListener,
                postClickListener,
                canEdit
            )
        }
    }
}

private fun Context.lightenText(
    text: String,
    firstPart: String,
    lastPart: String? = null
): SpannableString {
    val textSpan = SpannableString.valueOf(text)
    textSpan.setSpan(
        ForegroundColorSpan(
            colorRes(R.color.md_dark_text_54)
        ), 0, firstPart.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE
    )
    lastPart?.let {
        textSpan.setSpan(
            ForegroundColorSpan(
                colorRes(R.color.md_dark_text_54)
            ), text.length - it.length, text.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )
    }
    return textSpan
}

private fun createPostForChallengeViewModel(
    it: Post,
    challengeName: String,
    message: SpannableString,
    messageIcon: Int,
    reactions: List<ReactionViewModel>,
    reactListener: (postId: String, playerId: String) -> Unit,
    reactListListener: (reactions: List<Post.Reaction>) -> Unit,
    saveListener: ((postId: String, playerMessage: String?) -> Unit)?,
    postClickListener: ((postPlayerId: String) -> Unit)? = null,
    canEdit: Boolean = false
): PostViewModel.ForChallengeViewModel {
    return PostViewModel.ForChallengeViewModel(
        id = it.id,
        playerId = it.playerId,
        playerAvatar = AndroidAvatar.valueOf(it.playerAvatar.name),
        playerName = it.playerDisplayName,
        playerUsername = it.playerUsername,
        playerLevel = "Level ${it.playerLevel}",
        postedTime = DateUtils.getRelativeTimeSpanString(
            it.createdAt.toEpochMilli(),
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_ALL
        ).toString(),
        playerMessage = it.description ?: "",
        reactions = reactions,
        challengeName = challengeName,
        message = message,
        messageIcon = messageIcon,
        createdAt = it.createdAt.toEpochMilli(),
        post = it,
        reactListener = reactListener,
        reactListListener = reactListListener,
        saveListener = saveListener,
        postClickListener = postClickListener,
        canEdit = canEdit
    )
}

private fun createComplexViewModel(
    it: Post,
    title: String,
    message: SpannableString,
    image: Int,
    reactions: List<ReactionViewModel>,
    reactListener: (postId: String, playerId: String) -> Unit,
    reactListListener: (reactions: List<Post.Reaction>) -> Unit,
    saveListener: ((postId: String, playerMessage: String?) -> Unit)?,
    postClickListener: ((postPlayerId: String) -> Unit)? = null,
    canEdit: Boolean = false
): PostViewModel.ComplexViewModel {
    return PostViewModel.ComplexViewModel(
        id = it.id,
        playerId = it.playerId,
        playerAvatar = AndroidAvatar.valueOf(it.playerAvatar.name),
        playerName = it.playerDisplayName,
        playerUsername = it.playerUsername,
        playerLevel = "Level ${it.playerLevel}",
        postedTime = DateUtils.getRelativeTimeSpanString(
            it.createdAt.toEpochMilli(),
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_ALL
        ).toString(),
        playerMessage = it.description ?: "",
        reactions = reactions,
        title = title,
        message = message,
        image = image,
        createdAt = it.createdAt.toEpochMilli(),
        post = it,
        reactListener = reactListener,
        reactListListener = reactListListener,
        saveListener = saveListener,
        postClickListener = postClickListener,
        canEdit = canEdit
    )
}

private fun createSimpleViewModel(
    it: Post,
    message: SpannableString,
    messageIcon: Int,
    reactions: List<ReactionViewModel>,
    reactListener: (postId: String, playerId: String) -> Unit,
    reactListListener: (reactions: List<Post.Reaction>) -> Unit,
    saveListener: ((postId: String, playerMessage: String?) -> Unit)?,
    postClickListener: ((postPlayerId: String) -> Unit)? = null,
    canEdit: Boolean = false
): PostViewModel.SimpleViewModel {
    return PostViewModel.SimpleViewModel(
        id = it.id,
        playerId = it.playerId,
        playerAvatar = AndroidAvatar.valueOf(it.playerAvatar.name),
        playerName = it.playerDisplayName,
        playerUsername = it.playerUsername,
        playerLevel = "Level ${it.playerLevel}",
        postedTime = DateUtils.getRelativeTimeSpanString(
            it.createdAt.toEpochMilli(),
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_ALL
        ).toString(),
        message = message,
        messageIcon = messageIcon,
        playerMessage = it.description ?: "",
        reactions = reactions,
        createdAt = it.createdAt.toEpochMilli(),
        post = it,
        reactListener = reactListener,
        reactListListener = reactListListener,
        saveListener = saveListener,
        postClickListener = postClickListener,
        canEdit = canEdit
    )
}

interface ReactionPopupHandler {
    fun containerCoordinates(): IntArray
    fun showReactionPopup(x: Int, y: Float)
    fun hideReactionPopup()
    fun isPopupShown(): Boolean
}