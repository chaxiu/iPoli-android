package mypoli.android.pet

import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import mypoli.android.Constants
import mypoli.android.R
import mypoli.android.common.Reward
import mypoli.android.pet.PetMood.*
import java.lang.Math.abs

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 11/27/17.
 */
data class Pet(
    val name: String,
    val avatar: PetAvatar,
    val moodPoints: Int = Constants.DEFAULT_PET_HP,
    val healthPoints: Int = Constants.DEFAULT_PET_MP,
    val mood: PetMood = moodFor(moodPoints),
    val experienceBonus: Float = bonusFor(mood, MAX_XP_BONUS),
    val coinBonus: Float = bonusFor(mood, MAX_COIN_BONUS),
    val bountyBonus: Float = bonusFor(mood, MAX_BOUNTY_BONUS)
) {

    val isDead = healthPoints == 0

    fun updateHealthAndMoodPoints(healthPoints: Int, moodPoints: Int): Pet {

        require(!isDead)

        val newHealthPoints =
            if (healthPoints >= 0) addHealthPoints(healthPoints)
            else removeHealthPoints(abs(healthPoints))

        if (newHealthPoints == 0) {
            return copy(
                healthPoints = 0,
                moodPoints = 0,
                mood = SAD,
                coinBonus = 0f,
                experienceBonus = 0f,
                bountyBonus = 0f
            )
        }

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
            bountyBonus = bonusFor(newMood, MAX_BOUNTY_BONUS)
        )
    }

    fun setHealthAndMoodPoints(healthPoints: Int, moodPoints: Int): Pet {
        require(healthPoints >= 0)
        require(moodPoints >= 0)

        val newMood = moodFor(moodPoints)

        return copy(
            healthPoints = healthPoints,
            moodPoints = moodPoints,
            mood = newMood,
            coinBonus = bonusFor(newMood, MAX_COIN_BONUS),
            experienceBonus = bonusFor(newMood, MAX_XP_BONUS),
            bountyBonus = bonusFor(newMood, MAX_BOUNTY_BONUS)
        )
    }

    fun rewardFor(reward: Reward): Pet {
        val rewardHP = healthPointsForXP(reward.experience)
        val rewardMP = moodPointsForXP(reward.experience)
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
            bountyBonus = bonusFor(newMood, MAX_BOUNTY_BONUS)
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

    fun removeReward(reward: Reward): Pet {
        val rewardHP = healthPointsForXP(reward.experience)
        val rewardMP = moodPointsForXP(reward.experience)
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
            bountyBonus = bonusFor(newMood, MAX_BOUNTY_BONUS)
        )
    }

    private fun removeMoodPoints(oldHealthPoints: Int, newHealthPoints: Int, rewardMoodPoints: Int): Int {
        if (newHealthPoints == 0) {
            return 0
        }

        if (newHealthPoints < SICK_CUTOFF) {
            return Math.max(Math.min(moodPoints - rewardMoodPoints, GOOD_MIN_MOOD_POINTS - 1), 0)
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
        const val MAX_BOUNTY_BONUS = 16f

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

enum class PetItemType {
    HEAD, FACE, BODY
}

enum class PetItem(
    val experienceBonus: Int,
    val coinBonus: Int,
    val bountyBonus: Int,
    val type: PetItemType,
    val gemPrice: Int
) {
    GLASSES(4, 5, -9, PetItemType.FACE, 2),
    BEARD(-5, 5, 3, PetItemType.FACE, 2),
    MASK(10, -5, 2, PetItemType.FACE, 2),
    RED_HAT(1, 1, 1, PetItemType.HEAD, 4)
}

enum class AndroidPetItem(
    @StringRes val itemName: Int,
    @DrawableRes val image: Int) {

    GLASSES(R.string.pet_bear, R.drawable.pet_item_face_glasses),
    BEARD(R.string.pet_bear, R.drawable.pet_item_face_beard),
    MASK(R.string.pet_bear, R.drawable.pet_item_face_mask),
    RED_HAT(R.string.pet_bear, R.drawable.pet_item_head_had_red)
}

enum class PetAvatar(val gemPrice: Int, val feedingCategory: FeedingCategory) {
    SEAL(7, FeedingCategory.CARNIVOROUS),
    DONKEY(5, FeedingCategory.HERBIVOROUS),
    PIG(1, FeedingCategory.OMNIVOROUS),
    ELEPHANT(6, FeedingCategory.HERBIVOROUS),
    BEAVER(6, FeedingCategory.HERBIVOROUS),
    CHICKEN(6, FeedingCategory.OMNIVOROUS),
    BEAR(5, FeedingCategory.OMNIVOROUS),
    LION(8, FeedingCategory.CARNIVOROUS),
    CAT(4, FeedingCategory.CARNIVOROUS),
    MONKEY(5, FeedingCategory.OMNIVOROUS),
    DUCK(5, FeedingCategory.OMNIVOROUS),
    ZEBRA(8, FeedingCategory.HERBIVOROUS);

    enum class FeedingCategory { OMNIVOROUS, CARNIVOROUS, HERBIVOROUS }
}

enum class AndroidPetAvatar(
    @StringRes val petName: Int,
    @StringRes val description: Int,
    @DrawableRes val image: Int,
    @DrawableRes val headImage: Int,
    @DrawableRes val deadStateImage: Int,
    val moodImage: Map<PetMood, Int>) {

    SEAL(R.string.pet_seal, R.string.pet_seal_description, R.drawable.pet_1, R.drawable.pet_1_head, R.drawable.pet_1_dead,
        mapOf(
            SAD to R.drawable.pet_1_sad,
            GOOD to R.drawable.pet_1_good,
            HAPPY to R.drawable.pet_1_happy,
            AWESOME to R.drawable.pet_1_awesome
        )),
    PIG(R.string.pet_pig, R.string.pet_pig_description, R.drawable.pet_11, R.drawable.pet_11_head, R.drawable.pet_11_dead,
        mapOf(
            SAD to R.drawable.pet_11_sad,
            GOOD to R.drawable.pet_11_good,
            HAPPY to R.drawable.pet_11_happy,
            AWESOME to R.drawable.pet_11_awesome
        )),
    DONKEY(R.string.pet_donkey, R.string.pet_donkey_description, R.drawable.pet_2, R.drawable.pet_2_head, R.drawable.pet_2_dead,
        mapOf(
            SAD to R.drawable.pet_2_sad,
            GOOD to R.drawable.pet_2_good,
            HAPPY to R.drawable.pet_2_happy,
            AWESOME to R.drawable.pet_2_awesome
        )
    ),
    ELEPHANT(R.string.pet_elephant, R.string.pet_elephant_description, R.drawable.pet_3, R.drawable.pet_3_head, R.drawable.pet_3_dead,
        mapOf(
            SAD to R.drawable.pet_3_sad,
            GOOD to R.drawable.pet_3_good,
            HAPPY to R.drawable.pet_3_happy,
            AWESOME to R.drawable.pet_3_awesome
        )
    ),
    BEAVER(R.string.pet_beaver, R.string.pet_beaver_description, R.drawable.pet_4, R.drawable.pet_4_head, R.drawable.pet_4_dead,
        mapOf(
            SAD to R.drawable.pet_4_sad,
            GOOD to R.drawable.pet_4_good,
            HAPPY to R.drawable.pet_4_happy,
            AWESOME to R.drawable.pet_4_awesome
        )),
    CHICKEN(R.string.pet_chicken, R.string.pet_chicken_description, R.drawable.pet_5, R.drawable.pet_5_head, R.drawable.pet_5_dead,
        mapOf(
            SAD to R.drawable.pet_5_sad,
            GOOD to R.drawable.pet_5_good,
            HAPPY to R.drawable.pet_5_happy,
            AWESOME to R.drawable.pet_5_awesome
        )),
    BEAR(R.string.pet_bear, R.string.pet_bear_description, R.drawable.pet_6, R.drawable.pet_6_head, R.drawable.pet_6_dead,
        mapOf(
            SAD to R.drawable.pet_6_sad,
            GOOD to R.drawable.pet_6_good,
            HAPPY to R.drawable.pet_6_happy,
            AWESOME to R.drawable.pet_6_awesome
        )),
    LION(R.string.pet_lion, R.string.pet_lion_description, R.drawable.pet_7, R.drawable.pet_7_head, R.drawable.pet_7_dead,
        mapOf(
            SAD to R.drawable.pet_7_sad,
            GOOD to R.drawable.pet_7_good,
            HAPPY to R.drawable.pet_7_happy,
            AWESOME to R.drawable.pet_7_awesome
        )),
    CAT(R.string.pet_cat, R.string.pet_cat_description, R.drawable.pet_8, R.drawable.pet_8_head, R.drawable.pet_8_dead,
        mapOf(
            SAD to R.drawable.pet_8_sad,
            GOOD to R.drawable.pet_8_good,
            HAPPY to R.drawable.pet_8_happy,
            AWESOME to R.drawable.pet_8_awesome
        )),
    MONKEY(R.string.pet_monkey, R.string.pet_monkey_description, R.drawable.pet_9, R.drawable.pet_9_head, R.drawable.pet_9_dead,
        mapOf(
            SAD to R.drawable.pet_9_sad,
            GOOD to R.drawable.pet_9_good,
            HAPPY to R.drawable.pet_9_happy,
            AWESOME to R.drawable.pet_9_awesome
        )),
    DUCK(R.string.pet_duck, R.string.pet_duck_description, R.drawable.pet_10, R.drawable.pet_10_head, R.drawable.pet_10_dead,
        mapOf(
            SAD to R.drawable.pet_10_sad,
            GOOD to R.drawable.pet_10_good,
            HAPPY to R.drawable.pet_10_happy,
            AWESOME to R.drawable.pet_10_awesome
        )),
    ZEBRA(R.string.pet_zebra, R.string.pet_zebra_description, R.drawable.pet_12, R.drawable.pet_12_head, R.drawable.pet_12_dead,
        mapOf(
            SAD to R.drawable.pet_12_sad,
            GOOD to R.drawable.pet_12_good,
            HAPPY to R.drawable.pet_12_happy,
            AWESOME to R.drawable.pet_12_awesome
        ))
}