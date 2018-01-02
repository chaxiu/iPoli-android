package mypoli.android.player.persistence

import com.couchbase.lite.*
import mypoli.android.challenge.data.Challenge
import mypoli.android.common.persistence.BaseCouchbaseRepository
import mypoli.android.common.persistence.Repository
import mypoli.android.pet.*
import mypoli.android.player.*
import mypoli.android.player.data.Avatar
import mypoli.android.player.persistence.model.*
import mypoli.android.quest.ColorPack
import mypoli.android.quest.IconPack
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 8/2/17.
 */
interface PlayerRepository : Repository<Player> {
    fun hasPlayer(): Boolean
    fun findSchemaVersion(): Int?
}

class CouchbasePlayerRepository(database: Database, coroutineContext: CoroutineContext) : BaseCouchbaseRepository<Player, CouchbasePlayer>(database, coroutineContext), PlayerRepository {

    override fun findSchemaVersion(): Int? {
        val resultSet = Query.select(SelectResult.expression(Expression.property("schemaVersion")))
            .from(DataSource.database(database))
            .where(Expression.property("type").equalTo(CouchbasePlayer.TYPE))
            .limit(1).execute()

        return resultSet.next()?.getInt("schemaVersion")
    }

    override fun hasPlayer(): Boolean {
        val resultSet = Query.select(SelectResult.expression(Meta.id))
            .from(DataSource.database(database))
            .where(Expression.property("type").equalTo(CouchbasePlayer.TYPE))
            .limit(1).execute()
        return resultSet.next() != null
    }


    override val modelType = CouchbasePlayer.TYPE

    override fun toEntityObject(dataMap: MutableMap<String, Any?>): Player {
        val cp = CouchbasePlayer(dataMap)

        val cap = CouchbaseAuthProvider(cp.authProvider)
        val authProvider = AuthProvider(
            id = cap.id,
            provider = cap.provider,
            firstName = cap.firstName,
            lastName = cap.lastName,
            email = cap.email,
            image = cap.image
        )
        val cPet = CouchbasePet(cp.pet)
        val pet = Pet(
            name = cPet.name,
            avatar = PetAvatar.valueOf(cPet.avatar),
            equipment = createPetEquipment(cPet),
            moodPoints = cPet.moodPoints,
            healthPoints = cPet.healthPoints,
            coinBonus = cPet.coinBonus,
            experienceBonus = cPet.experienceBonus,
            bountyBonus = cPet.itemDropChanceBonus
        )

        val ci = CouchbaseInventory(cp.inventory)
        val inventory = Inventory(
            food = ci.food.entries.associate { Food.valueOf(it.key) to it.value.toInt() },
            pets = ci.pets.map {
                val cip = CouchbaseInventoryPet(it)
                InventoryPet(
                    cip.name,
                    PetAvatar.valueOf(cip.avatar),
                    cip.items.map { PetItem.valueOf(it) }.toSet()
                )
            }.toSet(),
            themes = ci.themes.map { Theme.valueOf(it) }.toSet(),
            colorPacks = ci.colorPacks.map { ColorPack.valueOf(it) }.toSet(),
            iconPacks = ci.iconPacks.map { IconPack.valueOf(it) }.toSet(),
            challenges = ci.challenges.map { Challenge.valueOf(it) }.toSet()
        )

        return Player(
            id = cp.id,
            schemaVersion = cp.schemaVersion,
            level = cp.level,
            coins = cp.coins,
            gems = cp.gems,
            experience = cp.experience,
            authProvider = authProvider,
            avatar = Avatar.fromCode(cp.avatarCode)!!,
            currentTheme = Theme.valueOf(cp.currentTheme),
            inventory = inventory,
            createdAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(cp.createdAt), ZoneId.systemDefault()),
            pet = pet
        )
    }

    private fun createPetEquipment(couchbasePet: CouchbasePet): PetEquipment {
        val e = CouchbasePetEquipment(couchbasePet.equipment)
        val toPetItem: (String?) -> PetItem? = { it?.let { PetItem.valueOf(it) } }
        return PetEquipment(toPetItem(e.hat), toPetItem(e.mask), toPetItem(e.bodyArmor))
    }

    override fun toCouchbaseObject(entity: Player) =
        CouchbasePlayer().also {
            it.id = entity.id
            it.type = CouchbasePlayer.TYPE
            it.schemaVersion = entity.schemaVersion
            it.level = entity.level
            it.coins = entity.coins
            it.gems = entity.gems
            it.experience = entity.experience
            it.authProvider = createCouchbaseAuthProvider(entity.authProvider).map
            it.avatarCode = entity.avatar.code
            it.createdAt = entity.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            it.currentTheme = entity.currentTheme.name
            it.pet = createCouchbasePet(entity.pet).map
            it.inventory = createCouchbaseInventory(entity.inventory).map
        }

    private fun createCouchbasePet(pet: Pet) =
        CouchbasePet().also {
            it.name = pet.name
            it.avatar = pet.avatar.name
            it.equipment = createCouchbasePetEquipment(pet.equipment).map
            it.healthPoints = pet.healthPoints
            it.moodPoints = pet.moodPoints
            it.coinBonus = pet.coinBonus
            it.experienceBonus = pet.experienceBonus
            it.itemDropChanceBonus = pet.bountyBonus
        }

    private fun createCouchbasePetEquipment(equipment: PetEquipment) =
        CouchbasePetEquipment().also {
            it.hat = equipment.hat?.name
            it.mask = equipment.mask?.name
            it.bodyArmor = equipment.bodyArmor?.name
        }

    private fun createCouchbaseAuthProvider(authProvider: AuthProvider) =
        CouchbaseAuthProvider().also {
            it.id = authProvider.id
            it.email = authProvider.email
            it.firstName = authProvider.firstName
            it.lastName = authProvider.lastName
            it.username = authProvider.username
            it.image = authProvider.image
            it.provider = authProvider.provider
        }

    private fun createCouchbaseInventory(inventory: Inventory) =
        CouchbaseInventory().also {
            it.food = inventory.food.entries
                .associate { it.key.name to it.value.toLong() }
                .toMutableMap()
            it.pets = inventory.pets
                .map { createCouchbaseInventoryPet(it).map }
            it.themes = inventory.themes.map { it.name }
            it.colorPacks = inventory.colorPacks.map { it.name }
            it.iconPacks = inventory.iconPacks.map { it.name }
            it.challenges = inventory.challenges.map { it.name }
        }

    private fun createCouchbaseInventoryPet(inventoryPet: InventoryPet) =
        CouchbaseInventoryPet().also {
            it.name = inventoryPet.name
            it.avatar = inventoryPet.avatar.name
            it.items = inventoryPet.items.map { it.name }
        }
}