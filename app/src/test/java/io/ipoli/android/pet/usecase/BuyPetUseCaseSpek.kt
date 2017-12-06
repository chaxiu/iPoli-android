package io.ipoli.android.pet.usecase

import io.ipoli.android.TestUtil
import io.ipoli.android.pet.Pet
import io.ipoli.android.pet.PetAvatar
import io.ipoli.android.player.Inventory
import io.ipoli.android.player.InventoryPet
import io.ipoli.android.player.Player
import org.amshove.kluent.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

/**
 * Created by Venelin Valkov <venelin@ipoli.io>
 * on 12/6/17.
 */
class BuyPetUseCaseSpek : Spek({
    describe("BuyPetUseCase") {
        fun executeUseCase(player: Player, pet: PetAvatar): BuyPetUseCase.Result {
            return BuyPetUseCase(TestUtil.playerRepoMock(player)).execute(pet)
        }

        it("should require not bought pet") {
            val pet = Pet(
                "",
                avatar = PetAvatar.ELEPHANT
            )

            val player = TestUtil.player().copy(
                pet = pet,
                inventory = Inventory(pets = listOf(InventoryPet.fromPet(pet)))
            )
            val exec = { executeUseCase(player, pet.avatar) }
            exec shouldThrow IllegalArgumentException::class
        }

        it("should not buy when not enough coins") {
            val player = TestUtil.player().copy(
                coins = PetAvatar.ELEPHANT.price - 1,
                inventory = Inventory()
            )
            val result = executeUseCase(player, PetAvatar.ELEPHANT)
            result.`should be`(BuyPetUseCase.Result.TooExpensive)
        }

        it("should buy pet") {
            val player = TestUtil.player().copy(
                coins = PetAvatar.ELEPHANT.price,
                inventory = Inventory()
            )
            val result = executeUseCase(player, PetAvatar.ELEPHANT)
            result.`should be instance of`(BuyPetUseCase.Result.PetBought::class)
            val newPlayer = (result as BuyPetUseCase.Result.PetBought).player
            newPlayer.coins.`should be equal to`(player.coins - PetAvatar.ELEPHANT.price)
            newPlayer.hasPet(PetAvatar.ELEPHANT).`should be true`()
        }
    }
})