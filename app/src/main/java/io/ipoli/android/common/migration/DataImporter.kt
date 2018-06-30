package io.ipoli.android.common.migration

import android.annotation.SuppressLint
import android.arch.persistence.room.Transaction
import android.content.Context
import android.support.annotation.WorkerThread
import com.crashlytics.android.Crashlytics
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Source
import io.ipoli.android.BuildConfig
import io.ipoli.android.Constants
import io.ipoli.android.challenge.persistence.FirestoreChallengeRepository
import io.ipoli.android.common.di.Module
import io.ipoli.android.dailychallenge.data.persistence.FirestoreDailyChallengeRepository
import io.ipoli.android.habit.persistence.FirestoreHabitRepository
import io.ipoli.android.myPoliApp
import io.ipoli.android.player.persistence.FirestorePlayerRepository
import io.ipoli.android.quest.data.persistence.FirestoreQuestRepository
import io.ipoli.android.repeatingquest.persistence.FirestoreRepeatingQuestRepository
import io.ipoli.android.tag.persistence.FirestoreTagRepository
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required
import timber.log.Timber

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 06/28/2018.
 */
class DataImporter(private val appContext: Context) : Injects<Module> {

    private val remoteDatabase by required { remoteDatabase }
    private val localDatabase by required { localDatabase }
    private val sharedPreferences by required { sharedPreferences }

    private val playerRepository by required { playerRepository }
    private val tagRepository by required { tagRepository }
    private val habitRepository by required { habitRepository }
    private val dailyChallengeRepository by required { dailyChallengeRepository }
    private val challengeRepository by required { challengeRepository }
    private val repeatingQuestRepository by required { repeatingQuestRepository }
    private val questRepository by required { questRepository }
    private val eventLogger by required { eventLogger }

    @SuppressLint("ApplySharedPref")
    @WorkerThread
    fun import() {
        inject(myPoliApp.module(appContext))

        val authUser = FirebaseAuth.getInstance().currentUser
        requireNotNull(authUser) { "DataImporter called without FirebaseAuth user" }

        sharedPreferences.edit().putBoolean(Constants.KEY_PLAYER_DATA_IMPORTED, false).commit()

        val remotePlayerId = authUser!!.uid

        val remotePlayerDoc = Tasks.await(
            remoteDatabase
                .collection("players")
                .document(remotePlayerId)
                .get(Source.SERVER)
        )

        if (!remotePlayerDoc.exists()) {
            sharedPreferences.edit().putBoolean(Constants.KEY_PLAYER_DATA_IMPORTED, true).commit()
            return
        }

        val remoteSchemaVersion = remotePlayerDoc.data!!["schemaVersion"] as Long

        if (remoteSchemaVersion > Constants.SCHEMA_VERSION) {
            // @TODO require upgrade dialog !!important
        }

        try {
            importData()
        } catch (e: Throwable) {
            if (BuildConfig.DEBUG) {
                Timber.e(e)
            } else {
                Crashlytics.logException(e)
            }
            return
        }

        sharedPreferences.edit()
            .putString(Constants.KEY_PLAYER_ID, remotePlayerId)
            .putBoolean(Constants.KEY_PLAYER_DATA_IMPORTED, true)
            .putInt(Constants.KEY_SCHEMA_VERSION, remoteSchemaVersion.toInt())
            .putLong(Constants.KEY_LAST_SYNC_MILLIS, System.currentTimeMillis())
            .commit()
        eventLogger.setPlayerId(remotePlayerId)
    }

    @Transaction
    private fun importData() {
        localDatabase.clearAllTables()

        val fp = FirestorePlayerRepository(remoteDatabase).find()!!
        playerRepository.save(fp)

        val ft = FirestoreTagRepository(remoteDatabase).findAllNotRemoved()
        tagRepository.save(ft)

        val fh = FirestoreHabitRepository(remoteDatabase).findAllNotRemoved()
        habitRepository.save(fh)

        val fdc = FirestoreDailyChallengeRepository(remoteDatabase).findAllNotRemoved()
        dailyChallengeRepository.save(fdc)

        challengeRepository.save(FirestoreChallengeRepository(remoteDatabase).findAllNotRemoved())

        val rqs = FirestoreRepeatingQuestRepository(remoteDatabase).findAllNotRemoved()
        repeatingQuestRepository.save(rqs)

        val rqIds = rqs.map { it.id }.toSet()
        val qs = FirestoreQuestRepository(remoteDatabase)
            .findAll()
            .filter {
                !it.isRemoved || rqIds.contains(it.repeatingQuestId)
            }
        questRepository.save(qs)
    }
}