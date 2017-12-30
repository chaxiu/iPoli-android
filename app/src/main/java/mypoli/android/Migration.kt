package mypoli.android

import com.couchbase.lite.*
import mypoli.android.player.persistence.model.CouchbasePlayer

/**
 * Created by Polina Zhelyazkova <polina@ipoli.io>
 * on 12/22/17.
 */
class Migration(private val database: Database) {

    fun run() {
        val resultSet = Query.select(SelectResult.expression(Meta.id))
            .from(DataSource.database(database))
            .where(Expression.property("type").equalTo(CouchbasePlayer.TYPE))
            .limit(1).execute()

        val playerId = resultSet.next().getString("_id")
        var doc = database.getDocument(playerId).toMutable()

        if (!doc.contains("schemaVersion")) {
            doc.setInt("schemaVersion", 1)
            doc.setInt("gems", 0)
            database.save(doc)
            doc = database.getDocument(playerId).toMutable()
        }

        if (doc.getInt("schemaVersion") == 1) {
            val inventoryPets = doc.getDictionary("inventory").getArray("pets")
            inventoryPets.forEach {
                (it as MutableDictionary).setArray("items", MutableArray())
            }
            val pet = doc.getDictionary("pet")
            val equipment = MutableDictionary()
            equipment.setString("hat", null)
            equipment.setString("mask", null)
            equipment.setString("bodyArmor", null)
            pet.setDictionary("equipment", equipment)
            doc.setInt("schemaVersion", 2)
            database.save(doc)
        }

    }
}