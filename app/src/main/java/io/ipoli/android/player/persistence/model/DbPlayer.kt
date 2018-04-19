package io.ipoli.android.player.persistence.model

import io.ipoli.android.common.persistence.FirestoreModel

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 12/1/17.
 */

data class DbPlayer(override val map: MutableMap<String, Any?> = mutableMapOf()) :
    FirestoreModel {
    override var id: String by map
    var username: String by map
    var displayName: String by map
    var schemaVersion: Int by map
    var level: Int by map
    var coins: Int by map
    var gems: Int by map
    var experience: Long by map
    var authProvider: MutableMap<String, Any?> by map
    var avatar: String by map
    var pet: MutableMap<String, Any?> by map
    var inventory: MutableMap<String, Any?> by map
    var membership: String by map
    var preferences: MutableMap<String, Any?> by map
    var achievements: List<MutableMap<String, Any?>> by map
    override var createdAt: Long by map
    override var updatedAt: Long by map
    override var removedAt: Long? by map
}

data class DbAuthProvider(val map: MutableMap<String, Any?> = mutableMapOf()) {
    var userId: String by map
    var provider: String by map
    var displayName: String by map
    var email: String by map
    var image: String by map
}

data class DbPet(val map: MutableMap<String, Any?> = mutableMapOf()) {
    var name: String by map
    var avatar: String by map
    var equipment: MutableMap<String, Any?> by map
    var moodPoints: Int by map
    var healthPoints: Int by map
    var experienceBonus: Float by map
    var coinBonus: Float by map
    var itemDropBonus: Float by map
}

data class DbPetEquipment(val map: MutableMap<String, Any?> = mutableMapOf()) {
    var hat: String? by map
    var mask: String? by map
    var bodyArmor: String? by map
}

data class DbInventory(val map: MutableMap<String, Any?> = mutableMapOf()) {
    var food: MutableMap<String, Long> by map
    var avatars: List<String> by map
    var pets: List<MutableMap<String, Any?>> by map
    var themes: List<String> by map
    var colorPacks: List<String> by map
    var iconPacks: List<String> by map
    var challenges: List<String> by map
    var powerUps: MutableMap<String, Long> by map
}

data class DbInventoryPet(val map: MutableMap<String, Any?> = mutableMapOf()) {
    var name: String by map
    var avatar: String by map
    var items: List<String> by map
}

data class DbPreferences(val map: MutableMap<String, Any?> = mutableMapOf()) {
    var theme: String by map
    var syncCalendars: List<MutableMap<String, Any?>> by map
    var productiveTimesOfDay: List<String> by map
    var workDays: List<String> by map
    var workStartTime: Int by map
    var workEndTime: Int by map
    var sleepStartTime: Int by map
    var sleepEndTime: Int by map
    var timeFormat: String by map
}

data class DbSyncCalendar(val map: MutableMap<String, Any?> = mutableMapOf()) {
    var id: String by map
    var name: String by map
}

data class DbUnlockedAchievement(val map: MutableMap<String, Any?> = mutableMapOf()) {
    var achievement: String by map
    var unlockTime: Int by map
    var unlockDate: Long by map
}