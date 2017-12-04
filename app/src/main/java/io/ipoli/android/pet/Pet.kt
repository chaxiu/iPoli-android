package io.ipoli.android.pet

import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import io.ipoli.android.Constants
import io.ipoli.android.R
import io.ipoli.android.pet.PetMood.*
import io.ipoli.android.quest.Quest
import java.lang.Math.abs

/**
 * Created by Polina Zhelyazkova <polina@ipoli.io>
 * on 11/27/17.
 */
data class Pet(
    val name: String,
    val avatar: PetAvatar,
    val moodPoints: Int = Constants.DEFAULT_PET_HP,
    val healthPoints: Int = Constants.DEFAULT_PET_HP,
    val mood: PetMood = moodFor(healthPoints),
    val experienceBonus: Float = bonusFor(mood, MAX_XP_BONUS),
    val coinBonus: Float = bonusFor(mood, MAX_COIN_BONUS),
    val unlockChanceBonus: Float = bonusFor(mood, MAX_UNLOCK_CHANCE_BONUS)
) {

    val isDead = healthPoints == 0

    fun updateHealthAndMoodPoints(healthPoints: Int, moodPoints: Int): Pet {

        require(!isDead)

        val newHealthPoints =
            if (healthPoints >= 0) addHealthPoints(healthPoints)
            else removeHealthPoints(abs(healthPoints))

        val newMoodPoints =
            if (moodPoints >= 0) addMoodPoints(newHealthPoints, moodPoints)
            else removeMoodPoints(this.healthPoints, newHealthPoints, abs(moodPoints))

        val newMood = moodFor(newMoodPoints)

        return copy(
            healthPoints = newHealthPoints,
            moodPoints = newMoodPoints,
            mood = newMood,
            coinBonus = bonusFor(newMood, MAX_COIN_BONUS),
            experienceBonus = bonusFor(newMood, MAX_XP_BONUS),
            unlockChanceBonus = bonusFor(newMood, MAX_UNLOCK_CHANCE_BONUS)
        )
    }

    fun rewardFor(quest: Quest): Pet {

        val rewardHP = healthPointsForXP(quest.experience!!)
        val rewardMP = moodPointsForXP(quest.experience)
        return addHealthAndMoodPoints(rewardHP, rewardMP)
    }

    fun addHealthAndMoodPoints(healthPoints: Int, moodPoints: Int): Pet {
        require(!isDead)
        require(healthPoints >= 0)
        require(moodPoints >= 0)

        val newHealthPoints = addHealthPoints(healthPoints)
        val newMoodPoints = addMoodPoints(newHealthPoints, moodPoints)
        val newMood = moodFor(newMoodPoints)

        return copy(
            healthPoints = newHealthPoints,
            moodPoints = newMoodPoints,
            mood = newMood,
            coinBonus = bonusFor(newMood, MAX_COIN_BONUS),
            experienceBonus = bonusFor(newMood, MAX_XP_BONUS),
            unlockChanceBonus = bonusFor(newMood, MAX_UNLOCK_CHANCE_BONUS)
        )
    }

    private fun addHealthPoints(rewardHP: Int) = Math.min(this.healthPoints + rewardHP, MAX_HP)

    private fun addMoodPoints(newHealthPoints: Int, rewardMoodPoints: Int) =
        if (newHealthPoints <= SICK_CUTOFF) {
            Math.min(moodPoints, GOOD_MIN_MOOD_POINTS - 1)
        } else {
            val moodBonusMultiplier = if (newHealthPoints >= HEALTHY_CUTOFF) 2 else 1
            Math.min(moodPoints + rewardMoodPoints * moodBonusMultiplier, MAX_MP)
        }

    fun removeRewardFor(quest: Quest): Pet {
        val rewardHP = healthPointsForXP(quest.experience!!)
        val rewardMP = moodPointsForXP(quest.experience)
        return removeHealthAndMoodPoints(rewardHP, rewardMP)
    }

    fun removeHealthAndMoodPoints(healthPoints: Int, moodPoints: Int): Pet {
        require(!isDead)
        require(healthPoints >= 0)
        require(moodPoints >= 0)

        val newHealthPoints = removeHealthPoints(healthPoints)
        val newMoodPoints = removeMoodPoints(this.healthPoints, newHealthPoints, moodPoints)
        val newMood = moodFor(newMoodPoints)

        return copy(
            healthPoints = newHealthPoints,
            moodPoints = newMoodPoints,
            mood = newMood,
            coinBonus = bonusFor(newMood, MAX_COIN_BONUS),
            experienceBonus = bonusFor(newMood, MAX_XP_BONUS),
            unlockChanceBonus = bonusFor(newMood, MAX_UNLOCK_CHANCE_BONUS)
        )
    }

    private fun removeMoodPoints(oldHealthPoints: Int, newHealthPoints: Int, rewardMoodPoints: Int): Int {
        if (newHealthPoints == 0) {
            return 0
        }
        val notHealthyAnymore = oldHealthPoints >= HEALTHY_CUTOFF && newHealthPoints < HEALTHY_CUTOFF
        val reduceMultiplier = if (notHealthyAnymore) 2 else 1
        return Math.max(moodPoints - rewardMoodPoints * reduceMultiplier, 0)
    }

    private fun removeHealthPoints(rewardHP: Int) = Math.max(this.healthPoints - rewardHP, 0)

    private fun healthPointsForXP(experience: Int) =
        Math.floor(experience / Constants.XP_TO_PET_HP_RATIO).toInt()

    private fun moodPointsForXP(experience: Int) =
        Math.floor(experience / Constants.XP_TO_PET_MOOD_RATIO).toInt()

    companion object {

        const val MAX_HP = 100
        const val MAX_MP = 100
        const val SICK_CUTOFF = 0.2 * MAX_HP
        const val HEALTHY_CUTOFF = 0.9 * MAX_HP
        const val AWESOME_MIN_MOOD_POINTS = 90
        const val HAPPY_MIN_MOOD_POINTS = 60
        const val GOOD_MIN_MOOD_POINTS = 35

        const val MAX_XP_BONUS = 20f
        const val MAX_COIN_BONUS = 18f
        const val MAX_UNLOCK_CHANCE_BONUS = 16f

        private fun bonusFor(mood: PetMood, maxBonus: Float): Float {
            val percentage = when (mood) {
                AWESOME -> 1.0f
                HAPPY -> 0.5f
                GOOD -> 0.25f
                SAD -> 0.1f
            }
            return maxBonus * percentage
        }

        private fun moodFor(moodPoints: Int) =
            when {
                moodPoints >= AWESOME_MIN_MOOD_POINTS -> AWESOME
                moodPoints >= HAPPY_MIN_MOOD_POINTS -> HAPPY
                moodPoints >= GOOD_MIN_MOOD_POINTS -> GOOD
                else -> SAD
            }
    }
}

enum class PetMood {
    SAD, GOOD, HAPPY, AWESOME
}

enum class PetAvatar(val price: Int, val feedingCategory: FeedingCategory) {
    SEAL(600, FeedingCategory.CARNIVOROUS),
    DONKEY(500, FeedingCategory.HERBIVOROUS),
    ELEPHANT(500, FeedingCategory.HERBIVOROUS),
    BEAVER(500, FeedingCategory.HERBIVOROUS),
    CHICKEN(700, FeedingCategory.OMNIVOROUS),
    BEAR(500, FeedingCategory.OMNIVOROUS),
    LION(500, FeedingCategory.CARNIVOROUS),
    CAT(500, FeedingCategory.CARNIVOROUS),
    MONKEY(500, FeedingCategory.OMNIVOROUS),
    DUCK(500, FeedingCategory.OMNIVOROUS),
    PIG(500, FeedingCategory.OMNIVOROUS),
    ZEBRA(500, FeedingCategory.HERBIVOROUS);

    enum class FeedingCategory { OMNIVOROUS, CARNIVOROUS, HERBIVOROUS }
}

enum class AndroidPetAvatar(
    @StringRes val petName: Int,
    @StringRes val description: Int,
    @DrawableRes val image: Int,
    @DrawableRes val headPicture: Int,
    @DrawableRes val deadStateImage: Int,
    val moodImage: Map<PetMood, Int>) {

    SEAL(R.string.pet_seal, R.string.pet_seal_description, R.drawable.pet_1, R.drawable.pet_1_head, R.drawable.pet_1_dead,
        mapOf(
            SAD to R.drawable.pet_1_sad,
            GOOD to R.drawable.pet_1_good,
            HAPPY to R.drawable.pet_1_happy,
            AWESOME to R.drawable.pet_1_awesome
        )),
    DONKEY(R.string.pet_donkey, R.string.pet_seal_description, R.drawable.pet_2, R.drawable.pet_2_head, R.drawable.pet_2_dead,
        mapOf(
            SAD to R.drawable.pet_2_sad,
            GOOD to R.drawable.pet_2_good,
            HAPPY to R.drawable.pet_2_happy,
            AWESOME to R.drawable.pet_2_awesome
        )
    ),
    ELEPHANT(R.string.pet_elephant, R.string.pet_seal_description, R.drawable.pet_3, R.drawable.pet_3_head, R.drawable.pet_3_dead,
        mapOf(
            SAD to R.drawable.pet_3_sad,
            GOOD to R.drawable.pet_3_good,
            HAPPY to R.drawable.pet_3_happy,
            AWESOME to R.drawable.pet_3_awesome
        )
    ),
    BEAVER(R.string.pet_beaver, R.string.pet_seal_description, R.drawable.pet_4, R.drawable.pet_4_head, R.drawable.pet_4_dead,
        mapOf(
            SAD to R.drawable.pet_4_sad,
            GOOD to R.drawable.pet_4_good,
            HAPPY to R.drawable.pet_4_happy,
            AWESOME to R.drawable.pet_4_awesome
        )),
    CHICKEN(R.string.pet_chicken, R.string.pet_seal_description, R.drawable.pet_5, R.drawable.pet_5_head, R.drawable.pet_5_dead,
        mapOf(
            SAD to R.drawable.pet_5_sad,
            GOOD to R.drawable.pet_5_good,
            HAPPY to R.drawable.pet_5_happy,
            AWESOME to R.drawable.pet_5_awesome
        )),
    BEAR(R.string.pet_chicken, R.string.pet_seal_description, R.drawable.pet_6, R.drawable.pet_6_head, R.drawable.pet_6_dead,
        mapOf(
            SAD to R.drawable.pet_6_sad,
            GOOD to R.drawable.pet_6_good,
            HAPPY to R.drawable.pet_6_happy,
            AWESOME to R.drawable.pet_6_awesome
        )),
    LION(R.string.pet_chicken, R.string.pet_seal_description, R.drawable.pet_7, R.drawable.pet_7_head, R.drawable.pet_7_dead,
        mapOf(
            SAD to R.drawable.pet_7_sad,
            GOOD to R.drawable.pet_7_good,
            HAPPY to R.drawable.pet_7_happy,
            AWESOME to R.drawable.pet_7_awesome
        )),
    CAT(R.string.pet_chicken, R.string.pet_seal_description, R.drawable.pet_8, R.drawable.pet_8_head, R.drawable.pet_8_dead,
        mapOf(
            SAD to R.drawable.pet_8_sad,
            GOOD to R.drawable.pet_8_good,
            HAPPY to R.drawable.pet_8_happy,
            AWESOME to R.drawable.pet_8_awesome
        )),
    MONKEY(R.string.pet_chicken, R.string.pet_seal_description, R.drawable.pet_9, R.drawable.pet_9_head, R.drawable.pet_9_dead,
        mapOf(
            SAD to R.drawable.pet_9_sad,
            GOOD to R.drawable.pet_9_good,
            HAPPY to R.drawable.pet_9_happy,
            AWESOME to R.drawable.pet_9_awesome
        )),
    DUCK(R.string.pet_chicken, R.string.pet_seal_description, R.drawable.pet_10, R.drawable.pet_10_head, R.drawable.pet_10_dead,
        mapOf(
            SAD to R.drawable.pet_10_sad,
            GOOD to R.drawable.pet_10_good,
            HAPPY to R.drawable.pet_10_happy,
            AWESOME to R.drawable.pet_10_awesome
        )),
    PIG(R.string.pet_chicken, R.string.pet_seal_description, R.drawable.pet_11, R.drawable.pet_11_head, R.drawable.pet_11_dead,
        mapOf(
            SAD to R.drawable.pet_11_sad,
            GOOD to R.drawable.pet_11_good,
            HAPPY to R.drawable.pet_11_happy,
            AWESOME to R.drawable.pet_11_awesome
        )),
    ZEBRA(R.string.pet_chicken, R.string.pet_seal_description, R.drawable.pet_12, R.drawable.pet_12_head, R.drawable.pet_12_dead,
        mapOf(
            SAD to R.drawable.pet_12_sad,
            GOOD to R.drawable.pet_12_good,
            HAPPY to R.drawable.pet_12_happy,
            AWESOME to R.drawable.pet_12_awesome
        ))
}