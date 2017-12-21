package mypoli.android.theme.usecase

import mypoli.android.common.UseCase
import mypoli.android.player.Player
import mypoli.android.player.Theme
import mypoli.android.player.persistence.PlayerRepository
import mypoli.android.theme.usecase.BuyThemeUseCase.Result.ThemeBought
import mypoli.android.theme.usecase.BuyThemeUseCase.Result.TooExpensive

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 12/12/17.
 */
class BuyThemeUseCase(private val playerRepository: PlayerRepository) : UseCase<Theme, BuyThemeUseCase.Result> {
    override fun execute(parameters: Theme): Result {
        val theme = parameters
        val player = playerRepository.find()
        requireNotNull(player)
        require(!player!!.hasTheme(theme))

        if (player.diamonds < theme.price) {
            return TooExpensive
        }

        val newPlayer = player.copy(
            diamonds = player.diamonds - theme.price,
            inventory = player.inventory.addTheme(theme)
        )

        return ThemeBought(playerRepository.save(newPlayer))
    }

    sealed class Result {
        data class ThemeBought(val player: Player) : Result()
        object TooExpensive : Result()
    }
}