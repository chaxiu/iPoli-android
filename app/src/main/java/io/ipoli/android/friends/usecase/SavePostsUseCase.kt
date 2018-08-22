package io.ipoli.android.friends.usecase

import io.ipoli.android.achievement.Achievement
import io.ipoli.android.challenge.entity.Challenge
import io.ipoli.android.challenge.entity.SharingPreference
import io.ipoli.android.challenge.persistence.ChallengeRepository
import io.ipoli.android.common.UseCase
import io.ipoli.android.common.datetime.days
import io.ipoli.android.common.datetime.daysBetween
import io.ipoli.android.common.datetime.minutes
import io.ipoli.android.friends.feed.data.Post
import io.ipoli.android.friends.feed.persistence.PostRepository
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.persistence.PlayerRepository
import io.ipoli.android.quest.Quest

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 07/16/2018.
 */
open class SavePostsUseCase(
    private val postRepository: PostRepository,
    private val playerRepository: PlayerRepository,
    private val challengeRepository: ChallengeRepository
) : UseCase<SavePostsUseCase.Params, Unit> {

    override fun execute(parameters: Params) {

        val player = parameters.player ?: playerRepository.find()!!
        if (!player.isLoggedIn()) {
            return
        }

        when (parameters) {
            is Params.LevelUp -> {
                if (parameters.newLevel % 5 == 0) {
                    postRepository.save(
                        Post(
                            playerId = player.id,
                            playerAvatar = player.avatar,
                            playerDisplayName = player.displayName ?: "Unknown Hero",
                            playerUsername = player.username!!,
                            playerLevel = parameters.newLevel,
                            data = Post.Data.LevelUp(parameters.newLevel),
                            description = null,
                            reactions = emptyList()
                        )
                    )
                }
            }

            is Params.DailyChallengeComplete -> {
                val stats = player.statistics
                savePost(
                    player = player,
                    data = Post.Data.DailyChallengeCompleted(
                        streak = stats.dailyChallengeCompleteStreak.count.toInt(),
                        bestStreak = stats.dailyChallengeBestStreak.toInt()
                    )
                )
            }

            is Params.DailyChallengeFailed -> {
                savePost(
                    player = player,
                    data = Post.Data.DailyChallengeFailed
                )
            }

            is Params.AchievementUnlocked -> {
                savePost(
                    player = player,
                    data = Post.Data.AchievementUnlocked(parameters.achievement)
                )
            }

            is Params.QuestsComplete -> {
                parameters.quests.forEach {
                    savePost(
                        player = player,
                        data = if (it.hasPomodoroTimer) {
                            Post.Data.QuestWithPomodoroShared(it.id, it.name, it.totalPomodoros!!)
                        } else {
                            Post.Data.QuestShared(
                                it.id,
                                it.name,
                                if (it.hasTimer) it.actualDuration.asMinutes else 0.minutes
                            )
                        }
                    )
                }
            }

            is Params.QuestFromChallengeComplete -> {
                val q = parameters.quest
                val challenge = parameters.challenge
                savePost(
                    player = player, data = if (q.hasPomodoroTimer) {
                        Post.Data.QuestWithPomodoroFromChallengeCompleted(
                            questId = q.id,
                            challengeId = challenge.id,
                            questName = q.name,
                            challengeName = challenge.name,
                            pomodoroCount = q.totalPomodoros!!
                        )
                    } else {
                        Post.Data.QuestFromChallengeCompleted(
                            questId = q.id,
                            challengeId = challenge.id,
                            questName = q.name,
                            challengeName = challenge.name,
                            durationTracked = if (q.hasTimer) q.actualDuration.asMinutes else 0.minutes
                        )
                    }
                )
            }

            is Params.ChallengesShared -> {
                challengeRepository.save(parameters.challenges.map { it.copy(sharingPreference = SharingPreference.FRIENDS) })
                parameters.challenges.forEach {

                    savePost(
                        player = player,
                        data = Post.Data.ChallengeShared(it.id, it.name)
                    )
                }
            }

            is Params.ChallengeComplete -> {
                val c = parameters.challenge
                savePost(
                    player = player,
                    data = Post.Data.ChallengeCompleted(
                        c.id,
                        c.name,
                        c.startDate.daysBetween(c.completedAtDate!!).days
                    )
                )
            }

        }
    }

    private fun savePost(player: Player, data: Post.Data) {
        postRepository.save(
            Post(
                playerId = player.id,
                playerAvatar = player.avatar,
                playerDisplayName = player.displayName ?: "Unknown Hero",
                playerUsername = player.username!!,
                playerLevel = player.level,
                data = data,
                description = null,
                reactions = emptyList()
            )
        )
    }

    sealed class Params {
        abstract val player: Player?

        data class LevelUp(val newLevel: Int, override val player: Player?) : Params()

        data class DailyChallengeComplete(override val player: Player? = null) : Params()

        data class DailyChallengeFailed(override val player: Player? = null) : Params()

        data class AchievementUnlocked(val achievement: Achievement, override val player: Player?) :
            Params()

        data class QuestsComplete(val quests: List<Quest>, override val player: Player?) : Params()

        data class QuestFromChallengeComplete(
            val quest: Quest,
            val challenge: Challenge,
            override val player: Player?
        ) : Params()

        data class ChallengesShared(val challenges: List<Challenge>, override val player: Player?) :
            Params()

        data class ChallengeComplete(
            val challenge: Challenge,
            override val player: Player? = null
        ) : Params()
    }
}