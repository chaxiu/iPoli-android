package io.ipoli.android.pet

import io.ipoli.android.common.mvi.Intent
import io.ipoli.android.common.mvi.ViewState

/**
 * Created by Venelin Valkov <venelin@ipoli.io>
 * on 11/24/17.
 */
sealed class PetIntent : Intent

object LoadDataIntent : PetIntent()
object ShowFoodList : PetIntent()
object HideFoodList : PetIntent()
data class Feed(val food: Food) : PetIntent()
data class ChangePetIntent(val pet: Pet) : PetIntent()

data class PetViewState(
    val type: StateType = StateType.DATA_LOADED,
    val stateName: String = "",
    val food: Food? = null,
    val wasFoodTasty: Boolean = false,
    val maxHP: Int = Pet.MAX_HP,
    val maxMP: Int = Pet.MAX_MP,
    val hp: Int = 0,
    val mp: Int = 0,
    val coinsBonus: Float = 0f,
    val xpBonus: Float = 0f,
    val unlockChanceBonus: Float = 0f,
    val avatar: PetAvatar? = null,
    val mood: PetMood? = null
) : ViewState {
    enum class StateType {
        LOADING, DATA_LOADED, FOOD_LIST_SHOWN, FOOD_LIST_HIDDEN, PET_FED,
        FOOD_TOO_EXPENSIVE, PET_CHANGED
    }
}