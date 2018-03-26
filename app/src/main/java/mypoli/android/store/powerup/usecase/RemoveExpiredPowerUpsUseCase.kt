package mypoli.android.store.powerup.usecase

import mypoli.android.common.UseCase
import mypoli.android.common.datetime.isAfterOrEqual
import mypoli.android.player.Player
import mypoli.android.player.persistence.PlayerRepository
import mypoli.android.store.powerup.usecase.RemoveExpiredPowerUpsUseCase.Params
import org.threeten.bp.LocalDate

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 03/19/2018.
 */
class RemoveExpiredPowerUpsUseCase(private val playerRepository: PlayerRepository) :
    UseCase<Params, Player> {

    data class Params(val currentDate: LocalDate)

    override fun execute(parameters: Params): Player {
        val p = playerRepository.find()
        require(p != null)

        val today = parameters.currentDate

        val nonExpiredPowerUps =
            p!!.inventory.powerUps
                .filter { it.expirationDate.isAfterOrEqual(today) }

        return playerRepository.save(
            p.copy(
                inventory = p.inventory.setPowerUps(nonExpiredPowerUps)
            )
        )
    }

}