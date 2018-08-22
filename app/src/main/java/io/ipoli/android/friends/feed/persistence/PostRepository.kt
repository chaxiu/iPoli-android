package io.ipoli.android.friends.feed.persistence

import android.arch.persistence.room.*
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import io.ipoli.android.achievement.Achievement
import io.ipoli.android.common.ErrorLogger
import io.ipoli.android.common.datetime.days
import io.ipoli.android.common.datetime.instant
import io.ipoli.android.common.datetime.minutes
import io.ipoli.android.common.persistence.RoomEntity
import io.ipoli.android.common.persistence.documents
import io.ipoli.android.common.persistence.getSync
import io.ipoli.android.friends.feed.data.Post
import io.ipoli.android.player.data.Avatar
import io.ipoli.android.player.persistence.model.DbPlayer
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ConflatedChannel
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.annotations.NotNull
import org.threeten.bp.Instant
import java.util.*
import java.util.concurrent.ExecutorService
import android.arch.persistence.room.Query as RoomQuery

interface PostRepository {

    fun save(post: Post)

    fun saveToRemoteDatabase(post: Post)

    fun findForAll(limit: Int, before: Instant = Instant.now()): List<Post>

    fun findForAll(limit: Int): List<Post>

    fun findForPlayer(playerId: String, limit: Int, before: Instant = Instant.now()): List<Post>

    fun findForPlayer(playerId: String, limit: Int): List<Post>

    fun listenForChange(): Channel<Unit>

    fun listenForPlayerChange(playerId: String): Channel<Unit>

    fun react(postPlayerId: String, postId: String, reactionType: Post.ReactionType)

    fun saveDescription(postId: String, description: String?)

    fun findLocal(limit: Int): List<Post>

    fun purgeLocal(id: String)

    fun hasPostForQuest(questId: String): Boolean

    fun hasPostForHabit(habitId: String): Boolean

    fun hasPostedQuests(questIds: List<String>): Map<String, Boolean>
}

class AndroidPostRepository(
    private val remoteDatabase: FirebaseFirestore,
    private val postDao: PostDao,
    private val executorService: ExecutorService
) : PostRepository {

    override fun findForPlayer(playerId: String, limit: Int, before: Instant): List<Post> {
        val dbPlayer = playerRef(playerId)
            .getSync()
            .let { DbPlayer(it.data!!) }

        val dbPosts =
            playerRef(playerId)
                .collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .startAfter(before.toEpochMilli())
                .limit(limit.toLong())
                .documents
                .map { DbPost(it.data!!) }

        return dbPosts.map {
            toEntityObject(it, dbPlayer)
        }
    }

    override fun findForPlayer(playerId: String, limit: Int): List<Post> {

        val dbPlayer = playerRef(playerId)
            .getSync()
            .let { DbPlayer(it.data!!) }

        val dbPosts =
            playerRef(playerId)
                .collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .documents
                .map { DbPost(it.data!!) }

        return dbPosts.map {
            toEntityObject(it, dbPlayer)
        }
    }

    override fun listenForPlayerChange(playerId: String): Channel<Unit> {
        val channel = FirestoreSnapshotChannel<Unit>()
        val snapshotListener = EventListener<QuerySnapshot> { _, exception ->

            if (exception != null) {
                ErrorLogger.log(exception)
                return@EventListener
            }

            channel.offer(Unit)
        }

        val listenerRegistration = playerRef(playerId)
            .collection("posts")
            .addSnapshotListener(executorService, snapshotListener)

        channel.listenerRegistration = listenerRegistration
        return channel
    }

    override fun findForAll(limit: Int): List<Post> {
        val playerId = FirebaseAuth.getInstance().currentUser!!.uid

        val dbRefs = playerRef(playerId)
            .collection("referencePosts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .documents
            .map { DbReferencePost(it.data!!) }

        val coroutineDispatcher = executorService.asCoroutineDispatcher()

        val playerIds = dbRefs.map { it.playerId }.toSet()

        val dbPlayerJobs = playerIds.map {
            async(coroutineDispatcher) {
                it to DbPlayer(playerRef(it).getSync().data!!)
            }
        }

        val dbPostJobs = dbRefs.map {
            async(coroutineDispatcher) {
                playerRef(it.playerId)
                    .collection("posts")
                    .document(it.postId)
                    .getSync()
                    .let { DbPost(it.data!!) }
            }
        }

        return runBlocking(coroutineDispatcher) {

            val dbPlayers = dbPlayerJobs.map {
                it.await()
            }.toMap()

            dbPostJobs.map {
                val dbPost = it.await()
                val pId = dbRefs.first { it.postId == dbPost.id }.playerId
                val dbPlayer = dbPlayers[pId]!!
                toEntityObject(dbPost, dbPlayer)
            }.sortedByDescending { p -> p.createdAt.toEpochMilli() }
        }
    }

    override fun findForAll(limit: Int, before: Instant): List<Post> {
        val playerId = FirebaseAuth.getInstance().currentUser!!.uid

        val dbRefs = playerRef(playerId)
            .collection("referencePosts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .startAfter(before.toEpochMilli())
            .limit(limit.toLong())
            .documents
            .map { DbReferencePost(it.data!!) }

        val coroutineDispatcher = executorService.asCoroutineDispatcher()

        val playerIds = dbRefs.map { it.playerId }.toSet()

        val dbPlayerJobs = playerIds.map {
            async(coroutineDispatcher) {
                it to DbPlayer(playerRef(it).getSync().data!!)
            }
        }

        val dbPostJobs = dbRefs.map {
            async(coroutineDispatcher) {
                playerRef(it.playerId)
                    .collection("posts")
                    .document(it.postId)
                    .getSync()
                    .let { DbPost(it.data!!) }
            }
        }

        return runBlocking(coroutineDispatcher) {

            val dbPlayers = dbPlayerJobs.map {
                it.await()
            }.toMap()

            dbPostJobs.map {
                val dbPost = it.await()
                val pId = dbRefs.first { it.postId == dbPost.id }.playerId
                val dbPlayer = dbPlayers[pId]!!
                toEntityObject(dbPost, dbPlayer)
            }.sortedByDescending { p -> p.createdAt.toEpochMilli() }
        }
    }

    override fun listenForChange(): Channel<Unit> {
        val playerId = FirebaseAuth.getInstance().currentUser!!.uid
        val channel = FirestoreSnapshotChannel<Unit>()
        val snapshotListener = EventListener<QuerySnapshot> { _, exception ->

            if (exception != null) {
                ErrorLogger.log(exception)
                return@EventListener
            }

            channel.offer(Unit)
        }

        val listenerRegistration = playerRef(playerId)
            .collection("referencePosts")
            .addSnapshotListener(executorService, snapshotListener)

        channel.listenerRegistration = listenerRegistration
        return channel
    }

    override fun save(post: Post) {
        try {
            saveToRemoteDatabase(post)
        } catch (e: Throwable) {
            ErrorLogger.log(e)
            saveToLocalDatabase(post)
        }
    }

    private fun saveToLocalDatabase(post: Post): Post {
        val newPost = post.copy(id = UUID.randomUUID().toString())
        val newRp = createRoomPost(newPost)
        postDao.save(newRp)
        return newPost
    }

    private fun createRoomPost(
        post: Post
    ): RoomPost {
        val rp = RoomPost(
            id = post.id,
            type = post.data.dbType.name,
            playerLevel = post.playerLevel.toLong(),
            description = post.description,
            reactions = post.reactions.map {
                it.playerId to mutableMapOf(it.reactionType.name to it.createdAt.toEpochMilli())
            }.toMap().toMutableMap(),
            questId = null,
            questName = null,
            habitId = null,
            habitName = null,
            challengeId = null,
            challengeName = null,
            streak = null,
            bestStreak = null,
            level = null,
            isGood = null,
            duration = null,
            pomodoroCount = null,
            achievement = null,
            createdAt = post.createdAt.toEpochMilli()
        )

        val d = post.data
        return when (d) {
            is Post.Data.DailyChallengeCompleted ->
                rp.copy(
                    streak = d.streak.toLong(),
                    bestStreak = d.bestStreak.toLong()
                )

            is Post.Data.DailyChallengeFailed ->
                rp

            is Post.Data.LevelUp ->
                rp.copy(level = d.level.toLong())

            is Post.Data.AchievementUnlocked ->
                rp.copy(achievement = d.achievement.name)

            is Post.Data.QuestShared ->
                rp.copy(
                    questId = d.questId,
                    questName = d.questName,
                    duration = d.durationTracked.longValue
                )

            is Post.Data.QuestWithPomodoroShared -> {
                rp.copy(
                    questId = d.questId,
                    questName = d.questName,
                    pomodoroCount = d.pomodoroCount.toLong()
                )
            }

            is Post.Data.ChallengeShared -> {
                rp.copy(
                    challengeId = d.challengeId,
                    challengeName = d.name
                )
            }

            is Post.Data.ChallengeCompleted -> {
                rp.copy(
                    challengeId = d.challengeId,
                    challengeName = d.name,
                    duration = d.duration.longValue
                )
            }

            is Post.Data.QuestFromChallengeCompleted -> {
                rp.copy(
                    questId = d.questId,
                    questName = d.questName,
                    challengeId = d.challengeId,
                    challengeName = d.challengeName,
                    duration = d.durationTracked.longValue
                )
            }

            is Post.Data.QuestWithPomodoroFromChallengeCompleted -> {
                rp.copy(
                    questId = d.questId,
                    questName = d.questName,
                    challengeId = d.challengeId,
                    challengeName = d.challengeName,
                    pomodoroCount = d.pomodoroCount.toLong()
                )
            }

            is Post.Data.QuestFromChallengeFailed -> {
                rp.copy(
                    questId = d.questId,
                    questName = d.questName,
                    challengeId = d.challengeId,
                    challengeName = d.challengeName
                )
            }

            is Post.Data.HabitFromChallengeCompleted -> {
                rp.copy(
                    habitId = d.habitId,
                    habitName = d.habitName,
                    challengeId = d.challengeId,
                    challengeName = d.challengeName,
                    isGood = d.isGood,
                    streak = d.streak.toLong(),
                    bestStreak = d.bestStreak.toLong()
                )
            }

            is Post.Data.HabitFromChallengeFailed -> {
                rp.copy(
                    habitId = d.habitId,
                    habitName = d.habitName,
                    challengeId = d.challengeId,
                    challengeName = d.challengeName,
                    isGood = d.isGood
                )
            }
        }
    }

    private val Post.Data.dbType: DbPost.PostType
        get() = when (this) {
            is Post.Data.DailyChallengeCompleted ->
                DbPost.PostType.DAILY_CHALLENGE_COMPLETED

            is Post.Data.DailyChallengeFailed ->
                DbPost.PostType.DAILY_CHALLENGE_FAILED

            is Post.Data.LevelUp ->
                DbPost.PostType.LEVEL_UP

            is Post.Data.AchievementUnlocked ->
                DbPost.PostType.ACHIEVEMENT_UNLOCKED

            is Post.Data.QuestShared ->
                DbPost.PostType.QUEST_SHARED

            is Post.Data.QuestWithPomodoroShared ->
                DbPost.PostType.QUEST_WITH_POMODORO_SHARED

            is Post.Data.ChallengeShared ->
                DbPost.PostType.CHALLENGE_SHARED

            is Post.Data.ChallengeCompleted ->
                DbPost.PostType.CHALLENGE_COMPLETED

            is Post.Data.QuestFromChallengeCompleted ->
                DbPost.PostType.QUEST_FROM_CHALLENGE_COMPLETED

            is Post.Data.QuestWithPomodoroFromChallengeCompleted ->
                DbPost.PostType.QUEST_WITH_POMODORO_FROM_CHALLENGE_COMPLETED

            is Post.Data.QuestFromChallengeFailed ->
                DbPost.PostType.QUEST_FROM_CHALLENGE_FAILED

            is Post.Data.HabitFromChallengeCompleted ->
                DbPost.PostType.HABIT_FROM_CHALLENGE_COMPLETED

            is Post.Data.HabitFromChallengeFailed ->
                DbPost.PostType.HABIT_FROM_CHALLENGE_FAILED
        }

    override fun saveToRemoteDatabase(post: Post) {

        val data = post.data

        val shouldNotSave = when (data) {
            is Post.Data.QuestShared ->
                hasPostForQuest(data.questId)

            is Post.Data.QuestWithPomodoroShared ->
                hasPostForQuest(data.questId)

            is Post.Data.QuestFromChallengeCompleted ->
                hasPostForQuest(data.questId)

            is Post.Data.QuestWithPomodoroFromChallengeCompleted ->
                hasPostForQuest(data.questId)

            is Post.Data.HabitFromChallengeCompleted ->
                hasPostForHabit(data.habitId)

            is Post.Data.HabitFromChallengeFailed ->
                hasPostForHabit(data.habitId)

            else -> false
        }

        if (shouldNotSave) {
            return
        }

        val playerId = FirebaseAuth.getInstance().currentUser!!.uid

        val playerRef = playerRef(playerId)

        playerRef.get(Source.SERVER)

        val friendIds = playerRef
            .collection("friends")
            .documents
            .map { it.id }

        val postsRef =
            playerRef
                .collection("posts")

        val postRef = postsRef.document()

        val newEntity = post.copy(id = postRef.id)

        var batch = remoteDatabase.batch()
        var cnt = 0
        batch.set(postRef, toDatabaseObject(newEntity).map)
        cnt++

        (friendIds + playerId).forEach {
            val refPost = playerRef(it)
                .collection("referencePosts")
                .document(newEntity.id)
            batch.set(refPost, toDatabaseReferencePost(playerId, newEntity).map)
            cnt++
            if (cnt == 500) {
                Tasks.await(batch.commit())
                batch = remoteDatabase.batch()
                cnt = 0
            }
        }

        Tasks.await(batch.commit())
    }

    override fun react(postPlayerId: String, postId: String, reactionType: Post.ReactionType) {
        val playerId = FirebaseAuth.getInstance().currentUser!!.uid
        val playerRef = playerRef(postPlayerId)

        val updateTime = System.currentTimeMillis()

        var batch = remoteDatabase.batch()

        batch.update(
            playerRef
                .collection("posts")
                .document(postId),
            mapOf(
                "reactions.$playerId" to mapOf(reactionType.name to Instant.now().toEpochMilli()),
                "updatedAt" to updateTime
            )
        )

        var cnt = 1

        val friendIds = playerRef
            .collection("friends")
            .documents
            .map { it.id }

        (friendIds + playerId).toSet().forEach {
            val refPost = playerRef(it)
                .collection("referencePosts")
                .document(postId)
            batch.update(refPost, mapOf("updatedAt" to updateTime))

            cnt++
            if (cnt == 500) {
                Tasks.await(batch.commit())
                batch = remoteDatabase.batch()
                cnt = 0
            }
        }

        Tasks.await(batch.commit())
    }

    override fun saveDescription(postId: String, description: String?) {
        val playerId = FirebaseAuth.getInstance().currentUser!!.uid
        val playerRef = playerRef(playerId)

        val updateTime = System.currentTimeMillis()

        var batch = remoteDatabase.batch()

        batch.update(
            playerRef
                .collection("posts")
                .document(postId),
            mapOf(
                "description" to description,
                "updatedAt" to updateTime
            )
        )

        var cnt = 1

        val friendIds = playerRef
            .collection("friends")
            .documents
            .map { it.id }

        (friendIds + playerId).forEach {
            val refPost = playerRef(it)
                .collection("referencePosts")
                .document(postId)
            batch.update(refPost, mapOf("updatedAt" to updateTime))

            cnt++
            if (cnt == 500) {
                Tasks.await(batch.commit())
                batch = remoteDatabase.batch()
                cnt = 0
            }
        }

        Tasks.await(batch.commit())
    }

    override fun findLocal(limit: Int): List<Post> {
        val playerId = FirebaseAuth.getInstance().currentUser!!.uid
        return postDao.findAll(limit).map { p ->
            val dbPost = DbPost().apply {
                id = p.id
                type = p.type
                playerLevel = p.playerLevel
                description = p.description
                reactions = p.reactions
                createdAt = p.createdAt
                updatedAt = p.createdAt
                questId = p.questId
                questName = p.questName
                habitId = p.habitId
                habitName = p.habitName
                challengeId = p.challengeId
                challengeName = p.challengeName
                level = p.level
                achievement = p.achievement
                duration = p.duration
                pomodoroCount = p.pomodoroCount
                isGood = p.isGood
                streak = p.streak
                bestStreak = p.bestStreak
            }
            val dbPlayer = DbPlayer().apply {
                id = playerId
                avatar = Avatar.AVATAR_01.name
                displayName = ""
                username = ""
            }
            toEntityObject(dbPost, dbPlayer)
        }
    }

    override fun purgeLocal(id: String) {
        postDao.purge(id)
    }

    override fun hasPostForQuest(questId: String): Boolean {
        val playerId = FirebaseAuth.getInstance().currentUser!!.uid

        return playerRef(playerId)
            .collection("posts")
            .whereEqualTo("questId", questId)
            .documents
            .isNotEmpty()
    }

    override fun hasPostForHabit(habitId: String): Boolean {
        val playerId = FirebaseAuth.getInstance().currentUser!!.uid

        return playerRef(playerId)
            .collection("posts")
            .whereEqualTo("habitId", habitId)
            .documents
            .isNotEmpty()
    }

    override fun hasPostedQuests(questIds: List<String>): Map<String, Boolean> {
        val result = mutableMapOf<String, Boolean>()

        questIds.forEach {
            result[it] = false
        }

        val playerId = FirebaseAuth.getInstance().currentUser!!.uid

        val postedLocalQuestIds = postDao.findPostedQuestIds(questIds).toSet()

        postedLocalQuestIds.forEach {
            result[it] = true
        }

        val unknownStatusQuestIds = questIds.toSet() - postedLocalQuestIds

        val coroutineDispatcher = executorService.asCoroutineDispatcher()

        val postJobs = unknownStatusQuestIds.map {
            async(coroutineDispatcher) {
                playerRef(playerId)
                    .collection("posts")
                    .whereEqualTo("questId", it)
                    .limit(1)
                    .documents
            }
        }


        return runBlocking(coroutineDispatcher) {
            postJobs.forEach {
                val docs = it.await()
                if (docs.isNotEmpty()) {
                    result[docs.first().getString("questId")!!] = true
                }
            }

            result
        }
    }

    class FirestoreSnapshotChannel<D> :
        ConflatedChannel<D>() {

        var listenerRegistration: ListenerRegistration? = null

        override fun afterClose(cause: Throwable?) {
            listenerRegistration?.remove()
        }
    }

    private fun playerRef(playerId: String) =
        remoteDatabase
            .collection("players")
            .document(playerId)

    private fun toDatabaseObject(post: Post) =
        DbPost().apply {
            id = post.id
            playerLevel = post.playerLevel.toLong()
            description = post.description
            reactions = post.reactions.map {
                it.playerId to mutableMapOf(it.reactionType.name to it.createdAt.toEpochMilli())
            }.toMap().toMutableMap()
            createdAt = post.createdAt.toEpochMilli()
            updatedAt = post.updatedAt.toEpochMilli()

            with(post) {
                when (data) {

                    is Post.Data.DailyChallengeCompleted -> {
                        type = DbPost.PostType.DAILY_CHALLENGE_COMPLETED.name
                        streak = data.streak.toLong()
                        bestStreak = data.bestStreak.toLong()
                    }

                    is Post.Data.DailyChallengeFailed -> {
                        type = DbPost.PostType.DAILY_CHALLENGE_FAILED.name
                    }

                    is Post.Data.LevelUp -> {
                        type = DbPost.PostType.LEVEL_UP.name
                        level = data.level.toLong()
                    }

                    is Post.Data.AchievementUnlocked -> {
                        type = DbPost.PostType.ACHIEVEMENT_UNLOCKED.name
                        achievement = data.achievement.name
                    }

                    is Post.Data.QuestShared -> {
                        type = DbPost.PostType.QUEST_SHARED.name
                        questId = data.questId
                        questName = data.questName
                        duration = data.durationTracked.longValue
                    }

                    is Post.Data.QuestWithPomodoroShared -> {
                        type = DbPost.PostType.QUEST_WITH_POMODORO_SHARED.name
                        questId = data.questId
                        questName = data.questName
                        pomodoroCount = data.pomodoroCount.toLong()
                    }

                    is Post.Data.ChallengeShared -> {
                        type = DbPost.PostType.CHALLENGE_SHARED.name
                        challengeId = data.challengeId
                        challengeName = data.name
                    }

                    is Post.Data.ChallengeCompleted -> {
                        type = DbPost.PostType.CHALLENGE_COMPLETED.name
                        challengeId = data.challengeId
                        challengeName = data.name
                        duration = data.duration.longValue
                    }

                    is Post.Data.QuestFromChallengeCompleted -> {
                        type = DbPost.PostType.QUEST_FROM_CHALLENGE_COMPLETED.name
                        questId = data.questId
                        questName = data.questName
                        challengeId = data.challengeId
                        challengeName = data.challengeName
                        duration = data.durationTracked.longValue
                    }

                    is Post.Data.QuestWithPomodoroFromChallengeCompleted -> {
                        type = DbPost.PostType.QUEST_WITH_POMODORO_FROM_CHALLENGE_COMPLETED.name
                        questId = data.questId
                        questName = data.questName
                        challengeId = data.challengeId
                        challengeName = data.challengeName
                        pomodoroCount = data.pomodoroCount.toLong()
                    }

                    is Post.Data.QuestFromChallengeFailed -> {
                        type = DbPost.PostType.QUEST_FROM_CHALLENGE_FAILED.name
                        questId = data.questId
                        questName = data.questName
                        challengeId = data.challengeId
                        challengeName = data.challengeName
                    }

                    is Post.Data.HabitFromChallengeCompleted -> {
                        type = DbPost.PostType.HABIT_FROM_CHALLENGE_COMPLETED.name
                        habitId = data.habitId
                        habitName = data.habitName
                        challengeId = data.challengeId
                        challengeName = data.challengeName
                        isGood = data.isGood
                        streak = data.streak.toLong()
                        bestStreak = data.bestStreak.toLong()
                    }

                    is Post.Data.HabitFromChallengeFailed -> {
                        type = DbPost.PostType.HABIT_FROM_CHALLENGE_FAILED.name
                        habitId = data.habitId
                        habitName = data.habitName
                        challengeId = data.challengeId
                        challengeName = data.challengeName
                        isGood = data.isGood
                    }
                }
            }
        }

    private fun toDatabaseReferencePost(playerId: String, post: Post) =
        DbReferencePost().apply {
            this.playerId = playerId
            postId = post.id
            createdAt = post.createdAt.toEpochMilli()
            updatedAt = post.updatedAt.toEpochMilli()
        }

    private fun toEntityObject(dbPost: DbPost, dbPlayer: DbPlayer) =
        Post(
            id = dbPost.id,
            playerId = dbPlayer.id,
            playerAvatar = Avatar.valueOf(dbPlayer.avatar),
            playerDisplayName = dbPlayer.displayName ?: "Unknown Hero",
            playerUsername = dbPlayer.username!!,
            playerLevel = dbPost.playerLevel.toInt(),
            data = createData(dbPost),
            reactions = dbPost.reactions.map {
                @Suppress("UNCHECKED_CAST")
                val v = (it.value as MutableMap<String, Long>).entries.first()
                Post.Reaction(
                    it.key,
                    Post.ReactionType.valueOf(v.key),
                    v.value.instant
                )
            },
            description = dbPost.description,
            createdAt = dbPost.createdAt.instant,
            updatedAt = dbPost.updatedAt.instant
        )

    private fun createData(dbPost: DbPost) =
        with(dbPost) {
            when (DbPost.PostType.valueOf(type)) {

                DbPost.PostType.DAILY_CHALLENGE_COMPLETED ->
                    Post.Data.DailyChallengeCompleted(streak!!.toInt(), bestStreak!!.toInt())

                DbPost.PostType.DAILY_CHALLENGE_FAILED ->
                    Post.Data.DailyChallengeFailed

                DbPost.PostType.ACHIEVEMENT_UNLOCKED ->
                    Post.Data.AchievementUnlocked(Achievement.valueOf(achievement!!))

                DbPost.PostType.LEVEL_UP ->
                    Post.Data.LevelUp(level!!.toInt())

                DbPost.PostType.QUEST_SHARED ->
                    Post.Data.QuestShared(questId!!, questName!!, duration!!.minutes)

                DbPost.PostType.QUEST_WITH_POMODORO_SHARED ->
                    Post.Data.QuestWithPomodoroShared(
                        questId!!,
                        questName!!,
                        pomodoroCount!!.toInt()
                    )

                DbPost.PostType.CHALLENGE_SHARED ->
                    Post.Data.ChallengeShared(challengeId!!, challengeName!!)

                DbPost.PostType.CHALLENGE_COMPLETED ->
                    Post.Data.ChallengeCompleted(challengeId!!, challengeName!!, duration!!.days)

                DbPost.PostType.QUEST_FROM_CHALLENGE_COMPLETED ->
                    Post.Data.QuestFromChallengeCompleted(
                        questId!!,
                        challengeId!!,
                        questName!!,
                        challengeName!!,
                        duration!!.minutes
                    )

                DbPost.PostType.QUEST_WITH_POMODORO_FROM_CHALLENGE_COMPLETED ->
                    Post.Data.QuestWithPomodoroFromChallengeCompleted(
                        questId!!,
                        challengeId!!,
                        questName!!,
                        challengeName!!,
                        pomodoroCount!!.toInt()
                    )

                DbPost.PostType.QUEST_FROM_CHALLENGE_FAILED ->
                    Post.Data.QuestFromChallengeFailed(
                        questId!!,
                        challengeId!!,
                        questName!!,
                        challengeName!!
                    )

                DbPost.PostType.HABIT_FROM_CHALLENGE_COMPLETED ->
                    Post.Data.HabitFromChallengeCompleted(
                        habitId!!,
                        challengeId!!,
                        habitName!!,
                        challengeName!!,
                        isGood!!,
                        streak!!.toInt(),
                        bestStreak!!.toInt()
                    )

                DbPost.PostType.HABIT_FROM_CHALLENGE_FAILED ->
                    Post.Data.HabitFromChallengeFailed(
                        habitId!!,
                        challengeId!!,
                        habitName!!,
                        challengeName!!,
                        isGood!!
                    )
            }
        }
}

class DbPost(val map: MutableMap<String, Any?> = mutableMapOf()) {
    var id: String by map
    var type: String by map
    var playerLevel: Long by map
    var description: String? by map
    var questId: String? by map
    var questName: String? by map
    var habitId: String? by map
    var habitName: String? by map
    var challengeId: String? by map
    var challengeName: String? by map
    var streak: Long? by map
    var bestStreak: Long? by map
    var level: Long? by map
    var isGood: Boolean? by map
    var duration: Long? by map
    var pomodoroCount: Long? by map
    var achievement: String? by map
    var reactions: MutableMap<String, Any?> by map
    var createdAt: Long by map
    var updatedAt: Long by map

    enum class PostType {
        DAILY_CHALLENGE_COMPLETED,
        DAILY_CHALLENGE_FAILED,
        ACHIEVEMENT_UNLOCKED,
        LEVEL_UP,
        QUEST_SHARED,
        QUEST_WITH_POMODORO_SHARED,
        CHALLENGE_SHARED,
        CHALLENGE_COMPLETED,
        QUEST_FROM_CHALLENGE_COMPLETED,
        QUEST_WITH_POMODORO_FROM_CHALLENGE_COMPLETED,
        QUEST_FROM_CHALLENGE_FAILED,
        HABIT_FROM_CHALLENGE_COMPLETED,
        HABIT_FROM_CHALLENGE_FAILED,
    }
}

class DbReferencePost(val map: MutableMap<String, Any?> = mutableMapOf()) {
    var playerId: String by map
    var postId: String by map
    var createdAt: Long by map
    var updatedAt: Long by map
}

@Dao
interface PostDao {

    @Insert(onConflict = OnConflictStrategy.FAIL)
    fun save(entity: RoomPost)

    @RoomQuery("DELETE FROM posts WHERE id = :id")
    fun purge(id: String)

    @RoomQuery("SELECT * FROM posts LIMIT :limit")
    fun findAll(limit: Int): List<RoomPost>

    @RoomQuery("SELECT DISTINCT(questId) FROM posts WHERE questId in (:questIds)")
    fun findPostedQuestIds(questIds: List<String>): List<String>
}

@Entity(
    tableName = "posts",
    indices = [
        Index("questId")
    ]
)
data class RoomPost(
    @NotNull
    @PrimaryKey(autoGenerate = false)
    override val id: String,
    val type: String,
    val playerLevel: Long,
    val description: String?,
    val questId: String?,
    val questName: String?,
    val habitId: String?,
    val habitName: String?,
    val challengeId: String?,
    val challengeName: String?,
    val streak: Long?,
    val bestStreak: Long?,
    val level: Long?,
    val isGood: Boolean?,
    val duration: Long?,
    val pomodoroCount: Long?,
    val achievement: String?,
    val reactions: MutableMap<String, Any?>,
    val createdAt: Long
) : RoomEntity