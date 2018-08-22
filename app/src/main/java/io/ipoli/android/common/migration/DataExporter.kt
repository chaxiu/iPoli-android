package io.ipoli.android.common.migration

import android.annotation.SuppressLint
import android.arch.persistence.room.Transaction
import android.content.Context
import android.support.annotation.WorkerThread
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.WriteBatch
import io.ipoli.android.Constants
import io.ipoli.android.MyPoliApp
import io.ipoli.android.challenge.persistence.FirestoreChallengeRepository
import io.ipoli.android.common.ErrorLogger
import io.ipoli.android.common.datetime.milliseconds
import io.ipoli.android.common.di.BackgroundModule
import io.ipoli.android.common.persistence.BaseFirestoreRepository
import io.ipoli.android.dailychallenge.data.persistence.FirestoreDailyChallengeRepository
import io.ipoli.android.habit.persistence.FirestoreHabitRepository
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.persistence.FirestorePlayerRepository
import io.ipoli.android.quest.Entity
import io.ipoli.android.quest.data.persistence.FirestoreQuestRepository
import io.ipoli.android.repeatingquest.persistence.FirestoreRepeatingQuestRepository
import io.ipoli.android.tag.persistence.FirestoreTagRepository
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required

class DataExporter(private val appContext: Context) : Injects<BackgroundModule> {

    private val remoteDatabase by required { remoteDatabase }
    private val sharedPreferences by required { sharedPreferences }
    private val internetConnectionChecker by required { internetConnectionChecker }

    private val playerRepository by required { playerRepository }
    private val tagRepository by required { tagRepository }
    private val habitRepository by required { habitRepository }
    private val dailyChallengeRepository by required { dailyChallengeRepository }
    private val challengeRepository by required { challengeRepository }
    private val repeatingQuestRepository by required { repeatingQuestRepository }
    private val questRepository by required { questRepository }
    private val postRepository by required { postRepository }

    @SuppressLint("ApplySharedPref")
    @WorkerThread
    fun export() {
        inject(MyPoliApp.backgroundModule(appContext))

        if (!internetConnectionChecker.isConnected()) return

        val authUser = FirebaseAuth.getInstance().currentUser
        requireNotNull(authUser) { "DataExporter called without FirebaseAuth user" }

        val syncTime = System.currentTimeMillis()

        exportPlayerData()
        exportCollections()

        sharedPreferences.edit()
            .putLong(Constants.KEY_LAST_SYNC_MILLIS, syncTime)
            .commit()
    }

    @SuppressLint("ApplySharedPref")
    @WorkerThread
    fun exportNewData() {
        inject(MyPoliApp.backgroundModule(appContext))

        if (!internetConnectionChecker.isConnected()) return

        requireNotNull(FirebaseAuth.getInstance().currentUser) { "DataExporter called without FirebaseAuth user" }

        val lastSync = sharedPreferences.getLong(Constants.KEY_LAST_SYNC_MILLIS, 0)

        val lastSyncMillis = lastSync.milliseconds

        val batch = remoteDatabase.batch()
        var count = 0

        val syncTime = System.currentTimeMillis()

        val fp = FirestorePlayerRepository(remoteDatabase)

        fp.addToBatch(playerRepository.findAllForSync(lastSyncMillis), batch)
        count++

        val (b, c) = exportEntities(
            entities = tagRepository.findAllForSync(lastSyncMillis),
            remoteRepo = FirestoreTagRepository(remoteDatabase),
            startBatch = batch,
            startCount = count
        )

        val (b1, c1) = exportEntities(
            entities = habitRepository.findAllForSync(lastSyncMillis),
            remoteRepo = FirestoreHabitRepository(remoteDatabase),
            startBatch = b,
            startCount = c
        )

        val (b2, c2) = exportEntities(
            entities = challengeRepository.findAllForSync(lastSyncMillis),
            remoteRepo = FirestoreChallengeRepository(remoteDatabase),
            startBatch = b1,
            startCount = c1
        )

        val (b3, c3) = exportEntities(
            entities = repeatingQuestRepository.findAllForSync(lastSyncMillis),
            remoteRepo = FirestoreRepeatingQuestRepository(remoteDatabase),
            startBatch = b2,
            startCount = c2
        )

        val (b4, c4) = exportEntities(
            entities = dailyChallengeRepository.findAllForSync(lastSyncMillis),
            remoteRepo = FirestoreDailyChallengeRepository(remoteDatabase),
            startBatch = b3,
            startCount = c3
        )

        val (lb, _) = exportEntities(
            entities = questRepository.findAllForSync(lastSyncMillis),
            remoteRepo = FirestoreQuestRepository(remoteDatabase),
            startBatch = b4,
            startCount = c4
        )

        Tasks.await(lb.commit())

        sharedPreferences.edit()
            .putLong(Constants.KEY_LAST_SYNC_MILLIS, syncTime)
            .commit()

        exportPosts()
    }

    private fun exportPosts() {
        var roomPost = postRepository.findLocal(1).firstOrNull()
        while (roomPost != null) {
            try {
                postRepository.saveToRemoteDatabase(roomPost)
            } catch (e: FirebaseFirestoreException) {
                ErrorLogger.log(e)
                return
            }
            postRepository.purgeLocal(roomPost.id)
            roomPost = postRepository.findLocal(1).firstOrNull()
        }

    }

    private fun <E : Entity> exportEntities(
        entities: List<E>,
        remoteRepo: BaseFirestoreRepository<E, *>,
        startBatch: WriteBatch,
        startCount: Int
    ): Pair<WriteBatch, Int> {
        var batch = startBatch
        var count = startCount
        entities.forEach {
            remoteRepo.addToBatch(it, batch)
            count++
            if (count == 500) {
                batch = commitAndCreateNewBatch(batch)
                count = 0
            }
        }

        return Pair(batch, count)
    }

    private fun commitAndCreateNewBatch(batch: WriteBatch): WriteBatch {
        Tasks.await(batch.commit())
        return remoteDatabase.batch()
    }

    @SuppressLint("ApplySharedPref")
    private fun exportPlayerData() {
        val batch = remoteDatabase.batch()
        val fp = FirestorePlayerRepository(remoteDatabase)
        val localPlayer = playerRepository.find()!!
        val remotePlayerId = FirebaseAuth.getInstance().currentUser!!.uid

        val newPlayer = if (localPlayer.id != remotePlayerId) {
            sharedPreferences
                .edit()
                .putString(Constants.KEY_PLAYER_ID, remotePlayerId)
                .commit()
            localPlayer.copy(id = remotePlayerId)
        } else localPlayer

        fp.addToBatch(newPlayer, batch)
        Tasks.await(batch.commit())

        if (newPlayer.id != localPlayer.id) {
            replacePlayer(newPlayer)
        }
    }

    @Transaction
    private fun replacePlayer(newPlayer: Player) {
        playerRepository.delete()
        playerRepository.save(newPlayer)
    }

    private fun exportCollections() {

        val ft = FirestoreTagRepository(remoteDatabase)
        val fh = FirestoreHabitRepository(remoteDatabase)
        val fc = FirestoreChallengeRepository(remoteDatabase)
        val fr = FirestoreRepeatingQuestRepository(remoteDatabase)

        val batch = remoteDatabase.batch()

        ft.addToBatch(tagRepository.findAll(), batch)
        fh.addToBatch(habitRepository.findAll(), batch)
        fc.addToBatch(challengeRepository.findAll(), batch)
        fr.addToBatch(repeatingQuestRepository.findAll(), batch)

        Tasks.await(batch.commit())

        exportDailyChallengesAndQuests()
    }

    private fun exportDailyChallengesAndQuests() {

        val fd = FirestoreDailyChallengeRepository(remoteDatabase)

        var batch = remoteDatabase.batch()

        var cnt = 0
        dailyChallengeRepository
            .findAll()
            .forEach {

                fd.addToBatch(it, batch)
                cnt++

                if (cnt >= 500) {
                    Tasks.await(batch.commit())
                    cnt = 0
                    batch = remoteDatabase.batch()
                }
            }

        val fq = FirestoreQuestRepository(remoteDatabase)
        questRepository
            .findAll()
            .forEach {
                fq.addToBatch(it, batch)
                cnt++

                if (cnt >= 500) {
                    Tasks.await(batch.commit())
                    cnt = 0
                    batch = remoteDatabase.batch()
                }
            }

        Tasks.await(batch.commit())
    }
}